package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.OrigemLead;
import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.model.TipoRelacionamento;
import br.com.poderfinanceiro.app.model.TipoVinculo;
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
    private String nomeOriginal = "";
    private String cpfOriginal = "";
    private String telefoneOriginal = "";
    private OrigemLead origemOriginal = OrigemLead.WHATSAPP;
    private LocalDate dataNascimentoOriginal = null;
    private TipoConvenio convenioOriginal = TipoConvenio.PADRAO;
    private TipoVinculo vinculoOriginal = TipoVinculo.CLT;
    private String matriculaOriginal = "";
    private BigDecimal rendaOriginal = BigDecimal.ZERO;
    private TipoRelacionamento classificacaoOriginal = TipoRelacionamento.LEAD;

    private boolean chkFgtsOriginal = false;
    private boolean chkInssOriginal = false;
    private boolean chkSiapeOriginal = false;
    private boolean chkForcasOriginal = false;
    private boolean chkBolsaFamiliaOriginal = false;
    private boolean chkContaLuzOriginal = false;
    private boolean chkCartaoOriginal = false;
    private boolean chkPortabilidadeOriginal = false;
    private boolean chkRefinOriginal = false;
    private boolean chkGarantiaOriginal = false;
    private boolean chkConsigPrivadoOriginal = false;
    private boolean chkPessoalOriginal = false;

    // --- 3. MÉTODOS DE SINCRONIZAÇÃO ---

    public void loadFromModel(Proponente p) {
        if (p == null) {
            reset();
            return;
        }

        id.set(p.getId());
        editando.set(true); // <-- GARANTE QUE É EDIÇÃO

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
        this.chkFgtsOriginal = chkFgts.get();
        this.chkInssOriginal = chkInss.get();
        this.chkSiapeOriginal = chkSiape.get();
        this.chkForcasOriginal = chkForcas.get();
        this.chkBolsaFamiliaOriginal = chkBolsaFamilia.get();
        this.chkContaLuzOriginal = chkContaLuz.get();
        this.chkCartaoOriginal = chkCartao.get();
        this.chkPortabilidadeOriginal = chkPortabilidade.get();
        this.chkRefinOriginal = chkRefin.get();
        this.chkGarantiaOriginal = chkGarantia.get();
        this.chkConsigPrivadoOriginal = chkConsigPrivado.get();
        this.chkPessoalOriginal = chkPessoal.get();

        this.nomeOriginal = nome.get();
        this.cpfOriginal = cpf.get();
        this.telefoneOriginal = telefone.get();
        this.origemOriginal = origem.get();
        this.dataNascimentoOriginal = dataNascimento.get();
        this.convenioOriginal = convenio.get();
        this.vinculoOriginal = vinculo.get();
        this.matriculaOriginal = matricula.get();
        this.rendaOriginal = renda.get();
        this.classificacaoOriginal = classificacao.get();
    }

    public Proponente mapToModel(Proponente target) {
        target.setId(id.get());
        target.setNomeCompleto(nome.get().trim());
        target.setCpf(cpf.get().trim());
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

        limparEstadoOriginal();
    }

    private void limparEstadoOriginal() {
        this.nomeOriginal = "";
        this.cpfOriginal = "";
        this.telefoneOriginal = "";
        this.origemOriginal = OrigemLead.WHATSAPP;
        this.dataNascimentoOriginal = null;
        this.convenioOriginal = TipoConvenio.PADRAO;
        this.vinculoOriginal = TipoVinculo.CLT;
        this.matriculaOriginal = "";
        this.rendaOriginal = BigDecimal.ZERO;
        this.classificacaoOriginal = TipoRelacionamento.LEAD;

        this.chkFgtsOriginal = false;
        this.chkInssOriginal = false;
        this.chkSiapeOriginal = false;
        this.chkForcasOriginal = false;
        this.chkBolsaFamiliaOriginal = false;
        this.chkContaLuzOriginal = false;
        this.chkCartaoOriginal = false;
        this.chkPortabilidadeOriginal = false;
        this.chkRefinOriginal = false;
        this.chkGarantiaOriginal = false;
        this.chkConsigPrivadoOriginal = false;
        this.chkPessoalOriginal = false;
    }

    public boolean isDirty() {
        return temAlteracoesPendentes();
    }

    public BooleanProperty dirtyProperty() {
        return new SimpleBooleanProperty(temAlteracoesPendentes());
    }

    // --- 4. REGRAS DE NEGÓCIO E VALIDAÇÕES DE TELA ---

    public BooleanBinding podeSalvarProperty() {
        return Bindings.createBooleanBinding(() -> {
            try {
                if (carregando.get())
                    return false;

                boolean nomeValido = nome.get() != null && !nome.get().trim().isEmpty();

                // CORREÇÃO CRÍTICA: Remove a máscara para contar exatamente os 11 números do
                // CPF
                String cpfLimpo = cpf.get() != null ? cpf.get().replaceAll("[^0-9]", "") : "";

                // NOVA REGRA: O CPF é válido se estiver VAZIO (opcional) OU se tiver exatamente
                // 11 números.
                boolean cpfValido = cpfLimpo.isEmpty() || cpfLimpo.length() == 11;

                boolean dadosCorretos = nomeValido && cpfValido;
                boolean houveAlteracao = temAlteracoesPendentes();

                // Se está editando um existente, precisa estar correto E alterado.
                // Se é um NOVO cadastro, basta estar correto.
                return editando.get() ? (dadosCorretos && houveAlteracao) : dadosCorretos;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        },
                nome, cpf, telefone, origem, dataNascimento, convenio, vinculo, matricula, renda, classificacao,
                chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
                chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal,
                editando, carregando);
    }

    public boolean temAlteracoesPendentes() {
        boolean rendaMudou;
        if (renda.get() == null && rendaOriginal == null) {
            rendaMudou = false;
        } else if (renda.get() == null || rendaOriginal == null) {
            rendaMudou = true;
        } else {
            rendaMudou = renda.get().compareTo(rendaOriginal) != 0;
        }

        // CORREÇÃO CRÍTICA: Remove a máscara antes de comparar se o CPF ou Telefone
        // mudou
        String cpfAtual = cpf.get() != null ? cpf.get().replaceAll("[^0-9]", "") : "";
        String cpfOrig = cpfOriginal != null ? cpfOriginal.replaceAll("[^0-9]", "") : "";

        String telAtual = telefone.get() != null ? telefone.get().replaceAll("[^0-9]", "") : "";
        String telOrig = telefoneOriginal != null ? telefoneOriginal.replaceAll("[^0-9]", "") : "";

        return !Objects.equals(nome.get(), nomeOriginal) ||
                !cpfAtual.equals(cpfOrig) ||
                !telAtual.equals(telOrig) ||
                !Objects.equals(origem.get(), origemOriginal) ||
                !Objects.equals(dataNascimento.get(), dataNascimentoOriginal) ||
                !Objects.equals(convenio.get(), convenioOriginal) ||
                !Objects.equals(vinculo.get(), vinculoOriginal) ||
                !Objects.equals(matricula.get(), matriculaOriginal) ||
                !Objects.equals(classificacao.get(), classificacaoOriginal) ||
                rendaMudou ||
                chkFgts.get() != chkFgtsOriginal ||
                chkInss.get() != chkInssOriginal ||
                chkSiape.get() != chkSiapeOriginal ||
                chkForcas.get() != chkForcasOriginal ||
                chkBolsaFamilia.get() != chkBolsaFamiliaOriginal ||
                chkContaLuz.get() != chkContaLuzOriginal ||
                chkCartao.get() != chkCartaoOriginal ||
                chkPortabilidade.get() != chkPortabilidadeOriginal ||
                chkRefin.get() != chkRefinOriginal ||
                chkGarantia.get() != chkGarantiaOriginal ||
                chkConsigPrivado.get() != chkConsigPrivadoOriginal ||
                chkPessoal.get() != chkPessoalOriginal;
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
}