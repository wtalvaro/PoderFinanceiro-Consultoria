package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.utils.ContatoUtils;
import br.com.poderfinanceiro.app.utils.DataUtils;
import br.com.poderfinanceiro.app.utils.DocumentoUtils;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Component
@Scope("prototype")
public class LeadController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String MSG_NOVO_CONTATO = "Cadastrar Novo Contato";
    private static final String MSG_EDITAR_CONTATO = "Editando Contato: ";
    private static final String PADRAO_DATA = "dd/MM/yyyy";

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private Label lblTituloTela;
    @FXML
    private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML
    private ComboBox<OrigemLeadModel> cbOrigem;
    @FXML
    private ComboBox<TipoVinculoModel> cbVinculo;
    @FXML
    private ComboBox<TipoRelacionamentoModel> cbClassificacao;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private ScrollPane scrollPrincipal;

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final LeadViewModel viewModel;

    public LeadController(LeadViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarTituloDinamico();
        configurarListasEFormatadoresDeData();
        estabelecerBindings();
        configurarAutoSelecao(txtRenda);
    }

    private void configurarTituloDinamico() {
        lblTituloTela.textProperty().bind(Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.isBlank()) ? MSG_NOVO_CONTATO : MSG_EDITAR_CONTATO + nome;
        }, viewModel.nomeProperty()));
    }

    private void configurarListasEFormatadoresDeData() {
        configurarCombo(cbOrigem, OrigemLeadModel.values(), OrigemLeadModel::fromString);
        configurarCombo(cbVinculo, TipoVinculoModel.values(), TipoVinculoModel::fromString);
        configurarCombo(cbClassificacao, TipoRelacionamentoModel.values(), TipoRelacionamentoModel::fromString);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PADRAO_DATA);
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values,
            Function<String, T> searcher) {
        combo.getItems().setAll(values);
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T obj) {
                return (obj != null) ? obj.getLabel() : "";
            }

            @Override
            public T fromString(String str) {
                return searcher.apply(str);
            }
        });
    }

    // =========================================================================
    // BINDINGS (SRP Aplicado)
    // =========================================================================
    private void estabelecerBindings() {
        vincularCamposSimples();
        vincularCamposComFormatacao();
    }

    private void vincularCamposSimples() {
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());
    }

    private void vincularCamposComFormatacao() {
        // Data de Nascimento
        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

        // Renda
        TextFormatter<BigDecimal> rendaFormatter = FinanceiroUtils.criarFormatadorMoeda();
        txtRenda.setTextFormatter(rendaFormatter);
        rendaFormatter.valueProperty().bindBidirectional(viewModel.rendaProperty());

        // CPF
        TextFormatter<String> cpfFormatter = DocumentoUtils.criarFormatadorCpf();
        txtCpf.setTextFormatter(cpfFormatter);
        cpfFormatter.valueProperty().bindBidirectional(viewModel.cpfProperty());

        // Telefone
        TextFormatter<String> telefoneFormatter = ContatoUtils.criarFormatadorTelefone();
        txtTelefone.setTextFormatter(telefoneFormatter);
        telefoneFormatter.valueProperty().bindBidirectional(viewModel.telefoneProperty());
    }

    // =========================================================================
    // UTILITÁRIOS DE UI (DRY Aplicado)
    // =========================================================================
    private void configurarAutoSelecao(TextField... campos) {
        for (TextField campo : campos) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }
}