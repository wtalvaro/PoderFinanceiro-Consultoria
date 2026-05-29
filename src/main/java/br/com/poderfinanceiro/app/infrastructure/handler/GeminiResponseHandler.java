package br.com.poderfinanceiro.app.infrastructure.handler;

import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler especializado no tratamento de respostas da IA. Implementa extração
 * agressiva de JSON para mitigar falhas de formatação do modelo.
 */
@Component
public class GeminiResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(GeminiResponseHandler.class);
    private static final String LOG_PREFIX = "[GeminiResponseHandler]";

    /**
     * Extrai o texto da resposta estruturada do Gemini.
     */
    public String extrairTexto(GeminiResponse response) {
        try {
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                return response.candidates().get(0).content().parts().get(0).text();
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao navegar no objeto GeminiResponse.", LOG_PREFIX);
        }
        return "";
    }

    /**
     * Scanner Agressivo: Localiza o primeiro '{' e o último '}' ignorando
     * qualquer texto ou marcação de markdown (crases) ao redor.
     */
    public String extrairTextoDeJsonBruto(String raw) {
        if (raw == null || raw.isBlank())
            return "";
        log.trace("{} [SISTEMA] Iniciando scan de JSON em string de tamanho: {}", LOG_PREFIX, raw.length());
        try {
            int firstBracket = raw.indexOf("{");
            int lastBracket = raw.lastIndexOf("}");
            if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                String jsonExtraido = raw.substring(firstBracket, lastBracket + 1);
                log.debug("{} [SISTEMA] JSON localizado e extraído com sucesso.", LOG_PREFIX);
                return jsonExtraido;
            }

            log.warn("{} [NEGOCIO] Nenhum delimitador JSON { } localizado na resposta bruta.", LOG_PREFIX);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro crítico durante o scan de JSON: {}", LOG_PREFIX, e.getMessage());
        }

        return "";
    }
}
