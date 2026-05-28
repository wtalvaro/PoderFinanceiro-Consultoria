package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;

import java.io.File;
import java.util.List;

public interface IDocumentoFacade {

    // --- Consultas ---
    List<DocumentoProponenteModel> listarDocumentosDoProponente(Long proponenteId);

    // --- Operações de Arquivo e Banco ---
    DocumentoProponenteModel salvarNovoDocumento(File arquivo, String tipo, ProponenteModel proponente) throws Exception;

    DocumentoProponenteModel atualizarTipoDocumento(Long docId, String novoTipo) throws Exception;

    DocumentoProponenteModel alternarStatusVerificacao(Long docId) throws Exception;

    void excluirDocumento(Long docId);

    // --- Utilitários ---
    boolean validarExistenciaArquivoFisico(DocumentoProponenteModel doc);
}
