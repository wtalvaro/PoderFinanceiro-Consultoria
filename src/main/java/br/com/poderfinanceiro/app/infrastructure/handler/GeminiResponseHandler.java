package br.com.poderfinanceiro.app.infrastructure.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.poderfinanceiro.app.application.dto.GeminiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(GeminiResponseHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extrairTexto(GeminiResponse response) {
        try {
            return response.candidates().get(0).content().parts().get(0).text();
        } catch (Exception e) {
            log.error("[GeminiResponseHandler] Falha ao extrair texto da resposta estruturada.");
            return "🤖 Sem resposta inteligível.";
        }
    }

    public String extrairTextoDeJsonBruto(String jsonBruto) {
        try {
            JsonNode node = objectMapper.readTree(jsonBruto);
            return node.at("/candidates/0/content/parts/0/text").asText().replaceAll("```json", "")
                    .replaceAll("```", "").trim();
        } catch (Exception e) {
            log.error("[GeminiResponseHandler] Falha ao realizar parse do JSON bruto.");
            return "[]";
        }
    }
}
