package br.com.poderfinanceiro.app.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.Random;
import java.util.concurrent.Callable;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String LOG_PREFIX = "[GeminiClient]";

    private final RestClient restClient;
    private final Random random = new Random();

    public GeminiClient() {
        this.restClient = RestClient.builder().build();
    }

    public <T> T post(String url, Object body, Class<T> responseType, String contexto) {
        log.info("{} [TELEMETRIA] SAÍDA INFRA -> API EXTERNA: Payload={}", LOG_PREFIX, body);
        return executarComResiliencia(() -> {
            log.trace("{} [TELEMETRIA] Enviando POST para: {} (Contexto: {})", LOG_PREFIX, url.split("\\?")[0],
                    contexto);
            return restClient.post().uri(url).body(body).retrieve().body(responseType);
        }, contexto);
    }

    public String get(String url, String contexto) {
        return executarComResiliencia(() -> {
            log.trace("{} [TELEMETRIA] Enviando GET para: {} (Contexto: {})", LOG_PREFIX, url.split("\\?")[0],
                    contexto);
            return restClient.get().uri(url).retrieve().body(String.class);
        }, contexto);
    }

    private <T> T executarComResiliencia(Callable<T> acao, String contexto) {
        int maxTentativas = 6;
        long delayInicial = 2000;

        for (int i = 1; i <= maxTentativas; i++) {
            try {
                return acao.call();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if ((status == 429 || status >= 500) && i < maxTentativas) {
                    long backoff = delayInicial * (long) Math.pow(2, i - 1);
                    long jitter = (long) (random.nextDouble() * 0.2 * backoff);
                    long tempoEspera = backoff + jitter;

                    log.warn("{} [SISTEMA] Erro {} em {}. Tentativa {}/{}. Retentando em {}ms...", LOG_PREFIX, status,
                            contexto, i, maxTentativas, tempoEspera);
                    try {
                        Thread.sleep(tempoEspera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                log.error("{} [AUDITORIA] Erro fatal HTTP {} no contexto {}.", LOG_PREFIX, status, contexto);
                throw e;
            } catch (Exception e) {
                log.error("{} [SISTEMA] Erro inesperado no cliente: {}", LOG_PREFIX, e.getMessage());
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Esgotadas as tentativas de resiliência para " + contexto);
    }
}
