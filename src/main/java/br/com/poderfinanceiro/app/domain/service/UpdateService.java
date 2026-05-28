package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Serviço de Gestão de Atualizações (OTA - Over The Air). Orquestra a
 * verificação de versão, download e aplicação de patches de software.
 */
@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private static final String LOG_PREFIX = "[UpdateService]";
    private static final String NOME_JAR_LOCAL = "PoderFinanceiro.jar";

    private final UpdateClient updateClient;

    @Value("${app.version:v1.0.0}") private String versaoAtual;

    public UpdateService(UpdateClient updateClient) {
        this.updateClient = updateClient;
        log.info("{} [SISTEMA] Serviço de atualização instanciado. Versão atual: {}", LOG_PREFIX, versaoAtual);
    }

    /**
     * Verifica se existe uma nova versão disponível no repositório remoto.
     */
    public String checarNovaVersao() {
        log.info("{} [TELEMETRIA] Verificando atualizações remotas.", LOG_PREFIX);
        try {
            GitHubReleaseDTO release = updateClient.buscarUltimaRelease();

            if (release != null && isVersaoNova(versaoAtual, release.tag_name())) {
                log.info("{} [NEGOCIO] Nova versão detectada: {}", LOG_PREFIX, release.tag_name());
                return release.tag_name();
            }
            log.info("{} [NEGOCIO] O sistema está operando na versão mais recente.", LOG_PREFIX);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao contatar servidor de atualizações: {}", LOG_PREFIX, e.getMessage());
        }
        return null;
    }

    /**
     * Orquestra o download e a execução do script de substituição do binário.
     */
    public void baixarEExecutarAtualizacao(String tag) {
        log.info("{} [TELEMETRIA] Iniciando procedimento de atualização para a versão {}", LOG_PREFIX, tag);

        String pastaInstalacao = System.getProperty("user.dir");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        try {
            // 1. Garantir infraestrutura de inicialização
            garantirInicializadorPersistente(pastaInstalacao, isWindows);

            // 2. Preparar download
            String downloadUrl = String.format(
                    "https://github.com/wtalvaro/PoderFinanceiro-Consultoria/releases/download/%s/%s", tag,
                    NOME_JAR_LOCAL);
            File tempJar = new File(System.getProperty("java.io.tmpdir"), "PoderFinanceiro_Update.jar");

            // 3. Download via Client (Loom-friendly I/O)
            updateClient.baixarArquivo(downloadUrl, tempJar);

            // 4. Gerar script de transição (Hot Swap)
            File scriptFile = gerarScriptAtualizacao(tempJar, isWindows, pastaInstalacao);

            log.info("{} [AUDITORIA] Aplicação pronta para transição. Reiniciando processo...", LOG_PREFIX);

            executarScriptESair(scriptFile, isWindows);

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro crítico durante a atualização: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Falha ao aplicar atualização: " + e.getMessage());
        }
    }

    private void garantirInicializadorPersistente(String pasta, boolean isWindows) {
        if (isWindows) {
            File inicializadorBat = new File(pasta, "iniciar.bat");
            if (!inicializadorBat.exists()) {
                try {
                    String conteudo = "@echo off\ncd /d \"%~dp0\"\nstart \"\" javaw -jar " + NOME_JAR_LOCAL
                            + "\nexit\n";
                    Files.writeString(inicializadorBat.toPath(), conteudo);
                    log.info("{} [SISTEMA] Inicializador 'iniciar.bat' gerado preventivamente.", LOG_PREFIX);
                } catch (Exception e) {
                    log.warn("{} [SISTEMA] Não foi possível criar o inicializador bat: {}", LOG_PREFIX, e.getMessage());
                }
            }
        }
    }

    private File gerarScriptAtualizacao(File tempJar, boolean isWindows, String pastaInstalacao) throws Exception {
        String caminhoJarAtual = pastaInstalacao + File.separator + NOME_JAR_LOCAL;
        File scriptFile;
        String scriptContent;

        if (isWindows) {
            scriptFile = new File(System.getProperty("java.io.tmpdir"), "update_app.bat");
            scriptContent = String.format(
                    "@echo off\ntimeout /t 3 /nobreak > nul\nmove /y \"%s\" \"%s\"\nstart /b java -jar \"%s\"\ndel \"%%~f0\"",
                    tempJar.getAbsolutePath(), caminhoJarAtual, caminhoJarAtual);
        } else {
            scriptFile = new File(System.getProperty("java.io.tmpdir"), "update_app.sh");
            scriptContent = String.format(
                    "#!/bin/bash\nsleep 3\nmv -f \"%s\" \"%s\"\nnohup java -jar \"%s\" >/dev/null 2>&1 &\nrm -- \"$0\"",
                    tempJar.getAbsolutePath(), caminhoJarAtual, caminhoJarAtual);
        }

        Files.writeString(scriptFile.toPath(), scriptContent);

        if (!isWindows) {
            Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(scriptFile.toPath()));
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(scriptFile.toPath(), perms);
        }

        log.debug("{} [SISTEMA] Script de transição gerado em: {}", LOG_PREFIX, scriptFile.getAbsolutePath());
        return scriptFile;
    }

    private void executarScriptESair(File scriptFile, boolean isWindows) throws Exception {
        ProcessBuilder pb = isWindows
                ? new ProcessBuilder("cmd", "/c", "start", "\"\"", "\"" + scriptFile.getAbsolutePath() + "\"")
                : new ProcessBuilder("/bin/bash", "-c", scriptFile.getAbsolutePath());

        pb.start();
        log.info("{} [AUDITORIA] Script de atualização disparado. Encerrando JVM atual.", LOG_PREFIX);
        System.exit(0);
    }

    private boolean isVersaoNova(String atual, String remota) {
        if (atual == null || remota == null)
            return false;

        String v1 = atual.toLowerCase().replace("v", "");
        String v2 = remota.toLowerCase().replace("v", "");

        String[] partesAtuais = v1.split("\\.");
        String[] partesRemotas = v2.split("\\.");
        int tamanho = Math.max(partesAtuais.length, partesRemotas.length);

        for (int i = 0; i < tamanho; i++) {
            int n1 = i < partesAtuais.length ? Integer.parseInt(partesAtuais[i]) : 0;
            int n2 = i < partesRemotas.length ? Integer.parseInt(partesRemotas[i]) : 0;
            if (n2 > n1)
                return true;
            if (n2 < n1)
                return false;
        }
        return false;
    }
}
