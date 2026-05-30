package br.com.poderfinanceiro.app.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO para mapeamento da resposta da API do GitHub.
 * Utiliza anotações Jackson para converter snake_case em camelCase.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubReleaseDTO(
        @JsonProperty("tag_name") String tagName,
        @JsonProperty("name") String name,
        @JsonProperty("body") String body,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("assets") List<GitHubAssetDTO> assets) {
    /**
     * Representa um asset (binário) da release.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubAssetDTO(
            @JsonProperty("name") String name,
            @JsonProperty("browser_download_url") String downloadUrl,
            @JsonProperty("size") Long size) {
    }
}
