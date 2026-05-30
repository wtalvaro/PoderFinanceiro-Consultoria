package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import java.util.ArrayList;

/**
 * Object Mother para ProponenteModel.
 * Localização: src/test/java (Exclusivo para testes).
 */
public class ProponenteModelBuilder {
    private final ProponenteModel instance;

    private ProponenteModelBuilder() {
        instance = new ProponenteModel();
        instance.setNomeCompleto("Cliente Teste");
        instance.setCpf("000.000.000-00");
        instance.setEnderecos(new ArrayList<>());
    }

    public static ProponenteModelBuilder umProponente() {
        return new ProponenteModelBuilder();
    }

    public ProponenteModelBuilder comId(Long id) {
        instance.setId(id);
        return this;
    }

    public ProponenteModelBuilder comCpf(String cpf) {
        instance.setCpf(cpf);
        return this;
    }

    public ProponenteModelBuilder comNome(String nome) {
        instance.setNomeCompleto(nome);
        return this;
    }

    public ProponenteModel build() {
        return instance;
    }
}
