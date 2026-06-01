package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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
     * Localiza a release pela tag e inicia o download para a raiz da aplicação.
     */
    public void baixarEExecutarAtualizacaoPorTag(String tag) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando busca de binário para a tag: {}", LOG_PREFIX, tag);

        GitHubReleaseDTO release = updateClient.buscarUltimaRelease();

        if (release == null || !tag.equals(release.tagName())) {
            log.error("{} [NEGOCIO] Tag solicitada ({}) inválida.", LOG_PREFIX, tag);
            throw new IllegalArgumentException("Versão não localizada.");
        }

        String urlDownload = release.assets().stream()
                .filter(asset -> asset.name().endsWith(".jar"))
                .map(GitHubReleaseDTO.GitHubAssetDTO::downloadUrl)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("JAR não encontrado na release."));

        // Gold Standard: Salva como 'update.jar' na pasta onde o app está rodando
        File destino = new File("update.jar");

        log.info("{} [TELEMETRIA] Baixando atualização para: {}", LOG_PREFIX, destino.getAbsolutePath());
        updateClient.baixarArquivo(urlDownload, destino);

        log.info("{} [AUDITORIA] Atualização v{} baixada. Reinicie o sistema para aplicar.", LOG_PREFIX, tag);
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
