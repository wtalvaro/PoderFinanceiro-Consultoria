package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Scope("prototype")
public class PropostaViewModel extends BaseViewModel<PropostaModel> {

    // --- 1. PROPERTIES ESPECÍFICAS DA PROPOSTA ---
    private final ObjectProperty<ProponenteModel> proponente = new SimpleObjectProperty<>();
    private final ObjectProperty<BancoModel> banco = new SimpleObjectProperty<>();
    private final ObjectProperty<UsuarioModel> usuario = new SimpleObjectProperty<>();

    private final ObjectProperty<TipoConvenioModel> convenio = new SimpleObjectProperty<>(TipoConvenioModel.PADRAO);
    private final ObjectProperty<BigDecimal> valorSolicitado = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> valorAprovado = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<Integer> prazoDesejado = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BigDecimal> taxaAplicada = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<Integer> quantidadeParcelas = new SimpleObjectProperty<>(1);

    private final ObjectProperty<StatusPropostaModel> status = new SimpleObjectProperty<>(StatusPropostaModel.DIGITADA);
    private final ObjectProperty<BigDecimal> valorParcela = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Propriedade crucial para o cálculo automatizado:
    private final ObjectProperty<Long> tabelaId = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> comissaoEstimada = new SimpleObjectProperty<>(BigDecimal.ZERO);

    private final StringProperty observacoes = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> dataSolicitacao = new SimpleObjectProperty<>();

    // --- 2. ESTADOS ORIGINAIS (Prontuário de Referência) ---
    private final ReadOnlyObjectWrapper<ProponenteModel> proponenteOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<BancoModel> bancoOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<TipoConvenioModel> convenioOriginal = new ReadOnlyObjectWrapper<>(
            TipoConvenioModel.PADRAO);
    private final ReadOnlyObjectWrapper<BigDecimal> valorSolicitadoOriginal = new ReadOnlyObjectWrapper<>(
            BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<Integer> prazoDesejadoOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<BigDecimal> valorAprovadoOriginal = new ReadOnlyObjectWrapper<>(
            BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<StatusPropostaModel> statusOriginal = new ReadOnlyObjectWrapper<>(
            StatusPropostaModel.DIGITADA);
    private final ReadOnlyObjectWrapper<Long> tabelaIdOriginal = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyStringWrapper observacoesOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Integer> quantidadeParcelasOriginal = new ReadOnlyObjectWrapper<>(1);
    private final ReadOnlyObjectWrapper<BigDecimal> valorParcelaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> taxaAplicadaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO
    // ==========================================================

    @Override
    protected void extrairId(PropostaModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(PropostaModel model) {
        proponente.set(model.getProponente());
        banco.set(model.getBanco());
        usuario.set(model.getUsuario());
        convenio.set(model.getConvenioOrgao() != null ? model.getConvenioOrgao() : TipoConvenioModel.PADRAO);
        valorSolicitado.set(model.getValorSolicitado() != null ? model.getValorSolicitado() : BigDecimal.ZERO);
        valorAprovado.set(model.getValorAprovado() != null ? model.getValorAprovado() : BigDecimal.ZERO);
        taxaAplicada.set(model.getTaxaAplicada() != null ? model.getTaxaAplicada() : BigDecimal.ZERO);
        quantidadeParcelas.set(model.getQuantidadeParcelas() != null ? model.getQuantidadeParcelas() : 0);
        status.set(model.getStatus() != null ? model.getStatus() : StatusPropostaModel.DIGITADA);
        valorParcela.set(model.getValorParcela() != null ? model.getValorParcela() : BigDecimal.ZERO);
        tabelaId.set(model.getTabelaId());
        comissaoEstimada.set(model.getComissaoEstimada() != null ? model.getComissaoEstimada() : BigDecimal.ZERO);
        observacoes.set(model.getObservacoes() != null ? model.getObservacoes() : "");
        dataSolicitacao.set(model.getDataSolicitacao());

        // 🚀 AQUI: Carrega o prazo desejado do banco
        prazoDesejado.set(model.getPrazoDesejado());
    }

    @Override
    protected void limparCampos() {
        proponente.set(null);
        banco.set(null);
        usuario.set(null);
        convenio.set(TipoConvenioModel.PADRAO);
        valorSolicitado.set(BigDecimal.ZERO);
        valorAprovado.set(BigDecimal.ZERO);
        taxaAplicada.set(BigDecimal.ZERO);
        quantidadeParcelas.set(1);
        status.set(StatusPropostaModel.DIGITADA);
        valorParcela.set(BigDecimal.ZERO);
        tabelaId.set(null);
        comissaoEstimada.set(BigDecimal.ZERO);
        observacoes.set("");
        dataSolicitacao.set(null);

        // 🚀 AQUI: Limpa o prazo desejado
        prazoDesejado.set(null);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        this.proponenteOriginal.set(proponente.get());
        this.bancoOriginal.set(banco.get());
        this.convenioOriginal.set(convenio.get());
        this.valorSolicitadoOriginal.set(valorSolicitado.get());
        this.valorAprovadoOriginal.set(valorAprovado.get());
        this.statusOriginal.set(status.get());
        this.tabelaIdOriginal.set(tabelaId.get());
        this.observacoesOriginal.set(observacoes.get());
        this.quantidadeParcelasOriginal.set(quantidadeParcelas.get());
        this.valorParcelaOriginal.set(valorParcela.get());
        this.taxaAplicadaOriginal.set(taxaAplicada.get());

        // 🚀 AQUI: Sincroniza o prazo desejado original
        this.prazoDesejadoOriginal.set(prazoDesejado.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !isProponenteIgual(proponente.get(), proponenteOriginal.get()) ||
                !isBancoIgual(banco.get(), bancoOriginal.get()) ||
                !Objects.equals(convenio.get(), convenioOriginal.get()) ||
                compareBigDecimal(valorSolicitado.get(), valorSolicitadoOriginal.get()) ||
                compareBigDecimal(valorAprovado.get(), valorAprovadoOriginal.get()) ||
                !Objects.equals(status.get(), statusOriginal.get()) ||
                !Objects.equals(tabelaId.get(), tabelaIdOriginal.get()) ||
                !Objects.equals(observacoes.get(), observacoesOriginal.get()) ||
                !Objects.equals(quantidadeParcelas.get(), quantidadeParcelasOriginal.get()) ||
                compareBigDecimal(valorParcela.get(), valorParcelaOriginal.get()) ||
                compareBigDecimal(taxaAplicada.get(), taxaAplicadaOriginal.get()) ||
                // 🚀 AQUI: Compara se o prazo desejado foi alterado na tela
                !Objects.equals(prazoDesejado.get(), prazoDesejadoOriginal.get());
    }

    @Override
    public PropostaModel atualizarModel(PropostaModel model) {
        if (model == null) {
            model = new PropostaModel();
        }
        model.setId(this.id.get());
        model.setProponente(this.proponente.get());
        model.setBanco(this.banco.get());
        model.setUsuario(this.usuario.get());
        model.setConvenioOrgao(this.convenio.get());
        model.setValorSolicitado(this.valorSolicitado.get());
        model.setValorAprovado(this.valorAprovado.get());
        model.setTaxaAplicada(this.taxaAplicada.get());
        model.setQuantidadeParcelas(this.quantidadeParcelas.get());
        model.setStatus(this.status.get());
        model.setValorParcela(this.valorParcela.get());
        model.setTabelaId(this.tabelaId.get());
        model.setComissaoEstimada(this.comissaoEstimada.get());
        model.setObservacoes(this.observacoes.get());
        model.setDataSolicitacao(this.dataSolicitacao.get());

        // 🚀 AQUI: Salva o dado da UI no model para ir pro banco
        model.setPrazoDesejado(this.prazoDesejado.get());

        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                proponente, banco, convenio, valorSolicitado, valorAprovado, status, tabelaId, observacoes,
                quantidadeParcelas, valorParcela, taxaAplicada,
                prazoDesejado, // 🚀 Adicionado

                proponenteOriginal.getReadOnlyProperty(),
                bancoOriginal.getReadOnlyProperty(),
                convenioOriginal.getReadOnlyProperty(),
                valorSolicitadoOriginal.getReadOnlyProperty(),
                valorAprovadoOriginal.getReadOnlyProperty(),
                statusOriginal.getReadOnlyProperty(),
                tabelaIdOriginal.getReadOnlyProperty(),
                observacoesOriginal.getReadOnlyProperty(),
                quantidadeParcelasOriginal.getReadOnlyProperty(),
                valorParcelaOriginal.getReadOnlyProperty(),
                taxaAplicadaOriginal.getReadOnlyProperty(),
                prazoDesejadoOriginal.getReadOnlyProperty() // 🚀 Adicionado
        };
    }

    // ==========================================================
    // REGRAS DE VALIDAÇÃO (O GATEKEEPER DE INTEGRIDADE)
    // ==========================================================

    @Override
    public boolean isValido() {
        // Validação estrita: Se qualquer um destes for nulo, o botão deve ficar
        // desabilitado.
        return this.banco.get() != null
                && this.tabelaId.get() != null
                && this.convenio.get() != null;
    }

    // ==========================================================
    // MÉTODOS DE COMPARAÇÃO INTERNA
    // ==========================================================

    private boolean isBancoIgual(BancoModel b1, BancoModel b2) {
        if (b1 == b2)
            return true;
        if (b1 == null || b2 == null)
            return false;
        // Compara apenas pelos IDs! O Proxy do Hibernate permite chamar getId() sem
        // estourar erro.
        return Objects.equals(b1.getId(), b2.getId());
    }

    private boolean isProponenteIgual(ProponenteModel p1, ProponenteModel p2) {
        if (p1 == p2)
            return true;
        if (p1 == null || p2 == null)
            return false;
        return Objects.equals(p1.getId(), p2.getId());
    }

    private boolean compareBigDecimal(BigDecimal val1, BigDecimal val2) {
        if (val1 == null && val2 == null)
            return false;
        if (val1 == null || val2 == null)
            return true;
        return val1.compareTo(val2) != 0;
    }

    // ==========================================================
    // GETTERS PARA BINDING NA UI
    // ==========================================================

    public ObjectProperty<ProponenteModel> proponenteProperty() {
        return proponente;
    }

    public ObjectProperty<BancoModel> bancoProperty() {
        return banco;
    }

    public ObjectProperty<TipoConvenioModel> convenioProperty() {
        return convenio;
    }

    public ObjectProperty<UsuarioModel> usuarioProperty() {
        return usuario;
    }

    public ObjectProperty<BigDecimal> valorSolicitadoProperty() {
        return valorSolicitado;
    }

    public ObjectProperty<BigDecimal> valorAprovadoProperty() {
        return valorAprovado;
    }

    public ObjectProperty<BigDecimal> taxaAplicadaProperty() {
        return taxaAplicada;
    }

    public ObjectProperty<Integer> quantidadeParcelasProperty() {
        return quantidadeParcelas;
    }

    public ObjectProperty<StatusPropostaModel> statusProperty() {
        return status;
    }

    public ObjectProperty<BigDecimal> valorParcelaProperty() {
        return valorParcela;
    }

    public ObjectProperty<Long> tabelaIdProperty() {
        return tabelaId;
    }

    public ObjectProperty<BigDecimal> comissaoEstimadaProperty() {
        return comissaoEstimada;
    }

    public StringProperty observacoesProperty() {
        return observacoes;
    }

    public ObjectProperty<LocalDate> dataSolicitacaoProperty() {
        return dataSolicitacao;
    }

    public ObjectProperty<Integer> prazoDesejadoProperty() {
        return prazoDesejado;
    }

}