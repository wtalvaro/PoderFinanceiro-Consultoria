package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.ComissaoModel;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.viewmodel.ComissaoViewModel;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.controlsfx.control.PopOver;
import javafx.scene.Node; 

@Component
public class ComissoesController {

    private final ComissaoRepository repository;
    private final ComissaoViewModel viewModel;
    private final MainController mainController;

    @FXML
    private TableView<ComissaoModel> tableComissoes;

    // Nomenclatura técnica para as colunas do ciclo
    @FXML
    private TableColumn<ComissaoModel, String> colPrevisao; // Sexta-feira
    @FXML
    private TableColumn<ComissaoModel, String> colCliente, colBanco, colValorBruto, colStatus;

    @FXML
    private TextField txtBusca;

    @FXML
    private VBox boxFormularioAjuste; // O ID que definimos no fx:define
    @FXML
    private Label lblTituloModal;
    @FXML
    private DatePicker dpRecebimentoBanco; // Quarta
    @FXML
    private CheckBox cbVerificado; // Quinta
    @FXML
    private DatePicker dpPrevisaoPagamento; // Sexta
    @FXML
    private TextField txtValorPagoPoder;
    @FXML
    private CheckBox cbContestada;
    @FXML
    private ComboBox<String> comboStatus;

    @FXML
    private Label lblTotalPendente;
    @FXML
    private Label lblTotalRecebido;

    @FXML
    private TableColumn<ComissaoModel, String> colRecebBanco;
    @FXML
    private TableColumn<ComissaoModel, String> colVlrPago;

    @FXML
    private Label lblStatusCiclo; // Um Label de destaque no topo do modal de ajuste
    @FXML
    private Button btnSalvarAjuste;

    @FXML
    private Label lblCicloBadge;
    @FXML
    private VBox bannerStatusCiclo;
    @FXML
    private TextArea txtObservacao;
    @FXML
    private Button btnSalvarConciliacao;

    private final ObservableList<ComissaoModel> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PopOver popOverAjuste; // Nossa nova janela flutuante

    public ComissoesController(ComissaoRepository repository, ComissaoViewModel viewModel,
            MainController mainController) {
        this.repository = repository;
        this.viewModel = viewModel;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltroReativo();
        configurarBindingsCicloFinanceiro();
        recarregarDados();

        // INICIALIZA O POPOVER
        popOverAjuste = new PopOver(boxFormularioAjuste);

        // 1. Zera todos os tempos de transição (remove o flicker de opacidade)
        popOverAjuste.setAnimated(false);
        popOverAjuste.setFadeInDuration(javafx.util.Duration.ZERO);
        popOverAjuste.setFadeOutDuration(javafx.util.Duration.ZERO);

        // 2. Aplica o CSS
        String css = getClass().getResource("/css/popover-custom.css").toExternalForm();
        popOverAjuste.getRoot().getStylesheets().add(css);

        // 3. Configurações visuais
        popOverAjuste.setArrowSize(0);
        popOverAjuste.setTitle("Ajuste de Comissão");
        popOverAjuste.setHeaderAlwaysVisible(true);
        popOverAjuste.setDetachable(true);
        popOverAjuste.setCornerRadius(0);

        // BLOQUEIO DE INTEGRIDADE FINANCEIRA: Datas são calculadas pelo sistema, não
        // digitadas.
        dpRecebimentoBanco.setEditable(false); // Impede de digitar a data
        dpRecebimentoBanco.setMouseTransparent(true); // Impede de clicar no calendário
        dpRecebimentoBanco.setFocusTraversable(false); // Impede de chegar lá com a tecla TAB

        dpPrevisaoPagamento.setEditable(false);
        dpPrevisaoPagamento.setMouseTransparent(true);
        dpPrevisaoPagamento.setFocusTraversable(false);
    }

    private void configurarTabela() {
        // Marco 1: Recebimento do Banco (Quarta)
        colRecebBanco.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDataRecebimentoBanco() != null
                        ? d.getValue().getDataRecebimentoBanco().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "-"));

        // Marco 3: Previsão de Pagamento (Sexta)
        colPrevisao.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPrevisaoPagamento() != null
                        ? d.getValue().getPrevisaoPagamento().format(df)
                        : "-"));

        colCliente.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getProposta().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProposta().getBanco().getNome()));

        // Financeiro: Expectativa vs Realidade
        colValorBruto.setCellValueFactory(d -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(d.getValue().getValorBrutoComissao())));

        colVlrPago.setCellValueFactory(d -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(d.getValue().getValorPagoPelaPoder())));

        configurarCelulaStatus();

        tableComissoes.setRowFactory(tv -> {
            TableRow<ComissaoModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    // ✅ Agora passamos também a 'row', para o balão saber onde abrir!
                    prepararAjuste(row.getItem(), row);
                }
            });
            return row;
        });
    }

    /**
     * Implementação do Filtro Reativo para a TableView.
     * Motivo: Permitir busca dinâmica sem recarregar dados do banco (Performance).
     */
    private void configurarFiltroReativo() {
        FilteredList<ComissaoModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBusca.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(comissao -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String filter = newValue.toLowerCase();

                return comissao.getProposta().getProponente().getNomeCompleto().toLowerCase().contains(filter) ||
                        comissao.getProposta().getBanco().getNome().toLowerCase().contains(filter) ||
                        comissao.getStatusPagamento().toLowerCase().contains(filter);
            });
        });

        SortedList<ComissaoModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableComissoes.comparatorProperty());
        tableComissoes.setItems(sortedData);
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

                    // Lógica de Sinalização Visual baseada no Ciclo
                    if (c.isContestada()) {
                        setText("⚠️ CONTESTADA");
                        setTextFill(Color.RED);
                    } else if ("Pago".equalsIgnoreCase(c.getStatusPagamento())) {
                        setText("✅ PAGO");
                        setTextFill(Color.GREEN);
                    } else if (c.isVerificadoConsultor()) {
                        setText("🔎 CONFERIDO");
                        setTextFill(Color.BLUE);
                    } else {
                        setText("⏳ PENDENTE");
                        setTextFill(Color.ORANGE);
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
    }

    private void configurarBindingsCicloFinanceiro() {
        // 0. FORÇA O FORMATO BRASILEIRO NOS DATEPICKERS (Evita truncamento ou formatos
        // do Linux)
        javafx.util.StringConverter<java.time.LocalDate> conversorData = new javafx.util.StringConverter<>() {
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter
                    .ofPattern("dd/MM/yyyy");

            @Override
            public String toString(java.time.LocalDate date) {
                if (date != null)
                    return dateFormatter.format(date);
                return "";
            }

            @Override
            public java.time.LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return java.time.LocalDate.parse(string, dateFormatter);
                }
                return null;
            }
        };

        dpRecebimentoBanco.setConverter(conversorData);
        dpPrevisaoPagamento.setConverter(conversorData);
        
        // 1. Sincronia de Data (Marco 1 - Quarta): UI (LocalDate) -> ViewModel
        // (LocalDateTime)
        dpRecebimentoBanco.valueProperty().addListener((obs, old, newDate) -> {
            viewModel.dataRecebimentoBancoProperty().set(newDate != null ? newDate.atStartOfDay() : null);
        });

        // 2. Sincronia de Data (Marco 3 - Sexta): Binding Direto (LocalDate <->
        // LocalDate)
        dpPrevisaoPagamento.valueProperty().bindBidirectional(viewModel.previsaoPagamentoProperty());

        // 3. Status e Contestação
        comboStatus.getItems().setAll("Pendente", "Pago", "Estornado");
        comboStatus.valueProperty().bindBidirectional(viewModel.statusPagamentoProperty());
        cbContestada.selectedProperty().bindBidirectional(viewModel.contestadaProperty());

        // 4. Conferência (Marco 2 - Quinta): Binding + Lógica de Deadline (15h)
        cbVerificado.selectedProperty().bindBidirectional(viewModel.verificadoConsultorProperty());
        aplicarTravaHorarioQuinta();

        // 5. Valor Pago (Liquidação): String <-> BigDecimal com Conversor
        txtValorPagoPoder.textProperty().bindBidirectional(viewModel.valorPagoPelaPoderProperty(),
                new javafx.util.converter.BigDecimalStringConverter() {
                    @Override
                    public BigDecimal fromString(String value) {
                        if (value == null || value.isEmpty())
                            return BigDecimal.ZERO;
                        // Remove R$, espaços e converte vírgula em ponto para o BigDecimal entender
                        String limpo = value.replaceAll("[R$\\s]", "").replace(",", ".");
                        try {
                            return new BigDecimal(limpo);
                        } catch (NumberFormatException e) {
                            return BigDecimal.ZERO;
                        }
                    }
                });

        txtObservacao.textProperty().bindBidirectional(viewModel.observacaoAjusteProperty());
    }

    /**
     * Aplica a regra de negócio do Deadline de Quinta-feira.
     * Se hoje for quinta-feira e passar das 15:00h, bloqueia a conferência.
     */
    private void aplicarTravaHorarioQuinta() {
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();

        // Verifica se hoje é Quinta (DayOfWeek 4) e se já passou das 15:00
        boolean prazoUltrapassado = agora.getDayOfWeek() == java.time.DayOfWeek.THURSDAY && agora.getHour() >= 15;

        if (prazoUltrapassado) {
            cbVerificado.setDisable(true);
            cbVerificado.setTooltip(new Tooltip("Prazo de conferência encerrado às 15:00h."));
        }
    }

    public void recarregarDados() {
        mainController.mostrarLoading("Sincronizando fluxo de caixa...");
        List<ComissaoModel> dados = repository.findAllComDetalhes();
        masterData.setAll(dados);
        atualizarCardsResumo(dados);
        mainController.ocultarLoading();
    }

    private void atualizarCardsResumo(List<ComissaoModel> comissoes) {
        BigDecimal totalPendente = comissoes.stream()
                .filter(c -> !"Pago".equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorBrutoComissao() != null ? c.getValorBrutoComissao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRecebido = comissoes.stream()
                .filter(c -> "Pago".equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorPagoPelaPoder() != null ? c.getValorPagoPelaPoder() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalPendente.setText(FinanceiroUtils.formatarParaExibicao(totalPendente));
        lblTotalRecebido.setText(FinanceiroUtils.formatarParaExibicao(totalRecebido));
    }

    // Adicione o Node anchor na assinatura
    private void prepararAjuste(ComissaoModel comissao, Node anchor) {
        viewModel.loadFromModel(comissao);

        if (comissao.getDataRecebimentoBanco() != null) {
            dpRecebimentoBanco.setValue(comissao.getDataRecebimentoBanco().toLocalDate());
        }
        if (comissao.getPrevisaoPagamento() != null) {
            dpPrevisaoPagamento.setValue(comissao.getPrevisaoPagamento());
        }

        lblTituloModal.setText("Conciliação: " + comissao.getProposta().getProponente().getNomeCompleto());
        
        // ✅ Pela Lógica de Resgate:
        String cicloBadge = comissao.getCicloReferencia();

        // Se estiver nulo no banco, mas tiver a data de recebimento, calculamos na
        // hora!
        if (cicloBadge == null && comissao.getDataRecebimentoBanco() != null) {
            cicloBadge = br.com.poderfinanceiro.app.utils.CicloFinanceiroUtils
                    .identificarCiclo(comissao.getDataRecebimentoBanco());
        }

        lblCicloBadge.setText("Ciclo: " + (cicloBadge != null ? cicloBadge : "Legado"));

        atualizarEstadoInterfaceCiclo(comissao);

        atualizarEstadoInterfaceCiclo(comissao);

        // ✅ LÓGICA DE CENTRALIZAÇÃO
        javafx.stage.Window window = tableComissoes.getScene().getWindow();

        // Mostramos o PopOver primeiro para ele calcular o tamanho interno
        // Mas com opacidade 0 para evitar o "flicker"
        popOverAjuste.setOpacity(0);
        popOverAjuste.show(window);

        // Calculamos o centro exato
        double x = window.getX() + (window.getWidth() / 2) - (popOverAjuste.getWidth() / 2);
        double y = window.getY() + (window.getHeight() / 2) - (popOverAjuste.getHeight() / 2);

        // Movemos para o centro e mostramos
        popOverAjuste.setX(x);
        popOverAjuste.setY(y);
        popOverAjuste.setOpacity(1);
    }

    private void atualizarEstadoInterfaceCiclo(ComissaoModel comissao) {
        LocalDateTime agora = LocalDateTime.now();
        boolean jaLiquidado = "Pago".equalsIgnoreCase(comissao.getStatusPagamento())
                || "Liquidado".equalsIgnoreCase(comissao.getStatusPagamento());

        boolean prazoContestacaoExpirado = false;
        if (comissao.getDataLimiteContestacao() != null) {
            prazoContestacaoExpirado = agora.isAfter(comissao.getDataLimiteContestacao());
        }

        if (jaLiquidado) {
            configurarSemoforo("✅ CICLO LIQUIDADO: Registro imutável e Arquivado.", "-color-success-subtle",
                    "-color-success-emphasis", true, true);
        } else if (prazoContestacaoExpirado) {
            configurarSemoforo("🟡 AGUARDANDO LIQUIDAÇÃO: O prazo de contestação do consultor expirou.",
                    "-color-warning-subtle", "-color-warning-emphasis", true, false);
        } else {
            configurarSemoforo("🔵 CICLO ABERTO: Conferência do consultor disponível até Quinta às 15:00.",
                    "-color-accent-subtle", "-color-accent-emphasis", false, false);
        }
    }

    // Método utilitário para controlar o layout de forma limpa
    private void configurarSemoforo(String texto, String corFundo, String corTexto, boolean travarConsultor,
            boolean travarTudo) {
        lblStatusCiclo.setText(texto);
        bannerStatusCiclo
                .setStyle("-fx-background-color: " + corFundo + "; -fx-padding: 10; -fx-background-radius: 5;");
        lblStatusCiclo.setStyle("-fx-text-fill: " + corTexto + "; -fx-font-weight: bold;");

        // Trava de Quinta-feira (Fase 1)
        if (cbVerificado != null)
            cbVerificado.setDisable(travarConsultor);
        if (cbContestada != null)
            cbContestada.setDisable(travarConsultor);

        // Trava de Sexta-feira (Fase 2)
        if (txtValorPagoPoder != null)
            txtValorPagoPoder.setDisable(travarTudo);
        if (comboStatus != null)
            comboStatus.setDisable(travarTudo);
        if (dpPrevisaoPagamento != null)
            dpPrevisaoPagamento.setDisable(travarTudo);
        if (dpRecebimentoBanco != null)
            dpRecebimentoBanco.setDisable(travarTudo); // <- ADICIONE ESTA LINHA
        if (txtObservacao != null)
            txtObservacao.setDisable(travarTudo);
        if (btnSalvarConciliacao != null)
            btnSalvarConciliacao.setDisable(travarTudo);
    }

    @FXML
    private void salvarAjuste() {
        if (viewModel.isDirty()) {
            // Buscamos a entidade que já está "atrelada" ao contexto do banco
            repository.findById(viewModel.idProperty().get()).ifPresent(comissaoDoBanco -> {

                // 🚀 O ViewModel injeta os valores da UI nesta entidade do banco
                ComissaoModel paraSalvar = viewModel.atualizarModel(comissaoDoBanco);

                // Log de depuração (opcional para o seu console do Fedora)
                System.out.println("Salvando valor: " + paraSalvar.getValorPagoPelaPoder());

                repository.save(paraSalvar);
                recarregarDados();
            });
        }
        fecharModal();
    }

    @FXML
    private void fecharModal() {
        // ❌ REMOVA: overlayAjuste.setVisible(false);
        if (popOverAjuste != null && popOverAjuste.isShowing()) {
            popOverAjuste.hide(); // Recolhe o balão
        }
        viewModel.reset();
        dpRecebimentoBanco.setValue(null);
    }
}