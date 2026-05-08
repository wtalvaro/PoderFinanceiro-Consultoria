package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.enums.OrigemLead;
import br.com.poderfinanceiro.app.model.enums.TipoConvenio;
import br.com.poderfinanceiro.app.model.enums.TipoRelacionamento;
import br.com.poderfinanceiro.app.model.enums.TipoVinculo;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Scope("prototype")
public class LeadViewModel extends BaseViewModel<Proponente> {

    // --- 1. PROPERTIES ESPECÍFICAS DO LEAD ---
    private final StringProperty nome = new SimpleStringProperty("");
    private final StringProperty cpf = new SimpleStringProperty("");
    private final StringProperty telefone = new SimpleStringProperty("");
    private final ObjectProperty<OrigemLead> origem = new SimpleObjectProperty<>(OrigemLead.WHATSAPP);

    private final ObjectProperty<LocalDate> dataNascimento = new SimpleObjectProperty<>();
    private final ObjectProperty<TipoConvenio> convenio = new SimpleObjectProperty<>(TipoConvenio.PADRAO);
    private final ObjectProperty<TipoVinculo> vinculo = new SimpleObjectProperty<>(TipoVinculo.CLT);
    private final StringProperty matricula = new SimpleStringProperty("");
    private final ObjectProperty<BigDecimal> renda = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<TipoRelacionamento> classificacao = new SimpleObjectProperty<>(
            TipoRelacionamento.LEAD);

    // --- 2. ESTADOS ORIGINAIS ---
    private final ReadOnlyStringWrapper nomeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper cpfOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper telefoneOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<OrigemLead> origemOriginal = new ReadOnlyObjectWrapper<>(OrigemLead.WHATSAPP);
    private final ReadOnlyObjectWrapper<LocalDate> dataNascimentoOriginal = new ReadOnlyObjectWrapper<>(null);
    private final ReadOnlyObjectWrapper<TipoConvenio> convenioOriginal = new ReadOnlyObjectWrapper<>(
            TipoConvenio.PADRAO);
    private final ReadOnlyObjectWrapper<TipoVinculo> vinculoOriginal = new ReadOnlyObjectWrapper<>(TipoVinculo.CLT);
    private final ReadOnlyStringWrapper matriculaOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<BigDecimal> rendaOriginal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<TipoRelacionamento> classificacaoOriginal = new ReadOnlyObjectWrapper<>(
            TipoRelacionamento.LEAD);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(Proponente model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(Proponente model) {
        nome.set(model.getNomeCompleto() != null ? model.getNomeCompleto() : "");
        cpf.set(model.getCpf() != null ? model.getCpf() : "");
        telefone.set(model.getTelefone() != null ? model.getTelefone() : "");
        origem.set(model.getOrigemConsentimento() != null ? model.getOrigemConsentimento() : OrigemLead.WHATSAPP);
        dataNascimento.set(model.getDataNascimento());
        convenio.set(model.getConvenioOrgao() != null ? model.getConvenioOrgao() : TipoConvenio.PADRAO);
        vinculo.set(model.getTipoVinculo() != null ? model.getTipoVinculo() : TipoVinculo.CLT);
        matricula.set(model.getMatricula() != null ? model.getMatricula() : "");
        renda.set(model.getRendaMensal() != null ? model.getRendaMensal() : BigDecimal.ZERO);
        classificacao.set(model.getClassificacao() != null ? model.getClassificacao() : TipoRelacionamento.LEAD);
    }

    @Override
    protected void limparCampos() {
        nome.set("");
        cpf.set("");
        telefone.set("");
        origem.set(OrigemLead.WHATSAPP);
        dataNascimento.set(null);
        convenio.set(TipoConvenio.PADRAO);
        vinculo.set(TipoVinculo.CLT);
        matricula.set("");
        renda.set(BigDecimal.ZERO);
        classificacao.set(TipoRelacionamento.LEAD);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        this.nomeOriginal.set(nome.get());
        this.cpfOriginal.set(cpf.get());
        this.telefoneOriginal.set(telefone.get());
        this.origemOriginal.set(origem.get());
        this.dataNascimentoOriginal.set(dataNascimento.get());
        this.convenioOriginal.set(convenio.get());
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
                !Objects.equals(convenio.get(), convenioOriginal.get()) ||
                !Objects.equals(vinculo.get(), vinculoOriginal.get()) ||
                !Objects.equals(matricula.get(), matriculaOriginal.get()) ||
                !Objects.equals(classificacao.get(), classificacaoOriginal.get()) ||
                rendaMudou;
    }

    @Override
    public Proponente atualizarModel(Proponente model) {
        if (model == null) {
            model = new Proponente();
        }
        model.setId(this.id.get());
        model.setNomeCompleto(this.nome.get());
        model.setCpf(this.cpf.get().replaceAll("[^0-9]", "")); // Limpeza de máscara para o banco
        model.setTelefone(this.telefone.get());
        model.setOrigemConsentimento(this.origem.get());
        model.setDataNascimento(this.dataNascimento.get());
        model.setConvenioOrgao(this.convenio.get());
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
                nome, cpf, telefone, origem, dataNascimento, convenio, vinculo, matricula, renda, classificacao,
                nomeOriginal.getReadOnlyProperty(),
                cpfOriginal.getReadOnlyProperty(),
                telefoneOriginal.getReadOnlyProperty(),
                origemOriginal.getReadOnlyProperty(),
                dataNascimentoOriginal.getReadOnlyProperty(),
                convenioOriginal.getReadOnlyProperty(),
                vinculoOriginal.getReadOnlyProperty(),
                matriculaOriginal.getReadOnlyProperty(),
                rendaOriginal.getReadOnlyProperty(),
                classificacaoOriginal.getReadOnlyProperty()
        };
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

    public ObjectProperty<OrigemLead> origemProperty() {
        return origem;
    }

    public ObjectProperty<LocalDate> dataNascimentoProperty() {
        return dataNascimento;
    }

    public ObjectProperty<TipoConvenio> convenioProperty() {
        return convenio;
    }

    public ObjectProperty<TipoVinculo> vinculoProperty() {
        return vinculo;
    }

    public StringProperty matriculaProperty() {
        return matricula;
    }

    public ObjectProperty<BigDecimal> rendaProperty() {
        return renda;
    }

    public ObjectProperty<TipoRelacionamento> classificacaoProperty() {
        return classificacao;
    }
}