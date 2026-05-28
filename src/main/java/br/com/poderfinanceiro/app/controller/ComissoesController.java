package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ComissaoUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.facade.IComissaoFacade;
import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.ComissaoViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * <h1>ComissoesController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela Gestão de Repasses (RV).
 * Implementa o padrão <b>Humble Object</b>, delegando cálculos financeiros,
 * travas de horário e persistência para a {@link IComissaoFacade}.
 * </p>
 */
@Component
public class ComissoesController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(ComissoesController.class);
    private static final String LOG_PREFIX = "[ComissoesController]";

    private static final String STATUS_PAGO = "Pago";
    private static final String STATUS_PENDENTE = "Pendente";
    private static final String STATUS_ESTORNADO = "Estornado";
    private static final String STATUS_LIQUIDADO = "Liquidado";

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String MSG_SYNC_CAIXA = "Sincronizando fluxo de caixa...";
    private static final String CSS_POPOVER = "/css/popover-custom.css";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IComissaoFacade comissaoFacade;
    private final ComissaoViewModel viewModel;
    private final Navigator navigator;
    private final ComissaoUIEventHub eventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TableView<ComissaoModel> tableComissoes;
    @FXML private TableColumn<ComissaoModel, String> colPrevisao, colCliente, colBanco, colValorBruto, colStatus, colRecebBanco, colVlrPago;
    @FXML private TextField txtBusca;
    @FXML private VBox boxFormularioAjuste;
    @FXML private Label lblTituloModal, lblTotalPendente, lblTotalRecebido, lblStatusCiclo, lblCicloBadge;
    @FXML private DatePicker dpRecebimentoBanco, dpPrevisaoPagamento;
    @FXML private CheckBox cbVerificado, cbContestada;
    @FXML private TextField txtValorPagoPoder;
    @FXML private ComboBox<String> comboStatus;
    @FXML private Button btnSalvarAjuste, btnSalvarConciliacao;
    @FXML private VBox bannerStatusCiclo;
    @FXML private TextArea txtObservacao;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<ComissaoModel> masterData = FXCollections.observableArrayList();
    private PopOver popOverAjuste;

    public ComissoesController(IComissaoFacade comissaoFacade, ComissaoViewModel viewModel, Navigator navigator,
            ComissaoUIEventHub eventHub) {
        this.comissaoFacade = comissaoFacade;
        this.viewModel = viewModel;
        this.navigator = navigator;
        this.eventHub = eventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Comissões...", LOG_PREFIX);
        eventHub.inscrever(this::recarregarDados);
        configurarPopOver();
        configurarBloqueiosFinanceiros();
        configurarTabela();
        configurarFiltroReativo();
        configurarBindingsCicloFinanceiro();
        recarregarDados();

        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d repasse(s)", Bindings.size(masterData)));
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    private void configurarPopOver() {
        log.trace("{} [UI] Configurando PopOver de ajuste.", LOG_PREFIX);
        URL cssUrl = getClass().getResource(CSS_POPOVER);
        if (cssUrl == null) {
            log.warn("{} [UI] CSS '{}' não encontrado. PopOver sem estilo customizado.", LOG_PREFIX, CSS_POPOVER);
        }

        popOverAjuste = new PopOver(boxFormularioAjuste);
        popOverAjuste.setAnimated(false);
        popOverAjuste.setFadeInDuration(Duration.ZERO);
        popOverAjuste.setFadeOutDuration(Duration.ZERO);

        if (cssUrl != null)
            popOverAjuste.getRoot().getStylesheets().add(cssUrl.toExternalForm());

        popOverAjuste.setArrowSize(0);
        popOverAjuste.setTitle("Ajuste de Comissão");
        popOverAjuste.setHeaderAlwaysVisible(true);
        popOverAjuste.setDetachable(true);
        popOverAjuste.setCornerRadius(0);
    }

    private void configurarBloqueiosFinanceiros() {
        log.trace("{} [UI] Travando interação direta nos DatePickers.", LOG_PREFIX);
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

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DA TABELA E FILTROS
    // ==========================================================================================
    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da TableView.", LOG_PREFIX);
        colRecebBanco.setCellValueFactory(d -> formatarDataHora(d.getValue().getDataRecebimentoBanco()));
        colPrevisao.setCellValueFactory(d -> formatarDataSimples(d.getValue().getPrevisaoPagamento()));
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProposta().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProposta().getBanco().getNome()));
        colValorBruto.setCellValueFactory(
                d -> new SimpleStringProperty(FinanceiroUtils.formatarParaExibicao(d.getValue().getValorBrutoComissao())));
        colVlrPago.setCellValueFactory(
                d -> new SimpleStringProperty(FinanceiroUtils.formatarParaExibicao(d.getValue().getValorPagoPelaPoder())));

        configurarCelulaStatus();
        configurarInteracaoLinhas();
    }

    private void configurarCelulaStatus() {
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    aplicarEstiloStatus(this, getTableRow().getItem());
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
                    log.info("{} [TELEMETRIA] Duplo clique na comissão ID={}", LOG_PREFIX, row.getItem().getId());
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
            log.debug("{} [UI] Filtro aplicado: '{}'", LOG_PREFIX, newValue);
        });

        SortedList<ComissaoModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableComissoes.comparatorProperty());
        tableComissoes.setItems(sortedData);
    }

    private boolean atendeCriterioDeBusca(ComissaoModel comissao, String termo) {
        if (termo == null || termo.isBlank())
            return true;
        String filter = termo.toLowerCase();
        return comissao.getProposta().getProponente().getNomeCompleto().toLowerCase().contains(filter)
                || comissao.getProposta().getBanco().getNome().toLowerCase().contains(filter)
                || comissao.getStatusPagamento().toLowerCase().contains(filter);
    }

    // ==========================================================================================
    // MÓDULO 7: BINDINGS E CICLO FINANCEIRO
    // ==========================================================================================
    private void configurarBindingsCicloFinanceiro() {
        log.trace("{} [UI] Configurando bindings bidirecionais.", LOG_PREFIX);
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

        dpRecebimentoBanco.valueProperty().addListener(
                (obs, old, newDate) -> viewModel.dataRecebimentoBancoProperty().set(newDate != null ? newDate.atStartOfDay() : null));
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
        txtValorPagoPoder.textProperty().bindBidirectional(viewModel.valorPagoPelaPoderProperty(), criarConversorBigDecimal());
    }

    private void aplicarTravaHorarioQuinta() {
        boolean prazoUltrapassado = comissaoFacade.isTravaQuintaFeiraAtiva();
        if (prazoUltrapassado) {
            cbVerificado.setDisable(true);
            cbVerificado.setTooltip(new Tooltip("Prazo de conferência encerrado às 15:00h."));
            log.info("{} [NEGOCIO] Trava de quinta-feira aplicada. Campo 'Verificado' desabilitado.", LOG_PREFIX);
        }
    }

    // ==========================================================================================
    // MÓDULO 8: COMUNICAÇÃO DE DADOS (ASYNC)
    // ==========================================================================================
    public void recarregarDados() {
        log.info("{} [TELEMETRIA] Iniciando recarga de comissões...", LOG_PREFIX);
        navigator.mostrarLoading(MSG_SYNC_CAIXA);

        AsyncUtils.executarTaskAsync(comissaoFacade::listarComissoes, dados -> {
            masterData.setAll(dados);
            atualizarCardsResumo(dados);
            comissaoFacade.atualizarContextoComissoes(dados);
            navigator.ocultarLoading();
            log.info("{} [TELEMETRIA] {} comissão(ões) carregada(s).", LOG_PREFIX, dados.size());
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [SISTEMA] Erro ao recarregar comissões: {}", LOG_PREFIX, erro.getMessage());
        });
    }

    private void atualizarCardsResumo(List<ComissaoModel> comissoes) {
        BigDecimal totalPendente = comissaoFacade.calcularTotalPendente(comissoes);
        BigDecimal totalRecebido = comissaoFacade.calcularTotalRecebido(comissoes);

        lblTotalPendente.setText(FinanceiroUtils.formatarParaExibicao(totalPendente));
        lblTotalRecebido.setText(FinanceiroUtils.formatarParaExibicao(totalRecebido));

        log.info("{} [UI] Totais atualizados. Pendente={} | Recebido={}", LOG_PREFIX, FinanceiroUtils.formatarParaExibicao(totalPendente),
                FinanceiroUtils.formatarParaExibicao(totalRecebido));
    }

    // ==========================================================================================
    // MÓDULO 9: INTERFACE DE AJUSTE (MODAL/POPOVER)
    // ==========================================================================================
    private void prepararAjuste(ComissaoModel comissao, Node anchor) {
        log.trace("{} [UI] Preparando PopOver de ajuste para comissão ID={}", LOG_PREFIX, comissao.getId());
        viewModel.loadFromModel(comissao);
        carregarDatasNoFormulario(comissao);

        lblTituloModal.setText("Conciliação: " + comissao.getProposta().getProponente().getNomeCompleto());
        lblCicloBadge.setText("Ciclo: " + comissaoFacade.resolverNomeCiclo(comissao));

        atualizarEstadoInterfaceCiclo(comissao);
        exibirPopOverCentralizado();
    }

    private void carregarDatasNoFormulario(ComissaoModel comissao) {
        if (comissao.getDataRecebimentoBanco() != null)
            dpRecebimentoBanco.setValue(comissao.getDataRecebimentoBanco().toLocalDate());
        if (comissao.getPrevisaoPagamento() != null)
            dpPrevisaoPagamento.setValue(comissao.getPrevisaoPagamento());
    }

    private void atualizarEstadoInterfaceCiclo(ComissaoModel comissao) {
        LocalDateTime agora = LocalDateTime.now();
        boolean jaLiquidado = STATUS_PAGO.equalsIgnoreCase(comissao.getStatusPagamento())
                || STATUS_LIQUIDADO.equalsIgnoreCase(comissao.getStatusPagamento());
        boolean prazoContestacaoExpirado = comissao.getDataLimiteContestacao() != null
                && agora.isAfter(comissao.getDataLimiteContestacao());

        if (jaLiquidado) {
            configurarSemoforo("✅ CICLO LIQUIDADO: Registro imutável e Arquivado.", "-color-success-subtle", "-color-success-emphasis",
                    true, true);
        } else if (prazoContestacaoExpirado) {
            configurarSemoforo("🟡 AGUARDANDO LIQUIDAÇÃO: O prazo de contestação do consultor expirou.", "-color-warning-subtle",
                    "-color-warning-emphasis", true, false);
        } else {
            configurarSemoforo("🔵 CICLO ABERTO: Conferência do consultor disponível até Quinta às 15:00.", "-color-accent-subtle",
                    "-color-accent-emphasis", false, false);
        }
    }

    private void configurarSemoforo(String texto, String corFundo, String corTexto, boolean travarConsultor, boolean travarTudo) {
        lblStatusCiclo.setText(texto);
        bannerStatusCiclo.setStyle("-fx-background-color: " + corFundo + "; -fx-padding: 10; -fx-background-radius: 5;");
        lblStatusCiclo.setStyle("-fx-text-fill: " + corTexto + "; -fx-font-weight: bold;");

        if (cbVerificado != null)
            cbVerificado.setDisable(travarConsultor);
        if (cbContestada != null)
            cbContestada.setDisable(travarConsultor);

        if (txtValorPagoPoder != null)
            txtValorPagoPoder.setDisable(travarTudo);
        if (comboStatus != null)
            comboStatus.setDisable(travarTudo);
        if (dpPrevisaoPagamento != null)
            dpPrevisaoPagamento.setDisable(travarTudo);
        if (dpRecebimentoBanco != null)
            dpRecebimentoBanco.setDisable(travarTudo);
        if (txtObservacao != null)
            txtObservacao.setDisable(travarTudo);
        if (btnSalvarConciliacao != null)
            btnSalvarConciliacao.setDisable(travarTudo);
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

    @FXML private void salvarAjuste() {
        Long idComissao = viewModel.idProperty().get();
        log.info("{} [TELEMETRIA] Solicitação de salvamento. ID={} | dirty={}", LOG_PREFIX, idComissao, viewModel.isDirty());

        if (!viewModel.isDirty()) {
            log.info("{} [NEGOCIO] Nenhuma alteração detectada. Salvamento ignorado.", LOG_PREFIX);
            fecharModal();
            return;
        }

        AsyncUtils.executarTaskAsync(() -> {
            ComissaoModel comissaoDoBanco = comissaoFacade.buscarComissaoPorId(idComissao);
            if (comissaoDoBanco == null)
                throw new IllegalStateException("Comissão não encontrada no banco.");
            return comissaoFacade.salvarConciliacao(viewModel.atualizarModel(comissaoDoBanco));
        }, salva -> {
            log.info("{} [AUDITORIA] Comissão ID={} salva com sucesso.", LOG_PREFIX, salva.getId());
            recarregarDados();
            fecharModal();
        }, erro -> {
            log.error("{} [AUDITORIA] FALHA ao persistir comissão ID={}. Erro: {}", LOG_PREFIX, idComissao, erro.getMessage());
            fecharModal();
        });
    }

    @FXML private void fecharModal() {
        log.trace("{} [UI] Fechando PopOver de ajuste.", LOG_PREFIX);
        if (popOverAjuste != null && popOverAjuste.isShowing())
            popOverAjuste.hide();
        viewModel.reset();
        dpRecebimentoBanco.setValue(null);
    }

    // ==========================================================================================
    // MÓDULO 10: UTILITÁRIOS INTERNOS
    // ==========================================================================================
    private SimpleStringProperty formatarDataHora(LocalDateTime data) {
        return new SimpleStringProperty(data != null ? data.format(FMT_DATA_HORA) : "-");
    }

    private SimpleStringProperty formatarDataSimples(LocalDate data) {
        return new SimpleStringProperty(data != null ? data.format(FMT_DATA) : "-");
    }

    private StringConverter<LocalDate> criarConversorDataBrasil() {
        return new StringConverter<>() {
            @Override public String toString(LocalDate date) {
                return date != null ? FMT_DATA.format(date) : "";
            }

            @Override public LocalDate fromString(String string) {
                if (string != null && !string.isBlank()) {
                    try {
                        return LocalDate.parse(string, FMT_DATA);
                    } catch (DateTimeParseException e) {
                        log.warn("{} [UI] Falha ao parsear data '{}'.", LOG_PREFIX, string);
                    }
                }
                return null;
            }
        };
    }

    private StringConverter<BigDecimal> criarConversorBigDecimal() {
        return new BigDecimalStringConverter() {
            @Override public BigDecimal fromString(String value) {
                if (value == null || value.isBlank())
                    return BigDecimal.ZERO;
                String limpo = value.replaceAll("[R$\\s]", "").replace(",", ".");
                try {
                    return new BigDecimal(limpo);
                } catch (NumberFormatException e) {
                    log.warn("{} [UI] Valor financeiro '{}' inválido. Retornando ZERO.", LOG_PREFIX, value);
                    return BigDecimal.ZERO;
                }
            }
        };
    }
}
