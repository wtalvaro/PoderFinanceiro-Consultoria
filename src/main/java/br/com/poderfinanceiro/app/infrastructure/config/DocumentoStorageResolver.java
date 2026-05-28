package br.com.poderfinanceiro.app.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DocumentoStorageResolver {

    private static final Logger log = LoggerFactory.getLogger(DocumentoStorageResolver.class);
    private static final String LOG_PREFIX = "[DocumentoStorageResolver]";
    private final Path rootDir;

    public DocumentoStorageResolver() {
        this.rootDir = Paths.get(System.getProperty("user.home"), "PoderFinanceiro_Docs");
        inicializarDiretorioRaiz();
    }

    private void inicializarDiretorioRaiz() {
        try {
            if (!Files.exists(rootDir)) {
                Files.createDirectories(rootDir);
                log.info("{} [SISTEMA] Diretório raiz de documentos criado: {}", LOG_PREFIX, rootDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("{} [SISTEMA] Falha crítica ao criar diretório raiz: {}", LOG_PREFIX, e.getMessage());
        }
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Path resolverPastaCliente(Long id, String nome) throws IOException {
        String nomePasta = String.format("CLIENTE_%03d_%s", id, nome.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase());
        Path pastaCliente = rootDir.resolve(nomePasta);
        if (!Files.exists(pastaCliente)) {
            Files.createDirectories(pastaCliente);
            log.debug("{} [SISTEMA] Pasta do cliente criada: {}", LOG_PREFIX, pastaCliente);
        }
        return pastaCliente;
    }
}
