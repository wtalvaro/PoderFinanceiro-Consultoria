package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.facade.IPlaybookFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.PlaybookService;
import br.com.poderfinanceiro.app.infrastructure.handler.GeminiResponseHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Implementação da Facade do Playbook. Atua como orquestrador de alto nível,
 * integrando serviços de domínio, segurança e inteligência artificial com
 * suporte a processamento assíncrono (Loom).
 */
@Service
public class PlaybookFacadeImpl implements IPlaybookFacade {

    private static final Logger log = LoggerFactory.getLogger(PlaybookFacadeImpl.class);
    private static final String LOG_PREFIX = "[PlaybookFacade]";

    private final PlaybookService playbookService;
    private final AuthService authService;
    private final GeminiService geminiService;
    private final GeminiResponseHandler responseHandler;
    private final ObjectMapper objectMapper;

    public PlaybookFacadeImpl(PlaybookService playbookService, AuthService authService, GeminiService geminiService,
            GeminiResponseHandler responseHandler) {
        this.playbookService = playbookService;
        this.authService = authService;
        this.geminiService = geminiService;
        this.responseHandler = responseHandler;
        this.objectMapper = new ObjectMapper();
        log.info("{} [SISTEMA] Facade inicializada com suporte a Virtual Threads.", LOG_PREFIX);
    }

    @Override public List<PlaybookItemModel> listarTodosOsScripts() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de scripts ao serviço de domínio.", LOG_PREFIX);
        return playbookService.listarTudoParaOPlaybook();
    }

    @Override public void salvarTodosOsScripts(List<PlaybookItemModel> scripts) {
        log.info("{} [AUDITORIA] Iniciando persistência em lote do Playbook. Itens: {}", LOG_PREFIX, scripts.size());
        playbookService.salvarTodos(scripts);
    }

    @Override public List<PlaybookItemModel> filtrarScripts(String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando lógica de filtragem para o termo: '{}'", LOG_PREFIX, termoBusca);
        List<PlaybookItemModel> todos = listarTodosOsScripts();

        if (termoBusca == null || termoBusca.isBlank()) {
            return todos;
        }

        String termoLower = termoBusca.toLowerCase().trim();
        return todos.stream()
                .filter(item -> item.getTitulo().toLowerCase().contains(termoLower)
                        || item.getCategoria().toLowerCase().contains(termoLower)
                        || item.getConteudo().toLowerCase().contains(termoLower))
                .toList();
    }

    @Override public String obterNomeConsultorLogado() {
        return authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor(a)";
    }

    @Override public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Recuperando modelos multimodais ativos.", LOG_PREFIX);
        String token = obterTokenOuLancar();
        return geminiService.listarModelosMultimodais(token);
    }

    /**
     * Estrutura um texto bruto em JSON utilizando IA. Este método é otimizado
     * para ser chamado via AsyncUtils para não bloquear a UI.
     */
    @Override public JsonNode estruturarTextoComIA(String textoBruto, String modeloEscolhido) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando estruturação cognitiva de texto bruto.", LOG_PREFIX);

        String token = obterTokenOuLancar();

        // Prompt de Engenharia de Vendas (Poderia ser movido para
        // GeminiPromptFactory para 100% de desacoplamento)
        String promptCompleto = String.format("""
                Você é um Diretor Comercial e Estrategista de Vendas especializado em correspondentes bancários.
                REGRAS ABSOLUTAS E INQUEBRÁVEIS:
                1. "conteudo": ESTE É O CAMPO PRINCIPAL. Extraia a mensagem de vendas na íntegra.
                2. "dica": INVENTE UMA DICA CURTA. É ESTRITAMENTE PROIBIDO colocar o texto da mensagem de vendas aqui.
                Retorne APENAS o objeto JSON puro e válido.
                --- inicio do conteudo ---
                %s
                --- final do conteudo ---
                """, textoBruto);

        // Chamada ao serviço de IA (Operação bloqueante de rede)
        String respostaBruta = geminiService.perguntarTexto(promptCompleto, token, modeloEscolhido);

        // Parsing seguro utilizando o Handler especializado
        String jsonLimpo = responseHandler.extrairTextoDeJsonBruto(respostaBruta);

        log.info("{} [AUDITORIA] Texto estruturado com sucesso via IA.", LOG_PREFIX);
        return objectMapper.readTree(jsonLimpo);
    }

    /**
     * Helper para garantir a presença da API Key antes de operações de IA.
     */
    private String obterTokenOuLancar() {
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            log.warn("{} [NEGOCIO] Operação de IA abortada: API Key não configurada.", LOG_PREFIX);
            throw new IllegalStateException("API Key do Gemini não configurada no perfil do consultor.");
        }
        return token;
    }

    /**
     * Exemplo de integração com AsyncUtils para chamadas a partir do
     * Controller. Este método demonstra como a Facade orquestra a execução em
     * Virtual Threads.
     */
    public void estruturarTextoAsync(String texto, String modelo, Consumer<JsonNode> onSuccess,
            Consumer<Throwable> onError) {
        log.debug("{} [SISTEMA] Orquestrando tarefa assíncrona via AsyncUtils (Project Loom).", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(() -> estruturarTextoComIA(texto, modelo), onSuccess, onError);
    }
}
