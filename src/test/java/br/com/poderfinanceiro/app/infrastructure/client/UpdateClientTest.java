package br.com.poderfinanceiro.app.infrastructure.client;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class UpdateClientTest {

    private UpdateClient updateClient;
    private RestClient restClient;
    private HttpClient httpClient;
    private final String MOCK_URL = "http://localhost:8080/api";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Mocking RestClient Fluent API
        restClient = mock(RestClient.class);
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.build()).thenReturn(restClient);

        httpClient = mock(HttpClient.class);

        updateClient = new UpdateClient(builder, httpClient, MOCK_URL);
    }

        @Test
    @DisplayName("Deve buscar a última release com sucesso via RestClient")
    @SuppressWarnings("rawtypes") // Removido "unchecked" por ser desnecessário neste contexto
    void deveBuscarUltimaRelease() {
        // GIVEN
        GitHubReleaseDTO mockDTO = new GitHubReleaseDTO("v1.0.1", "Release", "Body", "url", List.of());
        
        // Mocks das interfaces internas usando raw types
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        
        // Configuração da cadeia usando doReturn
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(MOCK_URL);
        doReturn(requestHeadersSpec).when(requestHeadersSpec).header(anyString(), anyString()); // Caso use headers futuramente
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(mockDTO).when(responseSpec).body(GitHubReleaseDTO.class);

        // WHEN
        GitHubReleaseDTO resultado = updateClient.buscarUltimaRelease();

        // THEN
        assertNotNull(resultado);
        assertEquals("v1.0.1", resultado.tagName());
        
        // Verificações
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(MOCK_URL);
    }


    @Test
    @DisplayName("Deve realizar o download do arquivo com sucesso via HttpClient")
    @SuppressWarnings("unchecked")
    void deveBaixarArquivoComSucesso() throws Exception {
        // GIVEN
        String downloadUrl = "http://cdn.github.com/app.jar";
        File destino = tempDir.resolve("update.jar").toFile();

        HttpResponse<Path> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(destino.toPath());

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // WHEN
        updateClient.baixarArquivo(downloadUrl, destino);

        // THEN
        verify(httpClient, times(1)).send(
                ArgumentMatchers.argThat(req -> req.uri().toString().equals(downloadUrl)),
                any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o download retornar erro HTTP 404")
    @SuppressWarnings("unchecked")
    void deveFalharNoDownloadComErroHttp() throws Exception {
        // GIVEN
        String downloadUrl = "http://cdn.github.com/erro.jar";
        File destino = tempDir.resolve("erro.jar").toFile();

        HttpResponse<Path> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // WHEN & THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> updateClient.baixarArquivo(downloadUrl, destino));

        assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    @DisplayName("Deve validar URL nula ou vazia no download")
    void deveValidarUrlInvalida() {
        File destino = new File("teste.jar");
        assertThrows(IllegalArgumentException.class, () -> updateClient.baixarArquivo(null, destino));
        assertThrows(IllegalArgumentException.class, () -> updateClient.baixarArquivo(" ", destino));
    }
}
