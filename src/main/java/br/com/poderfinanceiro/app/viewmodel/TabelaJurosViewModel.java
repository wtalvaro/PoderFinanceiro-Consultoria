package br.com.poderfinanceiro.app.viewmodel;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
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
@Getter
public class TabelaJurosViewModel extends BaseViewModel<TabelaJurosModel> {

    private static final Logger log = LoggerFactory.getLogger(TabelaJurosViewModel.class);

    // --- 1. PROPERTIES DA TABELA DE JUROS ---
    private final StringProperty nomeTabela = new SimpleStringProperty("");
    private final ObjectProperty<TipoConvenioModel> tipoConvenio = new SimpleObjectProperty<>(TipoConvenioModel.PADRAO);
    private final ObjectProperty<BigDecimal> taxaMensal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> comissaoPercentual = new SimpleObjectProperty<>(BigDecimal.ZERO);

    private final ObjectProperty<BigDecimal> valorMinimoEmprestimo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BigDecimal> valorMaximoEmprestimo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BancoModel> banco = new SimpleObjectProperty<>(null);

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

    // Construtor adicionado para log (não quebra contrato)
    public TabelaJurosViewModel() {
        log.debug("[TABELA_JUROS_VM] Instância criada (prototype)");
    }

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(TabelaJurosModel model) {
        log.debug("[TABELA_JUROS_VM] extrairId: model ID={}", model != null ? model.getId() : "null");
        this.id.set(model != null ? model.getId() : null);
    }

    @Override
    protected void preencherCampos(TabelaJurosModel model) {
        log.debug("[TABELA_JUROS_VM] preencherCampos: populando campos a partir do model ID={}",
                model != null ? model.getId() : "null");
        if (model == null) {
            limparCampos();
            return;
        }
        nomeTabela.set(model.getNomeTabela() != null ? model.getNomeTabela() : "");
        tipoConvenio.set(model.getTipoConvenio() != null ? model.getTipoConvenio() : TipoConvenioModel.PADRAO);
        taxaMensal.set(model.getTaxaMensal() != null ? model.getTaxaMensal() : BigDecimal.ZERO);
        comissaoPercentual.set(model.getComissaoPercentual() != null ? model.getComissaoPercentual() : BigDecimal.ZERO);
        valorMinimoEmprestimo
                .set(model.getValorMinimoEmprestimo() != null ? model.getValorMinimoEmprestimo() : BigDecimal.ZERO);
        valorMaximoEmprestimo
                .set(model.getValorMaximoEmprestimo() != null ? model.getValorMaximoEmprestimo() : BigDecimal.ZERO);
        banco.set(model.getBanco());
        rendaMinima.set(model.getRendaMinima() != null ? model.getRendaMinima() : BigDecimal.ZERO);
        prazoMinimo.set(model.getPrazoMinimo() != null ? model.getPrazoMinimo() : 1);
        prazoMaximo.set(model.getPrazoMaximo() != null ? model.getPrazoMaximo() : 96);
        idadeMinima.set(model.getIdadeMinima() != null ? model.getIdadeMinima() : 18);
        idadeMaxima.set(model.getIdadeMaxima() != null ? model.getIdadeMaxima() : 100);
        log.trace("[TABELA_JUROS_VM] Campos preenchidos: nome='{}', banco='{}', taxa={}", nomeTabela.get(),
                banco.get() != null ? banco.get().getNome() : "null", taxaMensal.get());
    }

    @Override
    protected void limparCampos() {
        log.debug("[TABELA_JUROS_VM] limparCampos: resetando todos os campos");
        nomeTabela.set("");
        tipoConvenio.set(TipoConvenioModel.PADRAO);
        taxaMensal.set(BigDecimal.ZERO);
        comissaoPercentual.set(BigDecimal.ZERO);
        valorMinimoEmprestimo.set(null);
        valorMaximoEmprestimo.set(null);
        banco.set(null);
        rendaMinima.set(BigDecimal.ZERO);
        prazoMinimo.set(1);
        prazoMaximo.set(96);
        idadeMinima.set(18);
        idadeMaxima.set(100);
        log.trace("[TABELA_JUROS_VM] Campos limpos");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        log.debug("[TABELA_JUROS_VM] sincronizarEstadoOriginal: salvando estado atual como original");
        nomeOriginal.set(nomeTabela.get());
        convenioOriginal.set(tipoConvenio.get());
        taxaOriginal.set(taxaMensal.get());
        comissaoOriginal.set(comissaoPercentual.get());
        minEmprestimoOriginal.set(valorMinimoEmprestimo.get());
        maxEmprestimoOriginal.set(valorMaximoEmprestimo.get());
        bancoOriginal.set(banco.get());
        rendaMinOriginal.set(rendaMinima.get());
        prazoMinOriginal.set(prazoMinimo.get());
        prazoMaxOriginal.set(prazoMaximo.get());
        idadeMinOriginal.set(idadeMinima.get());
        idadeMaxOriginal.set(idadeMaxima.get());
        log.trace("[TABELA_JUROS_VM] Estado original salvo: nome='{}', banco='{}'", nomeOriginal.get(),
                bancoOriginal.get() != null ? bancoOriginal.get().getNome() : "null");
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        boolean alterado = !Objects.equals(nomeTabela.get(), nomeOriginal.get()) ||
                !Objects.equals(tipoConvenio.get(), convenioOriginal.get()) ||
                !valoresIguais(taxaMensal.get(), taxaOriginal.get()) ||
                !valoresIguais(comissaoPercentual.get(), comissaoOriginal.get()) ||
                !valoresIguais(valorMinimoEmprestimo.get(), minEmprestimoOriginal.get()) ||
                !valoresIguais(valorMaximoEmprestimo.get(), maxEmprestimoOriginal.get()) ||
                !Objects.equals(banco.get(), bancoOriginal.get()) ||
                !valoresIguais(rendaMinima.get(), rendaMinOriginal.get()) ||
                !Objects.equals(prazoMinimo.get(), prazoMinOriginal.get()) ||
                !Objects.equals(prazoMaximo.get(), prazoMaxOriginal.get()) ||
                !Objects.equals(idadeMinima.get(), idadeMinOriginal.get()) ||
                !Objects.equals(idadeMaxima.get(), idadeMaxOriginal.get());
        log.trace("[TABELA_JUROS_VM] temAlteracoesPendentes: {}", alterado);
        return alterado;
    }

    @Override
    public TabelaJurosModel atualizarModel(TabelaJurosModel model) {
        log.debug("[TABELA_JUROS_VM] atualizarModel: model fornecido = {}", model != null ? "presente" : "null");
        if (model == null) {
            log.trace("[TABELA_JUROS_VM] Criando novo model (null -> novo)");
            model = new TabelaJurosModel();
        }
        model.setId(this.id.get());
        model.setNomeTabela(this.nomeTabela.get());
        model.setTipoConvenio(this.tipoConvenio.get());
        model.setTaxaMensal(this.taxaMensal.get());
        model.setComissaoPercentual(this.comissaoPercentual.get());
        model.setValorMinimoEmprestimo(this.valorMinimoEmprestimo.get());
        model.setValorMaximoEmprestimo(this.valorMaximoEmprestimo.get());
        model.setBanco(this.banco.get());
        model.setRendaMinima(this.rendaMinima.get());
        model.setPrazoMinimo(this.prazoMinimo.get());
        model.setPrazoMaximo(this.prazoMaximo.get());
        model.setIdadeMinima(this.idadeMinima.get());
        model.setIdadeMaxima(this.idadeMaxima.get());

        log.info("[TABELA_JUROS_VM] Model atualizado: ID={}, nome='{}', banco='{}', taxa={}, comissão={}",
                model.getId(), model.getNomeTabela(),
                model.getBanco() != null ? model.getBanco().getNome() : "null",
                model.getTaxaMensal(), model.getComissaoPercentual());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        Observable[] observaveis = new Observable[] {
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
        log.trace("[TABELA_JUROS_VM] getObservaveisParaDirty: {} observáveis registrados", observaveis.length);
        return observaveis;
    }

    @Override
    public boolean isValido() {
        log.trace("[TABELA_JUROS_VM] isValido: sempre verdadeiro (sem regras específicas)");
        return true;
    }

    // --- MÉTODOS AUXILIARES (Bisturis) ---

    private boolean valoresIguais(BigDecimal v1, BigDecimal v2) {
        if (v1 == null && v2 == null)
            return true;
        if (v1 == null || v2 == null)
            return false;
        boolean iguais = v1.compareTo(v2) == 0;
        log.trace("[TABELA_JUROS_VM] valoresIguais: {} vs {} -> {}", v1, v2, iguais);
        return iguais;
    }
}