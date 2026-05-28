package br.com.poderfinanceiro.app.infrastructure.factory;

import br.com.poderfinanceiro.app.dto.GeminiRequest;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class GeminiRequestFactory {

    public GeminiRequest criarRequestSimples(String prompt) {
        return new GeminiRequest(List.of(new GeminiRequest.Content(List.of(GeminiRequest.Part.ofText(prompt)))));
    }

    public GeminiRequest criarRequestComSistema(String instrucao, String pergunta, GeminiRequest.Part anexo,
            List<GeminiRequest.Content> historico) {
        GeminiRequest.Content systemContent = new GeminiRequest.Content(List.of(GeminiRequest.Part.ofText(instrucao)));

        List<GeminiRequest.Part> userParts = (anexo != null) ? List.of(GeminiRequest.Part.ofText(pergunta), anexo)
                : List.of(GeminiRequest.Part.ofText(pergunta));

        List<GeminiRequest.Content> contents = new java.util.ArrayList<>(historico);
        contents.add(new GeminiRequest.Content("user", userParts));

        return new GeminiRequest(systemContent, contents);
    }

    public GeminiRequest criarRequestOcr(String promptOcr, GeminiRequest.Part arquivoPart) {
        return new GeminiRequest(
                List.of(new GeminiRequest.Content(List.of(GeminiRequest.Part.ofText(promptOcr), arquivoPart))));
    }
}
