package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.infrastructure.config.DocumentoStorageResolver;
import br.com.poderfinanceiro.app.infrastructure.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Serviço de Domínio para Gestão de Documentos Digitais. Orquestra o ciclo de
 * vida de arquivos, garantindo integridade via SHA-256 e armazenamento
 * estruturado por proponente.
 */
@Service
@Transactional(readOnly = true)
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);
    private static final String LOG_PREFIX = "[DocumentoService]";

    private final DocumentoProponenteRepository repository;
    private final AuthService authService;
    private final DocumentoStorageResolver storageResolver;

    public DocumentoService(DocumentoProponenteRepository repository, AuthService authService,
            DocumentoStorageResolver storageResolver) {
        this.repository = repository;
        this.authService = authService;
        this.storageResolver = storageResolver;
        log.info("{} [SISTEMA] Serviço de documentos inicializado com armazenamento em: {}", LOG_PREFIX,
                storageResolver.getRootDir());
    }

    /**
     * Processa o upload de um novo documento, verificando duplicidade e
     * organizando em pastas.
     */
    @Transactional public DocumentoProponenteModel processarUpload(File arquivoOriginal, String tipoDoc,
            ProponenteModel proponente, PropostaModel proposta) throws Exception {

        log.info("{} [TELEMETRIA] Iniciando upload de documento. Tipo: {}, Proponente ID: {}", LOG_PREFIX, tipoDoc,
                proponente != null ? proponente.getId() : "NULL");

        // 1. Validações de Contexto
        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null) {
            log.error("{} [NEGOCIO] Upload abortado: Nenhum consultor logado.", LOG_PREFIX);
            throw new IllegalStateException("Sessão inválida.");
        }

        if (proponente == null) {
            log.error("{} [NEGOCIO] Upload abortado: Proponente não informado.", LOG_PREFIX);
            throw new IllegalArgumentException("Proponente obrigatório.");
        }

        // 2. Integridade e Duplicidade (SHA-256)
        String hash = calcularHashSha256(arquivoOriginal);
        if (repository.findByHashSha256(hash).isPresent()) {
            log.warn("{} [NEGOCIO] Documento duplicado detectado. Hash: {}", LOG_PREFIX, hash);
            throw new IllegalArgumentException("Este documento já foi anexado anteriormente.");
        }

        // 3. Preparação de Destino
        String extensao = obterExtensao(arquivoOriginal.getName());
        String novoNome = gerarNomeArquivoUnico(proponente.getNomeCompleto(), tipoDoc, extensao);
        Path pastaCliente = storageResolver.resolverPastaCliente(proponente.getId(), proponente.getNomeCompleto());
        Path caminhoDestino = pastaCliente.resolve(novoNome);

        // 4. Persistência Física (I/O Bloqueante - Loom Friendly)
        Files.copy(arquivoOriginal.toPath(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);
        log.debug("{} [SISTEMA] Arquivo físico persistido em: {}", LOG_PREFIX, caminhoDestino);

        // 5. Persistência em Banco de Dados
        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setProponente(proponente);
        doc.setProposta(proposta);
        doc.setUsuario(consultor);
        doc.setTipoDocumento(tipoDoc);
        doc.setArquivoPath(caminhoDestino.toString());
        doc.setHashSha256(hash);
        doc.setVerificado(false);

        DocumentoProponenteModel salvo = repository.save(doc);
        log.info("{} [AUDITORIA] Documento ID {} salvo com sucesso para o proponente {}.", LOG_PREFIX, salvo.getId(),
                proponente.getNomeCompleto());

        return salvo;
    }

    /**
     * Atualiza o tipo do documento e renomeia o arquivo físico para manter a
     * organização.
     */
    @Transactional public DocumentoProponenteModel atualizarTipoDocumento(Long documentoId, String novoTipo)
            throws Exception {
        log.info("{} [TELEMETRIA] Atualizando tipo do documento ID: {} para {}", LOG_PREFIX, documentoId, novoTipo);

        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        if (doc.getTipoDocumento().equalsIgnoreCase(novoTipo))
            return doc;

        Path caminhoAntigo = Paths.get(doc.getArquivoPath());
        if (Files.exists(caminhoAntigo)) {
            String extensao = obterExtensao(caminhoAntigo.getFileName().toString());
            String novoNome = gerarNomeArquivoUnico(doc.getProponente().getNomeCompleto(), novoTipo, extensao);
            Path caminhoNovo = caminhoAntigo.getParent().resolve(novoNome);

            Files.move(caminhoAntigo, caminhoNovo, StandardCopyOption.REPLACE_EXISTING);
            doc.setArquivoPath(caminhoNovo.toString());
            log.debug("{} [SISTEMA] Arquivo renomeado fisicamente para: {}", LOG_PREFIX, novoNome);
        }

        doc.setTipoDocumento(novoTipo);
        DocumentoProponenteModel atualizado = repository.save(doc);
        log.info("{} [AUDITORIA] Tipo de documento atualizado com sucesso. ID: {}", LOG_PREFIX, documentoId);

        return atualizado;
    }

    /**
     * Abre o documento no visualizador padrão do Sistema Operacional.
     * Orquestrado via AsyncUtils para evitar travamento da JavaFX Application
     * Thread.
     */
    public void abrirDocumento(DocumentoProponenteModel doc) {
        log.info("{} [TELEMETRIA] Solicitando abertura de documento ID: {}", LOG_PREFIX, doc.getId());

        AsyncUtils.executarTaskAsync(() -> {
            File file = new File(doc.getArquivoPath());
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                log.info("{} [AUDITORIA] Documento aberto no visualizador do SO: {}", LOG_PREFIX, file.getName());
            } else {
                log.warn("{} [SISTEMA] Falha ao abrir: Arquivo inexistente ou Desktop não suportado.", LOG_PREFIX);
            }
            return null;
        }, res -> {
        }, err -> log.error("{} [SISTEMA] Erro ao abrir documento: {}", LOG_PREFIX, err.getMessage()));
    }

    @Transactional public void excluirDocumento(Long documentoId) {
        log.info("{} [TELEMETRIA] Iniciando exclusão do documento ID: {}", LOG_PREFIX, documentoId);

        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        try {
            Files.deleteIfExists(Paths.get(doc.getArquivoPath()));
            log.debug("{} [SISTEMA] Arquivo físico removido com sucesso.", LOG_PREFIX);
        } catch (IOException e) {
            log.warn("{} [SISTEMA] Não foi possível remover o arquivo físico: {}", LOG_PREFIX, e.getMessage());
        }

        repository.delete(doc);
        log.info("{} [AUDITORIA] Documento ID {} removido do banco de dados.", LOG_PREFIX, documentoId);
    }

    @Transactional public DocumentoProponenteModel alternarVerificacao(Long documentoId) {
        log.debug("{} [NEGOCIO] Alternando status de verificação do documento ID: {}", LOG_PREFIX, documentoId);
        DocumentoProponenteModel doc = repository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        doc.setVerificado(!doc.getVerificado());
        return repository.save(doc);
    }

    public List<DocumentoProponenteModel> listarDoProponente(Long proponenteId) {
        return repository.findByProponenteIdOrderByIdAsc(proponenteId);
    }

    public List<DocumentoProponenteModel> buscarPorProposta(Long propostaId) {
        return repository.findByPropostaId(propostaId);
    }

    public List<DocumentoProponenteModel> buscarPorProponenteSemProposta(Long proponenteId) {
        return repository.findByProponenteIdAndPropostaIdIsNull(proponenteId);
    }

    // --- Utilitários Privados ---

    private String calcularHashSha256(File file) throws IOException, NoSuchAlgorithmException {
        log.trace("{} [SISTEMA] Calculando hash SHA-256 do arquivo: {}", LOG_PREFIX, file.getName());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return HexFormat.of().formatHex(digest.digest(fileBytes));
    }

    private String gerarNomeArquivoUnico(String nomeCompleto, String tipoDoc, String extensao) {
        String tipoSanitizado = tipoDoc.replaceAll("\\s+", "_").toUpperCase();
        String nomeSanitizado = nomeCompleto.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        return String.format("%s_%s_%d%s", tipoSanitizado, nomeSanitizado, System.currentTimeMillis(), extensao);
    }

    private String obterExtensao(String nomeArquivo) {
        int i = nomeArquivo.lastIndexOf('.');
        return (i > 0) ? nomeArquivo.substring(i) : "";
    }
}
