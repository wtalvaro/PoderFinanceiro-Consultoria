package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import br.com.poderfinanceiro.app.infrastructure.client.GeminiClient;
import br.com.poderfinanceiro.app.infrastructure.config.PlaybookResolver;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiRequestFactory;
import br.com.poderfinanceiro.app.infrastructure.handler.GeminiResponseHandler;
import br.com.poderfinanceiro.app.infrastructure.mapper.GeminiMediaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h1>GeminiServiceTest</h1>
 * <p>
 * Testes de Unidade para o Orquestrador de IA.
 * Valida a integração entre Factories, Mappers, Handlers e o Cliente de Rede.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @InjectMocks
    private GeminiService service;

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private GeminiPromptFactory promptFactory;
    @Mock
    private GeminiRequestFactory requestFactory;
    @Mock
    private GeminiResponseHandler responseHandler;
    @Mock
    private GeminiMediaMapper mediaMapper;
    @Mock
    private PlaybookResolver playbookResolver;

    @TempDir
    Path tempDir;

    private final String API_KEY = "AIza_test_key";
    private final String MODELO = "gemini-2.5-flash";

    @BeforeEach
    void setUp() {
        // Injeta a URL base que viria do application.properties
        ReflectionTestUtils.setField(service, "baseUrl", "https://generativelanguage.googleapis.com/v1beta/models");
    }

    @Test
    @DisplayName("Deve gerar texto simples orquestrando Factory, Client e Handler")
    void devePerguntarTextoComSucesso() {
        String prompt = "Olá IA";
        GeminiRequest mockRequest = mock(GeminiRequest.class);
        GeminiResponse mockResponse = mock(GeminiResponse.class);

        when(requestFactory.criarRequestSimples(prompt)).thenReturn(mockRequest);
        when(geminiClient.post(anyString(), eq(mockRequest), eq(GeminiResponse.class), anyString()))
                .thenReturn(mockResponse);
        when(responseHandler.extrairTexto(mockResponse)).thenReturn("Resposta da IA");

        String resultado = service.perguntarTexto(prompt, API_KEY, MODELO);

        assertThat(resultado).isEqualTo("Resposta da IA");
        verify(geminiClient).post(contains(MODELO), eq(mockRequest), any(), eq("TEXTO_SIMPLES"));
    }

    @Test
    @DisplayName("Deve retornar aviso quando a API Key não for fornecida")
    void deveAvisarFaltaDeApiKey() {
        String resultado = service.perguntarTexto("Prompt", null, MODELO);

        assertThat(resultado).contains("API Key não configurada");
        verifyNoInteractions(geminiClient, requestFactory);
    }

    @Test
    @DisplayName("Deve orquestrar consulta complexa ao assistente com anexo e histórico")
    void devePerguntarAoAssistenteComSucesso() throws Exception {
        // 1. Preparar arquivo fake
        File arquivoFisico = tempDir.resolve("documento.pdf").toFile();
        arquivoFisico.createNewFile();

        // 2. Mockar a cadeia de dependências
        when(playbookResolver.carregarPlaybook()).thenReturn("{}");
        when(promptFactory.getAnalistaCreditoPrompt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Prompt Sistema");

        GeminiRequest.Part mockPart = mock(GeminiRequest.Part.class);
        when(mediaMapper.toPart(arquivoFisico)).thenReturn(mockPart);

        GeminiRequest mockRequest = mock(GeminiRequest.class);
        when(requestFactory.criarRequestComSistema(anyString(), anyString(), eq(mockPart), anyList()))
                .thenReturn(mockRequest);

        GeminiResponse mockResponse = mock(GeminiResponse.class);
        when(geminiClient.post(anyString(), eq(mockRequest), eq(GeminiResponse.class), anyString()))
                .thenReturn(mockResponse);

        when(responseHandler.extrairTexto(mockResponse)).thenReturn("Análise do Assistente");

        // 3. Executar
        String resultado = service.perguntarAoAssistente("Pergunta", API_KEY, MODELO, arquivoFisico, "{}", "[]", "[]",
                "[]", List.of());

        // 4. Validar
        assertThat(resultado).isEqualTo("Análise do Assistente");
        verify(mediaMapper).toPart(arquivoFisico);
        verify(promptFactory).getAnalistaCreditoPrompt(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve orquestrar extração de tabelas via OCR")
    void deveExtrairTabelasComSucesso() throws Exception {
        File imagem = tempDir.resolve("tabela.png").toFile();
        imagem.createNewFile();

        GeminiRequest.Part mockPart = mock(GeminiRequest.Part.class);
        when(mediaMapper.toPart(imagem)).thenReturn(mockPart);
        when(promptFactory.getOcrTabelasPrompt()).thenReturn("Prompt OCR");

        GeminiRequest mockRequest = mock(GeminiRequest.class);
        when(requestFactory.criarRequestOcr(anyString(), eq(mockPart))).thenReturn(mockRequest);

        when(geminiClient.post(anyString(), eq(mockRequest), eq(String.class), anyString()))
                .thenReturn("{ \"json\": \"bruto\" }");

        when(responseHandler.extrairTextoDeJsonBruto(anyString())).thenReturn("[{\"banco\": \"ITAU\"}]");

        String resultado = service.extrairTabelasEmLote(imagem, API_KEY, MODELO);

        assertThat(resultado).contains("ITAU");
        verify(responseHandler).extrairTextoDeJsonBruto(anyString());
    }

    @Test
    @DisplayName("Deve retornar fallback de modelos quando a chave de API for nula")
    void deveRetornarFallbackDeModelos() {
        List<String> modelos = service.listarModelosMultimodais(null);

        assertThat(modelos).contains("gemini-2.5-flash");
        verifyNoInteractions(geminiClient);
    }
}
