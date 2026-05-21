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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class LeadController {

    private final LeadViewModel viewModel;

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

    public LeadController(LeadViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        configurarListasEFormatores();
        estabelecerBindings();
        configurarAutoSelecao();

        lblTituloTela.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.trim().isEmpty()) ? "Cadastrar Novo Contato" : "Editando Contato: " + nome;
        }, viewModel.nomeProperty()));
    }

    private void configurarListasEFormatores() {
        configurarCombo(cbOrigem, OrigemLeadModel.values(), OrigemLeadModel::fromString);
        configurarCombo(cbVinculo, TipoVinculoModel.values(), TipoVinculoModel::fromString);
        configurarCombo(cbClassificacao, TipoRelacionamentoModel.values(), TipoRelacionamentoModel::fromString);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new javafx.util.converter.LocalDateStringConverter(formatter, formatter));
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values,
            java.util.function.Function<String, T> searcher) {
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

    private void estabelecerBindings() {
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());

        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());

        TextFormatter<BigDecimal> rendaFormatter = FinanceiroUtils.criarFormatadorMoeda();
        txtRenda.setTextFormatter(rendaFormatter);
        rendaFormatter.valueProperty().bindBidirectional(viewModel.rendaProperty());

        TextFormatter<String> cpfFormatter = DocumentoUtils.criarFormatadorCpf();
        txtCpf.setTextFormatter(cpfFormatter);
        cpfFormatter.valueProperty().bindBidirectional(viewModel.cpfProperty());

        TextFormatter<String> telefoneFormatter = ContatoUtils.criarFormatadorTelefone();
        txtTelefone.setTextFormatter(telefoneFormatter);
        telefoneFormatter.valueProperty().bindBidirectional(viewModel.telefoneProperty());
    }

    private void configurarAutoSelecao() {
        TextField[] camposFinanceiros = { txtRenda };
        for (TextField campo : camposFinanceiros) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    javafx.application.Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }

}