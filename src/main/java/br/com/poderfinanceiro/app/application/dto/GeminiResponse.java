package br.com.poderfinanceiro.app.application.dto;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GeminiResponse(List<Candidate> candidates) {

    private static final Logger log = LoggerFactory.getLogger(GeminiResponse.class);

    public GeminiResponse {
        log.debug("[GEMINI_RESPONSE] Criando resposta: candidatesSize={}", candidates != null ? candidates.size() : 0);
    }

    public record Candidate(Content content) {
        private static final Logger log = LoggerFactory.getLogger(Candidate.class);

        public Candidate {
            log.trace("[GEMINI_RESPONSE_CANDIDATE] Criando candidato: contentPresent={}", content != null);
        }
    }

    public record Content(List<Part> parts) {
        private static final Logger log = LoggerFactory.getLogger(Content.class);

        public Content {
            log.trace("[GEMINI_RESPONSE_CONTENT] Criando content: partsSize={}", parts != null ? parts.size() : 0);
        }
    }

    public record Part(String text) {
        private static final Logger log = LoggerFactory.getLogger(Part.class);

        public Part {
            log.trace("[GEMINI_RESPONSE_PART] Criando part: textLength={}", text != null ? text.length() : 0);
        }
    }
}