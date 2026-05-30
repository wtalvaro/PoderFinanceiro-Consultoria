package br.com.poderfinanceiro.app.infrastructure.handler;

import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler especializado no tratamento de respostas da IA.
 * Implementa extração agressiva de JSON e conversão para DTOs.
 */
@Component
public class GeminiResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(GeminiResponseHandler.class);
    private static final String LOG_PREFIX = "[GeminiResponseHandler]";

    private final ObjectMapper objectMapper;

    public GeminiResponseHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        log.info("{} [SISTEMA] Handler de resposta IA inicializado com Jackson.", LOG_PREFIX);
    }

    /**
     * Extrai o texto bruto da estrutura aninhada do GeminiResponse.
     */
    public String extrairTexto(GeminiResponse response) {
        try {
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                return response.candidates().get(0).content().parts().get(0).text();
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao navegar no objeto GeminiResponse: {}", LOG_PREFIX, e.getMessage());
        }
        return "";
    }

    /**
     * Converte uma resposta da IA diretamente para um objeto DTO.
     */
    public <T> T converterParaObjeto(String raw, Class<T> clazz) {
        String json = extrairTextoDeJsonBruto(raw);
        if (json.isEmpty())
            return null;

        try {
            log.debug("{} [NEGOCIO] Convertendo JSON para objeto: {}", LOG_PREFIX, clazz.getSimpleName());
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao converter JSON para {}: {}", LOG_PREFIX, clazz.getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * Converte uma resposta da IA para uma lista de DTOs (Processamento em Lote).
     */
    public <T> List<T> converterParaLista(String raw, Class<T> clazz) {
        String json = extrairTextoDeJsonBruto(raw);
        if (json.isEmpty())
            return new ArrayList<>();

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
     * Scanner Agressivo: Localiza o bloco JSON (objeto ou lista) ignorando ruídos.
     * Restaurado nome original para compatibilidade com PlaybookFacadeImpl.
     */
    public String extrairTextoDeJsonBruto(String raw) {
        if (raw == null || raw.isBlank())
            return "";

        log.trace("{} [SISTEMA] Iniciando scan agressivo em string de tamanho: {}", LOG_PREFIX, raw.length());

        int firstBrace = raw.indexOf("{");
        int firstBracket = raw.indexOf("[");

        int start;
        if (firstBrace != -1 && (firstBracket == -1 || (firstBrace < firstBracket))) {
            start = firstBrace;
        } else {
            start = firstBracket;
        }

        int lastBrace = raw.lastIndexOf("}");
        int lastBracket = raw.lastIndexOf("]");
        int end = Math.max(lastBrace, lastBracket);

        if (start != -1 && end != -1 && end > start) {
            String extraido = raw.substring(start, end + 1);
            log.debug("{} [SISTEMA] JSON localizado e extraído com sucesso.", LOG_PREFIX);
            return extraido;
        }

        log.warn("{} [NEGOCIO] Nenhum delimitador JSON { } ou [ ] localizado.", LOG_PREFIX);
        return "";
    }
}
