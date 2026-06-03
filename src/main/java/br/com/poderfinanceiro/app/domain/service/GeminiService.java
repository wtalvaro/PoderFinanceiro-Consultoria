package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import br.com.poderfinanceiro.app.infrastructure.client.GeminiClient;
import br.com.poderfinanceiro.app.infrastructure.config.PlaybookResolver;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiRequestFactory;
import br.com.poderfinanceiro.app.infrastructure.handler.GeminiResponseHandler;
import br.com.poderfinanceiro.app.infrastructure.mapper.GeminiMediaMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <h1>GeminiService</h1>
 * <p>
 * Serviço Orquestrador de Inteligência Artificial para o ERP Poder Financeiro.
 * Implementa lógica de comunicação multimodal, OCR e análise cognitiva.
 * Otimizado para Java 25 (Project Loom) com cache thread-safe e logs rigorosos.
 * </p>
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final String LOG_PREFIX = "[GeminiService]";

    private final GeminiClient geminiClient;
    private final GeminiPromptFactory promptFactory;
    private final GeminiRequestFactory requestFactory;
    private final GeminiResponseHandler responseHandler;
    private final GeminiMediaMapper mediaMapper;
    private final PlaybookResolver playbookResolver;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String baseUrl;

    // Cache thread-safe para Virtual Threads (Project Loom)
    private final List<String> cacheModelos = new CopyOnWriteArrayList<>();
    private volatile boolean cacheCarregado = false;

    public GeminiService(GeminiClient geminiClient,
            GeminiPromptFactory promptFactory,
            GeminiRequestFactory requestFactory,
            GeminiResponseHandler responseHandler,
            GeminiMediaMapper mediaMapper,
            PlaybookResolver playbookResolver,
            ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.promptFactory = promptFactory;
        this.requestFactory = requestFactory;
        this.responseHandler = responseHandler;
        this.mediaMapper = mediaMapper;
        this.playbookResolver = playbookResolver;
        this.objectMapper = objectMapper;
        log.info("{} [SISTEMA] Orquestrador de IA inicializado com suporte a Virtual Threads e Jackson Injetado.",
                LOG_PREFIX);
    }

    /**
     * Realiza uma consulta de texto simples à IA.
     */
    public String perguntarTexto(String prompt, String apiKey, String modeloEscolhido) {
        log.info("{} [TELEMETRIA] Iniciando geração de texto simples. Modelo: {}", LOG_PREFIX, modeloEscolhido);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de consulta abortada: API Key não configurada.", LOG_PREFIX);
            return "⚠️ API Key não configurada.";
        }

        try {
            GeminiRequest payload = requestFactory.criarRequestSimples(prompt);
            String url = montarUrl(modeloEscolhido, apiKey);

            GeminiResponse response = geminiClient.post(url, payload, GeminiResponse.class, "TEXTO_SIMPLES");
            String resultado = responseHandler.extrairTexto(response);

            log.info("{} [AUDITORIA] Resposta de texto gerada com sucesso.", LOG_PREFIX);
            return resultado;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao processar consulta de texto: {}", LOG_PREFIX, e.getMessage());
            return "⚠️ Erro na consulta: " + e.getMessage();
        }
    }

    /**
     * Sobrecarga de compatibilidade para o Assistente Cognitivo.
     */
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, File arquivoAnexo,
            String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis, String jsonComissoes) {
        log.debug("{} [TELEMETRIA] Chamada de compatibilidade do assistente invocada.", LOG_PREFIX);
        return perguntarAoAssistente(perguntaUsuario, apiKeyDoConsultor, "gemini-1.5-flash", arquivoAnexo,
                jsonClienteAtivo, jsonTabelasJuros, jsonLinksUteis, jsonComissoes, List.of());
    }

    /**
     * Orquestra a consulta complexa ao Assistente Cognitivo (Analista de Crédito).
     */
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, String modeloEscolhido,
            File arquivoAnexo, String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis,
            String jsonComissoes, List<GeminiRequest.Content> historico) {

        log.info("{} [TELEMETRIA] Orquestrando consulta cognitiva multimodal. Modelo: {}", LOG_PREFIX, modeloEscolhido);

        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
            log.warn("{} [NEGOCIO] API Key ausente na orquestração do assistente.", LOG_PREFIX);
            return "⚠️ Chave de API ausente.";
        }

        try {
            // 1. Resolver Instrução de Sistema (Prompt)
            String playbook = playbookResolver.carregarPlaybook();
            String instrucao = promptFactory.getAnalistaCreditoPrompt(playbook, jsonClienteAtivo, jsonTabelasJuros,
                    jsonLinksUteis, jsonComissoes);

            // 2. Mapear Mídia se existir
            GeminiRequest.Part anexoPart = null;
            if (arquivoAnexo != null && arquivoAnexo.exists()) {
                log.debug("{} [SISTEMA] Anexo detectado: {}. Iniciando mapeamento de mídia.", LOG_PREFIX,
                        arquivoAnexo.getName());
                anexoPart = mediaMapper.toPart(arquivoAnexo);
            }

            // 3. Construir Payload via Factory
            GeminiRequest payload = requestFactory.criarRequestComSistema(instrucao, perguntaUsuario, anexoPart,
                    historico);

            // 4. Executar Chamada via Client
            String url = montarUrl(modeloEscolhido, apiKeyDoConsultor);
            GeminiResponse response = geminiClient.post(url, payload, GeminiResponse.class, "ASSISTENTE_COGNITIVO");

            // 5. Tratar Resposta via Handler
            String resultado = responseHandler.extrairTexto(response);

            log.info("{} [AUDITORIA] Resposta cognitiva orquestrada com sucesso.", LOG_PREFIX);
            return resultado;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica na orquestração do assistente: {}", LOG_PREFIX, e.getMessage());
            return "⚠️ Erro ao processar consulta: " + e.getMessage();
        }
    }

    /**
     * Orquestra a extração de tabelas financeiras via OCR.
     */
    public String extrairTabelasEmLote(File arquivoAnexo, String apiKey, String modeloEscolhido) {
        log.info("{} [TELEMETRIA] Iniciando processo de OCR em lote. Arquivo: {}", LOG_PREFIX, arquivoAnexo.getName());

        if (apiKey == null || apiKey.isBlank()) {
            log.error("{} [NEGOCIO] Falha no OCR: API Key não configurada.", LOG_PREFIX);
            throw new IllegalArgumentException("API Key não configurada.");
        }

        try {
            GeminiRequest.Part arquivoPart = mediaMapper.toPart(arquivoAnexo);
            String promptOcr = promptFactory.getOcrTabelasPrompt();

            GeminiRequest payload = requestFactory.criarRequestOcr(promptOcr, arquivoPart);

            String url = montarUrl(modeloEscolhido, apiKey);
            String rawJson = geminiClient.post(url, payload, String.class, "OCR_TABELAS");

            String resultado = responseHandler.extrairTextoDeJsonBruto(rawJson);

            log.info("{} [AUDITORIA] OCR processado e dados extraídos com sucesso.", LOG_PREFIX);
            return resultado;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro fatal no processo de OCR: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Erro ao analisar imagem: " + e.getMessage());
        }
    }

    /**
     * Lista os modelos multimodais disponíveis na API do Google.
     * Implementa cache thread-safe para otimização de performance.
     */
    public List<String> listarModelosMultimodais(String apiKey) {
        if (cacheCarregado && !cacheModelos.isEmpty()) {
            log.trace("{} [SISTEMA] Retornando lista de modelos via cache em memória.", LOG_PREFIX);
            return new ArrayList<>(cacheModelos);
        }

        log.info("{} [TELEMETRIA] Solicitando listagem de modelos multimodais à API externa.", LOG_PREFIX);
        List<String> fallback = List.of("gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("{} [NEGOCIO] API Key ausente para listagem. Utilizando modelos de fallback.", LOG_PREFIX);
            return fallback;
        }

        try {
            String url = baseUrl + "?key=" + apiKey;
            String respostaJson = geminiClient.get(url, "LISTAR_MODELOS");
            List<String> modelosEncontrados = new ArrayList<>();

            if (respostaJson != null) {
                JsonNode rootNode = objectMapper.readTree(respostaJson);
                if (rootNode.has("models")) {
                    for (JsonNode node : rootNode.get("models")) {
                        String cleanName = node.get("name").asText().replace("models/", "");
                        if (isModeloValido(cleanName)) {
                            modelosEncontrados.add(cleanName);
                        }
                    }
                }
            }

            modelosEncontrados.sort(Comparator.reverseOrder());

            if (!modelosEncontrados.isEmpty()) {
                cacheModelos.clear();
                cacheModelos.addAll(modelosEncontrados);
                cacheCarregado = true;
                log.info("{} [AUDITORIA] {} modelos carregados e sincronizados no cache.", LOG_PREFIX,
                        modelosEncontrados.size());
                return new ArrayList<>(cacheModelos);
            }

            return fallback;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao listar modelos: {}. Aplicando fallback.", LOG_PREFIX, e.getMessage());
            return fallback;
        }
    }

    /**
     * Invalida o cache de modelos (útil em caso de troca de API Key).
     */
    public void limparCache() {
        log.info("{} [SISTEMA] Solicitando invalidação do cache de modelos IA.", LOG_PREFIX);
        this.cacheModelos.clear();
        this.cacheCarregado = false;
    }

    /**
     * Valida se o modelo atende aos requisitos de versão e estabilidade do ERP.
     */
    private boolean isModeloValido(String name) {
        // Filtra modelos experimentais, previews ou versões legadas abaixo da 1.5
        if (!name.startsWith("gemini-") || name.contains("preview") || name.contains("exp")
                || name.contains("latest")) {
            return false;
        }

        try {
            String[] parts = name.split("-");
            if (parts.length > 1) {
                // Aceita apenas modelos da família 1.5 ou superior
                double versao = Double.parseDouble(parts[1]);
                return versao >= 1.5;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper para construção da URL da API com injeção de parâmetros.
     */
    private String montarUrl(String modelo, String apiKey) {
        return String.format("%s/%s:generateContent?key=%s", baseUrl, modelo, apiKey);
    }
}
