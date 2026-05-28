package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.PlaybookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaybookFacadeImpl implements IPlaybookFacade {

    private static final Logger log = LoggerFactory.getLogger(PlaybookFacadeImpl.class);
    private static final String LOG_PREFIX = "[PlaybookFacade]";

    private static final String PROMPT_ENGENHARIA_VENDAS = """
            Você é um Diretor Comercial e Estrategista de Vendas especializado em correspondentes bancários.
            REGRAS ABSOLUTAS E INQUEBRÁVEIS:
            1. "conteudo": ESTE É O CAMPO PRINCIPAL. Extraia a mensagem de vendas na íntegra.
            2. "dica": INVENTE UMA DICA CURTA. É ESTRITAMENTE PROIBIDO colocar o texto da mensagem de vendas aqui.
            Retorne APENAS o objeto JSON puro e válido. Não adicione crases de markdown (```json).
            Texto bruto real recebido do grupo para processar agora:
            """;

    private final PlaybookService playbookService;
    private final AuthService authService;
    private final GeminiService geminiService;

    public PlaybookFacadeImpl(PlaybookService playbookService, AuthService authService, GeminiService geminiService) {
        this.playbookService = playbookService;
        this.authService = authService;
        this.geminiService = geminiService;
        log.debug("{} [SISTEMA] Facade do Playbook instanciada.", LOG_PREFIX);
    }

    @Override public List<PlaybookItemModel> listarTodosOsScripts() {
        log.trace("{} [TELEMETRIA] Solicitando listagem completa do playbook.", LOG_PREFIX);
        return playbookService.listarTudoParaOPlaybook();
    }

    @Override public void salvarTodosOsScripts(List<PlaybookItemModel> scripts) {
        log.info("{} [AUDITORIA] Solicitando salvamento em lote do playbook. Total de itens: {}", LOG_PREFIX, scripts.size());
        playbookService.salvarTodos(scripts);
    }

    @Override public List<PlaybookItemModel> filtrarScripts(String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca: '{}'", LOG_PREFIX, termoBusca);
        List<PlaybookItemModel> todos = listarTodosOsScripts();

        if (termoBusca == null || termoBusca.isBlank()) {
            return todos;
        }

        String termoLower = termoBusca.toLowerCase().trim();
        return todos.stream()
                .filter(item -> item.getTitulo().toLowerCase().contains(termoLower)
                        || item.getCategoria().toLowerCase().contains(termoLower) || item.getConteudo().toLowerCase().contains(termoLower))
                .toList();
    }

    @Override public String obterNomeConsultorLogado() {
        return authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor(a)";
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

    @Override public JsonNode estruturarTextoComIA(String textoBruto, String modeloEscolhido) throws Exception {
        log.info("{} [TELEMETRIA] Solicitando estruturação de texto à IA. Modelo: {}", LOG_PREFIX, modeloEscolhido);

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("API Key do Gemini não configurada.");
        }

        String promptCompleto = PROMPT_ENGENHARIA_VENDAS + "--- inicio do conteudo --- \n" + textoBruto + "\n --- final do conteudo ---";
        String respostaBruta = geminiService.perguntarTexto(promptCompleto, token, modeloEscolhido);

        return formatarRespostaIAParaJson(respostaBruta);
    }

    private JsonNode formatarRespostaIAParaJson(String respostaBruta) throws Exception {
        log.trace("{} [SISTEMA] Realizando parse da resposta bruta da IA.", LOG_PREFIX);
        if (respostaBruta == null || respostaBruta.isBlank()) {
            throw new Exception("A inteligência artificial não retornou dados.");
        }

        int startIndex = respostaBruta.indexOf('{');
        int endIndex = respostaBruta.lastIndexOf('}');

        if (startIndex >= 0 && endIndex >= startIndex) {
            String jsonPuro = respostaBruta.substring(startIndex, endIndex + 1);
            return new ObjectMapper().readTree(jsonPuro.trim());
        }
        throw new Exception("Padrão JSON estruturado não localizado na resposta da IA.");
    }
}
