package br.com.poderfinanceiro.app.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PlaybookResolver {

    private static final Logger log = LoggerFactory.getLogger(PlaybookResolver.class);
    private static final String LOG_PREFIX = "[PlaybookResolver]";

    /**
     * Retorna o conteúdo textual do playbook.
     */
    public String carregarPlaybook() {
        log.trace("{} [SISTEMA] Iniciando carregamento do conteúdo do playbook.", LOG_PREFIX);
        try {
            Path caminho = obterCaminhoArquivo();
            if (Files.exists(caminho)) {
                String conteudo = Files.readString(caminho);
                log.debug("{} [SISTEMA] Conteúdo do playbook lido com sucesso.", LOG_PREFIX);
                return conteudo;
            }
            log.warn("{} [NEGOCIO] Arquivo de playbook não encontrado para leitura de texto.", LOG_PREFIX);
        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro ao ler conteúdo do playbook: {}", LOG_PREFIX, e.getMessage());
        }
        return "{}";
    }

    /**
     * Resolve e retorna o Path absoluto do arquivo de playbook baseado no SO.
     */
    public Path obterCaminhoArquivo() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        Path pastaBase;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            pastaBase = (appData != null) ? Paths.get(appData, "PoderFinanceiro")
                    : Paths.get(home, "AppData", "Roaming", "PoderFinanceiro");
        } else if (os.contains("mac")) {
            pastaBase = Paths.get(home, "Library", "Application Support", "PoderFinanceiro");
        } else {
            pastaBase = Paths.get(home, ".local", "share", "PoderFinanceiro");
        }

        return pastaBase.resolve("playbook_scripts.json");
    }
}
