package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;

/**
 * Object Mother para DocumentoProponenteModel.
 * Garante estados consistentes para testes de persistência documental.
 */
public class DocumentoProponenteModelBuilder {
    private final DocumentoProponenteModel instance;

    private DocumentoProponenteModelBuilder() {
        instance = new DocumentoProponenteModel();
        instance.setTipoDocumento("RG");
        instance.setArquivoPath("/storage/documentos/teste.pdf");
        instance.setHashSha256("hash_padrao_teste_" + System.nanoTime());
        instance.setVerificado(false);
    }

    public static DocumentoProponenteModelBuilder umDocumento() {
        return new DocumentoProponenteModelBuilder();
    }

    public DocumentoProponenteModelBuilder comTipo(String tipo) {
        instance.setTipoDocumento(tipo);
        return this;
    }

    public DocumentoProponenteModelBuilder comHash(String hash) {
        instance.setHashSha256(hash);
        return this;
    }

    public DocumentoProponenteModelBuilder verificado() {
        instance.setVerificado(true);
        return this;
    }

    public DocumentoProponenteModelBuilder vinculadoA(ProponenteModel p, PropostaModel prop, UsuarioModel u) {
        instance.setProponente(p);
        instance.setProposta(prop);
        instance.setUsuario(u);
        return this;
    }

    public DocumentoProponenteModel build() {
        return instance;
    }
}
