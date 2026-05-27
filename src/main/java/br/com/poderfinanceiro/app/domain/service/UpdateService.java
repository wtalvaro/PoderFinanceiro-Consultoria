package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.GitHubReleaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private final RestClient restClient = RestClient.builder().build();

    // URL fixa da API do GitHub para pegar a última release
    private static final String GITHUB_API_URL = "https://api.github.com/repos/wtalvaro/PoderFinanceiro-Consultoria/releases/latest";
    private static final String NOME_JAR_LOCAL = "PoderFinanceiro.jar";

    @Value("${app.version:v1.0.0}") // Lê do application.properties, padrão v1.0.0 se não existir
    private String versaoAtual;

    public String checarNovaVersao() {
        log.info("[UPDATE] Checando atualizações. Versão atual local: {}", versaoAtual);
        try {
            GitHubReleaseDTO release = restClient.get()
                    .uri(GITHUB_API_URL)
                    .retrieve()
                    .body(GitHubReleaseDTO.class);

            if (release != null && isVersaoNova(versaoAtual, release.tag_name())) {
                log.info("[UPDATE] Nova versão encontrada: {}", release.tag_name());
                return release.tag_name();
            }
            log.info("[UPDATE] O sistema já está na versão mais recente.");
        } catch (Exception e) {
            log.error("[UPDATE] Falha ao checar atualizações no GitHub: {}", e.getMessage());
            throw new RuntimeException("Não foi possível contatar o servidor de atualizações.");
        }
        return null;
    }

    public void baixarEExecutarAtualizacao(String tag) {
        log.info("[UPDATE] Iniciando processo de download e instalação da versão {}", tag);
        String downloadUrl = "https://github.com/wtalvaro/PoderFinanceiro-Consultoria/releases/download/" + tag + "/"
                + NOME_JAR_LOCAL;
        File tempJar = new File(System.getProperty("java.io.tmpdir"), "PoderFinanceiro_Update.jar");

        try {
            log.debug("[UPDATE] Baixando arquivo de: {}", downloadUrl);
            byte[] jarBytes = restClient.get().uri(downloadUrl).retrieve().body(byte[].class);
            Files.write(tempJar.toPath(), jarBytes);
            log.info("[UPDATE] Download concluído com sucesso ({} bytes).", jarBytes.length);

            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            File scriptFile = gerarScriptAtualizacao(tempJar, isWindows);

            log.info("[UPDATE] Script de transição gerado. Reiniciando a aplicação...");
            if (isWindows) {
                new ProcessBuilder("cmd", "/c", "start", "\"\"", "\"" + scriptFile.getAbsolutePath() + "\"").start();
            } else {
                new ProcessBuilder("/bin/bash", "-c", scriptFile.getAbsolutePath()).start();
            }

            System.exit(0); // Mata o processo Java atual para liberar o arquivo
        } catch (Exception e) {
            log.error("[UPDATE] Erro crítico ao processar atualização: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao baixar ou aplicar a atualização.");
        }
    }

    private File gerarScriptAtualizacao(File tempJar, boolean isWindows) throws Exception {
        String caminhoJarAtual = System.getProperty("user.dir") + File.separator + NOME_JAR_LOCAL;
        File scriptFile;
        String scriptContent;

        if (isWindows) {
            scriptFile = new File(System.getProperty("java.io.tmpdir"), "update_app.bat");
            scriptContent = String.format(
                    "@echo off\n" +
                            "timeout /t 3 /nobreak > nul\n" +
                            "move /y \"%s\" \"%s\"\n" +
                            "start /b java -jar \"%s\"\n" +
                            "del \"%%~f0\"",
                    tempJar.getAbsolutePath(), caminhoJarAtual, caminhoJarAtual);
        } else {
            // Script para Linux / Mac
            scriptFile = new File(System.getProperty("java.io.tmpdir"), "update_app.sh");
            scriptContent = String.format(
                    "#!/bin/bash\n" +
                            "sleep 3\n" +
                            "mv -f \"%s\" \"%s\"\n" +
                            "nohup java -jar \"%s\" >/dev/null 2>&1 &\n" +
                            "rm -- \"$0\"",
                    tempJar.getAbsolutePath(), caminhoJarAtual, caminhoJarAtual);
        }

        Files.writeString(scriptFile.toPath(), scriptContent);

        // Dar permissão de execução no Linux/Mac
        if (!isWindows) {
            Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(scriptFile.toPath()));
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(scriptFile.toPath(), perms);
        }

        return scriptFile;
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