package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class LeadViewModel {

    // --- 1. PROPERTIES (O Coração do Data Binding) ---

    private final BooleanProperty editando = new SimpleBooleanProperty(false);
    private final BooleanProperty carregando = new SimpleBooleanProperty(false);

    // Identificação
    private final StringProperty nome = new SimpleStringProperty("");
    private final StringProperty cpf = new SimpleStringProperty(""); // Guarda apenas os números: 12345678901
    private final StringProperty telefone = new SimpleStringProperty(""); // Guarda apenas os números
    private final StringProperty origem = new SimpleStringProperty("");

    // Perfil Operacional
    private final ObjectProperty<LocalDate> dataNascimento = new SimpleObjectProperty<>();
    private final ObjectProperty<TipoConvenio> convenio = new SimpleObjectProperty<>(TipoConvenio.PADRAO);
    private final StringProperty vinculo = new SimpleStringProperty("");
    private final StringProperty matricula = new SimpleStringProperty("");
    private final ObjectProperty<BigDecimal> renda = new SimpleObjectProperty<>(BigDecimal.ZERO);

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
    private String origemOriginal = "";
    private LocalDate dataNascimentoOriginal = null;
    private TipoConvenio convenioOriginal = TipoConvenio.PADRAO;
    private String vinculoOriginal = "";
    private String matriculaOriginal = "";
    private BigDecimal rendaOriginal = BigDecimal.ZERO;

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

    // --- 3. MÉTODOS DE SINCRONIZAÇÃO (INTEGRAÇÃO COM O BANCO) ---

    public void loadFromModel(Proponente p) {
        if (p == null) {
            editando.set(false);
            reset();
            return;
        }

        editando.set(true);

        // 1. Injeta os dados limpos nas propriedades
        nome.set(p.getNomeCompleto() != null ? p.getNomeCompleto() : "");
        // O TextFormatter do Controller agora é quem se preocupa em colocar pontos e
        // traços na tela
        cpf.set(p.getCpf() != null ? p.getCpf() : "");
        telefone.set(p.getTelefone() != null ? p.getTelefone() : "");
        origem.set(p.getOrigemConsentimento() != null ? p.getOrigemConsentimento() : "");
        dataNascimento.set(p.getDataNascimento());
        convenio.set(TipoConvenio.fromString(p.getConvenioOrgao()));
        vinculo.set(p.getTipoVinculo() != null ? p.getTipoVinculo() : "");
        matricula.set(p.getMatricula() != null ? p.getMatricula() : "");
        renda.set(p.getRendaMensal() != null ? p.getRendaMensal() : BigDecimal.ZERO);

        // Atualiza o estado interno das modalidades
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

        // 2. Tira uma "fotografia" do estado para detectar mudanças
        this.nomeOriginal = nome.get();
        this.cpfOriginal = cpf.get();
        this.telefoneOriginal = telefone.get();
        this.origemOriginal = origem.get();
        this.dataNascimentoOriginal = dataNascimento.get();
        this.convenioOriginal = convenio.get();
        this.vinculoOriginal = vinculo.get();
        this.matriculaOriginal = matricula.get();
        this.rendaOriginal = renda.get();
    }

    public Proponente mapToModel(Proponente target) {
        // Envia para o banco o valor puro e limpo
        target.setNomeCompleto(nome.get().trim());
        target.setCpf(cpf.get().trim());
        target.setTelefone(telefone.get());
        target.setOrigemConsentimento(origem.get());
        target.setDataNascimento(dataNascimento.get());
        target.setConvenioOrgao(convenio.get() != null ? convenio.get().getLabel() : null);
        target.setTipoVinculo(vinculo.get());
        target.setMatricula(matricula.get());
        target.setRendaMensal(renda.get());
        return target;
    }

    public void reset() {
        nome.set("");
        cpf.set("");
        telefone.set("");
        origem.set("");
        dataNascimento.set(null);
        convenio.set(TipoConvenio.PADRAO);
        vinculo.set("");
        matricula.set("");
        renda.set(BigDecimal.ZERO);

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
        this.origemOriginal = "";
        this.dataNascimentoOriginal = null;
        this.convenioOriginal = TipoConvenio.PADRAO;
        this.vinculoOriginal = "";
        this.matriculaOriginal = "";
        this.rendaOriginal = BigDecimal.ZERO;

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

    // --- 4. REGRAS DE NEGÓCIO E VALIDAÇÕES DE TELA ---

    public BooleanBinding podeSalvarProperty() {
        return Bindings.createBooleanBinding(() -> {
            try {
                if (carregando.get())
                    return false;

                boolean nomeValido = nome.get() != null && !nome.get().trim().isEmpty();

                // O CPF já vem limpo graças ao TextFormatter. Basta contar os caracteres.
                boolean cpfValido = cpf.get() != null && cpf.get().length() == 11;

                boolean dadosCorretos = nomeValido && cpfValido;
                boolean houveAlteracao = temAlteracoesPendentes();

                return editando.get() ? (dadosCorretos && houveAlteracao) : dadosCorretos;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        },
                nome, cpf, telefone, origem, dataNascimento, convenio, vinculo, matricula, renda,
                chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
                chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal,
                editando, carregando);
    }

    public boolean temAlteracoesPendentes() {
        // Helper seguro para comparar BigDecimal, tratando nulos adequadamente
        boolean rendaMudou;
        if (renda.get() == null && rendaOriginal == null) {
            rendaMudou = false;
        } else if (renda.get() == null || rendaOriginal == null) {
            rendaMudou = true;
        } else {
            rendaMudou = renda.get().compareTo(rendaOriginal) != 0;
        }

        return !Objects.equals(nome.get(), nomeOriginal) ||
                !Objects.equals(cpf.get(), cpfOriginal) ||
                !Objects.equals(telefone.get(), telefoneOriginal) ||
                !Objects.equals(origem.get(), origemOriginal) ||
                !Objects.equals(dataNascimento.get(), dataNascimentoOriginal) ||
                convenio.get() != convenioOriginal ||
                !Objects.equals(vinculo.get(), vinculoOriginal) ||
                !Objects.equals(matricula.get(), matriculaOriginal) ||
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

    public StringProperty origemProperty() {
        return origem;
    }

    public ObjectProperty<LocalDate> dataNascimentoProperty() {
        return dataNascimento;
    }

    public ObjectProperty<TipoConvenio> convenioProperty() {
        return convenio;
    }

    public StringProperty vinculoProperty() {
        return vinculo;
    }

    public StringProperty matriculaProperty() {
        return matricula;
    }

    public ObjectProperty<BigDecimal> rendaProperty() {
        return renda;
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