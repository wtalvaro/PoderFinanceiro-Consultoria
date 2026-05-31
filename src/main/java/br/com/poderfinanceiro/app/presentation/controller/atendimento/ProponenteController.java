package br.com.poderfinanceiro.app.presentation.controller.atendimento;

import br.com.poderfinanceiro.app.common.util.ContatoUtils;
import br.com.poderfinanceiro.app.common.util.DataUtils;
import br.com.poderfinanceiro.app.common.util.DocumentoUtils;
import br.com.poderfinanceiro.app.common.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.domain.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.domain.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.presentation.viewmodel.LeadViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * <h1>ProponenteController</h1>
 * Controlador Humble Object para o formulário de dados do cliente.
 */
@Component
@Scope("prototype")
public class ProponenteController {

    private static final Logger log = LoggerFactory.getLogger(ProponenteController.class);
    private static final String LOG_PREFIX = "[ProponenteController]";

    private static final String MSG_NOVO_CONTATO = "Cadastrar Novo Contato";
    private static final String MSG_EDITAR_CONTATO = "Editando Contato: ";
    private static final String PADRAO_DATA = "dd/MM/yyyy";

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

    public ProponenteController(LeadViewModel viewModel) {
        this.viewModel = viewModel;
        log.info("{} [SISTEMA] Instanciando controlador de formulário de proponente.", LOG_PREFIX);
    }

    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando componentes da UI e Bindings.", LOG_PREFIX);
        configurarTituloDinamico();
        configurarListasEFormatadoresDeData();
        estabelecerBindings();
        configurarAutoSelecao(txtRenda);
        log.debug("{} [SISTEMA] Inicialização concluída com sucesso.", LOG_PREFIX);
    }

    private void configurarTituloDinamico() {
        log.trace("{} [NEGOCIO] Configurando binding de título reativo.", LOG_PREFIX);
        lblTituloTela.textProperty().bind(Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.isBlank()) ? MSG_NOVO_CONTATO : MSG_EDITAR_CONTATO + nome;
        }, viewModel.nomeProperty()));
    }

    private void configurarListasEFormatadoresDeData() {
        log.debug("{} [SISTEMA] Carregando domínios de ComboBoxes e formatadores.", LOG_PREFIX);
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

    private void estabelecerBindings() {
        log.info("{} [SISTEMA] Estabelecendo sincronização bidirecional com ViewModel.", LOG_PREFIX);

        // Campos Simples
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());

        // Campos com Máscaras e Utilitários
        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

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

    private void configurarAutoSelecao(TextField... campos) {
        for (TextField campo : campos) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    log.trace("{} [UI] Auto-seleção ativada para campo: {}", LOG_PREFIX, campo.getId());
                    Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }
}
