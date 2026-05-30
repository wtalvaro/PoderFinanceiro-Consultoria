package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.PlaybookService;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;
import br.com.poderfinanceiro.app.infrastructure.handler.GeminiResponseHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para PlaybookFacadeImpl.
 * Valida a gestão de scripts e a integração com o motor de IA.
 */
class PlaybookFacadeTest {

    private PlaybookFacadeImpl facade;

    private PlaybookService playbookService;
    private AuthService authService;
    private GeminiService geminiService;
    private GeminiPromptFactory promptFactory;
    private GeminiResponseHandler responseHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        playbookService = mock(PlaybookService.class);
        authService = mock(AuthService.class);
        geminiService = mock(GeminiService.class);
        promptFactory = mock(GeminiPromptFactory.class);
        responseHandler = mock(GeminiResponseHandler.class);
        objectMapper = new ObjectMapper(); // Usamos uma instância real para validar o parse de JsonNode

        facade = new PlaybookFacadeImpl(
                playbookService, authService, geminiService, promptFactory, responseHandler, objectMapper);
    }

    @Test
    @DisplayName("Deve filtrar scripts por título ou categoria corretamente")
    void deveFiltrarScripts() {
        // GIVEN
        PlaybookItemModel i1 = new PlaybookItemModel();
        i1.setTitulo("Script INSS");
        i1.setCategoria("Vendas");
        PlaybookItemModel i2 = new PlaybookItemModel();
        i2.setTitulo("Abordagem");
        i2.setCategoria("Retenção");

        when(playbookService.listarTudoParaOPlaybook()).thenReturn(List.of(i1, i2));

        // WHEN
        List<PlaybookItemModel> resultado = facade.filtrarScripts("inss");

        // THEN
        assertEquals(1, resultado.size());
        assertEquals("Script INSS", resultado.get(0).getTitulo());
    }

    @Test
    @DisplayName("Deve estruturar texto bruto em JSON usando IA")
    void deveEstruturarTextoComIA() throws Exception {
        // GIVEN
        String textoBruto = "Script de teste";
        String token = "key-123";
        String modelo = "gemini-pro";
        String jsonResposta = "{\"titulo\": \"Teste\", \"conteudo\": \"Corpo\"}";

        UsuarioModel usuario = new UsuarioModel();
        usuario.setGeminiApiKey(token);
        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        when(promptFactory.getEstruturacaoScriptPrompt(anyString())).thenReturn("Prompt");
        when(geminiService.perguntarTexto(anyString(), eq(token), eq(modelo))).thenReturn("Resposta Bruta");
        when(responseHandler.extrairTextoDeJsonBruto(anyString())).thenReturn(jsonResposta);

        // WHEN
        JsonNode resultado = facade.estruturarTextoComIA(textoBruto, modelo);

        // THEN
        assertNotNull(resultado);
        assertEquals("Teste", resultado.get("titulo").asText());
        verify(geminiService).perguntarTexto(anyString(), eq(token), eq(modelo));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar usar IA sem API Key")
    void deveFalharSemApiKey() {
        // GIVEN
        when(authService.estaLogado()).thenReturn(false);

        // WHEN & THEN
        assertThrows(IllegalStateException.class, () -> facade.estruturarTextoComIA("texto", "modelo"));
    }

    @Test
    @DisplayName("Deve delegar o salvamento de scripts para o serviço")
    void deveSalvarScripts() {
        // GIVEN
        List<PlaybookItemModel> lista = List.of(new PlaybookItemModel());

        // WHEN
        facade.salvarTodosOsScripts(lista);

        // THEN
        verify(playbookService, times(1)).salvarTodos(lista);
    }
}
