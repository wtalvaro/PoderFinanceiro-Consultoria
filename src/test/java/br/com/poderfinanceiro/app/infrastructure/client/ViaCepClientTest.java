package br.com.poderfinanceiro.app.infrastructure.client;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViaCepClientTest {

    private ViaCepClient viaCepClient;
    private RestClient restClient;
    private final String MOCK_URL = "http://localhost:8080/ws/";

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.build()).thenReturn(restClient);

        viaCepClient = new ViaCepClient(builder, MOCK_URL);
    }

    @Test
    @DisplayName("Deve retornar endereço com sucesso em cenário nominal")
    @SuppressWarnings("rawtypes")
    void deveRetornarEnderecoComSucesso() {
        // GIVEN
        String cep = "01001000";
        ViaCepResponse mockResponse = mock(ViaCepResponse.class);
        when(mockResponse.cep()).thenReturn(cep);
        when(mockResponse.localidade()).thenReturn("São Paulo");

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(mockResponse).when(responseSpec).body(ViaCepResponse.class);

        // WHEN
        ViaCepResponse resultado = viaCepClient.getEndereco(cep);

        // THEN
        assertNotNull(resultado);
        assertEquals("São Paulo", resultado.localidade());
        verify(restClient, times(1)).get();
    }

    @Test
    @DisplayName("Deve retentar e obter sucesso após erro 500 temporário")
    @SuppressWarnings("rawtypes")
    void deveRetentarEmCasoDeErro500() {
        // GIVEN
        String cep = "01001000";
        ViaCepResponse mockResponse = mock(ViaCepResponse.class);

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();

        RestClientResponseException ex500 = mock(RestClientResponseException.class);
        when(ex500.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        doThrow(ex500) // 1ª tentativa: Erro
                .doReturn(mockResponse) // 2ª tentativa: Sucesso
                .when(responseSpec).body(ViaCepResponse.class);

        // WHEN
        ViaCepResponse resultado = viaCepClient.getEndereco(cep);

        // THEN
        assertNotNull(resultado);
        verify(responseSpec, times(2)).body(ViaCepResponse.class);
    }

    @Test
    @DisplayName("Deve lançar exceção imediatamente para erro 404 (Sem retentativa)")
    @SuppressWarnings("rawtypes")
    void deveFalharSemRetentativaPara404() {
        // GIVEN
        String cep = "99999999";
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();

        RestClientResponseException ex404 = mock(RestClientResponseException.class);
        when(ex404.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        doThrow(ex404).when(responseSpec).body(ViaCepResponse.class);

        // WHEN & THEN
        assertThrows(RestClientResponseException.class, () -> viaCepClient.getEndereco(cep));
        verify(responseSpec, times(1)).body(ViaCepResponse.class);
    }

    @Test
    @DisplayName("Deve validar formato de CEP antes da chamada")
    void deveValidarCepInvalido() {
        assertThrows(IllegalArgumentException.class, () -> viaCepClient.getEndereco("123"));
        assertThrows(IllegalArgumentException.class, () -> viaCepClient.getEndereco("ABC12345"));
        assertThrows(IllegalArgumentException.class, () -> viaCepClient.getEndereco(null));
    }
}
