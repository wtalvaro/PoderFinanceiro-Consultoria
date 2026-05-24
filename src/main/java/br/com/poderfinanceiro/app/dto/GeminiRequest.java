package br.com.poderfinanceiro.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        @JsonProperty("system_instruction") Content systemInstruction,
        List<Content> contents) {
    // Construtor de compatibilidade para quem não usa systemInstruction
    public GeminiRequest(List<Content> contents) {
        this(null, contents);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String role, List<Part> parts) {
        // Construtor legado (sem role) para evitar quebrar outros usos
        public Content(List<Part> parts) {
            this(null, parts);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        public static Part ofText(String text) {
            return new Part(text, null);
        }

        public static Part ofFile(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(String mimeType, String data) {
    }
}