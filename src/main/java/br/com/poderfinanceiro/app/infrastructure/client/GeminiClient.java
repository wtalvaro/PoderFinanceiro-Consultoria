package br.com.poderfinanceiro.app.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Cliente de Infraestrutura para comunicação com a API do Gemini.
 * Implementa resiliência agressiva com Backoff Exponencial e Jitter para
 * lidar com limites de cota (Rate Limit) e instabilidades de rede.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String LOG_PREFIX = "[GeminiClient]";

    private final RestClient restClient;
    
    /**
     * Construtor refatorado para permitir injeção do Builder.
     * Essencial para suporte a MockRestServiceServer nos testes.
     */
    public GeminiClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        log.info("{} [SISTEMA] Cliente Gemini inicializado com suporte a resiliência.", LOG_PREFIX);
    }

    /**
     * Executa uma requisição POST assíncrona ou síncrona com tratamento de erro.
     */
    public <T> T post(String url, Object body, Class<T> responseType, String contexto) {
        log.info("{} [TELEMETRIA] SAÍDA INFRA -> API EXTERNA: Payload={}", LOG_PREFIX, body);

        return executarComResiliencia(() -> {
            log.trace("{} [TELEMETRIA] Enviando POST para: {} (Contexto: {})", LOG_PREFIX, url.split("\\?")[0],
                    contexto);
            return restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        }, contexto);
    }

    /**
     * Executa uma requisição GET com tratamento de erro e resiliência.
     */
    public String get(String url, String contexto) {
        log.info("{} [TELEMETRIA] SAÍDA INFRA -> API EXTERNA: GET {}", LOG_PREFIX, url.split("\\?")[0]);

        return executarComResiliencia(() -> {
            log.trace("{} [TELEMETRIA] Enviando GET para: {} (Contexto: {})", LOG_PREFIX, url.split("\\?")[0],
                    contexto);
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        }, contexto);
    }

    /**
     * Motor de Resiliência: Implementa 6 tentativas com espera crescente.
     * Utiliza Virtual Threads amigáveis (Thread.sleep em Virtual Threads não
     * bloqueia a OS Thread).
     */
    private <T> T executarComResiliencia(Callable<T> acao, String contexto) {
        int maxTentativas = 6;
        long delayInicial = 2000;

        for (int i = 1; i <= maxTentativas; i++) {
            try {
                T resultado = acao.call();
                log.info("{} [AUDITORIA] Operação concluída com sucesso no contexto: {}", LOG_PREFIX, contexto);
                return resultado;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();

                // Se for erro recuperável (429 ou 5xx) E ainda houver tentativas restantes
                if ((status == 429 || status >= 500) && i < maxTentativas) {
                    long backoff = delayInicial * (long) Math.pow(2, i - 1);
                    long jitter = (long) (new Random().nextDouble() * 0.2 * backoff);
                    long tempoEspera = backoff + jitter;

                    log.warn("{} [SISTEMA] Erro {} em {}. Tentativa {}/{}. Retentando em {}ms...",
                            LOG_PREFIX, status, contexto, i, maxTentativas, tempoEspera);

                    try {
                        Thread.sleep(tempoEspera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupção durante backoff", ie);
                    }
                    continue;
                }

                // Se chegou aqui, ou o erro é fatal (ex: 403) ou as tentativas esgotaram
                if (i == maxTentativas) {
                    log.error("{} [AUDITORIA] Esgotadas as {} tentativas de resiliência para {}. Último erro: {}",
                            LOG_PREFIX, maxTentativas, contexto, e.getMessage());
                    throw new RuntimeException("Esgotadas as tentativas de resiliência para " + contexto, e);
                }

                log.error("{} [AUDITORIA] Erro fatal HTTP {} no contexto {}. Abortando.", LOG_PREFIX, status, contexto);
                throw e;
            } catch (Exception e) {
                log.error("{} [SISTEMA] Erro inesperado no cliente durante {}: {}", LOG_PREFIX, contexto,
                        e.getMessage());
                throw new RuntimeException(e);
            }
        }
        // Fallback de segurança (teoricamente inalcançável devido aos throws acima)
        throw new RuntimeException("Falha na orquestração de resiliência para " + contexto);
    }

}
