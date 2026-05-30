package br.com.poderfinanceiro.app.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolvedor de Playbooks e Scripts.
 * Gerencia a localização física dos arquivos de configuração baseada no SO.
 * Refatorado para suportar injeção de caminhos e testabilidade total.
 */
@Component
public class PlaybookResolver {

    private static final Logger log = LoggerFactory.getLogger(PlaybookResolver.class);
    private static final String LOG_PREFIX = "[PlaybookResolver]";
    private static final String NOME_ARQUIVO = "playbook_scripts.json";

    private final Path caminhoCustomizado;

    /**
     * Construtor Gold Standard.
     * 
     * @param pathStr Caminho opcional via application.properties. Se "default", usa
     *                lógica de SO.
     */
    public PlaybookResolver(@Value("${app.playbook.path:default}") String pathStr) {
        if ("default".equalsIgnoreCase(pathStr)) {
            this.caminhoCustomizado = null;
            log.info("{} [SISTEMA] Inicializado com resolução automática por Sistema Operacional.", LOG_PREFIX);
        } else {
            this.caminhoCustomizado = Paths.get(pathStr).toAbsolutePath().normalize();
            log.info("{} [SISTEMA] Inicializado com caminho customizado: {}", LOG_PREFIX, caminhoCustomizado);
        }
    }

    /**
     * Retorna o conteúdo textual do playbook.
     */
    public String carregarPlaybook() {
        log.trace("{} [TELEMETRIA] Solicitando leitura do conteúdo do playbook.", LOG_PREFIX);
        try {
            Path caminho = obterCaminhoArquivo();
            if (Files.exists(caminho)) {
                String conteudo = Files.readString(caminho);
                log.debug("{} [SISTEMA] Conteúdo do playbook lido com sucesso ({} bytes).",
                        LOG_PREFIX, conteudo.length());
                return conteudo;
            }
            log.warn("{} [NEGOCIO] Arquivo de playbook não localizado em: {}", LOG_PREFIX, caminho);
        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro crítico ao ler conteúdo do playbook: {}", LOG_PREFIX, e.getMessage());
        }
        return "{}";
    }

    /**
     * Resolve e retorna o Path absoluto do arquivo de playbook.
     */
    public Path obterCaminhoArquivo() {
        if (caminhoCustomizado != null) {
            return caminhoCustomizado.resolve(NOME_ARQUIVO);
        }

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
            // Padrão Linux (Fedora)
            pastaBase = Paths.get(home, ".local", "share", "PoderFinanceiro");
        }

        return pastaBase.resolve(NOME_ARQUIVO);
    }
}
