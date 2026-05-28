package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.domain.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.DataUtils;
import br.com.poderfinanceiro.app.util.DocumentoUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
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
 * <p>
 * Controlador de Interface (UI) responsável pelo formulário de dados do
 * cliente. Atua como um <b>Humble Object</b> puro, gerenciando apenas bindings
 * e formatações visuais. A persistência é orquestrada pelo
 * AtendimentoHubController.
 * </p>
 */
@Component @Scope("prototype")
public class ProponenteController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(ProponenteController.class);
    private static final String LOG_PREFIX = "[ProponenteController]";

    private static final String MSG_NOVO_CONTATO = "Cadastrar Novo Contato";
    private static final String MSG_EDITAR_CONTATO = "Editando Contato: ";
    private static final String PADRAO_DATA = "dd/MM/yyyy";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final LeadViewModel viewModel;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private Label lblTituloTela;
    @FXML private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML private ComboBox<OrigemLeadModel> cbOrigem;
    @FXML private ComboBox<TipoVinculoModel> cbVinculo;
    @FXML private ComboBox<TipoRelacionamentoModel> cbClassificacao;
    @FXML private DatePicker dpDataNascimento;
    @FXML private ProgressIndicator progress;
    @FXML private ScrollPane scrollPrincipal;

    public ProponenteController(LeadViewModel viewModel) {
        this.viewModel = viewModel;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring (Prototype).", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando formulário de proponente...", LOG_PREFIX);
        configurarTituloDinamico();
        configurarListasEFormatadoresDeData();
        estabelecerBindings();
        configurarAutoSelecao(txtRenda);
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarTituloDinamico() {
        log.trace("{} [UI] Configurando título dinâmico reativo.", LOG_PREFIX);
        lblTituloTela.textProperty().bind(Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.isBlank()) ? MSG_NOVO_CONTATO : MSG_EDITAR_CONTATO + nome;
        }, viewModel.nomeProperty()));
    }

    private void configurarListasEFormatadoresDeData() {
        log.trace("{} [UI] Carregando ComboBoxes e formatadores de data.", LOG_PREFIX);
        configurarCombo(cbOrigem, OrigemLeadModel.values(), OrigemLeadModel::fromString);
        configurarCombo(cbVinculo, TipoVinculoModel.values(), TipoVinculoModel::fromString);
        configurarCombo(cbClassificacao, TipoRelacionamentoModel.values(), TipoRelacionamentoModel::fromString);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PADRAO_DATA);
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values, Function<String, T> searcher) {
        combo.getItems().setAll(values);
        combo.setConverter(new StringConverter<T>() {
            @Override public String toString(T obj) {
                return (obj != null) ? obj.getLabel() : "";
            }

            @Override public T fromString(String str) {
                return searcher.apply(str);
            }
        });
    }

    private void estabelecerBindings() {
        log.trace("{} [UI] Estabelecendo bindings bidirecionais com o ViewModel.", LOG_PREFIX);
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

    // ==========================================================================================
    // MÓDULO 6: UTILITÁRIOS
    // ==========================================================================================
    private void configurarAutoSelecao(TextField... campos) {
        log.trace("{} [UI] Configurando auto-seleção de texto para campos numéricos.", LOG_PREFIX);
        for (TextField campo : campos) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado)
                    Platform.runLater(campo::selectAll);
            });
        }
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }
}
