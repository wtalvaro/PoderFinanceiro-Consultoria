package br.com.poderfinanceiro.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record GeminiRequest(List<Content> contents) {

    public record Content(List<Part> parts) {
    }

    // A anotação NON_NULL garante que se não houver ficheiro, ele não envia a chave
    // "inlineData" vazia
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {

        // Método auxiliar para enviar apenas texto
        public static Part ofText(String text) {
            return new Part(text, null);
        }

        // Método auxiliar para enviar o ficheiro (Holerite/PDF/Imagem)
        public static Part ofFile(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(String mimeType, String data) {
    }
}