package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.DocumentoProponente;
import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.Usuario;
import br.com.poderfinanceiro.app.repository.DocumentoProponenteRepository;
import org.springframework.stereotype.Service;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentoService {

    private final DocumentoProponenteRepository repository;
    private final AuthService authService;

    // Define a pasta raiz no diretório do usuário (OS-agnostic)
    private final Path ROOT_DIR = Paths.get(System.getProperty("user.home"), "PoderFinanceiro_Docs");

    public DocumentoService(DocumentoProponenteRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;

        // Garante que a pasta raiz exista ao iniciar o serviço
        try {
            if (!Files.exists(ROOT_DIR)) {
                Files.createDirectories(ROOT_DIR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DocumentoProponente processarUpload(File arquivoOriginal, String tipoDoc, Proponente proponente)
            throws Exception {
        Usuario consultor = authService.getUsuarioLogado();
        if (consultor == null)
            throw new IllegalStateException("Nenhum usuário logado.");

        // 1. Calcula o Hash para evitar duplicidade
        String hash = calcularHashSha256(arquivoOriginal);
        Optional<DocumentoProponente> docExistente = repository.findByHashSha256(hash);
        if (docExistente.isPresent()) {
            throw new IllegalArgumentException("Este exato documento já foi anexado ao sistema.");
        }

        // 2. Prepara o novo nome profissional e a pasta do cliente
        String extensao = obterExtensao(arquivoOriginal.getName());
        String nomeLimpo = proponente.getNomeCompleto().replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        String novoNomeArquivo = tipoDoc.replaceAll(" ", "_").toUpperCase() + "_" + nomeLimpo + extensao;

        Path pastaCliente = ROOT_DIR.resolve("CLIENTE_" + proponente.getId());
        if (!Files.exists(pastaCliente)) {
            Files.createDirectories(pastaCliente);
        }

        Path caminhoDestino = pastaCliente.resolve(novoNomeArquivo);

        // 3. Move/Copia o arquivo fisicamente
        Files.copy(arquivoOriginal.toPath(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);

        // 4. Salva no Banco de Dados
        DocumentoProponente doc = new DocumentoProponente();
        doc.setProponente(proponente);
        doc.setUsuario(consultor);
        doc.setTipoDocumento(tipoDoc);
        doc.setArquivoPath(caminhoDestino.toString());
        doc.setHashSha256(hash);
        doc.setVerificado(false); // Nasce como pendente de auditoria

        return repository.save(doc);
    }

    public List<DocumentoProponente> listarDoProponente(Long proponenteId) {
        return repository.findByProponenteId(proponenteId);
    }

    public void abrirDocumento(DocumentoProponente doc) {
        try {
            File file = new File(doc.getArquivoPath());
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            System.err.println("Erro ao abrir arquivo: " + e.getMessage());
        }
    }

    // --- Utilitários Internos ---

    private String calcularHashSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String obterExtensao(String nomeArquivo) {
        int i = nomeArquivo.lastIndexOf('.');
        return (i > 0) ? nomeArquivo.substring(i) : "";
    }
}