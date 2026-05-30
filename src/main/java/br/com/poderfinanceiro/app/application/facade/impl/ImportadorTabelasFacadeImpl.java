package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.application.facade.IImportadorTabelasFacade;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

/**
 * Implementação da Facade do Importador de Tabelas.
 * Orquestra a extração OCR via IA e a persistência em lote.
 * Refatorada para Injeção de Dependência (DI) e Logs Rigorosos.
 */
@Service
public class ImportadorTabelasFacadeImpl implements IImportadorTabelasFacade {

    private static final Logger log = LoggerFactory.getLogger(ImportadorTabelasFacadeImpl.class);
    private static final String LOG_PREFIX = "[ImportadorTabelasFacade]";

    private final GeminiService geminiService;
    private final AuthService authService;
    private final TabelaJurosService tabelaJurosService;
    private final BancoRepository bancoRepository;
    private final ObjectMapper objectMapper; // Injetado pelo Spring

    public ImportadorTabelasFacadeImpl(
            GeminiService geminiService,
            AuthService authService,
            TabelaJurosService tabelaJurosService,
            BancoRepository bancoRepository,
            ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.tabelaJurosService = tabelaJurosService;
        this.bancoRepository = bancoRepository;
        this.objectMapper = objectMapper;
        log.info("{} [SISTEMA] Facade do Importador inicializada com ObjectMapper centralizado.", LOG_PREFIX);
    }

    @Override
    public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Solicitando modelos de IA disponíveis para importação.", LOG_PREFIX);
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;

        if (token == null || token.isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de listar modelos sem API Key configurada.", LOG_PREFIX);
            return List.of();
        }

        List<String> modelos = geminiService.listarModelosMultimodais(token);
        log.debug("{} [TELEMETRIA] Modelos localizados: {}", LOG_PREFIX, modelos.size());
        return modelos;
    }

    @Override
    public List<BancoModel> listarBancosAtivos() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de bancos ativos para vínculo de tabelas.", LOG_PREFIX);
        return bancoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Override
    public List<TabelaImportadaDTO> extrairTabelasDeImagem(File arquivo, String modeloEscolhido) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando extração OCR via IA. Arquivo: {}, Modelo: {}", LOG_PREFIX,
                arquivo.getName(), modeloEscolhido);

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            log.error("{} [NEGOCIO] Operação abortada: API Key do Gemini não localizada.", LOG_PREFIX);
            throw new IllegalStateException("API Key do Gemini não configurada.");
        }

        // Chamada ao serviço de IA
        String jsonResposta = geminiService.extrairTabelasEmLote(arquivo, token, modeloEscolhido);

        log.trace("{} [SISTEMA] Realizando parse do JSON retornado pela IA via ObjectMapper injetado.", LOG_PREFIX);

        // Uso do ObjectMapper injetado (Thread-safe e configurado)
        List<TabelaImportadaDTO> tabelasExtraidas = objectMapper.readValue(jsonResposta, new TypeReference<>() {
        });

        log.info("{} [AUDITORIA] Extração concluída com sucesso. {} tabelas identificadas.", LOG_PREFIX,
                tabelasExtraidas.size());
        return tabelasExtraidas;
    }

    @Override
    @Transactional
    public void salvarLoteTabelas(List<TabelaImportadaDTO> lote) {
        log.info("{} [TELEMETRIA] Iniciando orquestração de salvamento em lote. Quantidade: {}", LOG_PREFIX,
                lote.size());

        try {
            tabelaJurosService.salvarLoteTabelasImportadas(lote);
            log.info("{} [AUDITORIA] Lote de {} tabelas persistido com sucesso no banco de dados.", LOG_PREFIX,
                    lote.size());
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica ao salvar lote de tabelas: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }
}
