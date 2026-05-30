package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import java.util.ArrayList;

/**
 * Data Builder (Object Mother) para BancoModel.
 * Garante estados consistentes para os testes unitários.
 */
public class BancoModelBuilder {
    private final BancoModel instance;

    private BancoModelBuilder() {
        instance = new BancoModel();
        instance.setNome("Banco Padrão Teste");
        instance.setCodigo("000");
        instance.setAtivo(true);
        instance.setTabelas(new ArrayList<>());
    }

    public static BancoModelBuilder umBanco() {
        return new BancoModelBuilder();
    }

    public BancoModelBuilder comId(Long id) {
        instance.setId(id);
        return this;
    }

    public BancoModelBuilder comNome(String nome) {
        instance.setNome(nome);
        return this;
    }

    public BancoModelBuilder comCodigo(String codigo) {
        instance.setCodigo(codigo);
        return this;
    }

    public BancoModel build() {
        return instance;
    }
}
