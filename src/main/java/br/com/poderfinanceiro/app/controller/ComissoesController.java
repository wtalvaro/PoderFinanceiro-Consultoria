package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ComissaoUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ComissaoService;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.ComissaoViewModel;
import javafx.application.Platform;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javafx.beans.binding.Bindings;

@Component
public class ComissoesController {

    private static final Logger log = LoggerFactory.getLogger(ComissoesController.class);

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
    @FXML
    private Label lblTotalRegistros;

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final ComissaoService service;
    private final ComissaoViewModel viewModel;
    private final Navigator navigator;
    private final AtendimentoContextService contextoService;
    private final ComissaoUIEventHub eventHub;

    private final ObservableList<ComissaoModel> masterData = FXCollections.observableArrayList();
    private PopOver popOverAjuste;

    public ComissoesController(ComissaoService service, ComissaoViewModel viewModel,
            Navigator navigator, AtendimentoContextService contextoService, ComissaoUIEventHub eventHub) {
        this.service = service;
        this.viewModel = viewModel;
        this.navigator = navigator;
        this.contextoService = contextoService;
        this.eventHub = eventHub;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.info("[COMISSOES] Inicializando ComissoesController...");
        eventHub.inscrever(this::recarregarDados);
        configurarPopOver();
        configurarBloqueiosFinanceiros();
        configurarTabela();
        configurarFiltroReativo();
        configurarBindingsCicloFinanceiro();
        recarregarDados();
        lblTotalRegistros.textProperty().bind(
                Bindings.format("Total: %d repasse(s)", Bindings.size(tableComissoes.getItems())));
        log.info("[COMISSOES] Inicialização concluída.");
    }

    private void configurarPopOver() {
        log.debug("[COMISSOES][POPOVER] Configurando PopOver de ajuste...");

        // Falha aqui causa NPE silencioso: o CSS não seria carregado e o PopOver
        // ficaria sem estilo
        URL cssUrl = getClass().getResource(CSS_POPOVER);
        if (cssUrl == null) {
            log.error("[COMISSOES][POPOVER] CRÍTICO: CSS '{}' não encontrado no classpath. " +
                    "O PopOver será exibido sem estilo customizado.", CSS_POPOVER);
        } else {
            log.debug("[COMISSOES][POPOVER] CSS localizado: {}", cssUrl.toExternalForm());
        }

        popOverAjuste = new PopOver(boxFormularioAjuste);
        popOverAjuste.setAnimated(false);
        popOverAjuste.setFadeInDuration(Duration.ZERO);
        popOverAjuste.setFadeOutDuration(Duration.ZERO);

        if (cssUrl != null) {
            popOverAjuste.getRoot().getStylesheets().add(cssUrl.toExternalForm());
        }

        popOverAjuste.setArrowSize(0);
        popOverAjuste.setTitle("Ajuste de Comissão");
        popOverAjuste.setHeaderAlwaysVisible(true);
        popOverAjuste.setDetachable(true);
        popOverAjuste.setCornerRadius(0);

        log.debug("[COMISSOES][POPOVER] PopOver configurado com sucesso.");
    }

    private void configurarBloqueiosFinanceiros() {
        // Datas são calculadas pelo sistema — não devem ser editadas manualmente
        log.debug("[COMISSOES][BLOQUEIO] Travando interação direta nos DatePickers (integridade financeira).");
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
        log.debug("[COMISSOES][TABELA] Configurando colunas e interações da TableView...");

        colRecebBanco.setCellValueFactory(d -> formatarDataHora(d.getValue().getDataRecebimentoBanco()));
        colPrevisao.setCellValueFactory(d -> formatarDataSimples(d.getValue().getPrevisaoPagamento()));
        colCliente.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getProposta().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getProposta().getBanco().getNome()));
        colValorBruto.setCellValueFactory(d -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(d.getValue().getValorBrutoComissao())));
        colVlrPago.setCellValueFactory(d -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(d.getValue().getValorPagoPelaPoder())));

        configurarCelulaStatus();
        configurarInteracaoLinhas();

        log.debug("[COMISSOES][TABELA] Configuração de colunas concluída.");
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
                    log.debug("[COMISSOES][TABELA] Duplo clique na comissão ID={} | status='{}'",
                            row.getItem().getId(), row.getItem().getStatusPagamento());
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

            long totalFiltrado = masterData.stream()
                    .filter(c -> atendeCriterioDeBusca(c, newValue))
                    .count();
            log.debug("[COMISSOES][FILTRO] Termo='{}' → {}/{} comissão(ões) exibida(s).",
                    newValue, totalFiltrado, masterData.size());
        });

        SortedList<ComissaoModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableComissoes.comparatorProperty());
        tableComissoes.setItems(sortedData);

        log.debug("[COMISSOES][FILTRO] Filtro reativo configurado sobre {} registro(s) em masterData.",
                masterData.size());
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
        log.debug("[COMISSOES][BINDING] Configurando bindings bidirecionais do ViewModel...");
        vincularDatas();
        vincularStatus();
        vincularConferencia();
        vincularValores();
        txtObservacao.textProperty().bindBidirectional(viewModel.observacaoAjusteProperty());
        log.debug("[COMISSOES][BINDING] Todos os bindings configurados com sucesso.");
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
        boolean isQuinta = agora.getDayOfWeek() == DayOfWeek.THURSDAY;
        boolean prazoUltrapassado = isQuinta && agora.getHour() >= 15;

        // Regra de negócio crítica: log permite auditar quando a trava foi ou não
        // aplicada
        if (prazoUltrapassado) {
            cbVerificado.setDisable(true);
            cbVerificado.setTooltip(new Tooltip("Prazo de conferência encerrado às 15:00h."));
            log.info("[COMISSOES][CICLO] Trava de quinta-feira aplicada. " +
                    "Hora atual={}h — campo 'Verificado' desabilitado.", agora.getHour());
        } else {
            log.debug("[COMISSOES][CICLO] Trava de quinta-feira NÃO aplicada. " +
                    "Dia={} | Hora={}h (trava só ativa quinta >= 15h).",
                    agora.getDayOfWeek(), agora.getHour());
        }
    }

    // =========================================================================
    // COMUNICAÇÃO DE DADOS
    // =========================================================================
    public void recarregarDados() {
        log.info("[COMISSOES][DADOS] Iniciando recarga de comissões...");
        long inicio = System.currentTimeMillis();
        navigator.mostrarLoading(MSG_SYNC_CAIXA);

        List<ComissaoModel> dados = service.listarTodasComDetalhes();
        masterData.setAll(dados);
        atualizarCardsResumo(dados);
        contextoService.setComissoesAtivas(dados);

        navigator.ocultarLoading();

        long tempo = System.currentTimeMillis() - inicio;
        // Contadores por status ajudam a detectar dados inconsistentes no banco
        long qtdPagas = dados.stream().filter(c -> STATUS_PAGO.equalsIgnoreCase(c.getStatusPagamento())).count();
        long qtdPendentes = dados.stream().filter(c -> STATUS_PENDENTE.equalsIgnoreCase(c.getStatusPagamento()))
                .count();
        long qtdContestadas = dados.stream().filter(ComissaoModel::isContestada).count();

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::recarregarDados);
            return;
        }

        log.info("[COMISSOES][DADOS] {} comissão(ões) carregada(s) em {}ms. " +
                "Pagas={} | Pendentes={} | Contestadas={}",
                dados.size(), tempo, qtdPagas, qtdPendentes, qtdContestadas);
    }

    private void atualizarCardsResumo(List<ComissaoModel> comissoes) {
        BigDecimal totalPendente = somarValoresPorStatus(comissoes, false);
        BigDecimal totalRecebido = somarValoresPorStatus(comissoes, true);

        lblTotalPendente.setText(FinanceiroUtils.formatarParaExibicao(totalPendente));
        lblTotalRecebido.setText(FinanceiroUtils.formatarParaExibicao(totalRecebido));

        // Totais financeiros são o dado mais crítico da tela — sempre info, nunca debug
        log.info("[COMISSOES][RESUMO] Totais atualizados. Pendente={} | Recebido={}",
                FinanceiroUtils.formatarParaExibicao(totalPendente),
                FinanceiroUtils.formatarParaExibicao(totalRecebido));
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
        log.info("[COMISSOES][AJUSTE] Abrindo ajuste. ID={} | Status='{}' | Valor bruto={} | Ciclo='{}'",
                comissao.getId(),
                comissao.getStatusPagamento(),
                FinanceiroUtils.formatarParaExibicao(comissao.getValorBrutoComissao()),
                comissao.getCicloReferencia() != null ? comissao.getCicloReferencia() : "a calcular");

        viewModel.loadFromModel(comissao);
        carregarDatasNoFormulario(comissao);

        lblTituloModal.setText("Conciliação: " + comissao.getProposta().getProponente().getNomeCompleto());

        String nomeCiclo = resolverNomeCiclo(comissao);
        lblCicloBadge.setText("Ciclo: " + nomeCiclo);

        atualizarEstadoInterfaceCiclo(comissao);
        exibirPopOverCentralizado();
    }

    private void carregarDatasNoFormulario(ComissaoModel comissao) {
        if (comissao.getDataRecebimentoBanco() != null) {
            dpRecebimentoBanco.setValue(comissao.getDataRecebimentoBanco().toLocalDate());
            log.debug("[COMISSOES][AJUSTE] Data recebimento banco: {}",
                    comissao.getDataRecebimentoBanco().format(FMT_DATA_HORA));
        } else {
            log.debug("[COMISSOES][AJUSTE] Comissão ID={} sem data de recebimento banco.", comissao.getId());
        }

        if (comissao.getPrevisaoPagamento() != null) {
            dpPrevisaoPagamento.setValue(comissao.getPrevisaoPagamento());
            log.debug("[COMISSOES][AJUSTE] Previsão de pagamento: {}",
                    comissao.getPrevisaoPagamento().format(FMT_DATA));
        } else {
            log.debug("[COMISSOES][AJUSTE] Comissão ID={} sem previsão de pagamento definida.", comissao.getId());
        }
    }

    private String resolverNomeCiclo(ComissaoModel comissao) {
        String ciclo = comissao.getCicloReferencia();

        if (ciclo != null) {
            log.debug("[COMISSOES][CICLO] Ciclo obtido do registro: '{}'", ciclo);
            return ciclo;
        }

        if (comissao.getDataRecebimentoBanco() != null) {
            ciclo = CicloFinanceiroUtils.identificarCiclo(comissao.getDataRecebimentoBanco());
            log.debug("[COMISSOES][CICLO] Ciclo calculado via data de recebimento: '{}'", ciclo);
            return ciclo;
        }

        // Comissão sem ciclo nem data de recebimento — pode indicar dado incompleto
        log.warn("[COMISSOES][CICLO] Comissão ID={} sem ciclo de referência e sem data de recebimento. " +
                "Classificada como 'Legado'.", comissao.getId());
        return "Legado";
    }

    private void atualizarEstadoInterfaceCiclo(ComissaoModel comissao) {
        LocalDateTime agora = LocalDateTime.now();

        boolean jaLiquidado = STATUS_PAGO.equalsIgnoreCase(comissao.getStatusPagamento()) ||
                STATUS_LIQUIDADO.equalsIgnoreCase(comissao.getStatusPagamento());

        boolean prazoContestacaoExpirado = comissao.getDataLimiteContestacao() != null &&
                agora.isAfter(comissao.getDataLimiteContestacao());

        // Estado do ciclo financeiro é a decisão mais importante deste bloco —
        // determina quais campos ficam editáveis. Sempre info.
        if (jaLiquidado) {
            log.info("[COMISSOES][CICLO] Estado: LIQUIDADO. ID={} | Status='{}'. Todos os campos bloqueados.",
                    comissao.getId(), comissao.getStatusPagamento());
            configurarSemoforo("✅ CICLO LIQUIDADO: Registro imutável e Arquivado.",
                    "-color-success-subtle", "-color-success-emphasis", true, true);

        } else if (prazoContestacaoExpirado) {
            log.info("[COMISSOES][CICLO] Estado: AGUARDANDO LIQUIDAÇÃO. ID={} | Prazo limite expirado em '{}'.",
                    comissao.getId(),
                    comissao.getDataLimiteContestacao().format(FMT_DATA_HORA));
            configurarSemoforo("🟡 AGUARDANDO LIQUIDAÇÃO: O prazo de contestação do consultor expirou.",
                    "-color-warning-subtle", "-color-warning-emphasis", true, false);

        } else {
            String prazoStr = comissao.getDataLimiteContestacao() != null
                    ? comissao.getDataLimiteContestacao().format(FMT_DATA_HORA)
                    : "não definido";
            log.info("[COMISSOES][CICLO] Estado: ABERTO. ID={} | Prazo de contestação: '{}'.",
                    comissao.getId(), prazoStr);
            configurarSemoforo("🔵 CICLO ABERTO: Conferência do consultor disponível até Quinta às 15:00.",
                    "-color-accent-subtle", "-color-accent-emphasis", false, false);
        }
    }

    private void configurarSemoforo(String texto, String corFundo, String corTexto,
            boolean travarConsultor, boolean travarTudo) {
        lblStatusCiclo.setText(texto);
        bannerStatusCiclo.setStyle(
                "-fx-background-color: " + corFundo + "; -fx-padding: 10; -fx-background-radius: 5;");
        lblStatusCiclo.setStyle("-fx-text-fill: " + corTexto + "; -fx-font-weight: bold;");

        alternarEstadoCamposConsultor(travarConsultor);
        alternarEstadoCamposGestor(travarTudo);

        log.debug("[COMISSOES][CICLO] Semáforo aplicado. travarConsultor={} | travarGestor={}",
                travarConsultor, travarTudo);
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

        log.debug("[COMISSOES][POPOVER] PopOver exibido. Posição: x={} y={}", (int) x, (int) y);
    }

    @FXML
    private void salvarAjuste() {
        Long idComissao = viewModel.idProperty().get();
        boolean dirty = viewModel.isDirty();

        log.info("[COMISSOES][SALVAR] Solicitação de salvamento. ID={} | dirty={}",
                idComissao, dirty);

        if (!dirty) {
            log.info("[COMISSOES][SALVAR] Nenhuma alteração detectada. Salvamento ignorado. ID={}", idComissao);
            fecharModal();
            return;
        }

        ComissaoModel comissaoDoBanco = service.buscarPorId(idComissao);
        if (comissaoDoBanco == null) {
            log.error("[COMISSOES][SALVAR] CRÍTICO: Comissão ID={} não encontrada no banco " +
                    "para atualização. Dado pode ter sido removido externamente.", idComissao);
            fecharModal();
            return;
        }

        ComissaoModel paraSalvar = viewModel.atualizarModel(comissaoDoBanco);
        try {
            ComissaoModel salva = service.salvarConciliacao(paraSalvar);
            log.info("[COMISSOES][SALVAR] Comissão ID={} salva com sucesso. " +
                    "Novo status='{}' | Valor pago={}",
                    salva.getId(),
                    salva.getStatusPagamento(),
                    FinanceiroUtils.formatarParaExibicao(salva.getValorPagoPelaPoder()));
            recarregarDados();
        } catch (Exception ex) {
            log.error("[COMISSOES][SALVAR] FALHA ao persistir comissão ID={}. Erro: {}",
                    idComissao, ex.getMessage(), ex);
        }

        fecharModal();
    }

    @FXML
    private void fecharModal() {
        log.debug("[COMISSOES][MODAL] Fechando PopOver de ajuste.");
        if (popOverAjuste != null && popOverAjuste.isShowing()) {
            popOverAjuste.hide();
        }
        viewModel.reset();
        dpRecebimentoBanco.setValue(null);
        log.debug("[COMISSOES][MODAL] ViewModel resetado e DatePicker limpo.");
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
                    } catch (DateTimeParseException e) {
                        log.warn("[COMISSOES][CONVERSOR] Falha ao parsear data '{}'. " +
                                "Formato esperado: dd/MM/yyyy.", string);
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
                    // Era silencioso no original — em campo financeiro, isso deve ser visível
                    log.warn("[COMISSOES][CONVERSOR] Valor financeiro '{}' inválido após limpeza ('{}'). " +
                            "Retornando ZERO.", value, limpo);
                    return BigDecimal.ZERO;
                }
            }
        };
    }
}