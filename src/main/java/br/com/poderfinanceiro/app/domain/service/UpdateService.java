package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * <h1>UpdateService</h1>
 * <p>
 * Serviço de Domínio responsável pela orquestração do ciclo de vida de
 * atualizações.
 * Implementa lógica de comparação SemVer robusta e resolução de caminhos
 * absolutos
 * para garantir compatibilidade com Windows 11 e Linux.
 * </p>
 */
@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private static final String LOG_PREFIX = "[UpdateService]";
    private static final String UPDATE_FILE_NAME = "update.jar";
    private static final String PREFIXO_V = "v";

    private final UpdateClient updateClient;
    private final String versaoAtual;

    public UpdateService(UpdateClient updateClient, @Value("${app.version:1.0.0}") String versaoAtual) {
        this.updateClient = updateClient;
        this.versaoAtual = normalizarVersao(versaoAtual);
        log.info("{} [SISTEMA] Serviço de atualização inicializado. Versão local: {}", LOG_PREFIX, this.versaoAtual);
    }

    /**
     * Verifica se existe uma nova versão disponível no repositório remoto.
     */
    public Optional<GitHubReleaseDTO> checarNovaVersao() {
        log.info("{} [TELEMETRIA] Iniciando verificação de versão remota no GitHub.", LOG_PREFIX);
        try {
            GitHubReleaseDTO ultimaRelease = updateClient.buscarUltimaRelease();

            if (ultimaRelease == null || ultimaRelease.tagName() == null) {
                log.warn("{} [SISTEMA] Resposta nula ou inválida do servidor de atualizações.", LOG_PREFIX);
                return Optional.empty();
            }

            String versaoRemota = normalizarVersao(ultimaRelease.tagName());

            if (isVersaoNova(versaoRemota, versaoAtual)) {
                log.info("{} [NEGOCIO] Nova release detectada: {} (Atual: {})",
                        LOG_PREFIX, ultimaRelease.tagName(), versaoAtual);
                return Optional.of(ultimaRelease);
            }

            log.debug("{} [NEGOCIO] O sistema já está operando na versão mais recente.", LOG_PREFIX);
            return Optional.empty();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica ao consultar o GitHub: {}", LOG_PREFIX, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Localiza o binário correto e realiza o download para a raiz absoluta da
     * aplicação.
     */
    public void baixarEExecutarAtualizacaoPorTag(String tag) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando orquestração de download para a tag: {}", LOG_PREFIX, tag);

        GitHubReleaseDTO release = updateClient.buscarUltimaRelease();

        // Normalização de tags para comparação segura (v2.1.6.1 == 2.1.6.1)
        String tagAlvoNormalizada = normalizarVersao(tag);
        String tagRemotaNormalizada = (release != null && release.tagName() != null)
                ? normalizarVersao(release.tagName())
                : "";

        if (release == null || !tagAlvoNormalizada.equals(tagRemotaNormalizada)) {
            log.error("{} [NEGOCIO] Inconsistência de versão detectada: Alvo={} | Remota={}",
                    LOG_PREFIX, tagAlvoNormalizada, tagRemotaNormalizada);
            throw new IllegalArgumentException("A versão solicitada não corresponde à última release disponível.");
        }

        // Filtro rigoroso de assets: Busca o JAR principal, ignorando fontes e
        // documentação
        String urlDownload = release.assets().stream()
                .filter(asset -> asset.name().endsWith(".jar"))
                .filter(asset -> !asset.name().toLowerCase().contains("sources"))
                .filter(asset -> !asset.name().toLowerCase().contains("javadoc"))
                .map(GitHubReleaseDTO.GitHubAssetDTO::downloadUrl)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Binário executável (.jar) não localizado na release " + tag));

        // Resolução de caminho absoluto baseado no diretório de execução do usuário
        // Garante que o update.jar seja salvo na mesma pasta do PoderFinanceiro.jar
        String diretorioApp = System.getProperty("user.dir");
        File arquivoDestino = Paths.get(diretorioApp, UPDATE_FILE_NAME).toFile();

        log.info("{} [TELEMETRIA] Destino absoluto do download: {}", LOG_PREFIX, arquivoDestino.getAbsolutePath());

        try {
            updateClient.baixarArquivo(urlDownload, arquivoDestino);
            log.info("{} [AUDITORIA] Download concluído com sucesso. Tamanho: {} bytes.",
                    LOG_PREFIX, arquivoDestino.length());
        } catch (Exception e) {
            log.error("{} [AUDITORIA] Falha crítica no download do binário: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    /**
     * Normaliza strings de versão removendo prefixos 'v' e espaços em branco.
     * Visibilidade alterada para package-private para suportar testes unitários.
     */
    String normalizarVersao(String versao) {
        if (versao == null)
            return "0.0.0";
        return versao.toLowerCase()
                .replace(PREFIXO_V, "")
                .trim();
    }

    /**
     * Algoritmo de comparação de versões semânticas (SemVer).
     * Suporta versões com múltiplos pontos (ex: 2.1.5.1 vs 2.1.6).
     * Visibilidade alterada para package-private para suportar testes unitários.
     */
    boolean isVersaoNova(String remota, String atual) {
        try {
            String[] partesRemota = remota.split("\\.");
            String[] partesAtual = atual.split("\\.");
            int maxPartes = Math.max(partesRemota.length, partesAtual.length);

            for (int i = 0; i < maxPartes; i++) {
                int vRemota = i < partesRemota.length ? Integer.parseInt(partesRemota[i].replaceAll("\\D", "")) : 0;
                int vAtual = i < partesAtual.length ? Integer.parseInt(partesAtual[i].replaceAll("\\D", "")) : 0;

                if (vRemota > vAtual)
                    return true;
                if (vRemota < vAtual)
                    return false;
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao processar comparação numérica de versões: {}", LOG_PREFIX, e.getMessage());
        }
        return false;
    }
}
