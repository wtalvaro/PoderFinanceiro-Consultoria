package br.com.poderfinanceiro.app.infrastructure.client;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

/**
 * Cliente de Infraestrutura para gestão de atualizações via GitHub.
 * Refatorado para testabilidade total e injeção de endpoints.
 */
@Component
public class UpdateClient {

    private static final Logger log = LoggerFactory.getLogger(UpdateClient.class);
    private static final String LOG_PREFIX = "[UpdateClient]";

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final String apiUrl;

    public UpdateClient(
            RestClient.Builder restClientBuilder,
            HttpClient httpClient,
            @Value("${app.update.github-url:https://api.github.com/repos/wtalvaro/PoderFinanceiro-Consultoria/releases/latest}") String apiUrl) {
        this.restClient = restClientBuilder.build();
        this.httpClient = httpClient;
        this.apiUrl = apiUrl;
        log.info("{} [SISTEMA] Cliente de atualização inicializado. Endpoint: {}", LOG_PREFIX, apiUrl);
    }

    /**
     * Consulta os metadados da última versão estável no repositório.
     */
    public GitHubReleaseDTO buscarUltimaRelease() {
        log.trace("{} [TELEMETRIA] Consultando metadados de release.", LOG_PREFIX);
        try {
            return restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(GitHubReleaseDTO.class);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao consultar metadados de atualização: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    /**
     * Realiza o download físico do arquivo de atualização.
     */
    public void baixarArquivo(String url, File destino) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando download do binário. Destino: {}", LOG_PREFIX, destino.getName());

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL de download inválida.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        // O BodyHandlers.ofFile gerencia a escrita em disco de forma performática
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destino.toPath()));

        if (response.statusCode() >= 400) {
            log.error("{} [SISTEMA] Falha no download. Status HTTP: {}", LOG_PREFIX, response.statusCode());
            throw new RuntimeException("Erro ao baixar atualização: HTTP " + response.statusCode());
        }

        log.info("{} [AUDITORIA] Download concluído com sucesso. Path: {}", LOG_PREFIX, destino.getAbsolutePath());
    }
}
