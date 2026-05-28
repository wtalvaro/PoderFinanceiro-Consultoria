package br.com.poderfinanceiro.app.infrastructure.client;

import br.com.poderfinanceiro.app.dto.GitHubReleaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

@Component
public class UpdateClient {

    private static final Logger log = LoggerFactory.getLogger(UpdateClient.class);
    private static final String LOG_PREFIX = "[UpdateClient]";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/wtalvaro/PoderFinanceiro-Consultoria/releases/latest";

    private final RestClient restClient;
    private final HttpClient httpClient;

    public UpdateClient() {
        this.restClient = RestClient.builder().build();
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        log.info("{} [SISTEMA] Cliente de atualização inicializado.", LOG_PREFIX);
    }

    public GitHubReleaseDTO buscarUltimaRelease() {
        log.trace("{} [TELEMETRIA] Consultando API do GitHub para última release.", LOG_PREFIX);
        return restClient.get().uri(GITHUB_API_URL).retrieve().body(GitHubReleaseDTO.class);
    }

    public void baixarArquivo(String url, File destino) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando download do binário: {}", LOG_PREFIX, url);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destino.toPath()));

        if (response.statusCode() >= 400) {
            log.error("{} [SISTEMA] Falha no download. Status HTTP: {}", LOG_PREFIX, response.statusCode());
            throw new RuntimeException("Erro ao baixar atualização: HTTP " + response.statusCode());
        }
        log.info("{} [AUDITORIA] Download concluído com sucesso para: {}", LOG_PREFIX, destino.getAbsolutePath());
    }
}
