package br.com.poderfinanceiro.app.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Componente de Infraestrutura para gestão de arquivos físicos.
 * Refatorado para evitar criação de pastas na raiz do projeto.
 * Utiliza caminhos padrão do SO para isolamento de dados.
 */
@Component
public class DocumentoStorageResolver {

    private static final Logger log = LoggerFactory.getLogger(DocumentoStorageResolver.class);
    private static final String LOG_PREFIX = "[DocumentoStorage]";
    private static final String ALGORITMO_HASH = "SHA-256";

    private final Path rootPath;

    public DocumentoStorageResolver(@Value("${app.storage.root:default}") String root) {
        this.rootPath = resolverCaminhoRaiz(root);
        inicializarDiretorioRaiz();
    }

    /**
     * Resolve o caminho base de armazenamento de forma segura.
     */
    private Path resolverCaminhoRaiz(String root) {
        if (!"default".equalsIgnoreCase(root)) {
            return Paths.get(root).toAbsolutePath().normalize();
        }

        // Lógica Gold Standard para caminhos de dados do usuário
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        Path base;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = (appData != null) ? Paths.get(appData, "PoderFinanceiro")
                    : Paths.get(home, "AppData", "Roaming", "PoderFinanceiro");
        } else if (os.contains("mac")) {
            base = Paths.get(home, "Library", "Application Support", "PoderFinanceiro");
        } else {
            // Fedora/Linux: Segue o padrão XDG (~/.local/share)
            base = Paths.get(home, ".local", "share", "PoderFinanceiro");
        }

        return base.resolve("storage").resolve("documentos").toAbsolutePath().normalize();
    }

    private void inicializarDiretorioRaiz() {
        try {
            Files.createDirectories(rootPath);
            log.info("{} [SISTEMA] Storage físico isolado em: {}", LOG_PREFIX, rootPath);
        } catch (IOException e) {
            log.error("{} [SISTEMA] Falha crítica ao inicializar storage: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Erro de I/O na inicialização do storage", e);
        }
    }

    public Path getRootDir() {
        return this.rootPath;
    }

    public Path resolverPastaCliente(Long idCliente, String nomeCliente) {
        String nomeSanitizado = nomeCliente.trim().replaceAll("[\\s\\W]+", "_").toUpperCase();
        Path pastaCliente = rootPath.resolve("clientes").resolve(idCliente + "_" + nomeSanitizado);

        try {
            Files.createDirectories(pastaCliente);
            return pastaCliente;
        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro ao criar pasta do cliente {}: {}", LOG_PREFIX, idCliente, e.getMessage());
            throw new RuntimeException("Falha na organização física do cliente", e);
        }
    }

    public StorageResult salvar(InputStream inputStream, String nomeOriginal) {
        log.info("{} [TELEMETRIA] Persistindo arquivo: {}", LOG_PREFIX, nomeOriginal);
        try {
            LocalDate hoje = LocalDate.now();
            Path diretorioDestino = rootPath
                    .resolve("geral")
                    .resolve(String.valueOf(hoje.getYear()))
                    .resolve(String.format("%02d", hoje.getMonthValue()));

            Files.createDirectories(diretorioDestino);

            String extensao = extrairExtensao(nomeOriginal);
            String novoNome = UUID.randomUUID() + extensao;
            Path arquivoFinal = diretorioDestino.resolve(novoNome);

            Files.copy(inputStream, arquivoFinal, StandardCopyOption.REPLACE_EXISTING);

            String hash = calcularHash(arquivoFinal);
            long tamanho = Files.size(arquivoFinal);

            log.info("{} [AUDITORIA] Arquivo armazenado. Hash: {} | Tamanho: {} bytes", LOG_PREFIX, hash, tamanho);
            return new StorageResult(rootPath.relativize(arquivoFinal).toString(), hash, tamanho);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro fatal no salvamento físico: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Erro ao persistir documento no storage", e);
        }
    }

    private String extrairExtensao(String nome) {
        if (nome == null || !nome.contains("."))
            return "";
        return nome.substring(nome.lastIndexOf(".")).toLowerCase();
    }

    private String calcularHash(Path arquivo) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
        try (InputStream is = Files.newInputStream(arquivo)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public record StorageResult(String pathRelativo, String hash, long tamanho) {
    }
}
