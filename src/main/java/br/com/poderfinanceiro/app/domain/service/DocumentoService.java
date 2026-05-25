package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.DocumentoProponenteRepository;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final DocumentoProponenteRepository repository;
    private final AuthService authService;
    private final Path ROOT_DIR = Paths.get(System.getProperty("user.home"), "PoderFinanceiro_Docs");

    public DocumentoService(DocumentoProponenteRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
        try {
            if (!Files.exists(ROOT_DIR))
                Files.createDirectories(ROOT_DIR);
        } catch (Exception e) {
            log.error("[SERVICE][DOcUMENTO] Erro: {}", e.getMessage(), e);
        }
    }

    /**
     * 🧪 GERADOR DE IDENTIDADE ÚNICA
     * Cria nomes como: RG_WAGNER_1715585400123.pdf
     */
    private String gerarNomeArquivoUnico(String nomeCompleto, String tipoDoc, String extensao) {
        String tipoSanitizado = tipoDoc.replaceAll("\\s+", "_").toUpperCase();
        String nomeSanitizado = nomeCompleto.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        long timestamp = System.currentTimeMillis(); // O segredo da unicidade

        return String.format("%s_%s_%d%s", tipoSanitizado, nomeSanitizado, timestamp, extensao);
    }

    public DocumentoProponenteModel processarUpload(File arquivoOriginal, String tipoDoc, ProponenteModel proponente,
            PropostaModel proposta) throws Exception {
        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null)
            throw new IllegalStateException("Nenhum usuário logado.");

        // 1. Evita duplicidade exata (pelo conteúdo do arquivo)
        String hash = calcularHashSha256(arquivoOriginal);
        if (repository.findByHashSha256(hash).isPresent()) {
            throw new IllegalArgumentException("Este exato documento já foi anexado ao sistema.");
        }

        // 2. Prepara a pasta e o NOME ÚNICO
        String extensao = obterExtensao(arquivoOriginal.getName());
        String novoNomeArquivo = gerarNomeArquivoUnico(proponente.getNomeCompleto(), tipoDoc, extensao);

        String nomePasta = String.format("CLIENTE_%03d_%s", proponente.getId(),
                proponente.getNomeCompleto().replaceAll("[^a-zA-Z0-9]", "_").toUpperCase());

        Path pastaCliente = ROOT_DIR.resolve(nomePasta);
        if (!Files.exists(pastaCliente))
            Files.createDirectories(pastaCliente);

        Path caminhoDestino = pastaCliente.resolve(novoNomeArquivo);

        // 3. Gravação Física (Sem risco de sobrescrever)
        Files.copy(arquivoOriginal.toPath(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);

        // 4. Registro no Prontuário (Banco de Dados)
        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setProponente(proponente);
        doc.setProposta(proposta);
        doc.setUsuario(consultor);
        doc.setTipoDocumento(tipoDoc);
        doc.setArquivoPath(caminhoDestino.toString());
        doc.setHashSha256(hash);
        doc.setVerificado(false);

        return repository.save(doc);
    }

    @Transactional
    public DocumentoProponenteModel atualizarTipoDocumento(Long documentoId, String novoTipo) throws Exception {
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        if (doc.getTipoDocumento().equals(novoTipo))
            return doc;

        Path caminhoAntigo = Paths.get(doc.getArquivoPath());

        if (Files.exists(caminhoAntigo)) {
            String extensao = obterExtensao(caminhoAntigo.getFileName().toString());
            // 🩹 APLICANDO A UNICIDADE TAMBÉM NA RENOMEAÇÃO
            String novoNomeArquivo = gerarNomeArquivoUnico(doc.getProponente().getNomeCompleto(), novoTipo, extensao);

            Path caminhoNovo = caminhoAntigo.getParent().resolve(novoNomeArquivo);
            Files.move(caminhoAntigo, caminhoNovo, StandardCopyOption.REPLACE_EXISTING);

            doc.setArquivoPath(caminhoNovo.toString());
        }

        doc.setTipoDocumento(novoTipo);
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
            log.error(("Erro ao abrir arquivo: " + e.getMessage()));
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
            log.error(("Aviso: Não foi possível deletar o arquivo físico: " + e.getMessage()));
        }

        repository.delete(doc);
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

    /**
     * Busca documentos vinculados especificamente a uma proposta (Esteira).
     */
    public List<DocumentoProponenteModel> buscarPorProposta(Long propostaId) {
        return repository.findByPropostaId(propostaId);
    }

    /**
     * Busca documentos gerais do proponente que não estão presos a nenhuma proposta
     * (Lead).
     */
    public List<DocumentoProponenteModel> buscarPorProponenteSemProposta(Long proponenteId) {
        return repository.findByProponenteIdAndPropostaIdIsNull(proponenteId);
    }
}