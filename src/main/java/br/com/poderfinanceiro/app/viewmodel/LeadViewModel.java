package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.OrigemLead;
import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.model.TipoRelacionamento;
import br.com.poderfinanceiro.app.model.TipoVinculo;
import jakarta.annotation.PostConstruct;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Scope("prototype")
public class LeadViewModel {

    // --- 1. PROPERTIES (O Coração do Data Binding) ---

    private final BooleanProperty editando = new SimpleBooleanProperty(false);
    private final BooleanProperty carregando = new SimpleBooleanProperty(false);
    private final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);

    // Identificação
    private final StringProperty nome = new SimpleStringProperty("");
    private final StringProperty cpf = new SimpleStringProperty("");
    private final StringProperty telefone = new SimpleStringProperty("");
    private final ObjectProperty<OrigemLead> origem = new SimpleObjectProperty<>(null);

    // Perfil Operacional
    private final ObjectProperty<LocalDate> dataNascimento = new SimpleObjectProperty<>();
    private final ObjectProperty<TipoConvenio> convenio = new SimpleObjectProperty<>(TipoConvenio.PADRAO);
    private final ObjectProperty<TipoVinculo> vinculo = new SimpleObjectProperty<>(TipoVinculo.CLT);
    private final StringProperty matricula = new SimpleStringProperty("");
    private final ObjectProperty<BigDecimal> renda = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<TipoRelacionamento> classificacao = new SimpleObjectProperty<>(
            TipoRelacionamento.LEAD);

    // Modalidades de Crédito
    private final BooleanProperty chkFgts = new SimpleBooleanProperty(false);
    private final BooleanProperty chkInss = new SimpleBooleanProperty(false);
    private final BooleanProperty chkSiape = new SimpleBooleanProperty(false);
    private final BooleanProperty chkForcas = new SimpleBooleanProperty(false);
    private final BooleanProperty chkBolsaFamilia = new SimpleBooleanProperty(false);
    private final BooleanProperty chkContaLuz = new SimpleBooleanProperty(false);
    private final BooleanProperty chkCartao = new SimpleBooleanProperty(false);
    private final BooleanProperty chkPortabilidade = new SimpleBooleanProperty(false);
    private final BooleanProperty chkRefin = new SimpleBooleanProperty(false);
    private final BooleanProperty chkGarantia = new SimpleBooleanProperty(false);
    private final BooleanProperty chkConsigPrivado = new SimpleBooleanProperty(false);
    private final BooleanProperty chkPessoal = new SimpleBooleanProperty(false);

    // --- 2. ESTADOS ORIGINAIS (Para o Dirty Checking) ---
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

    private final ReadOnlyBooleanWrapper chkFgtsOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkInssOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkSiapeOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkForcasOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkBolsaFamiliaOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkContaLuzOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkCartaoOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkPortabilidadeOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkRefinOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkGarantiaOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkConsigPrivadoOriginal = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper chkPessoalOriginal = new ReadOnlyBooleanWrapper(false);

    // --- 3. MÉTODOS DE SINCRONIZAÇÃO ---

    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);

    @PostConstruct // CRÍTICO: Faz o Spring chamar este método ao criar o ViewModel
    public void initialize() {
        // Vinculamos a propriedade dirty ao resultado do método temAlteracoesPendentes
        // Ela será reavaliada automaticamente sempre que QUALQUER dependência mudar.
        dirty.bind(Bindings.createBooleanBinding(this::temAlteracoesPendentes,
                nome, nomeOriginal.getReadOnlyProperty(),
                cpf, cpfOriginal.getReadOnlyProperty(),
                telefone, telefoneOriginal.getReadOnlyProperty(),
                origem, origemOriginal.getReadOnlyProperty(),
                dataNascimento, dataNascimentoOriginal.getReadOnlyProperty(),
                convenio, convenioOriginal.getReadOnlyProperty(),
                vinculo, vinculoOriginal.getReadOnlyProperty(),
                matricula, matriculaOriginal.getReadOnlyProperty(),
                renda, rendaOriginal.getReadOnlyProperty(),
                classificacao, classificacaoOriginal.getReadOnlyProperty(),
                chkFgts, chkFgtsOriginal.getReadOnlyProperty(),
                chkInss, chkInssOriginal.getReadOnlyProperty(),
                chkSiape, chkSiapeOriginal.getReadOnlyProperty(),
                chkForcas, chkForcasOriginal.getReadOnlyProperty(),
                chkBolsaFamilia, chkBolsaFamiliaOriginal.getReadOnlyProperty(),
                chkContaLuz, chkContaLuzOriginal.getReadOnlyProperty(),
                chkCartao, chkCartaoOriginal.getReadOnlyProperty(),
                chkPortabilidade, chkPortabilidadeOriginal.getReadOnlyProperty(),
                chkRefin, chkRefinOriginal.getReadOnlyProperty(),
                chkGarantia, chkGarantiaOriginal.getReadOnlyProperty(),
                chkConsigPrivado, chkConsigPrivadoOriginal.getReadOnlyProperty(),
                chkPessoal, chkPessoalOriginal.getReadOnlyProperty(),
                editando, carregando));
    }

    public void loadFromModel(Proponente p) {
        if (p == null) {
            reset();
            return;
        }

        id.set(p.getId());

        // Injeta os dados nas propriedades
        nome.set(p.getNomeCompleto() != null ? p.getNomeCompleto() : "");
        cpf.set(p.getCpf() != null ? p.getCpf() : "");
        telefone.set(p.getTelefone() != null ? p.getTelefone() : "");
        origem.set(p.getOrigemConsentimento() != null ? p.getOrigemConsentimento() : OrigemLead.WHATSAPP);
        dataNascimento.set(p.getDataNascimento());
        convenio.set(p.getConvenioOrgao() != null ? p.getConvenioOrgao() : TipoConvenio.PADRAO);
        vinculo.set(p.getTipoVinculo() != null ? p.getTipoVinculo() : TipoVinculo.CLT);
        matricula.set(p.getMatricula() != null ? p.getMatricula() : "");
        renda.set(p.getRendaMensal() != null ? p.getRendaMensal() : BigDecimal.ZERO);
        classificacao.set(p.getClassificacao() != null ? p.getClassificacao() : TipoRelacionamento.LEAD);

        // 2. DEPOIS: Sincroniza o "Original" com o que acabou de ser injetado[cite: 7]
        sincronizarEstadoOriginal();
        editando.set(true); // <-- GARANTE QUE É EDIÇÃO
    }

    public Proponente mapToModel(Proponente target) {
        // Dados de Identificação e Perfil
        target.setId(id.get());
        target.setNomeCompleto(nome.get().trim());
        target.setCpf(cpf.get().replaceAll("[^0-9]", "")); // Limpeza de máscara para o banco
        target.setTelefone(telefone.get());
        target.setOrigemConsentimento(origem.get() != null ? origem.get() : OrigemLead.WHATSAPP);
        target.setDataNascimento(dataNascimento.get());
        target.setConvenioOrgao(convenio.get() != null ? convenio.get() : TipoConvenio.PADRAO);
        target.setTipoVinculo(vinculo.get() != null ? vinculo.get() : TipoVinculo.CLT);
        target.setMatricula(matricula.get());
        target.setRendaMensal(renda.get());
        target.setClassificacao(classificacao.get() != null ? classificacao.get() : TipoRelacionamento.LEAD);

        return target;
    }

    public void reset() {
        // CORREÇÃO CRÍTICA: Garante que é um NOVO contato
        id.set(null);
        editando.set(false);

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

        chkFgts.set(false);
        chkInss.set(false);
        chkSiape.set(false);
        chkForcas.set(false);
        chkBolsaFamilia.set(false);
        chkContaLuz.set(false);
        chkCartao.set(false);
        chkPortabilidade.set(false);
        chkRefin.set(false);
        chkGarantia.set(false);
        chkConsigPrivado.set(false);
        chkPessoal.set(false);

        sincronizarEstadoOriginal();
    }

    private void sincronizarEstadoOriginal() {
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

        // Agora capturamos o valor real vindo do banco, não o lixo da memória[cite: 7]
        this.chkFgtsOriginal.set(chkFgts.get());
        this.chkInssOriginal.set(chkInss.get());
        this.chkSiapeOriginal.set(chkSiape.get());
        this.chkForcasOriginal.set(chkForcas.get());
        this.chkBolsaFamiliaOriginal.set(chkBolsaFamilia.get());
        this.chkContaLuzOriginal.set(chkContaLuz.get());
        this.chkCartaoOriginal.set(chkCartao.get());
        this.chkPortabilidadeOriginal.set(chkPortabilidade.get());
        this.chkRefinOriginal.set(chkRefin.get());
        this.chkGarantiaOriginal.set(chkGarantia.get());
        this.chkConsigPrivadoOriginal.set(chkConsigPrivado.get());
        this.chkPessoalOriginal.set(chkPessoal.get());
    }

    public boolean isDirty() {
        return dirty.get();
    }

    // --- 4. REGRAS DE NEGÓCIO E VALIDAÇÕES DE TELA ---

    public BooleanBinding podeSalvarProperty() {
        return Bindings.createBooleanBinding(() -> {
            try {
                if (carregando.get())
                    return false;

                // Validações básicas
                boolean nomeValido = nome.get() != null && !nome.get().trim().isEmpty();
                String cpfLimpo = cpf.get() != null ? cpf.get().replaceAll("[^0-9]", "") : "";
                boolean cpfValido = cpfLimpo.isEmpty() || cpfLimpo.length() == 11;

                boolean dadosCorretos = nomeValido && cpfValido;
                boolean houveAlteracao = dirty.get(); // Reage instantaneamente agora

                // Se editando, precisa ser válido E alterado. Se novo, apenas válido.
                return editando.get() ? (dadosCorretos && houveAlteracao) : dadosCorretos;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, dirty, nome, cpf, editando, carregando); // Adicionadas dependências diretas de validação);
    }

    public boolean temAlteracoesPendentes() {
        boolean rendaMudou;
        if (renda.get() == null && rendaOriginal.get() == null) {
            rendaMudou = false;
        } else if (renda.get() == null || rendaOriginal.get() == null) {
            rendaMudou = true;
        } else {
            rendaMudou = renda.get().compareTo(rendaOriginal.get()) != 0;
        }

        // CORREÇÃO CRÍTICA: Remove a máscara antes de comparar se o CPF ou Telefone
        // mudou
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
                rendaMudou ||
                chkFgts.get() != chkFgtsOriginal.get() ||
                chkInss.get() != chkInssOriginal.get() ||
                chkSiape.get() != chkSiapeOriginal.get() ||
                chkForcas.get() != chkForcasOriginal.get() ||
                chkBolsaFamilia.get() != chkBolsaFamiliaOriginal.get() ||
                chkContaLuz.get() != chkContaLuzOriginal.get() ||
                chkCartao.get() != chkCartaoOriginal.get() ||
                chkPortabilidade.get() != chkPortabilidadeOriginal.get() ||
                chkRefin.get() != chkRefinOriginal.get() ||
                chkGarantia.get() != chkGarantiaOriginal.get() ||
                chkConsigPrivado.get() != chkConsigPrivadoOriginal.get() ||
                chkPessoal.get() != chkPessoalOriginal.get();
    }

    // --- 5. GETTERS DAS PROPERTIES ---

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

    public BooleanProperty chkFgtsProperty() {
        return chkFgts;
    }

    public BooleanProperty chkInssProperty() {
        return chkInss;
    }

    public BooleanProperty chkSiapeProperty() {
        return chkSiape;
    }

    public BooleanProperty chkForcasProperty() {
        return chkForcas;
    }

    public BooleanProperty chkBolsaFamiliaProperty() {
        return chkBolsaFamilia;
    }

    public BooleanProperty chkContaLuzProperty() {
        return chkContaLuz;
    }

    public BooleanProperty chkCartaoProperty() {
        return chkCartao;
    }

    public BooleanProperty chkPortabilidadeProperty() {
        return chkPortabilidade;
    }

    public BooleanProperty chkRefinProperty() {
        return chkRefin;
    }

    public BooleanProperty chkGarantiaProperty() {
        return chkGarantia;
    }

    public BooleanProperty chkConsigPrivadoProperty() {
        return chkConsigPrivado;
    }

    public BooleanProperty chkPessoalProperty() {
        return chkPessoal;
    }

    public BooleanProperty carregandoProperty() {
        return carregando;
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty.getReadOnlyProperty();
    }
}