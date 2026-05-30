package br.com.poderfinanceiro.app.infrastructure.client;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Cliente de Infraestrutura para consulta de endereços via ViaCEP.
 * Implementa motor de resiliência com Backoff Exponencial e Jitter.
 */
@Component
public class ViaCepClient {

    private static final Logger log = LoggerFactory.getLogger(ViaCepClient.class);
    private static final String LOG_PREFIX = "[ViaCepClient]";

    private final RestClient restClient;
    private final String baseUrl;
    private final Random random = new Random();

    /**
     * Construtor Gold Standard para Injeção de Dependência.
     */
    public ViaCepClient(
            RestClient.Builder restClientBuilder,
            @Value("${viacep.api.url:https://viacep.com.br/ws/}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        log.info("{} [SISTEMA] Cliente ViaCEP inicializado. Endpoint: {}", LOG_PREFIX, baseUrl);
    }

    /**
     * Busca dados de endereço baseados no CEP.
     */
    public ViaCepResponse getEndereco(String cep) {
        if (cep == null || !cep.matches("\\d{8}")) {
            log.warn("{} [NEGOCIO] CEP inválido fornecido: {}", LOG_PREFIX, cep);
            throw new IllegalArgumentException("Formato de CEP inválido.");
        }

        String url = String.format("%s%s/json/", baseUrl, cep);

        return executarComResiliencia(() -> {
            log.trace("{} [TELEMETRIA] Solicitando dados para o CEP: {}", LOG_PREFIX, cep);
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ViaCepResponse.class);
        }, "BUSCA_CEP_" + cep);
    }

    /**
     * Motor de Resiliência: Tenta recuperar a operação em caso de falhas
     * transientes.
     */
    private <T> T executarComResiliencia(Callable<T> acao, String contexto) {
        int maxTentativas = 4;
        long delayInicial = 100; // Reduzido para testes, em prod pode ser maior

        for (int i = 1; i <= maxTentativas; i++) {
            try {
                return acao.call();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();

                // Erros 429 (Rate Limit) ou 5xx (Servidor) permitem retentativa
                if ((status == 429 || status >= 500) && i < maxTentativas) {
                    long backoff = delayInicial * (long) Math.pow(2, i - 1);
                    long jitter = (long) (random.nextDouble() * 0.2 * backoff);
                    long tempoEspera = backoff + jitter;

                    log.warn("{} [SISTEMA] Erro HTTP {} em {}. Tentativa {}/{}. Retentando em {}ms...",
                            LOG_PREFIX, status, contexto, i, maxTentativas, tempoEspera);

                    try {
                        Thread.sleep(tempoEspera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operação interrompida", ie);
                    }
                    continue;
                }
                log.error("{} [AUDITORIA] Erro fatal na comunicação com ViaCEP. Status: {}", LOG_PREFIX, status);
                throw e;
            } catch (Exception e) {
                log.error("{} [SISTEMA] Falha inesperada no cliente ViaCEP: {}", LOG_PREFIX, e.getMessage());
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Esgotadas as tentativas de resiliência para " + contexto);
    }
}
