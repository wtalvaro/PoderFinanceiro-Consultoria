package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;

import java.math.BigDecimal;

/**
 * Object Mother para PropostaModel.
 * Localização: src/test/java (Exclusivo para testes).
 */
public class PropostaModelBuilder {

    private final PropostaModel instance;

    private PropostaModelBuilder() {
        instance = new PropostaModel();
        instance.setValorSolicitado(new BigDecimal("1000.00"));
        instance.setStatus(StatusPropostaModel.DIGITADA);
    }

    public static PropostaModelBuilder umaProposta() {
        return new PropostaModelBuilder();
    }

    public PropostaModelBuilder comId(Long id) {
        instance.setId(id);
        return this;
    }

    public PropostaModelBuilder comValor(String valor) {
        instance.setValorSolicitado(new BigDecimal(valor));
        return this;
    }

    public PropostaModelBuilder comStatus(StatusPropostaModel status) {
        instance.setStatus(status);
        return this;
    }

    /**
     * Vincula as entidades obrigatórias para persistência íntegra.
     */
    public PropostaModelBuilder vinculadoA(ProponenteModel p, BancoModel b, UsuarioModel u) {
        instance.setProponente(p);
        instance.setBanco(b);
        instance.setUsuario(u);
        return this;
    }

    public PropostaModel build() {
        return instance;
    }
}
