package br.com.poderfinanceiro.app.viewmodel;

import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Scope("prototype")
public class PropostaViewModel extends BaseViewModel<PropostaModel> {

    private static final Logger log = LoggerFactory.getLogger(PropostaViewModel.class);

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

    // Construtor adicionado para log (não quebra contrato, pois era implícito)
    public PropostaViewModel() {
        log.debug("[PROPOSTA_VM] Instância criada (prototype)");
    }

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO
    // ==========================================================

    @Override
    protected void extrairId(PropostaModel model) {
        log.debug("[PROPOSTA_VM] extrairId: model ID={}", model != null ? model.getId() : "null");
        this.id.set(model != null ? model.getId() : null);
    }

    @Override
    protected void preencherCampos(PropostaModel model) {
        log.debug("[PROPOSTA_VM] preencherCampos: populando campos a partir do model ID={}",
                model != null ? model.getId() : "null");
        if (model == null) {
            log.warn("[PROPOSTA_VM] preencherCampos: modelo nulo recebido, limpando campos");
            limparCampos();
            return;
        }
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
        prazoDesejado.set(model.getPrazoDesejado());
        log.trace("[PROPOSTA_VM] Campos preenchidos: status='{}', convenio='{}', valorSolicitado={}", status.get(),
                convenio.get(), valorSolicitado.get());
    }

    @Override
    protected void limparCampos() {
        log.debug("[PROPOSTA_VM] limparCampos: resetando todos os campos");
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
        prazoDesejado.set(null);
        log.trace("[PROPOSTA_VM] Campos limpos");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        log.debug("[PROPOSTA_VM] sincronizarEstadoOriginal: salvando estado atual como original");
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
        this.prazoDesejadoOriginal.set(prazoDesejado.get());
        log.trace("[PROPOSTA_VM] Estado original salvo: status='{}', convenio='{}', prazoDesejado={}",
                statusOriginal.get(), convenioOriginal.get(), prazoDesejadoOriginal.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        boolean alterado = !isProponenteIgual(proponente.get(), proponenteOriginal.get()) ||
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
                !Objects.equals(prazoDesejado.get(), prazoDesejadoOriginal.get());
        log.trace("[PROPOSTA_VM] temAlteracoesPendentes: {}", alterado);
        return alterado;
    }

    @Override
    public PropostaModel atualizarModel(PropostaModel model) {
        log.debug("[PROPOSTA_VM] atualizarModel: model fornecido = {}", model != null ? "presente" : "null");
        if (model == null) {
            log.trace("[PROPOSTA_VM] Criando novo model (null -> novo)");
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
        model.setPrazoDesejado(this.prazoDesejado.get());

        log.info(
                "[PROPOSTA_VM] Model atualizado: ID={}, status='{}', convenio='{}', valorSolicitado={}, prazoDesejado={}",
                model.getId(), model.getStatus(), model.getConvenioOrgao(), model.getValorSolicitado(),
                model.getPrazoDesejado());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        Observable[] observaveis = new Observable[] {
                proponente, banco, convenio, valorSolicitado, valorAprovado, status, tabelaId, observacoes,
                quantidadeParcelas, valorParcela, taxaAplicada,
                prazoDesejado,
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
                prazoDesejadoOriginal.getReadOnlyProperty()
        };
        log.trace("[PROPOSTA_VM] getObservaveisParaDirty: {} observáveis registrados", observaveis.length);
        return observaveis;
    }

    // ==========================================================
    // REGRAS DE VALIDAÇÃO (O GATEKEEPER DE INTEGRIDADE)
    // ==========================================================

    @Override
    public boolean isValido() {
        boolean valido = this.banco.get() != null
                && this.tabelaId.get() != null
                && this.convenio.get() != null;
        log.trace("[PROPOSTA_VM] isValido: banco={}, tabelaId={}, convenio={} -> {}",
                this.banco.get() != null, this.tabelaId.get() != null, this.convenio.get() != null, valido);
        return valido;
    }

    // ==========================================================
    // MÉTODOS DE COMPARAÇÃO INTERNA
    // ==========================================================

    private boolean isBancoIgual(BancoModel b1, BancoModel b2) {
        if (b1 == b2)
            return true;
        if (b1 == null || b2 == null)
            return false;
        boolean igual = Objects.equals(b1.getId(), b2.getId());
        log.trace("[PROPOSTA_VM] isBancoIgual: IDs {} e {} -> {}", b1.getId(), b2.getId(), igual);
        return igual;
    }

    private boolean isProponenteIgual(ProponenteModel p1, ProponenteModel p2) {
        if (p1 == p2)
            return true;
        if (p1 == null || p2 == null)
            return false;
        boolean igual = Objects.equals(p1.getId(), p2.getId());
        log.trace("[PROPOSTA_VM] isProponenteIgual: IDs {} e {} -> {}", p1.getId(), p2.getId(), igual);
        return igual;
    }

    private boolean compareBigDecimal(BigDecimal val1, BigDecimal val2) {
        if (val1 == null && val2 == null)
            return false;
        if (val1 == null || val2 == null)
            return true;
        boolean diferente = val1.compareTo(val2) != 0;
        if (diferente)
            log.trace("[PROPOSTA_VM] compareBigDecimal: {} != {}", val1, val2);
        return diferente;
    }

    // ==========================================================
    // GETTERS PARA BINDING NA UI
    // ==========================================================

    public ObjectProperty<ProponenteModel> proponenteProperty() {
        log.trace("[PROPOSTA_VM] proponenteProperty acessado");
        return proponente;
    }

    public ObjectProperty<BancoModel> bancoProperty() {
        log.trace("[PROPOSTA_VM] bancoProperty acessado");
        return banco;
    }

    public ObjectProperty<TipoConvenioModel> convenioProperty() {
        log.trace("[PROPOSTA_VM] convenioProperty acessado");
        return convenio;
    }

    public ObjectProperty<UsuarioModel> usuarioProperty() {
        log.trace("[PROPOSTA_VM] usuarioProperty acessado");
        return usuario;
    }

    public ObjectProperty<BigDecimal> valorSolicitadoProperty() {
        log.trace("[PROPOSTA_VM] valorSolicitadoProperty acessado");
        return valorSolicitado;
    }

    public ObjectProperty<BigDecimal> valorAprovadoProperty() {
        log.trace("[PROPOSTA_VM] valorAprovadoProperty acessado");
        return valorAprovado;
    }

    public ObjectProperty<BigDecimal> taxaAplicadaProperty() {
        log.trace("[PROPOSTA_VM] taxaAplicadaProperty acessado");
        return taxaAplicada;
    }

    public ObjectProperty<Integer> quantidadeParcelasProperty() {
        log.trace("[PROPOSTA_VM] quantidadeParcelasProperty acessado");
        return quantidadeParcelas;
    }

    public ObjectProperty<StatusPropostaModel> statusProperty() {
        log.trace("[PROPOSTA_VM] statusProperty acessado");
        return status;
    }

    public ObjectProperty<BigDecimal> valorParcelaProperty() {
        log.trace("[PROPOSTA_VM] valorParcelaProperty acessado");
        return valorParcela;
    }

    public ObjectProperty<Long> tabelaIdProperty() {
        log.trace("[PROPOSTA_VM] tabelaIdProperty acessado");
        return tabelaId;
    }

    public ObjectProperty<BigDecimal> comissaoEstimadaProperty() {
        log.trace("[PROPOSTA_VM] comissaoEstimadaProperty acessado");
        return comissaoEstimada;
    }

    public StringProperty observacoesProperty() {
        log.trace("[PROPOSTA_VM] observacoesProperty acessado");
        return observacoes;
    }

    public ObjectProperty<LocalDate> dataSolicitacaoProperty() {
        log.trace("[PROPOSTA_VM] dataSolicitacaoProperty acessado");
        return dataSolicitacao;
    }

    public ObjectProperty<Integer> prazoDesejadoProperty() {
        log.trace("[PROPOSTA_VM] prazoDesejadoProperty acessado");
        return prazoDesejado;
    }
}