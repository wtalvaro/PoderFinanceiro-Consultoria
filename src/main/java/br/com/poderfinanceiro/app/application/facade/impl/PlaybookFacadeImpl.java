package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.IPlaybookFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.PlaybookService;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;
import br.com.poderfinanceiro.app.infrastructure.handler.GeminiResponseHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Implementação da Facade de Playbook.
 * Orquestra a gestão de scripts de vendas e a estruturação cognitiva via IA.
 */
@Service
public class PlaybookFacadeImpl implements IPlaybookFacade {

    private static final Logger log = LoggerFactory.getLogger(PlaybookFacadeImpl.class);
    private static final String LOG_PREFIX = "[PlaybookFacade]";

    private final PlaybookService playbookService;
    private final AuthService authService;
    private final GeminiService geminiService;
    private final GeminiPromptFactory promptFactory;
    private final GeminiResponseHandler responseHandler;
    private final ObjectMapper objectMapper;

    public PlaybookFacadeImpl(PlaybookService playbookService, AuthService authService, GeminiService geminiService,
            GeminiPromptFactory promptFactory, GeminiResponseHandler responseHandler, ObjectMapper objectMapper) {
        this.playbookService = playbookService;
        this.authService = authService;
        this.geminiService = geminiService;
        this.promptFactory = promptFactory;
        this.responseHandler = responseHandler;
        this.objectMapper = objectMapper;
        log.info("{} [SISTEMA] Facade de Playbook inicializada com suporte a IA Cognitiva.", LOG_PREFIX);
    }

    @Override
    public List<PlaybookItemModel> listarTodosOsScripts() {
        log.trace("{} [TELEMETRIA] Solicitando listagem completa de scripts do Playbook.", LOG_PREFIX);
        return playbookService.listarTudoParaOPlaybook();
    }

    @Override
    public void salvarTodosOsScripts(List<PlaybookItemModel> scripts) {
        log.info("{} [AUDITORIA] Iniciando persistência em lote de {} scripts.", LOG_PREFIX, scripts.size());
        playbookService.salvarTodos(scripts);
        log.info("{} [AUDITORIA] Scripts persistidos com sucesso.", LOG_PREFIX);
    }

    @Override
    public List<PlaybookItemModel> filtrarScripts(String termo) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca no Playbook: '{}'", LOG_PREFIX, termo);
        List<PlaybookItemModel> todos = listarTodosOsScripts();
        if (termo == null || termo.isBlank())
            return todos;

        String lower = termo.toLowerCase().trim();
        return todos.stream().filter(
                i -> i.getTitulo().toLowerCase().contains(lower) || i.getCategoria().toLowerCase().contains(lower))
                .toList();
    }

    @Override
    public String obterNomeConsultorLogado() {
        log.trace("{} [TELEMETRIA] Recuperando nome do consultor para personalização de scripts.", LOG_PREFIX);
        return authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor(a)";
    }

    @Override
    public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Consultando modelos multimodais disponíveis.", LOG_PREFIX);
        return geminiService.listarModelosMultimodais(obterTokenOuLancar());
    }

    @Override
    public JsonNode estruturarTextoComIA(String textoBruto, String modeloEscolhido) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando estruturação cognitiva de texto bruto.", LOG_PREFIX);

        String token = obterTokenOuLancar();
        String prompt = promptFactory.getEstruturacaoScriptPrompt(textoBruto);

        log.debug("{} [NEGOCIO] Invocando GeminiService para estruturação.", LOG_PREFIX);
        String respostaBruta = geminiService.perguntarTexto(prompt, token, modeloEscolhido);

        // Extração Agressiva via Scanner (Resiliência de IA)
        String jsonLimpo = responseHandler.extrairTextoDeJsonBruto(respostaBruta);

        if (jsonLimpo.isEmpty()) {
            log.error("{} [SISTEMA] Falha crítica: Nenhum JSON localizado na resposta da IA.", LOG_PREFIX);
            throw new RuntimeException("A IA falhou em estruturar o JSON. Tente novamente.");
        }

        log.info("{} [AUDITORIA] Texto estruturado com sucesso via IA.", LOG_PREFIX);
        return objectMapper.readTree(jsonLimpo);
    }

    private String obterTokenOuLancar() {
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            log.warn("{} [NEGOCIO] Operação de IA abortada: API Key não configurada.", LOG_PREFIX);
            throw new IllegalStateException("API Key não configurada.");
        }
        return token;
    }

    /**
     * Orquestração assíncrona para não bloquear a UI Thread do JavaFX.
     */
    public void estruturarTextoAsync(String texto, String modelo, Consumer<JsonNode> onSuccess,
            Consumer<Throwable> onError) {
        log.info("{} [TELEMETRIA] Disparando tarefa assíncrona de estruturação cognitiva.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(() -> estruturarTextoComIA(texto, modelo), onSuccess, onError);
    }
}
