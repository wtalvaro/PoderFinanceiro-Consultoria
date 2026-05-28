package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

/**
 * Serviço de Copiloto para Simulações de Crédito. Orquestra a inteligência
 * artificial e cálculos matemáticos para apoiar a decisão do consultor.
 */
@Service
public class SimulacaoCopilotoService {

        private static final Logger log = LoggerFactory.getLogger(SimulacaoCopilotoService.class);
        private static final String LOG_PREFIX = "[SimulacaoCopilotoService]";

        private final TabelaJurosRepository tabelaJurosRepository;
        private final GeminiService geminiService;
        private final AuthService authService;
        private final GeminiPromptFactory promptFactory;

        public SimulacaoCopilotoService(TabelaJurosRepository tabelaJurosRepository, GeminiService geminiService,
                        AuthService authService, GeminiPromptFactory promptFactory) {
                this.tabelaJurosRepository = tabelaJurosRepository;
                this.geminiService = geminiService;
                this.authService = authService;
                this.promptFactory = promptFactory;
                log.info("{} [SISTEMA] Serviço de Copiloto de Simulação inicializado.", LOG_PREFIX);
        }

        /**
         * Processa uma simulação rápida baseada em rascunho, buscando tabelas
         * elegíveis e calculando projeções.
         */
        @Transactional(readOnly = true) public List<ResultadoSimulacaoDTO> processarSimulacaoRapida(
                        SimulacaoRascunhoDTO rascunho) {
                log.info("{} [TELEMETRIA] Iniciando processamento de simulação rápida.", LOG_PREFIX);

                if (rascunho == null) {
                        log.warn("{} [NEGOCIO] Rascunho nulo recebido para simulação.", LOG_PREFIX);
                        return List.of();
                }

                try {
                        TipoConvenioModel convenioEnum = TipoConvenioModel.valueOf(rascunho.tipoConvenio());

                        List<TabelaJurosModel> tabelasValidas = tabelaJurosRepository.findTabelasElegiveis(convenioEnum,
                                        rascunho.idade(), rascunho.valorDesejado(), rascunho.prazoDesejado());

                        log.debug("{} [NEGOCIO] {} tabelas elegíveis localizadas para o perfil.", LOG_PREFIX,
                                        tabelasValidas.size());

                        List<ResultadoSimulacaoDTO> resultados = tabelasValidas.stream().map(tabela -> {
                                BigDecimal parcela = calcularParcelaEstimada(rascunho.valorDesejado(),
                                                tabela.getTaxaMensal(), rascunho.prazoDesejado());
                                BigDecimal comissao = calcularComissao(rascunho.valorDesejado(),
                                                tabela.getComissaoPercentual());
                                return new ResultadoSimulacaoDTO(tabela, comissao, parcela);
                        }).sorted(Comparator.comparing(ResultadoSimulacaoDTO::comissaoEstimada).reversed())
                                        .collect(Collectors.toList());

                        log.info("{} [AUDITORIA] Simulação concluída com {} resultados gerados.", LOG_PREFIX,
                                        resultados.size());
                        return resultados;

                } catch (IllegalArgumentException e) {
                        log.error("{} [NEGOCIO] Tipo de convênio inválido: {}", LOG_PREFIX, rascunho.tipoConvenio());
                        return List.of();
                }
        }

        /**
         * Utiliza IA para extrair a margem consignável de um documento
         * (PDF/Imagem).
         */
        public String extrairMargemDocumento(File arquivo) {
                log.info("{} [TELEMETRIA] Solicitando extração de margem via IA para o arquivo: {}", LOG_PREFIX,
                                arquivo != null ? arquivo.getName() : "NULL");

                if (arquivo == null || !arquivo.exists()) {
                        log.error("{} [NEGOCIO] Falha na extração: Arquivo inválido ou inexistente.", LOG_PREFIX);
                        return "Erro: Arquivo não fornecido ou não encontrado.";
                }

                String apiKey = authService.getUsuarioLogado().getGeminiApiKey();
                String prompt = promptFactory.getMargemDocumentoPrompt();

                log.debug("{} [SISTEMA] Enviando prompt de análise de margem para o GeminiService.", LOG_PREFIX);

                String resposta = geminiService.perguntarAoAssistente(prompt, apiKey, "gemini-2.5-flash", arquivo, "{}",
                                "[]", "[]", "[]", List.of());

                log.info("{} [AUDITORIA] Análise de margem concluída pela IA.", LOG_PREFIX);
                return resposta;
        }

        /**
         * Gera uma recomendação estratégica baseada no ranking de simulação.
         */
        public String gerarRecomendacaoInteligenteIA(SimulacaoRascunhoDTO perfil, List<ResultadoSimulacaoDTO> ranking,
                        String modeloEscolhido) {
                log.info("{} [TELEMETRIA] Gerando recomendação estratégica via IA. Modelo: {}", LOG_PREFIX,
                                modeloEscolhido);

                if (ranking == null || ranking.isEmpty()) {
                        log.warn("{} [NEGOCIO] Recomendação abortada: Ranking de simulação vazio.", LOG_PREFIX);
                        return "Nenhuma tabela encontrada para este perfil.";
                }

                String apiKey = authService.getUsuarioLogado().getGeminiApiKey();
                String prompt = promptFactory.getRecomendacaoEstrategicaPrompt(perfil, ranking);

                log.debug("{} [SISTEMA] Solicitando recomendação estratégica ao GeminiService.", LOG_PREFIX);
                String resposta = geminiService.perguntarTexto(prompt, apiKey, modeloEscolhido);

                log.info("{} [AUDITORIA] Recomendação estratégica gerada com sucesso.", LOG_PREFIX);
                return resposta;
        }

        // --- Métodos de Cálculo Financeiro (Privados) ---

        private BigDecimal calcularComissao(BigDecimal valorOperacao, BigDecimal percentualComissao) {
                if (valorOperacao == null || percentualComissao == null)
                        return BigDecimal.ZERO;

                return valorOperacao.multiply(percentualComissao).divide(new BigDecimal("100"), 2,
                                RoundingMode.HALF_UP);
        }

        private BigDecimal calcularParcelaEstimada(BigDecimal valor, BigDecimal taxa, Integer prazo) {
                if (valor == null || taxa == null || prazo == null || prazo == 0)
                        return BigDecimal.ZERO;

                // Cálculo linear simplificado para estimativa rápida de
                // copiloto
                BigDecimal taxaDecimal = taxa.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                return valor.multiply(taxaDecimal).setScale(2, RoundingMode.HALF_UP);
        }
}
