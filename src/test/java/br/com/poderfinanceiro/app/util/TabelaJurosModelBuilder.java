package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import java.math.BigDecimal;

/**
 * Object Mother para TabelaJurosModel.
 * Garante que a tabela nasça com os campos mínimos para o motor de busca.
 */
public class TabelaJurosModelBuilder {
    private final TabelaJurosModel instance;

    private TabelaJurosModelBuilder() {
        instance = new TabelaJurosModel();
        instance.setNomeTabela("Tabela Teste");
        instance.setTipoConvenio(TipoConvenioModel.INSS_CONSIGNADO);
        instance.setTaxaMensal(new BigDecimal("1.80"));
        instance.setComissaoPercentual(new BigDecimal("12.00"));
        instance.setAtivo(true);
    }

    public static TabelaJurosModelBuilder umaTabela() {
        return new TabelaJurosModelBuilder();
    }

    public TabelaJurosModelBuilder comId(Long id) {
        instance.setId(id);
        return this;
    }

    public TabelaJurosModelBuilder comBanco(BancoModel banco) {
        instance.setBanco(banco);
        return this;
    }

    public TabelaJurosModelBuilder comNome(String nome) {
        instance.setNomeTabela(nome);
        return this;
    }

    public TabelaJurosModelBuilder comTaxa(String taxa) {
        instance.setTaxaMensal(new BigDecimal(taxa));
        return this;
    }

    public TabelaJurosModel build() {
        return instance;
    }
}
