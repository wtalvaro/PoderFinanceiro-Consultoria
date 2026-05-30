package br.com.poderfinanceiro.app.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("[Infra] Teste de Unidade - GeminiClient (Resiliência)")
class GeminiClientTest {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientTest.class);
    private static final String LOG_PREFIX = "[GeminiClientTest]";

    private GeminiClient geminiClient;
    private MockRestServiceServer server;
    private final String URL_MOCK = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    @BeforeEach
    void setUp() {
        log.debug("{} [SISTEMA] Configurando ambiente de teste manual para RestClient.", LOG_PREFIX);

        // 1. Criamos um Builder do RestClient manualmente
        RestClient.Builder builder = RestClient.builder();

        // 2. Vinculamos o MockRestServiceServer a este Builder
        // Isso interceptará todas as chamadas feitas pelo RestClient gerado por este
        // builder
        server = MockRestServiceServer.bindTo(builder).build();

        // 3. Instanciamos o cliente com o builder mockado
        geminiClient = new GeminiClient(builder);
    }

    @Test
    @DisplayName("Deve retornar sucesso na primeira tentativa (Caminho Feliz)")
    void deveRetornarSucessoImediato() {
        log.info("{} [TELEMETRIA] Iniciando teste de caminho feliz.", LOG_PREFIX);

        server.expect(requestTo(URL_MOCK))
                .andRespond(withSuccess("{\"status\": \"OK\"}", MediaType.APPLICATION_JSON));

        String resultado = geminiClient.post(URL_MOCK, "{}", String.class, "Teste Sucesso");

        assertThat(resultado).contains("OK");
        server.verify();
    }

    @Test
    @DisplayName("Deve realizar retry ao receber erro 429 (Rate Limit) e ter sucesso na segunda tentativa")
    void deveRealizarRetryNoErro429() {
        log.info("{} [TELEMETRIA] Iniciando teste de resiliência (Retry 429).", LOG_PREFIX);

        // GIVEN: 1ª Falha (429), 2ª Sucesso (200)
        server.expect(requestTo(URL_MOCK)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(URL_MOCK))
                .andRespond(withSuccess("{\"status\": \"RECUPERADO\"}", MediaType.APPLICATION_JSON));

        // WHEN
        String resultado = geminiClient.post(URL_MOCK, "{}", String.class, "Teste Retry 429");

        // THEN
        assertThat(resultado).contains("RECUPERADO");
        server.verify();
    }

    @Test
    @DisplayName("Deve abortar imediatamente ao receber erro 403 (Forbidden) - Erro Não-Recuperável")
    void deveAbortarNoErro403() {
        log.info("{} [TELEMETRIA] Iniciando teste de erro fatal (403).", LOG_PREFIX);

        server.expect(requestTo(URL_MOCK)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> geminiClient.post(URL_MOCK, "{}", String.class, "Teste Fatal"))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção de exaustão após 6 falhas consecutivas de servidor (500)")
    void deveFalharAposExaustao() {
        log.info("{} [TELEMETRIA] Iniciando teste de exaustão de retentativas.", LOG_PREFIX);

        // GIVEN: 6 falhas seguidas
        for (int i = 0; i < 6; i++) {
            server.expect(requestTo(URL_MOCK)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // WHEN & THEN
        assertThatThrownBy(() -> geminiClient.post(URL_MOCK, "{}", String.class, "Teste Exaustao"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Esgotadas as tentativas")
                .hasCauseInstanceOf(RestClientResponseException.class); // Valida que a causa original foi preservada

        server.verify();
    }

}
