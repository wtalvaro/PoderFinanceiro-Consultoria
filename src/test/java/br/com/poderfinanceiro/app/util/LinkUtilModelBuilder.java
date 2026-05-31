package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;

/**
 * Object Mother para LinkUtilModel.
 * Garante estados consistentes para testes de base de conhecimento.
 */
public class LinkUtilModelBuilder {
    private final LinkUtilModel instance;

    private LinkUtilModelBuilder() {
        instance = new LinkUtilModel();
        instance.setTitulo("Link de Teste");
        instance.setUrl("https://exemplo.com");
        instance.setCategoria(CategoriaLinkModel.OUTROS);
        instance.setTags("teste, utilitario");
    }

    public static LinkUtilModelBuilder umLink() {
        return new LinkUtilModelBuilder();
    }

    public LinkUtilModelBuilder comTitulo(String titulo) {
        instance.setTitulo(titulo);
        return this;
    }

    public LinkUtilModelBuilder comCategoria(CategoriaLinkModel categoria) {
        instance.setCategoria(categoria);
        return this;
    }

    public LinkUtilModelBuilder comTags(String tags) {
        instance.setTags(tags);
        return this;
    }

    public LinkUtilModel build() {
        return instance;
    }
}
