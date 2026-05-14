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
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    // Elementos do Overlay de Ciclo Financeiro
    @FXML
    private VBox overlayAjuste;
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

    private final ObservableList<ComissaoModel> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                    prepararAjuste(row.getItem());
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

    private void prepararAjuste(ComissaoModel comissao) {
        // 1. Carrega os dados no ViewModel (Garante que o ID e valores brutos fiquem
        // prontos)
        viewModel.loadFromModel(comissao);

        // 2. Sincronização Manual (Initial UI Setup)
        // Converte LocalDateTime (Banco) para LocalDate (UI)
        if (comissao.getDataRecebimentoBanco() != null) {
            dpRecebimentoBanco.setValue(comissao.getDataRecebimentoBanco().toLocalDate());
        } else {
            dpRecebimentoBanco.setValue(null);
        }

        // Previsão de Pagamento (LocalDate para LocalDate)
        if (comissao.getPrevisaoPagamento() != null) {
            dpPrevisaoPagamento.setValue(comissao.getPrevisaoPagamento());
        } else {
            dpPrevisaoPagamento.setValue(null);
        }

        lblTituloModal.setText("Conciliação: " + comissao.getProposta().getProponente().getNomeCompleto());

        // 3. Exibe o Overlay (State Transition)
        overlayAjuste.setVisible(true);
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
        overlayAjuste.setVisible(false);
        viewModel.reset();
        dpRecebimentoBanco.setValue(null);
    }
}