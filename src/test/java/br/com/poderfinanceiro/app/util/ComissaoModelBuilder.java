package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import java.math.BigDecimal;

public class ComissaoModelBuilder {

    private final ComissaoModel instance;

    private ComissaoModelBuilder() {
        instance = new ComissaoModel();
        instance.setStatusPagamento("Pendente");
        instance.setValorBrutoComissao(new BigDecimal("1000.00"));
        instance.setValorLiquidoConsultor(new BigDecimal("900.00"));
    }

    public static ComissaoModelBuilder umaComissao() {
        return new ComissaoModelBuilder();
    }

    public ComissaoModelBuilder comStatus(String status) {
        instance.setStatusPagamento(status);
        return this;
    }

    public ComissaoModelBuilder vinculadaA(UsuarioModel usuario, PropostaModel proposta) {
        instance.setUsuario(usuario);
        instance.setProposta(proposta);
        return this;
    }

    public ComissaoModel build() {
        return instance;
    }
}
