package br.com.poderfinanceiro.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        @JsonProperty("system_instruction") Content systemInstruction,
        List<Content> contents) {

    private static final Logger log = LoggerFactory.getLogger(GeminiRequest.class);

    // Construtor de compatibilidade para quem não usa systemInstruction
    public GeminiRequest(List<Content> contents) {
        this(null, contents);
    }

    // Construtor canônico (principal)
    public GeminiRequest(Content systemInstruction, List<Content> contents) {
        this.systemInstruction = systemInstruction;
        this.contents = contents;
        log.debug("[GEMINI_REQUEST] Criando request: systemInstruction={}, contentsSize={}",
                systemInstruction != null ? "presente" : "ausente",
                contents != null ? contents.size() : 0);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String role, List<Part> parts) {
        private static final Logger log = LoggerFactory.getLogger(Content.class);

        // Construtor legado (sem role) para evitar quebrar outros usos
        public Content(List<Part> parts) {
            this(null, parts);
        }

        // Construtor canônico
        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
            log.trace("[GEMINI_REQUEST_CONTENT] Criando content: role={}, partsSize={}", role,
                    parts != null ? parts.size() : 0);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        private static final Logger log = LoggerFactory.getLogger(Part.class);

        public static Part ofText(String text) {
            log.trace("[GEMINI_REQUEST_PART] Criando part de texto (length={})", text != null ? text.length() : 0);
            return new Part(text, null);
        }

        public static Part ofFile(String mimeType, String base64Data) {
            log.trace("[GEMINI_REQUEST_PART] Criando part de arquivo: mimeType={}, dataLength={}", mimeType,
                    base64Data != null ? base64Data.length() : 0);
            return new Part(null, new InlineData(mimeType, base64Data));
        }

        // Construtor canônico implícito já é suficiente, mas podemos adicionar log
        // opcional
        public Part {
            log.trace("[GEMINI_REQUEST_PART] Criando part: textPresent={}, inlineDataPresent={}", text != null,
                    inlineData != null);
        }
    }

    public record InlineData(String mimeType, String data) {
        private static final Logger log = LoggerFactory.getLogger(InlineData.class);

        public InlineData {
            log.trace("[GEMINI_REQUEST_INLINEDATA] Criando inlineData: mimeType={}, dataLength={}", mimeType,
                    data != null ? data.length() : 0);
        }
    }
}