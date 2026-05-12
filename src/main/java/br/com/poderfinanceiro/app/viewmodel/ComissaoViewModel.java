package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.ComissaoModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Scope("prototype")
public class ComissaoViewModel extends BaseViewModel<ComissaoModel> {

    // --- 1. PROPERTIES ESPECÍFICAS (Monitoramento Atual) ---
    private final ObjectProperty<PropostaModel> proposta = new SimpleObjectProperty<>();
    private final ObjectProperty<UsuarioModel> usuario = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> valorBrutoComissao = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> impostosRetidos = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> valorLiquidoConsultor = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<LocalDate> dataPrevisaoPagamento = new SimpleObjectProperty<>();
    private final StringProperty statusPagamento = new SimpleStringProperty("Pendente");
    private final ObjectProperty<LocalDateTime> dataRecebimento = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> valorPagoPelaPoder = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final BooleanProperty contestada = new SimpleBooleanProperty(false);

    // --- 2. ESTADOS ORIGINAIS (Prontuário de Referência para Dirty Checking) ---
    private final ReadOnlyObjectWrapper<PropostaModel> propostaOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<UsuarioModel> usuarioOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<BigDecimal> valorBrutoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> impostosOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> valorLiquidoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<LocalDate> dataPrevisaoOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyStringWrapper statusOriginal = new ReadOnlyStringWrapper("Pendente");
    private final ReadOnlyObjectWrapper<LocalDateTime> dataRecebimentoOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<BigDecimal> valorPagoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyBooleanWrapper contestadaOriginal = new ReadOnlyBooleanWrapper(false);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(ComissaoModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(ComissaoModel model) {
        proposta.set(model.getProposta());
        usuario.set(model.getUsuario());
        valorBrutoComissao.set(model.getValorBrutoComissao());
        impostosRetidos.set(model.getImpostosRetidos());
        valorLiquidoConsultor.set(model.getValorLiquidoConsultor());
        dataPrevisaoPagamento.set(model.getDataPrevisaoPagamento());
        statusPagamento.set(model.getStatusPagamento() != null ? model.getStatusPagamento() : "Pendente");
        dataRecebimento.set(model.getDataRecebimento());
        valorPagoPelaPoder.set(model.getValorPagoPelaPoder() != null ? model.getValorPagoPelaPoder() : BigDecimal.ZERO);
        contestada.set(model.isContestada());
    }

    @Override
    protected void limparCampos() {
        proposta.set(null);
        usuario.set(null);
        valorBrutoComissao.set(BigDecimal.ZERO);
        impostosRetidos.set(BigDecimal.ZERO);
        valorLiquidoConsultor.set(BigDecimal.ZERO);
        dataPrevisaoPagamento.set(null);
        statusPagamento.set("Pendente");
        dataRecebimento.set(null);
        valorPagoPelaPoder.set(BigDecimal.ZERO);
        contestada.set(false);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        propostaOriginal.set(proposta.get());
        usuarioOriginal.set(usuario.get());
        valorBrutoOriginal.set(valorBrutoComissao.get());
        impostosOriginal.set(impostosRetidos.get());
        valorLiquidoOriginal.set(valorLiquidoConsultor.get());
        dataPrevisaoOriginal.set(dataPrevisaoPagamento.get());
        statusOriginal.set(statusPagamento.get());
        dataRecebimentoOriginal.set(dataRecebimento.get());
        valorPagoOriginal.set(valorPagoPelaPoder.get());
        contestadaOriginal.set(contestada.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(proposta.get(), propostaOriginal.get()) ||
                !Objects.equals(usuario.get(), usuarioOriginal.get()) ||
                compareBigDecimal(valorBrutoComissao.get(), valorBrutoOriginal.get()) ||
                compareBigDecimal(impostosRetidos.get(), impostosOriginal.get()) ||
                compareBigDecimal(valorLiquidoConsultor.get(), valorLiquidoOriginal.get()) ||
                !Objects.equals(dataPrevisaoPagamento.get(), dataPrevisaoOriginal.get()) ||
                !Objects.equals(statusPagamento.get(), statusOriginal.get()) ||
                !Objects.equals(dataRecebimento.get(), dataRecebimentoOriginal.get()) ||
                compareBigDecimal(valorPagoPelaPoder.get(), valorPagoOriginal.get()) ||
                contestada.get() != contestadaOriginal.get();
    }

    /**
     * Auxiliar para comparar BigDecimals ignorando a escala (ex: 0.0 e 0.00)
     */
    private boolean compareBigDecimal(BigDecimal val1, BigDecimal val2) {
        if (val1 == null && val2 == null)
            return false;
        if (val1 == null || val2 == null)
            return true;
        return val1.compareTo(val2) != 0;
    }

    @Override
    public ComissaoModel atualizarModel(ComissaoModel model) {
        if (model == null) {
            model = new ComissaoModel();
        }
        model.setId(this.id.get());
        model.setProposta(this.proposta.get());
        model.setUsuario(this.usuario.get());
        model.setValorBrutoComissao(this.valorBrutoComissao.get());
        model.setImpostosRetidos(this.impostosRetidos.get());
        model.setValorLiquidoConsultor(this.valorLiquidoConsultor.get());
        model.setDataPrevisaoPagamento(this.dataPrevisaoPagamento.get());
        model.setStatusPagamento(this.statusPagamento.get());
        model.setDataRecebimento(this.dataRecebimento.get());
        model.setValorPagoPelaPoder(this.valorPagoPelaPoder.get());
        model.setContestada(this.contestada.get());

        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                proposta, usuario, valorBrutoComissao, impostosRetidos, valorLiquidoConsultor,
                dataPrevisaoPagamento, statusPagamento, dataRecebimento, valorPagoPelaPoder, contestada,
                propostaOriginal.getReadOnlyProperty(),
                usuarioOriginal.getReadOnlyProperty(),
                valorBrutoOriginal.getReadOnlyProperty(),
                impostosOriginal.getReadOnlyProperty(),
                valorLiquidoOriginal.getReadOnlyProperty(),
                dataPrevisaoOriginal.getReadOnlyProperty(),
                statusOriginal.getReadOnlyProperty(),
                dataRecebimentoOriginal.getReadOnlyProperty(),
                valorPagoOriginal.getReadOnlyProperty(),
                contestadaOriginal.getReadOnlyProperty()
        };
    }

    // ==========================================================
    // GETTERS DAS PROPERTIES (Binding com a UI)
    // ==========================================================

    public ObjectProperty<PropostaModel> propostaProperty() {
        return proposta;
    }

    public ObjectProperty<UsuarioModel> usuarioProperty() {
        return usuario;
    }

    public ObjectProperty<BigDecimal> valorBrutoComissaoProperty() {
        return valorBrutoComissao;
    }

    public ObjectProperty<BigDecimal> impostosRetidosProperty() {
        return impostosRetidos;
    }

    public ObjectProperty<BigDecimal> valorLiquidoConsultorProperty() {
        return valorLiquidoConsultor;
    }

    public ObjectProperty<LocalDate> dataPrevisaoPagamentoProperty() {
        return dataPrevisaoPagamento;
    }

    public StringProperty statusPagamentoProperty() {
        return statusPagamento;
    }

    public ObjectProperty<LocalDateTime> dataRecebimentoProperty() {
        return dataRecebimento;
    }

    public ObjectProperty<BigDecimal> valorPagoPelaPoderProperty() {
        return valorPagoPelaPoder;
    }

    public BooleanProperty contestadaProperty() {
        return contestada;
    }
}