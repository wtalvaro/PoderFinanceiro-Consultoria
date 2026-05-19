package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.model.enums.TipoVinculoModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Scope("prototype")
public class LeadViewModel extends BaseViewModel<ProponenteModel> {

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
    private final ReadOnlyObjectWrapper<OrigemLeadModel> origemOriginal = new ReadOnlyObjectWrapper<>(OrigemLeadModel.WHATSAPP);
    private final ReadOnlyObjectWrapper<LocalDate> dataNascimentoOriginal = new ReadOnlyObjectWrapper<>(null);
    private final ReadOnlyObjectWrapper<TipoVinculoModel> vinculoOriginal = new ReadOnlyObjectWrapper<>(TipoVinculoModel.CLT);
    private final ReadOnlyStringWrapper matriculaOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<BigDecimal> rendaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<TipoRelacionamentoModel> classificacaoOriginal = new ReadOnlyObjectWrapper<>(
            TipoRelacionamentoModel.LEAD);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(ProponenteModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(ProponenteModel model) {
        nome.set(model.getNomeCompleto() != null ? model.getNomeCompleto() : "");
        cpf.set(model.getCpf() != null ? model.getCpf() : "");
        telefone.set(model.getTelefone() != null ? model.getTelefone() : "");
        origem.set(model.getOrigemConsentimento() != null ? model.getOrigemConsentimento() : OrigemLeadModel.WHATSAPP);
        dataNascimento.set(model.getDataNascimento());
        vinculo.set(model.getTipoVinculo() != null ? model.getTipoVinculo() : TipoVinculoModel.CLT);
        matricula.set(model.getMatricula() != null ? model.getMatricula() : "");
        renda.set(model.getRendaMensal() != null ? model.getRendaMensal() : BigDecimal.ZERO);
        classificacao.set(model.getClassificacao() != null ? model.getClassificacao() : TipoRelacionamentoModel.LEAD);
    }

    @Override
    protected void limparCampos() {
        nome.set("");
        cpf.set("");
        telefone.set("");
        origem.set(OrigemLeadModel.WHATSAPP);
        dataNascimento.set(null);
        vinculo.set(TipoVinculoModel.CLT);
        matricula.set("");
        renda.set(BigDecimal.ZERO);
        classificacao.set(TipoRelacionamentoModel.LEAD);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        this.nomeOriginal.set(nome.get());
        this.cpfOriginal.set(cpf.get());
        this.telefoneOriginal.set(telefone.get());
        this.origemOriginal.set(origem.get());
        this.dataNascimentoOriginal.set(dataNascimento.get());
        this.vinculoOriginal.set(vinculo.get());
        this.matriculaOriginal.set(matricula.get());
        this.rendaOriginal.set(renda.get());
        this.classificacaoOriginal.set(classificacao.get());
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

        // Mantemos a sua excelente lógica de ignorar as máscaras na hora de comparar o
        // "sujo"
        String cpfAtual = cpf.get() != null ? cpf.get().replaceAll("[^0-9]", "") : "";
        String cpfOrig = cpfOriginal.get() != null ? cpfOriginal.get().replaceAll("[^0-9]", "") : "";

        String telAtual = telefone.get() != null ? telefone.get().replaceAll("[^0-9]", "") : "";
        String telOrig = telefoneOriginal.get() != null ? telefoneOriginal.get().replaceAll("[^0-9]", "") : "";

        return !Objects.equals(nome.get(), nomeOriginal.get()) ||
                !cpfAtual.equals(cpfOrig) ||
                !telAtual.equals(telOrig) ||
                !Objects.equals(origem.get(), origemOriginal.get()) ||
                !Objects.equals(dataNascimento.get(), dataNascimentoOriginal.get()) ||
                !Objects.equals(vinculo.get(), vinculoOriginal.get()) ||
                !Objects.equals(matricula.get(), matriculaOriginal.get()) ||
                !Objects.equals(classificacao.get(), classificacaoOriginal.get()) ||
                rendaMudou;
    }

    @Override
    public ProponenteModel atualizarModel(ProponenteModel model) {
        if (model == null) {
            model = new ProponenteModel();
        }
        model.setId(this.id.get());
        model.setNomeCompleto(this.nome.get());
        model.setCpf(this.cpf.get().replaceAll("[^0-9]", "")); // Limpeza de máscara para o banco
        model.setTelefone(this.telefone.get());
        model.setOrigemConsentimento(this.origem.get());
        model.setDataNascimento(this.dataNascimento.get());
        model.setTipoVinculo(this.vinculo.get());
        model.setMatricula(this.matricula.get());
        model.setRendaMensal(this.renda.get());

        if (this.classificacao.get() != null) {
            model.setClassificacao(this.classificacao.get());
        }

        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
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
    }
    
    @Override
    public boolean isValido() {
        // Por padrão, se não tiver regras específicas, retorna verdadeiro
        return true;
    }

    // ==========================================================
    // GETTERS DAS PROPERTIES EXCLUSIVAS
    // ==========================================================

    public StringProperty nomeProperty() {
        return nome;
    }

    public StringProperty cpfProperty() {
        return cpf;
    }

    public StringProperty telefoneProperty() {
        return telefone;
    }

    public ObjectProperty<OrigemLeadModel> origemProperty() {
        return origem;
    }

    public ObjectProperty<LocalDate> dataNascimentoProperty() {
        return dataNascimento;
    }

    public ObjectProperty<TipoVinculoModel> vinculoProperty() {
        return vinculo;
    }

    public StringProperty matriculaProperty() {
        return matricula;
    }

    public ObjectProperty<BigDecimal> rendaProperty() {
        return renda;
    }

    public ObjectProperty<TipoRelacionamentoModel> classificacaoProperty() {
        return classificacao;
    }
}