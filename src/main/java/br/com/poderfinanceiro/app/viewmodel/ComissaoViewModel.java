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
    private final ObjectProperty<BigDecimal> valorPagoPelaPoder = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final StringProperty statusPagamento = new SimpleStringProperty("Pendente");
    private final BooleanProperty contestada = new SimpleBooleanProperty(false);

    // Marcos Temporais do Ciclo
    private final ObjectProperty<LocalDateTime> dataRecebimentoBanco = new SimpleObjectProperty<>();
    private final BooleanProperty verificadoConsultor = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> dataVerificacao = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> previsaoPagamento = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> dataPagamentoConsultor = new SimpleObjectProperty<>();

    // --- 2. ESTADOS ORIGINAIS (Referência para Dirty Checking) ---
    private final ReadOnlyObjectWrapper<PropostaModel> propostaOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<UsuarioModel> usuarioOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<BigDecimal> valorBrutoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> valorLiquidoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> valorPagoOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyStringWrapper statusOriginal = new ReadOnlyStringWrapper("Pendente");
    private final ReadOnlyBooleanWrapper contestadaOriginal = new ReadOnlyBooleanWrapper(false);

    private final ReadOnlyObjectWrapper<LocalDateTime> dataBancoOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper verificadoOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<LocalDate> previsaoOriginal = new ReadOnlyObjectWrapper<>();

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
        valorPagoPelaPoder.set(model.getValorPagoPelaPoder() != null ? model.getValorPagoPelaPoder() : BigDecimal.ZERO);
        statusPagamento.set(model.getStatusPagamento() != null ? model.getStatusPagamento() : "Pendente");
        contestada.set(model.isContestada());

        // Ciclo Financeiro
        dataRecebimentoBanco.set(model.getDataRecebimentoBanco());
        verificadoConsultor.set(model.isVerificadoConsultor());
        dataVerificacao.set(model.getDataVerificacao());
        previsaoPagamento.set(model.getPrevisaoPagamento());
        dataPagamentoConsultor.set(model.getDataPagamentoConsultor());
    }

    @Override
    protected void limparCampos() {
        proposta.set(null);
        usuario.set(null);
        valorBrutoComissao.set(BigDecimal.ZERO);
        impostosRetidos.set(BigDecimal.ZERO);
        valorLiquidoConsultor.set(BigDecimal.ZERO);
        valorPagoPelaPoder.set(BigDecimal.ZERO);
        statusPagamento.set("Pendente");
        contestada.set(false);
        dataRecebimentoBanco.set(null);
        verificadoConsultor.set(false);
        dataVerificacao.set(null);
        previsaoPagamento.set(null);
        dataPagamentoConsultor.set(null);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        propostaOriginal.set(proposta.get());
        usuarioOriginal.set(usuario.get());
        valorBrutoOriginal.set(valorBrutoComissao.get());
        valorLiquidoOriginal.set(valorLiquidoConsultor.get());
        valorPagoOriginal.set(valorPagoPelaPoder.get());
        statusOriginal.set(statusPagamento.get());
        contestadaOriginal.set(contestada.get());

        dataBancoOriginal.set(dataRecebimentoBanco.get());
        verificadoOriginal.set(verificadoConsultor.get());
        previsaoOriginal.set(previsaoPagamento.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(proposta.get(), propostaOriginal.get()) ||
                compareBigDecimal(valorBrutoComissao.get(), valorBrutoOriginal.get()) ||
                compareBigDecimal(valorPagoPelaPoder.get(), valorPagoOriginal.get()) ||
                !Objects.equals(statusPagamento.get(), statusOriginal.get()) ||
                !Objects.equals(dataRecebimentoBanco.get(), dataBancoOriginal.get()) ||
                verificadoConsultor.get() != verificadoOriginal.get() ||
                !Objects.equals(previsaoPagamento.get(), previsaoOriginal.get()) ||
                contestada.get() != contestadaOriginal.get();
    }

    @Override
    public ComissaoModel atualizarModel(ComissaoModel model) {
        if (model == null)
            model = new ComissaoModel();

        model.setId(this.id.get());
        model.setProposta(this.proposta.get());
        model.setUsuario(this.usuario.get());
        model.setValorBrutoComissao(this.valorBrutoComissao.get());
        model.setImpostosRetidos(this.impostosRetidos.get());
        model.setValorLiquidoConsultor(this.valorLiquidoConsultor.get());
        model.setValorPagoPelaPoder(this.valorPagoPelaPoder.get());
        model.setContestada(this.contestada.get());
        model.setStatusPagamento(this.statusPagamento.get());

        // Ciclo
        model.setDataRecebimentoBanco(this.dataRecebimentoBanco.get());
        model.setVerificadoConsultor(this.verificadoConsultor.get());
        model.setDataVerificacao(this.dataVerificacao.get());
        model.setPrevisaoPagamento(this.previsaoPagamento.get());
        model.setDataPagamentoConsultor(this.dataPagamentoConsultor.get());

        return model;
    }

    private boolean compareBigDecimal(BigDecimal val1, BigDecimal val2) {
        if (val1 == null && val2 == null)
            return false;
        if (val1 == null || val2 == null)
            return true;
        return val1.compareTo(val2) != 0;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                proposta, valorBrutoComissao, statusPagamento, dataRecebimentoBanco,
                verificadoConsultor, previsaoPagamento, contestada,
                propostaOriginal.getReadOnlyProperty(),
                valorBrutoOriginal.getReadOnlyProperty(),
                statusOriginal.getReadOnlyProperty(),
                dataBancoOriginal.getReadOnlyProperty(),
                verificadoOriginal.getReadOnlyProperty(),
                previsaoOriginal.getReadOnlyProperty(),
                contestadaOriginal.getReadOnlyProperty()
        };
    }

    // --- Getters das Properties (Binding) ---
    public ObjectProperty<PropostaModel> propostaProperty() {
        return proposta;
    }

    public ObjectProperty<UsuarioModel> usuarioProperty() {
        return usuario;
    }

    public ObjectProperty<BigDecimal> valorBrutoComissaoProperty() {
        return valorBrutoComissao;
    }

    public StringProperty statusPagamentoProperty() {
        return statusPagamento;
    }

    public BooleanProperty verificadoConsultorProperty() {
        return verificadoConsultor;
    }

    public ObjectProperty<LocalDateTime> dataRecebimentoBancoProperty() {
        return dataRecebimentoBanco;
    }

    public ObjectProperty<LocalDate> previsaoPagamentoProperty() {
        return previsaoPagamento;
    }

    public BooleanProperty contestadaProperty() {
        return contestada;
    }

    public ObjectProperty<BigDecimal> valorPagoPelaPoderProperty() {
        return valorPagoPelaPoder;
    }
}