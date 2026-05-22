package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.ComissaoModel;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.viewmodel.ComissaoViewModel;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.utils.CicloFinanceiroUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import org.controlsfx.control.PopOver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class ComissoesController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String STATUS_PAGO = "Pago";
    private static final String STATUS_PENDENTE = "Pendente";
    private static final String STATUS_ESTORNADO = "Estornado";
    private static final String STATUS_LIQUIDADO = "Liquidado";

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String MSG_SYNC_CAIXA = "Sincronizando fluxo de caixa...";
    private static final String CSS_POPOVER = "/css/popover-custom.css";

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private TableView<ComissaoModel> tableComissoes;
    @FXML
    private TableColumn<ComissaoModel, String> colPrevisao;
    @FXML
    private TableColumn<ComissaoModel, String> colCliente, colBanco, colValorBruto, colStatus;
    @FXML
    private TableColumn<ComissaoModel, String> colRecebBanco, colVlrPago;

    @FXML
    private TextField txtBusca;
    @FXML
    private VBox boxFormularioAjuste;
    @FXML
    private Label lblTituloModal, lblTotalPendente, lblTotalRecebido, lblStatusCiclo, lblCicloBadge;
    @FXML
    private DatePicker dpRecebimentoBanco, dpPrevisaoPagamento;
    @FXML
    private CheckBox cbVerificado, cbContestada;
    @FXML
    private TextField txtValorPagoPoder;
    @FXML
    private ComboBox<String> comboStatus;
    @FXML
    private Button btnSalvarAjuste, btnSalvarConciliacao;
    @FXML
    private VBox bannerStatusCiclo;
    @FXML
    private TextArea txtObservacao;

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final ComissaoRepository repository;
    private final ComissaoViewModel viewModel;
    private final MainController mainController;
    private final AtendimentoContextService contextoService;

    private final ObservableList<ComissaoModel> masterData = FXCollections.observableArrayList();
    private PopOver popOverAjuste;

    public ComissoesController(ComissaoRepository repository, ComissaoViewModel viewModel,
            MainController mainController, AtendimentoContextService contextoService) {
        this.repository = repository;
        this.viewModel = viewModel;
        this.mainController = mainController;
        this.contextoService = contextoService;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarPopOver();
        configurarBloqueiosFinanceiros();
        configurarTabela();
        configurarFiltroReativo();
        configurarBindingsCicloFinanceiro();
        recarregarDados();
    }

    private void configurarPopOver() {
        popOverAjuste = new PopOver(boxFormularioAjuste);
        popOverAjuste.setAnimated(false);
        popOverAjuste.setFadeInDuration(Duration.ZERO);
        popOverAjuste.setFadeOutDuration(Duration.ZERO);
        popOverAjuste.getRoot().getStylesheets().add(getClass().getResource(CSS_POPOVER).toExternalForm());
        popOverAjuste.setArrowSize(0);
        popOverAjuste.setTitle("Ajuste de Comissão");
        popOverAjuste.setHeaderAlwaysVisible(true);
        popOverAjuste.setDetachable(true);
        popOverAjuste.setCornerRadius(0);
    }

    private void configurarBloqueiosFinanceiros() {
        // Bloqueio de integridade: Datas calculadas, não digitadas.
        travarInteracaoDatePicker(dpRecebimentoBanco);
        travarInteracaoDatePicker(dpPrevisaoPagamento);
    }

    private void travarInteracaoDatePicker(DatePicker datePicker) {
        if (datePicker != null) {
            datePicker.setEditable(false);
            datePicker.setMouseTransparent(true);
            datePicker.setFocusTraversable(false);
        }
    }

    // =========================================================================
    // CONFIGURAÇÃO DA TABELA E FILTROS
    // =========================================================================
    private void configurarTabela() {
        colRecebBanco.setCellValueFactory(d -> formatarDataHora(d.getValue().getDataRecebimentoBanco()));
        colPrevisao.setCellValueFactory(d -> formatarDataSimples(d.getValue().getPrevisaoPagamento()));

        colCliente.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getProposta().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProposta().getBanco().getNome()));

        colValorBruto.setCellValueFactory(d -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(d.getValue().getValorBrutoComissao())));
        colVlrPago.setCellValueFactory(d -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(d.getValue().getValorPagoPelaPoder())));

        configurarCelulaStatus();
        configurarInteracaoLinhas();
    }

    private void configurarCelulaStatus() {
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ComissaoModel c = getTableRow().getItem();
                    aplicarEstiloStatus(this, c);
                }
            }
        });
    }

    private void aplicarEstiloStatus(TableCell<ComissaoModel, String> celula, ComissaoModel comissao) {
        celula.setStyle("-fx-font-weight: bold;");
        if (comissao.isContestada()) {
            celula.setText("⚠️ CONTESTADA");
            celula.setTextFill(Color.RED);
        } else if (STATUS_PAGO.equalsIgnoreCase(comissao.getStatusPagamento())) {
            celula.setText("✅ PAGO");
            celula.setTextFill(Color.GREEN);
        } else if (comissao.isVerificadoConsultor()) {
            celula.setText("🔎 CONFERIDO");
            celula.setTextFill(Color.BLUE);
        } else {
            celula.setText("⏳ PENDENTE");
            celula.setTextFill(Color.ORANGE);
        }
    }

    private void configurarInteracaoLinhas() {
        tableComissoes.setRowFactory(tv -> {
            TableRow<ComissaoModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    prepararAjuste(row.getItem(), row);
                }
            });
            return row;
        });
    }

    private void configurarFiltroReativo() {
        FilteredList<ComissaoModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBusca.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(comissao -> atendeCriterioDeBusca(comissao, newValue));
        });

        SortedList<ComissaoModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableComissoes.comparatorProperty());
        tableComissoes.setItems(sortedData);
    }

    private boolean atendeCriterioDeBusca(ComissaoModel comissao, String termo) {
        if (termo == null || termo.isBlank())
            return true;

        String filter = termo.toLowerCase();
        return comissao.getProposta().getProponente().getNomeCompleto().toLowerCase().contains(filter) ||
                comissao.getProposta().getBanco().getNome().toLowerCase().contains(filter) ||
                comissao.getStatusPagamento().toLowerCase().contains(filter);
    }

    // =========================================================================
    // BINDINGS E CICLO FINANCEIRO
    // =========================================================================
    private void configurarBindingsCicloFinanceiro() {
        vincularDatas();
        vincularStatus();
        vincularConferencia();
        vincularValores();
        txtObservacao.textProperty().bindBidirectional(viewModel.observacaoAjusteProperty());
    }

    private void vincularDatas() {
        StringConverter<LocalDate> conversorData = criarConversorDataBrasil();
        dpRecebimentoBanco.setConverter(conversorData);
        dpPrevisaoPagamento.setConverter(conversorData);

        dpRecebimentoBanco.valueProperty().addListener((obs, old, newDate) -> viewModel.dataRecebimentoBancoProperty()
                .set(newDate != null ? newDate.atStartOfDay() : null));
        dpPrevisaoPagamento.valueProperty().bindBidirectional(viewModel.previsaoPagamentoProperty());
    }

    private void vincularStatus() {
        comboStatus.getItems().setAll(STATUS_PENDENTE, STATUS_PAGO, STATUS_ESTORNADO);
        comboStatus.valueProperty().bindBidirectional(viewModel.statusPagamentoProperty());
        cbContestada.selectedProperty().bindBidirectional(viewModel.contestadaProperty());
    }

    private void vincularConferencia() {
        cbVerificado.selectedProperty().bindBidirectional(viewModel.verificadoConsultorProperty());
        aplicarTravaHorarioQuinta();
    }

    private void vincularValores() {
        txtValorPagoPoder.textProperty().bindBidirectional(
                viewModel.valorPagoPelaPoderProperty(),
                criarConversorBigDecimal());
    }

    private void aplicarTravaHorarioQuinta() {
        LocalDateTime agora = LocalDateTime.now();
        boolean prazoUltrapassado = agora.getDayOfWeek() == DayOfWeek.THURSDAY && agora.getHour() >= 15;

        if (prazoUltrapassado) {
            cbVerificado.setDisable(true);
            cbVerificado.setTooltip(new Tooltip("Prazo de conferência encerrado às 15:00h."));
        }
    }

    // =========================================================================
    // COMUNICAÇÃO DE DADOS
    // =========================================================================
    public void recarregarDados() {
        mainController.mostrarLoading(MSG_SYNC_CAIXA);

        List<ComissaoModel> dados = repository.findAllComDetalhes();
        masterData.setAll(dados);
        atualizarCardsResumo(dados);
        contextoService.setComissoesAtivas(dados);

        mainController.ocultarLoading();
    }

    private void atualizarCardsResumo(List<ComissaoModel> comissoes) {
        BigDecimal totalPendente = somarValoresPorStatus(comissoes, false);
        BigDecimal totalRecebido = somarValoresPorStatus(comissoes, true);

        lblTotalPendente.setText(FinanceiroUtils.formatarParaExibicao(totalPendente));
        lblTotalRecebido.setText(FinanceiroUtils.formatarParaExibicao(totalRecebido));
    }

    private BigDecimal somarValoresPorStatus(List<ComissaoModel> comissoes, boolean isRecebido) {
        return comissoes.stream()
                .filter(c -> isRecebido == STATUS_PAGO.equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> {
                    BigDecimal valor = isRecebido ? c.getValorPagoPelaPoder() : c.getValorBrutoComissao();
                    return valor != null ? valor : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =========================================================================
    // INTERFACE DE AJUSTE (MODAL/POPOVER)
    // =========================================================================
    private void prepararAjuste(ComissaoModel comissao, Node anchor) {
        viewModel.loadFromModel(comissao);
        carregarDatasNoFormulario(comissao);

        lblTituloModal.setText("Conciliação: " + comissao.getProposta().getProponente().getNomeCompleto());
        lblCicloBadge.setText("Ciclo: " + resolverNomeCiclo(comissao));

        atualizarEstadoInterfaceCiclo(comissao);
        exibirPopOverCentralizado();
    }

    private void carregarDatasNoFormulario(ComissaoModel comissao) {
        if (comissao.getDataRecebimentoBanco() != null) {
            dpRecebimentoBanco.setValue(comissao.getDataRecebimentoBanco().toLocalDate());
        }
        if (comissao.getPrevisaoPagamento() != null) {
            dpPrevisaoPagamento.setValue(comissao.getPrevisaoPagamento());
        }
    }

    private String resolverNomeCiclo(ComissaoModel comissao) {
        String cicloBadge = comissao.getCicloReferencia();
        if (cicloBadge == null && comissao.getDataRecebimentoBanco() != null) {
            cicloBadge = CicloFinanceiroUtils.identificarCiclo(comissao.getDataRecebimentoBanco());
        }
        return cicloBadge != null ? cicloBadge : "Legado";
    }

    private void atualizarEstadoInterfaceCiclo(ComissaoModel comissao) {
        LocalDateTime agora = LocalDateTime.now();
        boolean jaLiquidado = STATUS_PAGO.equalsIgnoreCase(comissao.getStatusPagamento()) ||
                STATUS_LIQUIDADO.equalsIgnoreCase(comissao.getStatusPagamento());

        boolean prazoContestacaoExpirado = comissao.getDataLimiteContestacao() != null &&
                agora.isAfter(comissao.getDataLimiteContestacao());

        if (jaLiquidado) {
            configurarSemoforo("✅ CICLO LIQUIDADO: Registro imutável e Arquivado.",
                    "-color-success-subtle", "-color-success-emphasis", true, true);
        } else if (prazoContestacaoExpirado) {
            configurarSemoforo("🟡 AGUARDANDO LIQUIDAÇÃO: O prazo de contestação do consultor expirou.",
                    "-color-warning-subtle", "-color-warning-emphasis", true, false);
        } else {
            configurarSemoforo("🔵 CICLO ABERTO: Conferência do consultor disponível até Quinta às 15:00.",
                    "-color-accent-subtle", "-color-accent-emphasis", false, false);
        }
    }

    private void configurarSemoforo(String texto, String corFundo, String corTexto, boolean travarConsultor,
            boolean travarTudo) {
        lblStatusCiclo.setText(texto);
        bannerStatusCiclo
                .setStyle("-fx-background-color: " + corFundo + "; -fx-padding: 10; -fx-background-radius: 5;");
        lblStatusCiclo.setStyle("-fx-text-fill: " + corTexto + "; -fx-font-weight: bold;");

        alternarEstadoCamposConsultor(travarConsultor);
        alternarEstadoCamposGestor(travarTudo);
    }

    private void alternarEstadoCamposConsultor(boolean travar) {
        if (cbVerificado != null)
            cbVerificado.setDisable(travar);
        if (cbContestada != null)
            cbContestada.setDisable(travar);
    }

    private void alternarEstadoCamposGestor(boolean travar) {
        if (txtValorPagoPoder != null)
            txtValorPagoPoder.setDisable(travar);
        if (comboStatus != null)
            comboStatus.setDisable(travar);
        if (dpPrevisaoPagamento != null)
            dpPrevisaoPagamento.setDisable(travar);
        if (dpRecebimentoBanco != null)
            dpRecebimentoBanco.setDisable(travar);
        if (txtObservacao != null)
            txtObservacao.setDisable(travar);
        if (btnSalvarConciliacao != null)
            btnSalvarConciliacao.setDisable(travar);
    }

    private void exibirPopOverCentralizado() {
        Window window = tableComissoes.getScene().getWindow();
        popOverAjuste.setOpacity(0);
        popOverAjuste.show(window);

        double x = window.getX() + (window.getWidth() / 2) - (popOverAjuste.getWidth() / 2);
        double y = window.getY() + (window.getHeight() / 2) - (popOverAjuste.getHeight() / 2);

        popOverAjuste.setX(x);
        popOverAjuste.setY(y);
        popOverAjuste.setOpacity(1);
    }

    @FXML
    private void salvarAjuste() {
        if (viewModel.isDirty()) {
            repository.findById(viewModel.idProperty().get()).ifPresent(comissaoDoBanco -> {
                ComissaoModel paraSalvar = viewModel.atualizarModel(comissaoDoBanco);
                repository.save(paraSalvar);
                recarregarDados();
            });
        }
        fecharModal();
    }

    @FXML
    private void fecharModal() {
        if (popOverAjuste != null && popOverAjuste.isShowing()) {
            popOverAjuste.hide();
        }
        viewModel.reset();
        dpRecebimentoBanco.setValue(null);
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS (Fábricas de Conversores)
    // =========================================================================
    private SimpleStringProperty formatarDataHora(LocalDateTime data) {
        return new SimpleStringProperty(data != null ? data.format(FMT_DATA_HORA) : "-");
    }

    private SimpleStringProperty formatarDataSimples(LocalDate data) {
        return new SimpleStringProperty(data != null ? data.format(FMT_DATA) : "-");
    }

    private StringConverter<LocalDate> criarConversorDataBrasil() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? FMT_DATA.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isBlank()) {
                    try {
                        return LocalDate.parse(string, FMT_DATA);
                    } catch (DateTimeParseException ignored) {
                    }
                }
                return null;
            }
        };
    }

    private StringConverter<BigDecimal> criarConversorBigDecimal() {
        return new BigDecimalStringConverter() {
            @Override
            public BigDecimal fromString(String value) {
                if (value == null || value.isBlank())
                    return BigDecimal.ZERO;
                String limpo = value.replaceAll("[R$\\s]", "").replace(",", ".");
                try {
                    return new BigDecimal(limpo);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        };
    }
}