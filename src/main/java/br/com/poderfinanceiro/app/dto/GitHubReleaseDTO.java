package br.com.poderfinanceiro.app.dto;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GitHubReleaseDTO(String tag_name, List<Asset> assets) {

    private static final Logger log = LoggerFactory.getLogger(PlaybookItemDTO.class);

    public GitHubReleaseDTO {
        log.debug("[GITHUB_RELEASE_DTO] Criado: tag_name='{}', assets_count='{}'", tag_name, assets.size());
    }

    public record Asset(String name, String browser_download_url) {
    }
}