package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.ICopilotoFacade;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.domain.service.SimulacaoCopilotoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service
public class CopilotoFacadeImpl implements ICopilotoFacade {

    private static final Logger log = LoggerFactory.getLogger(CopilotoFacadeImpl.class);
    private static final String LOG_PREFIX = "[CopilotoFacade]";

    private final SimulacaoCopilotoService copilotoService;
    private final ProponenteService proponenteService;
    private final GeminiService geminiService;
    private final AuthService authService;

    public CopilotoFacadeImpl(SimulacaoCopilotoService copilotoService, ProponenteService proponenteService, GeminiService geminiService,
            AuthService authService) {
        this.copilotoService = copilotoService;
        this.proponenteService = proponenteService;
        this.geminiService = geminiService;
        this.authService = authService;
        log.debug("{} [SISTEMA] Facade do Copiloto instanciada.", LOG_PREFIX);
    }

    @Override public List<ProponenteModel> listarClientesCarteira() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de clientes da carteira.", LOG_PREFIX);
        return proponenteService.listarMinhaCarteira();
    }

    @Override public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Solicitando modelos de IA disponíveis.", LOG_PREFIX);
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            log.warn("{} [NEGOCIO] Token ausente. Retornando lista vazia.", LOG_PREFIX);
            return List.of();
        }
        return geminiService.listarModelosMultimodais(token);
    }

    @Override public List<ResultadoSimulacaoDTO> processarSimulacaoRapida(SimulacaoRascunhoDTO rascunho) {
        log.info("{} [TELEMETRIA] ENTRADA FACADE -> SERVICE: DTO={}", LOG_PREFIX, rascunho);
        return copilotoService.processarSimulacaoRapida(rascunho);
    }

    @Override public String extrairMargemDocumento(File arquivo) {
        log.info("{} [TELEMETRIA] Extraindo margem do documento: {}", LOG_PREFIX, arquivo.getName());
        String respostaCompleta = copilotoService.extrairMargemDocumento(arquivo);

        if (respostaCompleta != null && respostaCompleta.contains("RESULTADO FINAL:")) {
            String[] partes = respostaCompleta.split("RESULTADO FINAL:");
            String margemLimpa = partes[1].trim().replaceAll("[^0-9,]", "");
            if (!margemLimpa.isEmpty() && !margemLimpa.equals("0") && !margemLimpa.equals("0,00")) {
                log.info("{} [NEGOCIO] Margem extraída com sucesso: {}", LOG_PREFIX, margemLimpa);
                return margemLimpa;
            }
        }
        log.warn("{} [NEGOCIO] Não foi possível extrair uma margem válida do documento.", LOG_PREFIX);
        return null;
    }

    @Override public ConselhoIADTO gerarConselhoEReordenarRanking(SimulacaoRascunhoDTO rascunho, List<ResultadoSimulacaoDTO> rankingAtual,
            String modeloEscolhido) {
        log.info("{} [TELEMETRIA] Solicitando conselho à IA. Modelo: {}", LOG_PREFIX, modeloEscolhido);
        String resposta = copilotoService.gerarRecomendacaoInteligenteIA(rascunho, rankingAtual, modeloEscolhido);

        List<Integer> recomendacoesIA = new ArrayList<>();
        List<ResultadoSimulacaoDTO> novoRanking = new ArrayList<>(rankingAtual);

        Pattern p = Pattern.compile("\\[TOP:\\s*([\\d,\\s]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(resposta);

        if (m.find()) {
            try {
                String[] numerosExtraidos = m.group(1).split(",");
                for (String numeroStr : numerosExtraidos) {
                    int indiceReal = Integer.parseInt(numeroStr.trim()) - 1;
                    if (indiceReal >= 0 && indiceReal < rankingAtual.size()) {
                        recomendacoesIA.add(indiceReal);
                    }
                }
                resposta = resposta.replace(m.group(0), "").trim();

                if (!recomendacoesIA.isEmpty()) {
                    // CORREÇÃO: Lambda explícita para evitar o warning de
                    // unboxing
                    List<ResultadoSimulacaoDTO> topChoices = recomendacoesIA.stream().filter(java.util.Objects::nonNull)
                            .map(index -> rankingAtual.get(index)).toList();

                    List<ResultadoSimulacaoDTO> remainingChoices = IntStream.range(0, rankingAtual.size())
                            .filter(i -> !recomendacoesIA.contains(i)).mapToObj(rankingAtual::get).toList();

                    novoRanking.clear();
                    novoRanking.addAll(topChoices);
                    novoRanking.addAll(remainingChoices);

                    recomendacoesIA.clear();
                    for (int i = 0; i < topChoices.size(); i++) {
                        recomendacoesIA.add(i);
                    }
                    log.info("{} [NEGOCIO] Ranking reordenado com base no conselho da IA.", LOG_PREFIX);
                }
            } catch (Exception ex) {
                log.error("{} [SISTEMA] Erro ao processar ranking da IA: {}", LOG_PREFIX, ex.getMessage());
            }
        } else {
            log.warn("{} [NEGOCIO] Padrão [TOP: ...] não encontrado na resposta da IA.", LOG_PREFIX);
        }

        return new ConselhoIADTO(resposta, novoRanking, recomendacoesIA);
    }

    @Override public int calcularIdade(LocalDate dataNascimento) {
        if (dataNascimento == null)
            return 0;
        int idade = Period.between(dataNascimento, LocalDate.now()).getYears();
        log.trace("{} [NEGOCIO] Idade calculada: {}", LOG_PREFIX, idade);
        return idade;
    }
}
