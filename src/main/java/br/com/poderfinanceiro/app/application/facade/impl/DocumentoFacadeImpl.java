package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.IDocumentoFacade;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.DocumentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Service
public class DocumentoFacadeImpl implements IDocumentoFacade {

    private static final Logger log = LoggerFactory.getLogger(DocumentoFacadeImpl.class);
    private static final String LOG_PREFIX = "[DocumentoFacade]";

    private final DocumentoService documentoService;

    public DocumentoFacadeImpl(DocumentoService documentoService) {
        this.documentoService = documentoService;
        log.debug("{} [SISTEMA] Facade de Documentos instanciada.", LOG_PREFIX);
    }

    @Override public List<DocumentoProponenteModel> listarDocumentosDoProponente(Long proponenteId) {
        log.trace("{} [TELEMETRIA] Solicitando listagem de documentos para o proponente ID: {}", LOG_PREFIX, proponenteId);
        return documentoService.listarDoProponente(proponenteId);
    }

    @Override @Transactional public DocumentoProponenteModel salvarNovoDocumento(File arquivo, String tipo, ProponenteModel proponente)
            throws Exception {
        log.info("{} [TELEMETRIA] Iniciando upload de documento. Tipo: {}, Proponente ID: {}", LOG_PREFIX, tipo, proponente.getId());

        if (arquivo == null || !arquivo.exists()) {
            log.warn("{} [NEGOCIO] Tentativa de salvar documento com arquivo físico inválido.", LOG_PREFIX);
            throw new IllegalArgumentException("O arquivo selecionado não é válido ou não existe.");
        }

        DocumentoProponenteModel salvo = documentoService.processarUpload(arquivo, tipo, proponente, null);
        log.info("{} [AUDITORIA] Documento salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
        return salvo;
    }

    @Override @Transactional public DocumentoProponenteModel atualizarTipoDocumento(Long docId, String novoTipo) throws Exception {
        log.info("{} [AUDITORIA] Atualizando tipo do documento ID: {} para '{}'", LOG_PREFIX, docId, novoTipo);
        return documentoService.atualizarTipoDocumento(docId, novoTipo);
    }

    @Override @Transactional public DocumentoProponenteModel alternarStatusVerificacao(Long docId) throws Exception {
        log.info("{} [AUDITORIA] Alternando status de verificação do documento ID: {}", LOG_PREFIX, docId);
        return documentoService.alternarVerificacao(docId);
    }

    @Override public void excluirDocumento(Long docId) {
        log.warn("{} [AUDITORIA] Solicitando exclusão do documento ID: {}", LOG_PREFIX, docId);
        documentoService.excluirDocumento(docId);
    }

    @Override public boolean validarExistenciaArquivoFisico(DocumentoProponenteModel doc) {
        if (doc == null || doc.getArquivoPath() == null)
            return false;
        boolean existe = new File(doc.getArquivoPath()).exists();
        log.trace("{} [SISTEMA] Verificação de existência física do arquivo ID {}: {}", LOG_PREFIX, doc.getId(), existe);
        return existe;
    }
}
