package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

/**
 * Serviço de Domínio para gestão do ciclo de vida de atualizações.
 * Implementa a lógica de comparação de versões e gestão de binários.
 */
@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private static final String LOG_PREFIX = "[UpdateService]";

    private final UpdateClient updateClient;
    private final String versaoAtual;

    public UpdateService(UpdateClient updateClient, @Value("${app.version:1.0.0}") String versaoAtual) {
        this.updateClient = updateClient;
        this.versaoAtual = versaoAtual;
        log.info("{} [SISTEMA] Serviço de atualização iniciado. Versão atual: {}", LOG_PREFIX, versaoAtual);
    }

    /**
     * Verifica se existe uma nova versão disponível no GitHub.
     */
    public Optional<GitHubReleaseDTO> checarNovaVersao() {
        log.trace("{} [TELEMETRIA] Consultando última release no GitHub.", LOG_PREFIX);
        try {
            GitHubReleaseDTO ultimaRelease = updateClient.buscarUltimaRelease();

            if (ultimaRelease == null || ultimaRelease.tagName() == null) {
                return Optional.empty();
            }

            String versaoRemota = ultimaRelease.tagName().replace("v", "");

            if (isVersaoNova(versaoRemota, versaoAtual)) {
                log.info("{} [NEGOCIO] Nova versão detectada: {} (Atual: {})",
                        LOG_PREFIX, ultimaRelease.tagName(), versaoAtual);
                return Optional.of(ultimaRelease);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao verificar atualização: {}", LOG_PREFIX, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Localiza a release pela tag e inicia o download.
     */
    public void baixarEExecutarAtualizacaoPorTag(String tag) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando busca de binário para a tag: {}", LOG_PREFIX, tag);

        GitHubReleaseDTO release = updateClient.buscarUltimaRelease();

        if (release == null || !tag.equals(release.tagName())) {
            log.error("{} [NEGOCIO] A tag solicitada ({}) não corresponde à última release disponível.", LOG_PREFIX,
                    tag);
            throw new IllegalArgumentException("Versão não localizada para download.");
        }

        String urlDownload = release.assets().stream()
                .filter(asset -> asset.name().endsWith(".jar") || asset.name().endsWith(".exe"))
                .map(GitHubReleaseDTO.GitHubAssetDTO::downloadUrl)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhum binário compatível encontrado na release " + tag));

        File destino = Files.createTempFile("poder-financeiro-update-", ".jar").toFile();
        destino.deleteOnExit();

        updateClient.baixarArquivo(urlDownload, destino);

        log.info("{} [AUDITORIA] Atualização baixada com sucesso: {}", LOG_PREFIX, destino.getAbsolutePath());
        // Lógica de reinicialização seria disparada aqui ou via script externo
    }

    boolean isVersaoNova(String remota, String atual) {
        try {
            String[] partesRemota = remota.split("\\.");
            String[] partesAtual = atual.split("\\.");
            int length = Math.max(partesRemota.length, partesAtual.length);
            for (int i = 0; i < length; i++) {
                int vRemota = i < partesRemota.length ? Integer.parseInt(partesRemota[i]) : 0;
                int vAtual = i < partesAtual.length ? Integer.parseInt(partesAtual[i]) : 0;
                if (vRemota > vAtual)
                    return true;
                if (vRemota < vAtual)
                    return false;
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao comparar versões: {}", LOG_PREFIX, e.getMessage());
        }
        return false;
    }
}
