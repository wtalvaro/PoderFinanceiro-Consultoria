package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;

/**
 * Object Mother para EnderecoProponenteModel.
 * Garante estados consistentes para testes de localização.
 */
public class EnderecoProponenteModelBuilder {
    private final EnderecoProponenteModel instance;

    private EnderecoProponenteModelBuilder() {
        instance = new EnderecoProponenteModel();
        instance.setCep("01001000");
        instance.setTipoLogradouro(TipoLogradouroModel.RUA);
        instance.setLogradouro("Praça da Sé");
        instance.setNumero("S/N");
        instance.setBairro("Sé");
        instance.setCidade("São Paulo");
        instance.setUf(UfModel.SP);
        instance.setPrincipal(true);
    }

    public static EnderecoProponenteModelBuilder umEndereco() {
        return new EnderecoProponenteModelBuilder();
    }

    public EnderecoProponenteModelBuilder comCep(String cep) {
        instance.setCep(cep);
        return this;
    }

    public EnderecoProponenteModelBuilder vinculadoA(ProponenteModel proponente) {
        instance.setProponente(proponente);
        return this;
    }

    public EnderecoProponenteModel build() {
        return instance;
    }
}
