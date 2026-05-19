package br.com.poderfinanceiro.app.viewmodel;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Component
@Scope("prototype")
@Getter // <-- O Lombok atua aqui, gerando todos os getters (ex: getNomeTabela())
        // automaticamente!
public class TabelaJurosViewModel extends BaseViewModel<TabelaJurosModel> {

    // --- 1. PROPERTIES DA TABELA DE JUROS ---
    private final StringProperty nomeTabela = new SimpleStringProperty("");
    private final ObjectProperty<TipoConvenioModel> tipoConvenio = new SimpleObjectProperty<>(TipoConvenioModel.PADRAO);
    private final ObjectProperty<BigDecimal> taxaMensal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> comissaoPercentual = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Adicionei os limites pois o simulador vai precisar deles para a "triagem"
    private final ObjectProperty<BigDecimal> valorMinimoEmprestimo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BigDecimal> valorMaximoEmprestimo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BancoModel> banco = new SimpleObjectProperty<>(null);

    // 🚀 UNIFICAÇÃO: Novas propriedades gráficas de limites de Renda, Prazos e
    // Idades
    private final ObjectProperty<BigDecimal> rendaMinima = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<Integer> prazoMinimo = new SimpleObjectProperty<>(1);
    private final ObjectProperty<Integer> prazoMaximo = new SimpleObjectProperty<>(96);
    private final ObjectProperty<Integer> idadeMinima = new SimpleObjectProperty<>(18);
    private final ObjectProperty<Integer> idadeMaxima = new SimpleObjectProperty<>(100);

    // --- 2. ESTADOS ORIGINAIS PARA DIRTY CHECKING ---
    private final ReadOnlyStringWrapper nomeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<TipoConvenioModel> convenioOriginal = new ReadOnlyObjectWrapper<>(
            TipoConvenioModel.PADRAO);
    private final ReadOnlyObjectWrapper<BigDecimal> taxaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> comissaoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> minEmprestimoOriginal = new ReadOnlyObjectWrapper<>(
            BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> maxEmprestimoOriginal = new ReadOnlyObjectWrapper<>(
            BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BancoModel> bancoOriginal = new ReadOnlyObjectWrapper<>(null);

    private final ReadOnlyObjectWrapper<BigDecimal> rendaMinOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<Integer> prazoMinOriginal = new ReadOnlyObjectWrapper<>(1);
    private final ReadOnlyObjectWrapper<Integer> prazoMaxOriginal = new ReadOnlyObjectWrapper<>(96);
    private final ReadOnlyObjectWrapper<Integer> idadeMinOriginal = new ReadOnlyObjectWrapper<>(18);
    private final ReadOnlyObjectWrapper<Integer> idadeMaxOriginal = new ReadOnlyObjectWrapper<>(100);
    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(TabelaJurosModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(TabelaJurosModel model) {
        nomeTabela.set(model.getNomeTabela() != null ? model.getNomeTabela() : "");
        tipoConvenio.set(model.getTipoConvenio() != null ? model.getTipoConvenio() : TipoConvenioModel.PADRAO);
        taxaMensal.set(model.getTaxaMensal() != null ? model.getTaxaMensal() : BigDecimal.ZERO);
        comissaoPercentual.set(model.getComissaoPercentual() != null ? model.getComissaoPercentual() : BigDecimal.ZERO);
        valorMinimoEmprestimo
                .set(model.getValorMinimoEmprestimo() != null ? model.getValorMinimoEmprestimo() : BigDecimal.ZERO);
        valorMaximoEmprestimo
                .set(model.getValorMaximoEmprestimo() != null ? model.getValorMaximoEmprestimo() : BigDecimal.ZERO);
        banco.set(model.getBanco());
        // Unificados
        rendaMinima.set(model.getRendaMinima() != null ? model.getRendaMinima() : BigDecimal.ZERO);
        prazoMinimo.set(model.getPrazoMinimo() != null ? model.getPrazoMinimo() : 1);
        prazoMaximo.set(model.getPrazoMaximo() != null ? model.getPrazoMaximo() : 96);
        idadeMinima.set(model.getIdadeMinima() != null ? model.getIdadeMinima() : 18);
        idadeMaxima.set(model.getIdadeMaxima() != null ? model.getIdadeMaxima() : 100);
    }

    @Override
    protected void limparCampos() {
        nomeTabela.set("");
        tipoConvenio.set(TipoConvenioModel.PADRAO);
        taxaMensal.set(BigDecimal.ZERO);
        comissaoPercentual.set(BigDecimal.ZERO);
        valorMinimoEmprestimo.set(null);
        valorMaximoEmprestimo.set(null);
        banco.set(null);
        // Unificados
        rendaMinima.set(BigDecimal.ZERO);
        prazoMinimo.set(1);
        prazoMaximo.set(96);
        idadeMinima.set(18);
        idadeMaxima.set(100);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        nomeOriginal.set(nomeTabela.get());
        convenioOriginal.set(tipoConvenio.get());
        taxaOriginal.set(taxaMensal.get());
        comissaoOriginal.set(comissaoPercentual.get());
        minEmprestimoOriginal.set(valorMinimoEmprestimo.get());
        maxEmprestimoOriginal.set(valorMaximoEmprestimo.get());
        bancoOriginal.set(banco.get());// Unificados
        rendaMinOriginal.set(rendaMinima.get());
        prazoMinOriginal.set(prazoMinimo.get());
        prazoMaxOriginal.set(prazoMaximo.get());
        idadeMinOriginal.set(idadeMinima.get());
        idadeMaxOriginal.set(idadeMaxima.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(nomeTabela.get(), nomeOriginal.get()) ||
                !Objects.equals(tipoConvenio.get(), convenioOriginal.get()) ||
                !valoresIguais(taxaMensal.get(), taxaOriginal.get()) ||
                !valoresIguais(comissaoPercentual.get(), comissaoOriginal.get()) ||
                !valoresIguais(valorMinimoEmprestimo.get(), minEmprestimoOriginal.get()) ||
                !valoresIguais(valorMaximoEmprestimo.get(), maxEmprestimoOriginal.get()) ||
                !Objects.equals(banco.get(), bancoOriginal.get()) ||
                // Checks Unificados
                !valoresIguais(rendaMinima.get(), rendaMinOriginal.get()) ||
                !Objects.equals(prazoMinimo.get(), prazoMinOriginal.get()) ||
                !Objects.equals(prazoMaximo.get(), prazoMaxOriginal.get()) ||
                !Objects.equals(idadeMinima.get(), idadeMinOriginal.get()) ||
                !Objects.equals(idadeMaxima.get(), idadeMaxOriginal.get());
    }

    @Override
    public TabelaJurosModel atualizarModel(TabelaJurosModel model) {
        if (model == null) {
            model = new TabelaJurosModel();
        }
        model.setId(this.id.get());
        model.setNomeTabela(this.nomeTabela.get());
        model.setTipoConvenio(this.tipoConvenio.get());
        model.setTaxaMensal(this.taxaMensal.get());
        model.setComissaoPercentual(this.comissaoPercentual.get());
        model.setValorMinimoEmprestimo(this.valorMinimoEmprestimo.get());
        model.setValorMaximoEmprestimo(this.valorMaximoEmprestimo.get());
        model.setBanco(this.banco.get());// Unificados
        model.setRendaMinima(this.rendaMinima.get());
        model.setPrazoMinimo(this.prazoMinimo.get());
        model.setPrazoMaximo(this.prazoMaximo.get());
        model.setIdadeMinima(this.idadeMinima.get());
        model.setIdadeMaxima(this.idadeMaxima.get());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                nomeTabela, tipoConvenio, taxaMensal, comissaoPercentual, valorMinimoEmprestimo, valorMaximoEmprestimo,
                banco,
                rendaMinima, prazoMinimo, prazoMaximo, idadeMinima, idadeMaxima,
                nomeOriginal.getReadOnlyProperty(), convenioOriginal.getReadOnlyProperty(),
                taxaOriginal.getReadOnlyProperty(), comissaoOriginal.getReadOnlyProperty(),
                minEmprestimoOriginal.getReadOnlyProperty(), maxEmprestimoOriginal.getReadOnlyProperty(),
                bancoOriginal.getReadOnlyProperty(), rendaMinOriginal.getReadOnlyProperty(),
                prazoMinOriginal.getReadOnlyProperty(), prazoMaxOriginal.getReadOnlyProperty(),
                idadeMinOriginal.getReadOnlyProperty(), idadeMaxOriginal.getReadOnlyProperty()
        };
    }

    @Override
    public boolean isValido() {
        // Por padrão, se não tiver regras específicas, retorna verdadeiro
        return true;
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