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
        log.debug("[DOCUMENTO_SERVICE] Construtor: Inicializando serviço de documentos");
        try {
            if (!Files.exists(ROOT_DIR)) {
                Files.createDirectories(ROOT_DIR);
                log.info("[DOCUMENTO_SERVICE] Diretório raiz de documentos criado em: {}", ROOT_DIR.toAbsolutePath());
            } else {
                log.debug("[DOCUMENTO_SERVICE] Diretório raiz já existe: {}", ROOT_DIR.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("[DOCUMENTO_SERVICE] Erro ao criar diretório raiz de documentos: {}", e.getMessage(), e);
        }
    }

    /**
     * 🧪 GERADOR DE IDENTIDADE ÚNICA
     * Cria nomes como: RG_WAGNER_1715585400123.pdf
     */
    private String gerarNomeArquivoUnico(String nomeCompleto, String tipoDoc, String extensao) {
        String tipoSanitizado = tipoDoc.replaceAll("\\s+", "_").toUpperCase();
        String nomeSanitizado = nomeCompleto.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        long timestamp = System.currentTimeMillis();

        String nomeGerado = String.format("%s_%s_%d%s", tipoSanitizado, nomeSanitizado, timestamp, extensao);
        log.trace("[DOCUMENTO_SERVICE] gerarNomeArquivoUnico: '{}' -> '{}'", tipoDoc, nomeGerado);
        return nomeGerado;
    }

    public DocumentoProponenteModel processarUpload(File arquivoOriginal, String tipoDoc, ProponenteModel proponente,
            PropostaModel proposta) throws Exception {
        log.info(
                "[DOCUMENTO_SERVICE] processarUpload: Iniciando upload para proponente ID={}, tipoDoc='{}', arquivo='{}'",
                proponente != null ? proponente.getId() : "null", tipoDoc, arquivoOriginal.getName());

        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null) {
            log.error("[DOCUMENTO_SERVICE] processarUpload: Nenhum usuário logado - upload cancelado");
            throw new IllegalStateException("Nenhum usuário logado.");
        }
        log.debug("[DOCUMENTO_SERVICE] processarUpload: Consultor autenticado: ID={}", consultor.getId());

        if (proponente == null) {
            log.error("[DOCUMENTO_SERVICE] processarUpload: Proponente nulo - upload cancelado");
            throw new IllegalArgumentException("Proponente não informado.");
        }

        String hash = calcularHashSha256(arquivoOriginal);
        log.trace("[DOCUMENTO_SERVICE] processarUpload: Hash SHA-256 calculado: {}", hash);

        if (repository.findByHashSha256(hash).isPresent()) {
            log.warn("[DOCUMENTO_SERVICE] processarUpload: Documento duplicado detectado (hash={})", hash);
            throw new IllegalArgumentException("Este exato documento já foi anexado ao sistema.");
        }

        String extensao = obterExtensao(arquivoOriginal.getName());
        String novoNomeArquivo = gerarNomeArquivoUnico(proponente.getNomeCompleto(), tipoDoc, extensao);

        String nomePasta = String.format("CLIENTE_%03d_%s", proponente.getId(),
                proponente.getNomeCompleto().replaceAll("[^a-zA-Z0-9]", "_").toUpperCase());

        Path pastaCliente = ROOT_DIR.resolve(nomePasta);
        if (!Files.exists(pastaCliente)) {
            Files.createDirectories(pastaCliente);
            log.debug("[DOCUMENTO_SERVICE] processarUpload: Pasta do cliente criada: {}", pastaCliente);
        }

        Path caminhoDestino = pastaCliente.resolve(novoNomeArquivo);
        Files.copy(arquivoOriginal.toPath(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);
        log.debug("[DOCUMENTO_SERVICE] processarUpload: Arquivo copiado para: {}", caminhoDestino);

        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setProponente(proponente);
        doc.setProposta(proposta);
        doc.setUsuario(consultor);
        doc.setTipoDocumento(tipoDoc);
        doc.setArquivoPath(caminhoDestino.toString());
        doc.setHashSha256(hash);
        doc.setVerificado(false);

        DocumentoProponenteModel salvo = repository.save(doc);
        log.info("[DOCUMENTO_SERVICE] processarUpload: Documento salvo com ID={}, caminho={}", salvo.getId(),
                salvo.getArquivoPath());
        return salvo;
    }

    @Transactional
    public DocumentoProponenteModel atualizarTipoDocumento(Long documentoId, String novoTipo) throws Exception {
        log.debug("[DOCUMENTO_SERVICE] atualizarTipoDocumento: Atualizando tipo do documento ID={} para '{}'",
                documentoId, novoTipo);
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> {
                    log.warn("[DOCUMENTO_SERVICE] atualizarTipoDocumento: Documento ID={} não encontrado", documentoId);
                    return new IllegalArgumentException("Documento não encontrado.");
                });

        if (doc.getTipoDocumento().equals(novoTipo)) {
            log.trace("[DOCUMENTO_SERVICE] atualizarTipoDocumento: Tipo já é '{}', nenhuma alteração necessária",
                    novoTipo);
            return doc;
        }

        Path caminhoAntigo = Paths.get(doc.getArquivoPath());

        if (Files.exists(caminhoAntigo)) {
            String extensao = obterExtensao(caminhoAntigo.getFileName().toString());
            String novoNomeArquivo = gerarNomeArquivoUnico(doc.getProponente().getNomeCompleto(), novoTipo, extensao);
            Path caminhoNovo = caminhoAntigo.getParent().resolve(novoNomeArquivo);
            Files.move(caminhoAntigo, caminhoNovo, StandardCopyOption.REPLACE_EXISTING);
            log.debug("[DOCUMENTO_SERVICE] atualizarTipoDocumento: Arquivo renomeado de '{}' para '{}'",
                    caminhoAntigo.getFileName(), novoNomeArquivo);
            doc.setArquivoPath(caminhoNovo.toString());
        } else {
            log.warn(
                    "[DOCUMENTO_SERVICE] atualizarTipoDocumento: Arquivo físico não encontrado em '{}' - apenas alterando tipo no banco",
                    caminhoAntigo);
        }

        doc.setTipoDocumento(novoTipo);
        DocumentoProponenteModel atualizado = repository.save(doc);
        log.info("[DOCUMENTO_SERVICE] atualizarTipoDocumento: Documento ID={} atualizado para tipo='{}'",
                atualizado.getId(), atualizado.getTipoDocumento());
        return atualizado;
    }

    public List<DocumentoProponenteModel> listarDoProponente(Long proponenteId) {
        log.debug("[DOCUMENTO_SERVICE] listarDoProponente: Buscando documentos do proponente ID={}", proponenteId);
        List<DocumentoProponenteModel> docs = repository.findByProponenteIdOrderByIdAsc(proponenteId);
        log.info("[DOCUMENTO_SERVICE] listarDoProponente: {} documento(s) encontrado(s)", docs.size());
        return docs;
    }

    public void abrirDocumento(DocumentoProponenteModel doc) {
        log.debug("[DOCUMENTO_SERVICE] abrirDocumento: Solicitando abertura do documento ID={}, caminho={}",
                doc.getId(), doc.getArquivoPath());
        try {
            File file = new File(doc.getArquivoPath());
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                log.info("[DOCUMENTO_SERVICE] abrirDocumento: Arquivo aberto com sucesso: {}", file.getAbsolutePath());
            } else {
                log.warn(
                        "[DOCUMENTO_SERVICE] abrirDocumento: Arquivo não existe ou Desktop não suportado - path={}, exists={}, desktopSupported={}",
                        doc.getArquivoPath(), file.exists(), Desktop.isDesktopSupported());
            }
        } catch (Exception e) {
            log.error("[DOCUMENTO_SERVICE] abrirDocumento: Erro ao abrir arquivo: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public DocumentoProponenteModel alternarVerificacao(Long documentoId) {
        log.debug("[DOCUMENTO_SERVICE] alternarVerificacao: Alternando status de verificação do documento ID={}",
                documentoId);
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> {
                    log.warn("[DOCUMENTO_SERVICE] alternarVerificacao: Documento ID={} não encontrado", documentoId);
                    return new IllegalArgumentException("Documento não encontrado.");
                });

        boolean novoStatus = !doc.getVerificado();
        doc.setVerificado(novoStatus);
        DocumentoProponenteModel atualizado = repository.save(doc);
        log.info("[DOCUMENTO_SERVICE] alternarVerificacao: Documento ID={} teve verificação alterada para {}",
                documentoId, novoStatus);
        return atualizado;
    }

    @Transactional
    public void excluirDocumento(Long documentoId) {
        log.info("[DOCUMENTO_SERVICE] excluirDocumento: Iniciando exclusão do documento ID={}", documentoId);
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> {
                    log.warn("[DOCUMENTO_SERVICE] excluirDocumento: Documento ID={} não encontrado", documentoId);
                    return new IllegalArgumentException("Documento não encontrado.");
                });

        try {
            Files.deleteIfExists(Paths.get(doc.getArquivoPath()));
            log.debug("[DOCUMENTO_SERVICE] excluirDocumento: Arquivo físico removido: {}", doc.getArquivoPath());
        } catch (Exception e) {
            log.warn("[DOCUMENTO_SERVICE] excluirDocumento: Não foi possível deletar o arquivo físico: {}",
                    e.getMessage());
        }

        repository.delete(doc);
        log.info("[DOCUMENTO_SERVICE] excluirDocumento: Documento ID={} removido do banco de dados", documentoId);
    }

    // --- Utilitários Internos ---

    private String calcularHashSha256(File file) throws Exception {
        log.trace("[DOCUMENTO_SERVICE] calcularHashSha256: Calculando hash do arquivo '{}'", file.getName());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hash = sb.toString();
        log.trace("[DOCUMENTO_SERVICE] calcularHashSha256: Hash calculado: {}", hash);
        return hash;
    }

    private String obterExtensao(String nomeArquivo) {
        int i = nomeArquivo.lastIndexOf('.');
        String ext = (i > 0) ? nomeArquivo.substring(i) : "";
        log.trace("[DOCUMENTO_SERVICE] obterExtensao: '{}' -> '{}'", nomeArquivo, ext);
        return ext;
    }

    /**
     * Busca documentos vinculados especificamente a uma proposta (Esteira).
     */
    public List<DocumentoProponenteModel> buscarPorProposta(Long propostaId) {
        log.debug("[DOCUMENTO_SERVICE] buscarPorProposta: Buscando documentos para proposta ID={}", propostaId);
        List<DocumentoProponenteModel> docs = repository.findByPropostaId(propostaId);
        log.info("[DOCUMENTO_SERVICE] buscarPorProposta: {} documento(s) encontrado(s) para proposta ID={}",
                docs.size(), propostaId);
        return docs;
    }

    /**
     * Busca documentos gerais do proponente que não estão presos a nenhuma proposta
     * (Lead).
     */
    public List<DocumentoProponenteModel> buscarPorProponenteSemProposta(Long proponenteId) {
        log.debug(
                "[DOCUMENTO_SERVICE] buscarPorProponenteSemProposta: Buscando documentos sem proposta do proponente ID={}",
                proponenteId);
        List<DocumentoProponenteModel> docs = repository.findByProponenteIdAndPropostaIdIsNull(proponenteId);
        log.info("[DOCUMENTO_SERVICE] buscarPorProponenteSemProposta: {} documento(s) sem proposta encontrado(s)",
                docs.size());
        return docs;
    }
}