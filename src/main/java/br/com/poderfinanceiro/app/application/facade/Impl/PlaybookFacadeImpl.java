package br.com.poderfinanceiro.app.application.facade.Impl;

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
            GeminiPromptFactory promptFactory, GeminiResponseHandler responseHandler) {
        this.playbookService = playbookService;
        this.authService = authService;
        this.geminiService = geminiService;
        this.promptFactory = promptFactory;
        this.responseHandler = responseHandler;
        this.objectMapper = new ObjectMapper();
    }

    @Override public List<PlaybookItemModel> listarTodosOsScripts() {
        return playbookService.listarTudoParaOPlaybook();
    }

    @Override public void salvarTodosOsScripts(List<PlaybookItemModel> scripts) {
        playbookService.salvarTodos(scripts);
    }

    @Override public List<PlaybookItemModel> filtrarScripts(String termo) {
        List<PlaybookItemModel> todos = listarTodosOsScripts();
        if (termo == null || termo.isBlank())
            return todos;
        String lower = termo.toLowerCase().trim();
        return todos.stream().filter(
                i -> i.getTitulo().toLowerCase().contains(lower) || i.getCategoria().toLowerCase().contains(lower))
                .toList();
    }

    @Override public String obterNomeConsultorLogado() {
        return authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor(a)";
    }

    @Override public List<String> listarModelosIADisponiveis() {
        return geminiService.listarModelosMultimodais(obterTokenOuLancar());
    }

    @Override public JsonNode estruturarTextoComIA(String textoBruto, String modeloEscolhido) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando estruturação cognitiva.", LOG_PREFIX);

        String token = obterTokenOuLancar();
        String prompt = promptFactory.getEstruturacaoScriptPrompt(textoBruto);
        String respostaBruta = geminiService.perguntarTexto(prompt, token, modeloEscolhido);

        // 1. Extração Agressiva via Scanner
        String jsonLimpo = responseHandler.extrairTextoDeJsonBruto(respostaBruta);

        log.info("{} [TELEMETRIA] JSON Saneado para Parse: '{}'", LOG_PREFIX, jsonLimpo);

        if (jsonLimpo.isEmpty()) {
            log.error("{} [SISTEMA] Falha ao localizar JSON na resposta: {}", LOG_PREFIX, respostaBruta);
            throw new RuntimeException("A IA falhou em estruturar o JSON. Tente novamente.");
        }

        return objectMapper.readTree(jsonLimpo);
    }

    private String obterTokenOuLancar() {
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank())
            throw new IllegalStateException("API Key não configurada.");
        return token;
    }

    public void estruturarTextoAsync(String texto, String modelo, Consumer<JsonNode> onSuccess,
            Consumer<Throwable> onError) {
        AsyncUtils.executarTaskAsync(() -> estruturarTextoComIA(texto, modelo), onSuccess, onError);
    }
}
