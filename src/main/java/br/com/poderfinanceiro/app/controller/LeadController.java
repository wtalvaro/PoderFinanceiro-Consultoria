package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.OrigemLead;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.model.TipoRelacionamento;
import br.com.poderfinanceiro.app.model.TipoVinculo;
import br.com.poderfinanceiro.app.model.Labeled;
import br.com.poderfinanceiro.app.utils.ContatoUtils;
import br.com.poderfinanceiro.app.utils.DataUtils;
import br.com.poderfinanceiro.app.utils.DocumentoUtils;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype") // Essencial para isolar cada cliente
public class LeadController {

    // --- DEPENDÊNCIAS ---
    private final LeadViewModel viewModel;

    // --- FXML BINDINGS ---
    @FXML
    private Label lblTituloTela;
    @FXML
    private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML
    private ComboBox<OrigemLead> cbOrigem;
    @FXML
    private ComboBox<TipoVinculo> cbVinculo;
    @FXML
    private ComboBox<TipoConvenio> cbConvenio;
    @FXML
    private ComboBox<TipoRelacionamento> cbClassificacao;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private ScrollPane scrollPrincipal;

    public LeadController(LeadViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        configurarListasEFormatores();
        estabelecerBindings();

        // A Label agora vai reagir automaticamente a qualquer mudança no nome (seja do
        // banco ou da digitação)
        lblTituloTela.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.trim().isEmpty()) ? "Cadastrar Novo Contato" : "Editando Contato: " + nome;
        }, viewModel.nomeProperty()));
    }

    // ========================================================================
    // SETUP E BINDINGS
    // ========================================================================

    private void configurarListasEFormatores() {
        // 1. Populando as listas
        configurarCombo(cbOrigem, OrigemLead.values(), OrigemLead::fromString);
        configurarCombo(cbVinculo, TipoVinculo.values(), TipoVinculo::fromString);
        configurarCombo(cbConvenio, TipoConvenio.values(), TipoConvenio::fromString);
        configurarCombo(cbClassificacao, TipoRelacionamento.values(), TipoRelacionamento::fromString);

        // Formato de data brasileiro para o DatePicker
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new javafx.util.converter.LocalDateStringConverter(formatter, formatter));
    }

    /**
     * Método Genérico com Strategy Pattern.
     * T: Deve ser um Enum e implementar Labeled.
     * searcher: Uma função que define como converter String de volta para o Enum.
     */
    private <T extends Enum<T> & Labeled> void configurarCombo(ComboBox<T> combo, T[] values,
            java.util.function.Function<String, T> searcher) {
        combo.getItems().setAll(values);

        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T obj) {
                return (obj != null) ? obj.getLabel() : ""; // Usa o label para exibição
            }

            @Override
            public T fromString(String str) {
                return searcher.apply(str); // Usa a estratégia de busca específica do Enum
            }
        });
    }

    private void estabelecerBindings() {
        // 1. TextFields e ComboBoxes (Sem máscara especial)
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());
        // ====================================================================
        // BINDING DA DATA DE NASCIMENTO (Com Máscara Automática)
        // ====================================================================

        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();

        // Aplicamos o formatador ao campo de texto interno do DatePicker
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);

        // Sincronizamos o valor do formatador com o ViewModel
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());

        // Sincronizamos o valor do DatePicker com o formatador para que
        // ao selecionar no calendário, o texto também seja atualizado corretamente.
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

        cbConvenio.valueProperty().bindBidirectional(viewModel.convenioProperty());
        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());

        // ====================================================================
        // 3. CAMPOS COM MÁSCARA (TextFormatter)
        // ====================================================================

        // --- RENDA ---
        TextFormatter<BigDecimal> rendaFormatter = FinanceiroUtils.criarFormatadorMoeda();
        txtRenda.setTextFormatter(rendaFormatter);
        rendaFormatter.valueProperty().bindBidirectional(viewModel.rendaProperty());

        // --- CPF ---
        TextFormatter<String> cpfFormatter = DocumentoUtils.criarFormatadorCpf();
        txtCpf.setTextFormatter(cpfFormatter);
        cpfFormatter.valueProperty().bindBidirectional(viewModel.cpfProperty());

        // --- TELEFONE ---
        TextFormatter<String> telefoneFormatter = ContatoUtils.criarFormatadorTelefone();
        txtTelefone.setTextFormatter(telefoneFormatter);
        telefoneFormatter.valueProperty().bindBidirectional(viewModel.telefoneProperty());
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }
}