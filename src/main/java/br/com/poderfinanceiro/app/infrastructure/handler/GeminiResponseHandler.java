package br.com.poderfinanceiro.app.infrastructure.handler;

import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler especializado no tratamento de respostas da IA Gemini.
 * Implementa extração agressiva de JSON e conversão para DTOs do sistema.
 * Refatorado para Injeção de Dependência e conformidade com Java 25.
 */
@Component
public class GeminiResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(GeminiResponseHandler.class);
    private static final String LOG_PREFIX = "[GeminiResponseHandler]";

    private final ObjectMapper objectMapper;

    /**
     * Construtor Gold Standard: Utiliza o ObjectMapper configurado globalmente pelo
     * Spring.
     */
    public GeminiResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("{} [SISTEMA] Handler de resposta IA inicializado com ObjectMapper injetado.", LOG_PREFIX);
    }

    /**
     * Extrai o texto bruto da estrutura aninhada do GeminiResponse.
     */
    public String extrairTexto(GeminiResponse response) {
        log.trace("{} [TELEMETRIA] Extraindo texto bruto de GeminiResponse.", LOG_PREFIX);
        try {
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                return response.candidates().get(0).content().parts().get(0).text();
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao navegar na estrutura da resposta: {}", LOG_PREFIX, e.getMessage());
        }
        return "";
    }

    /**
     * Converte uma resposta da IA diretamente para um objeto DTO.
     */
    public <T> T converterParaObjeto(String raw, Class<T> clazz) {
        String json = extrairTextoDeJsonBruto(raw);
        if (json.isEmpty()) {
            log.warn("{} [NEGOCIO] Falha na conversão: Nenhum JSON localizado para a classe {}.", LOG_PREFIX,
                    clazz.getSimpleName());
            return null;
        }

        try {
            log.debug("{} [NEGOCIO] Convertendo JSON para objeto: {}", LOG_PREFIX, clazz.getSimpleName());
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro de desserialização para {}: {}", LOG_PREFIX, clazz.getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * Converte uma resposta da IA para uma lista de DTOs (Processamento em Lote).
     */
    public <T> List<T> converterParaLista(String raw, Class<T> clazz) {
        String json = extrairTextoDeJsonBruto(raw);
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            log.debug("{} [NEGOCIO] Convertendo JSON para lista de: {}", LOG_PREFIX, clazz.getSimpleName());
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao converter JSON para lista de {}: {}", LOG_PREFIX, clazz.getSimpleName(),
                    e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Scanner Agressivo: Localiza o bloco JSON (objeto ou lista) ignorando ruídos
     * de Markdown.
     */
    public String extrairTextoDeJsonBruto(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        log.trace("{} [TELEMETRIA] Iniciando scan agressivo de delimitadores JSON.", LOG_PREFIX);

        int firstBrace = raw.indexOf("{");
        int firstBracket = raw.indexOf("[");

        // Determina o ponto de início real (o que vier primeiro)
        int start = -1;
        if (firstBrace != -1 && firstBracket != -1)
            start = Math.min(firstBrace, firstBracket);
        else if (firstBrace != -1)
            start = firstBrace;
        else if (firstBracket != -1)
            start = firstBracket;

        if (start == -1) {
            log.debug("{} [NEGOCIO] Nenhum delimitador JSON localizado no texto fornecido.", LOG_PREFIX);
            return "";
        }

        int lastBrace = raw.lastIndexOf("}");
        int lastBracket = raw.lastIndexOf("]");
        int end = Math.max(lastBrace, lastBracket);

        if (end > start) {
            String extraido = raw.substring(start, end + 1);
            log.debug("{} [AUDITORIA] Bloco JSON extraído com sucesso ({} bytes).", LOG_PREFIX, extraido.length());
            return extraido;
        }

        log.debug("{} [NEGOCIO] Estrutura JSON incompleta ou malformada detectada.", LOG_PREFIX);
        return "";
    }
}
