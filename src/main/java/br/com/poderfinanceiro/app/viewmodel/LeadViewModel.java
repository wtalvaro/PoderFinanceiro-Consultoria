package br.com.poderfinanceiro.app.viewmodel;

import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoVinculoModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Scope("prototype")
public class LeadViewModel extends BaseViewModel<ProponenteModel> {

    private static final Logger log = LoggerFactory.getLogger(LeadViewModel.class);

    // --- 1. PROPERTIES ESPECÍFICAS DO LEAD ---
    private final StringProperty nome = new SimpleStringProperty("");
    private final StringProperty cpf = new SimpleStringProperty("");
    private final StringProperty telefone = new SimpleStringProperty("");
    private final ObjectProperty<OrigemLeadModel> origem = new SimpleObjectProperty<>(OrigemLeadModel.WHATSAPP);

    private final ObjectProperty<LocalDate> dataNascimento = new SimpleObjectProperty<>();
    private final ObjectProperty<TipoVinculoModel> vinculo = new SimpleObjectProperty<>(TipoVinculoModel.CLT);
    private final StringProperty matricula = new SimpleStringProperty("");
    private final ObjectProperty<BigDecimal> renda = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<TipoRelacionamentoModel> classificacao = new SimpleObjectProperty<>(
            TipoRelacionamentoModel.LEAD);

    // --- 2. ESTADOS ORIGINAIS ---
    private final ReadOnlyStringWrapper nomeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper cpfOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper telefoneOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<OrigemLeadModel> origemOriginal = new ReadOnlyObjectWrapper<>(
            OrigemLeadModel.WHATSAPP);
    private final ReadOnlyObjectWrapper<LocalDate> dataNascimentoOriginal = new ReadOnlyObjectWrapper<>(null);
    private final ReadOnlyObjectWrapper<TipoVinculoModel> vinculoOriginal = new ReadOnlyObjectWrapper<>(
            TipoVinculoModel.CLT);
    private final ReadOnlyStringWrapper matriculaOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<BigDecimal> rendaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<TipoRelacionamentoModel> classificacaoOriginal = new ReadOnlyObjectWrapper<>(
            TipoRelacionamentoModel.LEAD);

    // Construtor para log
    public LeadViewModel() {
        log.debug("[LEAD_VM] Instância criada (prototype)");
    }

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(ProponenteModel model) {
        log.debug("[LEAD_VM] extrairId: model ID={}", model != null ? model.getId() : "null");
        this.id.set(model != null ? model.getId() : null);
    }

    @Override
    protected void preencherCampos(ProponenteModel model) {
        log.debug("[LEAD_VM] preencherCampos: populando campos a partir do model ID={}",
                model != null ? model.getId() : "null");
        if (model == null) {
            limparCampos();
            return;
        }
        nome.set(model.getNomeCompleto() != null ? model.getNomeCompleto() : "");
        cpf.set(model.getCpf() != null ? model.getCpf() : "");
        telefone.set(model.getTelefone() != null ? model.getTelefone() : "");
        origem.set(model.getOrigemConsentimento() != null ? model.getOrigemConsentimento() : OrigemLeadModel.WHATSAPP);
        dataNascimento.set(model.getDataNascimento());
        vinculo.set(model.getTipoVinculo() != null ? model.getTipoVinculo() : TipoVinculoModel.CLT);
        matricula.set(model.getMatricula() != null ? model.getMatricula() : "");
        renda.set(model.getRendaMensal() != null ? model.getRendaMensal() : BigDecimal.ZERO);
        classificacao.set(model.getClassificacao() != null ? model.getClassificacao() : TipoRelacionamentoModel.LEAD);
        log.trace("[LEAD_VM] Campos preenchidos: nome='{}', cpf='{}', telefone='{}'", nome.get(), cpf.get(),
                telefone.get());
    }

    @Override
    protected void limparCampos() {
        log.debug("[LEAD_VM] limparCampos: resetando todos os campos");
        nome.set("");
        cpf.set("");
        telefone.set("");
        origem.set(OrigemLeadModel.WHATSAPP);
        dataNascimento.set(null);
        vinculo.set(TipoVinculoModel.CLT);
        matricula.set("");
        renda.set(BigDecimal.ZERO);
        classificacao.set(TipoRelacionamentoModel.LEAD);
        log.trace("[LEAD_VM] Campos limpos");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        log.debug("[LEAD_VM] sincronizarEstadoOriginal: salvando estado atual como original");
        this.nomeOriginal.set(nome.get());
        this.cpfOriginal.set(cpf.get());
        this.telefoneOriginal.set(telefone.get());
        this.origemOriginal.set(origem.get());
        this.dataNascimentoOriginal.set(dataNascimento.get());
        this.vinculoOriginal.set(vinculo.get());
        this.matriculaOriginal.set(matricula.get());
        this.rendaOriginal.set(renda.get());
        this.classificacaoOriginal.set(classificacao.get());
        log.trace("[LEAD_VM] Estado original salvo: nome='{}', cpf='{}'", nomeOriginal.get(), cpfOriginal.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        boolean rendaMudou;
        if (renda.get() == null && rendaOriginal.get() == null) {
            rendaMudou = false;
        } else if (renda.get() == null || rendaOriginal.get() == null) {
            rendaMudou = true;
        } else {
            rendaMudou = renda.get().compareTo(rendaOriginal.get()) != 0;
        }

        String cpfAtual = cpf.get() != null ? cpf.get().replaceAll("[^0-9]", "") : "";
        String cpfOrig = cpfOriginal.get() != null ? cpfOriginal.get().replaceAll("[^0-9]", "") : "";

        String telAtual = telefone.get() != null ? telefone.get().replaceAll("[^0-9]", "") : "";
        String telOrig = telefoneOriginal.get() != null ? telefoneOriginal.get().replaceAll("[^0-9]", "") : "";

        boolean alterado = !Objects.equals(nome.get(), nomeOriginal.get()) ||
                !cpfAtual.equals(cpfOrig) ||
                !telAtual.equals(telOrig) ||
                !Objects.equals(origem.get(), origemOriginal.get()) ||
                !Objects.equals(dataNascimento.get(), dataNascimentoOriginal.get()) ||
                !Objects.equals(vinculo.get(), vinculoOriginal.get()) ||
                !Objects.equals(matricula.get(), matriculaOriginal.get()) ||
                !Objects.equals(classificacao.get(), classificacaoOriginal.get()) ||
                rendaMudou;
        log.trace("[LEAD_VM] temAlteracoesPendentes: {}", alterado);
        return alterado;
    }

    @Override
    public ProponenteModel atualizarModel(ProponenteModel model) {
        log.debug("[LEAD_VM] atualizarModel: model fornecido = {}", model != null ? "presente" : "null");
        if (model == null) {
            log.trace("[LEAD_VM] Criando novo model (null -> novo)");
            model = new ProponenteModel();
        }
        model.setId(this.id.get());
        model.setNomeCompleto(this.nome.get());
        model.setCpf(this.cpf.get().replaceAll("[^0-9]", ""));
        model.setTelefone(this.telefone.get());
        model.setOrigemConsentimento(this.origem.get());
        model.setDataNascimento(this.dataNascimento.get());
        model.setTipoVinculo(this.vinculo.get());
        model.setMatricula(this.matricula.get());
        model.setRendaMensal(this.renda.get());

        if (this.classificacao.get() != null) {
            model.setClassificacao(this.classificacao.get());
        }

        log.info("[LEAD_VM] Model atualizado: ID={}, nome='{}', cpf='{}'", model.getId(), model.getNomeCompleto(),
                model.getCpf());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        Observable[] observaveis = new Observable[] {
                nome, cpf, telefone, origem, dataNascimento, vinculo, matricula, renda, classificacao,
                nomeOriginal.getReadOnlyProperty(),
                cpfOriginal.getReadOnlyProperty(),
                telefoneOriginal.getReadOnlyProperty(),
                origemOriginal.getReadOnlyProperty(),
                dataNascimentoOriginal.getReadOnlyProperty(),
                vinculoOriginal.getReadOnlyProperty(),
                matriculaOriginal.getReadOnlyProperty(),
                rendaOriginal.getReadOnlyProperty(),
                classificacaoOriginal.getReadOnlyProperty()
        };
        log.trace("[LEAD_VM] getObservaveisParaDirty: {} observáveis registrados", observaveis.length);
        return observaveis;
    }

    @Override
    public boolean isValido() {
        log.trace("[LEAD_VM] isValido: sempre verdadeiro (sem regras específicas)");
        return true;
    }

    // ==========================================================
    // GETTERS DAS PROPERTIES EXCLUSIVAS
    // ==========================================================

    public StringProperty nomeProperty() {
        log.trace("[LEAD_VM] nomeProperty acessado");
        return nome;
    }

    public StringProperty cpfProperty() {
        log.trace("[LEAD_VM] cpfProperty acessado");
        return cpf;
    }

    public StringProperty telefoneProperty() {
        log.trace("[LEAD_VM] telefoneProperty acessado");
        return telefone;
    }

    public ObjectProperty<OrigemLeadModel> origemProperty() {
        log.trace("[LEAD_VM] origemProperty acessado");
        return origem;
    }

    public ObjectProperty<LocalDate> dataNascimentoProperty() {
        log.trace("[LEAD_VM] dataNascimentoProperty acessado");
        return dataNascimento;
    }

    public ObjectProperty<TipoVinculoModel> vinculoProperty() {
        log.trace("[LEAD_VM] vinculoProperty acessado");
        return vinculo;
    }

    public StringProperty matriculaProperty() {
        log.trace("[LEAD_VM] matriculaProperty acessado");
        return matricula;
    }

    public ObjectProperty<BigDecimal> rendaProperty() {
        log.trace("[LEAD_VM] rendaProperty acessado");
        return renda;
    }

    public ObjectProperty<TipoRelacionamentoModel> classificacaoProperty() {
        log.trace("[LEAD_VM] classificacaoProperty acessado");
        return classificacao;
    }
}