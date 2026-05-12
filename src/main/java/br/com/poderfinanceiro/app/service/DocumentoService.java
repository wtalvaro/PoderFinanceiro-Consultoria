package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.repository.DocumentoProponenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public DocumentoProponenteModel processarUpload(File arquivoOriginal, String tipoDoc, ProponenteModel proponente)
            throws Exception {
        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null)
            throw new IllegalStateException("Nenhum usuário logado.");

        // 1. Calcula o Hash para evitar duplicidade
        String hash = calcularHashSha256(arquivoOriginal);
        Optional<DocumentoProponenteModel> docExistente = repository.findByHashSha256(hash);
        if (docExistente.isPresent()) {
            throw new IllegalArgumentException("Este exato documento já foi anexado ao sistema.");
        }

        // 2. Prepara o novo nome profissional e a pasta do cliente
        String extensao = obterExtensao(arquivoOriginal.getName());
        String nomeLimpo = proponente.getNomeCompleto().replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        String novoNomeArquivo = tipoDoc.replaceAll(" ", "_").toUpperCase() + "_" + nomeLimpo + extensao;
        String nomePasta = String.format("CLIENTE_%03d_%s", proponente.getId(), nomeLimpo);

        Path pastaCliente = ROOT_DIR.resolve(nomePasta);
        if (!Files.exists(pastaCliente)) {
            Files.createDirectories(pastaCliente);
        }

        Path caminhoDestino = pastaCliente.resolve(novoNomeArquivo);

        // 3. Move/Copia o arquivo fisicamente
        Files.copy(arquivoOriginal.toPath(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);

        // 4. Salva no Banco de Dados
        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setProponente(proponente);
        doc.setUsuario(consultor);
        doc.setTipoDocumento(tipoDoc);
        doc.setArquivoPath(caminhoDestino.toString());
        doc.setHashSha256(hash);
        doc.setVerificado(false); // Nasce como pendente de auditoria

        return repository.save(doc);
    }

    public List<DocumentoProponenteModel> listarDoProponente(Long proponenteId) {
        return repository.findByProponenteIdOrderByIdAsc(proponenteId);
    }

    public void abrirDocumento(DocumentoProponenteModel doc) {
        try {
            File file = new File(doc.getArquivoPath());
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            System.err.println("Erro ao abrir arquivo: " + e.getMessage());
        }
    }

    @Transactional
    public DocumentoProponenteModel alternarVerificacao(Long documentoId) {
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        // Inverte o status atual
        doc.setVerificado(!doc.getVerificado());

        return repository.save(doc);
    }

    @Transactional
    public void excluirDocumento(Long documentoId) {
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        // Apaga o arquivo físico do computador para não lotar o HD
        try {
            Files.deleteIfExists(Paths.get(doc.getArquivoPath()));
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível deletar o arquivo físico: " + e.getMessage());
        }

        repository.delete(doc);
    }

    @Transactional
    public DocumentoProponenteModel atualizarTipoDocumento(Long documentoId, String novoTipo) throws Exception {
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        if (doc.getTipoDocumento().equals(novoTipo)) {
            return doc; // Nada mudou
        }

        Path caminhoAntigo = Paths.get(doc.getArquivoPath());

        // Se o arquivo físico existir, nós o renomeamos para refletir o novo tipo
        if (Files.exists(caminhoAntigo)) {
            String extensao = obterExtensao(caminhoAntigo.getFileName().toString());
            String nomeLimpo = doc.getProponente().getNomeCompleto().replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
            String novoNomeArquivo = novoTipo.replaceAll(" ", "_").toUpperCase() + "_" + nomeLimpo + extensao;

            Path caminhoNovo = caminhoAntigo.getParent().resolve(novoNomeArquivo);
            Files.move(caminhoAntigo, caminhoNovo, StandardCopyOption.REPLACE_EXISTING);

            doc.setArquivoPath(caminhoNovo.toString());
        }

        doc.setTipoDocumento(novoTipo);
        return repository.save(doc);
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