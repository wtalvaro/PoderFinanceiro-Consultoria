package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.GeminiRequest;
import br.com.poderfinanceiro.app.dto.GeminiResponse;
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

/**
 * Serviço Orquestrador de Inteligência Artificial para o ERP Poder Financeiro.
 * Implementa Arquitetura Limpa através da delegação de responsabilidades para
 * componentes especializados.
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

    @Value("${gemini.api.url}") private String baseUrl;

    public GeminiService(GeminiClient geminiClient, GeminiPromptFactory promptFactory,
            GeminiRequestFactory requestFactory, GeminiResponseHandler responseHandler, GeminiMediaMapper mediaMapper,
            PlaybookResolver playbookResolver) {
        this.geminiClient = geminiClient;
        this.promptFactory = promptFactory;
        this.requestFactory = requestFactory;
        this.responseHandler = responseHandler;
        this.mediaMapper = mediaMapper;
        this.playbookResolver = playbookResolver;
        this.objectMapper = new ObjectMapper();
        log.info("{} [SISTEMA] Orquestrador de IA instanciado com sucesso.", LOG_PREFIX);
    }

    /**
     * Realiza uma consulta de texto simples à IA.
     */
    public String perguntarTexto(String prompt, String apiKey, String modeloEscolhido) {
        log.info("{} [TELEMETRIA] Iniciando geração de texto simples. Modelo: {}", LOG_PREFIX, modeloEscolhido);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de perguntarTexto sem API Key.", LOG_PREFIX);
            return "⚠️ API Key não configurada.";
        }

        try {
            GeminiRequest payload = requestFactory.criarRequestSimples(prompt);
            String url = montarUrl(modeloEscolhido, apiKey);

            GeminiResponse response = geminiClient.post(url, payload, GeminiResponse.class, "TEXTO_SIMPLES");
            String resultado = responseHandler.extrairTexto(response);

            log.info("{} [AUDITORIA] Texto simples gerado com sucesso.", LOG_PREFIX);
            return resultado;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao processar perguntarTexto: {}", LOG_PREFIX, e.getMessage());
            return "⚠️ Erro na consulta: " + e.getMessage();
        }
    }

    /**
     * Sobrecarga de compatibilidade para o Assistente Cognitivo.
     */
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, File arquivoAnexo,
            String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis, String jsonComissoes) {
        log.debug("{} [TELEMETRIA] Chamada de compatibilidade do assistente iniciada.", LOG_PREFIX);
        return perguntarAoAssistente(perguntaUsuario, apiKeyDoConsultor, "gemini-2.5-flash", arquivoAnexo,
                jsonClienteAtivo, jsonTabelasJuros, jsonLinksUteis, jsonComissoes, List.of());
    }

    /**
     * Orquestra a consulta complexa ao Assistente Cognitivo (Analista de
     * Crédito).
     */
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, String modeloEscolhido,
            File arquivoAnexo, String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis,
            String jsonComissoes, List<GeminiRequest.Content> historico) {

        log.info("{} [TELEMETRIA] Orquestrando consulta cognitiva. Modelo: {}", LOG_PREFIX, modeloEscolhido);

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

            log.info("{} [AUDITORIA] Resposta do assistente orquestrada com sucesso.", LOG_PREFIX);
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
        log.info("{} [TELEMETRIA] Iniciando orquestração de OCR em lote. Arquivo: {}", LOG_PREFIX,
                arquivoAnexo.getName());

        if (apiKey == null || apiKey.isBlank()) {
            log.error("{} [NEGOCIO] Falha no OCR: API Key ausente.", LOG_PREFIX);
            throw new IllegalArgumentException("API Key não configurada.");
        }

        try {
            // 1. Preparar Mídia e Prompt
            GeminiRequest.Part arquivoPart = mediaMapper.toPart(arquivoAnexo);
            String promptOcr = promptFactory.getOcrTabelasPrompt();

            // 2. Criar Payload
            GeminiRequest payload = requestFactory.criarRequestOcr(promptOcr, arquivoPart);

            // 3. Executar Chamada (Retorno String para parse manual de JSON
            // bruto)
            String url = montarUrl(modeloEscolhido, apiKey);
            String rawJson = geminiClient.post(url, payload, String.class, "OCR_TABELAS");

            // 4. Extrair e Limpar JSON via Handler
            String resultado = responseHandler.extrairTextoDeJsonBruto(rawJson);

            log.info("{} [AUDITORIA] OCR processado e extraído com sucesso.", LOG_PREFIX);
            return resultado;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro fatal no processo de OCR: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Erro ao analisar imagem: " + e.getMessage());
        }
    }

    /**
     * Lista os modelos multimodais disponíveis na API do Google.
     */
    public List<String> listarModelosMultimodais(String apiKey) {
        log.info("{} [TELEMETRIA] Solicitando listagem de modelos multimodais.", LOG_PREFIX);
        List<String> fallback = List.of("gemini-3.5-flash", "gemini-3.1-pro", "gemini-2.5-pro", "gemini-2.5-flash");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("{} [NEGOCIO] API Key ausente para listagem. Usando fallback.", LOG_PREFIX);
            return fallback;
        }

        try {
            String url = baseUrl + "?key=" + apiKey;
            String respostaJson = geminiClient.get(url, "LISTAR_MODELOS");
            List<String> modelos = new ArrayList<>();

            if (respostaJson != null) {
                JsonNode rootNode = objectMapper.readTree(respostaJson);
                if (rootNode.has("models")) {
                    for (JsonNode node : rootNode.get("models")) {
                        String cleanName = node.get("name").asText().replace("models/", "");
                        if (isModeloValido(cleanName)) {
                            modelos.add(cleanName);
                        }
                    }
                }
            }

            modelos.sort(Comparator.reverseOrder());
            log.info("{} [AUDITORIA] {} modelos carregados com sucesso.", LOG_PREFIX, modelos.size());
            return modelos.isEmpty() ? fallback : modelos;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao listar modelos: {}. Usando fallback.", LOG_PREFIX, e.getMessage());
            return fallback;
        }
    }

    /**
     * Valida se o modelo atende aos requisitos de versão e estabilidade.
     */
    private boolean isModeloValido(String name) {
        if (!name.startsWith("gemini-") || name.contains("preview") || name.contains("exp")
                || name.contains("latest")) {
            return false;
        }
        try {
            String[] parts = name.split("-");
            if (parts.length > 1) {
                double versao = Double.parseDouble(parts[1]);
                return versao >= 2.5;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper para construção da URL da API.
     */
    private String montarUrl(String modelo, String apiKey) {
        return String.format("%s/%s:generateContent?key=%s", baseUrl, modelo, apiKey);
    }
}
