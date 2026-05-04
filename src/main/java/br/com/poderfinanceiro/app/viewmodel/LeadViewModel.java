package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class LeadViewModel {

    // --- 1. PROPERTIES (O Coração do Data Binding) ---

    // Controle de Estado
    private final BooleanProperty editando = new SimpleBooleanProperty(false);

    // Identificação
    private final StringProperty nome = new SimpleStringProperty("");
    private final StringProperty cpf = new SimpleStringProperty("");
    private final StringProperty telefone = new SimpleStringProperty("");
    private final StringProperty origem = new SimpleStringProperty("");

    // Perfil Operacional
    private final ObjectProperty<LocalDate> dataNascimento = new SimpleObjectProperty<>();
    private final ObjectProperty<TipoConvenio> convenio = new SimpleObjectProperty<>(TipoConvenio.PADRAO);
    private final StringProperty vinculo = new SimpleStringProperty("");
    private final StringProperty matricula = new SimpleStringProperty("");
    private final ObjectProperty<BigDecimal> renda = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Todas as 12 Modalidades (CheckBoxes)
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

    // 1. Nova propriedade para monitorar se o Spring está salvando
    private final BooleanProperty carregando = new SimpleBooleanProperty(false);

    // --- 3. MÉTODOS DE SINCRONIZAÇÃO ---

    public void loadFromModel(Proponente p) {
        if (p == null) {
            editando.set(false);
            reset();
            return;
        }

        editando.set(true);

        // 1. Sincroniza Propriedades Visuais
        nome.set(p.getNomeCompleto() != null ? p.getNomeCompleto() : "");
        cpf.set(p.getCpf() != null ? p.getCpf() : "");
        telefone.set(p.getTelefone() != null ? p.getTelefone() : "");
        origem.set(p.getOrigemConsentimento() != null ? p.getOrigemConsentimento() : "");
        dataNascimento.set(p.getDataNascimento());
        convenio.set(TipoConvenio.fromString(p.getConvenioOrgao()));
        vinculo.set(p.getTipoVinculo() != null ? p.getTipoVinculo() : "");
        matricula.set(p.getMatricula() != null ? p.getMatricula() : "");
        renda.set(p.getRendaMensal() != null ? p.getRendaMensal() : BigDecimal.ZERO);

        // Nota: Se o seu Model Proponente ainda não tem os campos de modalidade,
        // o código abaixo garante que o "Original" ignore mudanças feitas na UI após o
        // load.
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

        // 2. Sincroniza o "Carimbo" Original (Dirty Checking)
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

    // --- 4. A LÓGICA DE ATIVAÇÃO DO BOTÃO SALVAR ---
    public BooleanBinding podeSalvarProperty() {
        return Bindings.createBooleanBinding(() -> {
            try {
                // 1. REGRA PRIORITÁRIA: Se o sistema estiver salvando, o botão SEMPRE fica
                // desativado
                if (carregando.get()) {
                    return false;
                }

                // 2. Validação de Obrigatoriedade (Nome e CPF)
                // Extraímos apenas números do CPF para validar se tem 11 dígitos
                boolean nomeValido = nome.get() != null && !nome.get().trim().isEmpty();
                String cpfNumeros = cpf.get() != null ? cpf.get().replaceAll("[^0-9]", "") : "";
                boolean cpfValido = cpfNumeros.length() == 11;

                boolean dadosCorretos = nomeValido && cpfValido;

                // 3. "Dirty Checking" Global (Verifica se QUALQUER campo mudou em relação ao
                // original)
                // Usamos java.util.Objects.equals para segurança contra valores nulos
                boolean houveAlteracao = !java.util.Objects.equals(nome.get(), nomeOriginal) ||
                        !java.util.Objects.equals(cpf.get(), cpfOriginal) ||
                        !java.util.Objects.equals(telefone.get(), telefoneOriginal) ||
                        !java.util.Objects.equals(origem.get(), origemOriginal) ||
                        !java.util.Objects.equals(dataNascimento.get(), dataNascimentoOriginal) ||
                        convenio.get() != convenioOriginal ||
                        !java.util.Objects.equals(vinculo.get(), vinculoOriginal) ||
                        !java.util.Objects.equals(matricula.get(), matriculaOriginal) ||
                        (renda.get() != null && renda.get().compareTo(rendaOriginal) != 0) ||

                // Comparação de todas as 12 modalidades
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

                // 4. Lógica Final de Ativação
                if (!editando.get()) {
                    // Para Novo Lead: Ativa se os dados obrigatórios estiverem corretos
                    return dadosCorretos;
                } else {
                    // Para Edição: Ativa se estiver correto E houver alguma mudança real
                    return dadosCorretos && houveAlteracao;
                }

            } catch (Exception e) {
                // Em caso de erro interno no binding, imprime o log no terminal para depuração
                e.printStackTrace();
                return false;
            }
        },
                // --- DEPENDÊNCIAS (O JavaFX observa todas elas para disparar a reavaliação)
                // ---
                nome, cpf, telefone, origem, dataNascimento, convenio, vinculo, matricula, renda,
                chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
                chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal,
                editando, carregando);
    }

    /**
     * Verifica se há qualquer diferença entre o que está na tela
     * e o que foi carregado originalmente do banco.
     */
    public boolean temAlteracoesPendentes() {
        return !java.util.Objects.equals(nome.get(), nomeOriginal) ||
                !java.util.Objects.equals(cpf.get(), cpfOriginal) ||
                !java.util.Objects.equals(telefone.get(), telefoneOriginal) ||
                !java.util.Objects.equals(origem.get(), origemOriginal) ||
                !java.util.Objects.equals(dataNascimento.get(), dataNascimentoOriginal) ||
                convenio.get() != convenioOriginal ||
                !java.util.Objects.equals(vinculo.get(), vinculoOriginal) ||
                !java.util.Objects.equals(matricula.get(), matriculaOriginal) ||
                (renda.get() != null && renda.get().compareTo(rendaOriginal) != 0) ||
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
    
    // --- 5. GETTERS DAS PROPERTIES (Para o Controller) ---

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