package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.TabelaJuros;
import br.com.poderfinanceiro.app.model.enums.TipoConvenio;
import javafx.beans.Observable;
import javafx.beans.property.*;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
@Scope("prototype")
@Getter // <-- O Lombok atua aqui, gerando todos os getters (ex: getNomeTabela())
        // automaticamente!
public class TabelaJurosViewModel extends BaseViewModel<TabelaJuros> {

    // --- 1. PROPERTIES DA TABELA DE JUROS ---
    private final StringProperty nomeTabela = new SimpleStringProperty("");
    private final ObjectProperty<TipoConvenio> tipoConvenio = new SimpleObjectProperty<>(TipoConvenio.PADRAO);
    private final ObjectProperty<BigDecimal> taxaMensal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> comissaoPercentual = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Adicionei os limites pois o simulador vai precisar deles para a "triagem"
    private final ObjectProperty<BigDecimal> valorMinimoEmprestimo = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> valorMaximoEmprestimo = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // --- 2. ESTADOS ORIGINAIS PARA DIRTY CHECKING ---
    private final ReadOnlyStringWrapper nomeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<TipoConvenio> convenioOriginal = new ReadOnlyObjectWrapper<>(
            TipoConvenio.PADRAO);
    private final ReadOnlyObjectWrapper<BigDecimal> taxaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> comissaoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> minEmprestimoOriginal = new ReadOnlyObjectWrapper<>(
            BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> maxEmprestimoOriginal = new ReadOnlyObjectWrapper<>(
            BigDecimal.ZERO);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(TabelaJuros model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(TabelaJuros model) {
        nomeTabela.set(model.getNomeTabela() != null ? model.getNomeTabela() : "");
        tipoConvenio.set(model.getTipoConvenio() != null ? model.getTipoConvenio() : TipoConvenio.PADRAO);
        taxaMensal.set(model.getTaxaMensal() != null ? model.getTaxaMensal() : BigDecimal.ZERO);
        comissaoPercentual.set(model.getComissaoPercentual() != null ? model.getComissaoPercentual() : BigDecimal.ZERO);
        valorMinimoEmprestimo
                .set(model.getValorMinimoEmprestimo() != null ? model.getValorMinimoEmprestimo() : BigDecimal.ZERO);
        valorMaximoEmprestimo
                .set(model.getValorMaximoEmprestimo() != null ? model.getValorMaximoEmprestimo() : BigDecimal.ZERO);
    }

    @Override
    protected void limparCampos() {
        nomeTabela.set("");
        tipoConvenio.set(TipoConvenio.PADRAO);
        taxaMensal.set(BigDecimal.ZERO);
        comissaoPercentual.set(BigDecimal.ZERO);
        valorMinimoEmprestimo.set(BigDecimal.ZERO);
        valorMaximoEmprestimo.set(BigDecimal.ZERO);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        nomeOriginal.set(nomeTabela.get());
        convenioOriginal.set(tipoConvenio.get());
        taxaOriginal.set(taxaMensal.get());
        comissaoOriginal.set(comissaoPercentual.get());
        minEmprestimoOriginal.set(valorMinimoEmprestimo.get());
        maxEmprestimoOriginal.set(valorMaximoEmprestimo.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(nomeTabela.get(), nomeOriginal.get()) ||
                !Objects.equals(tipoConvenio.get(), convenioOriginal.get()) ||
                !valoresIguais(taxaMensal.get(), taxaOriginal.get()) ||
                !valoresIguais(comissaoPercentual.get(), comissaoOriginal.get()) ||
                !valoresIguais(valorMinimoEmprestimo.get(), minEmprestimoOriginal.get()) ||
                !valoresIguais(valorMaximoEmprestimo.get(), maxEmprestimoOriginal.get());
    }

    @Override
    public TabelaJuros atualizarModel(TabelaJuros model) {
        if (model == null) {
            model = new TabelaJuros();
        }
        model.setId(this.id.get());
        model.setNomeTabela(this.nomeTabela.get());
        model.setTipoConvenio(this.tipoConvenio.get());
        model.setTaxaMensal(this.taxaMensal.get());
        model.setComissaoPercentual(this.comissaoPercentual.get());
        model.setValorMinimoEmprestimo(this.valorMinimoEmprestimo.get());
        model.setValorMaximoEmprestimo(this.valorMaximoEmprestimo.get());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                nomeTabela, tipoConvenio, taxaMensal, comissaoPercentual, valorMinimoEmprestimo, valorMaximoEmprestimo,
                nomeOriginal.getReadOnlyProperty(),
                convenioOriginal.getReadOnlyProperty(),
                taxaOriginal.getReadOnlyProperty(),
                comissaoOriginal.getReadOnlyProperty(),
                minEmprestimoOriginal.getReadOnlyProperty(),
                maxEmprestimoOriginal.getReadOnlyProperty()
        };
    }

    // --- MÉTODOS AUXILIARES (Bisturis) ---

    /**
     * Compara dois BigDecimals ignorando a escala (ex: 2.0 é igual a 2.00).
     */
    private boolean valoresIguais(BigDecimal v1, BigDecimal v2) {
        if (v1 == null && v2 == null)
            return true;
        if (v1 == null || v2 == null)
            return false;
        return v1.compareTo(v2) == 0;
    }
}