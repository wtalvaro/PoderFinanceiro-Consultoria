package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Comissao;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.viewmodel.ComissaoViewModel;
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

    @FXML
    private TableView<Comissao> tableComissoes;
    @FXML
    private TableColumn<Comissao, String> colData, colCliente, colBanco, colValorBruto, colStatus;
    @FXML
    private TextField txtBusca;

    // Elementos do Overlay/Modal de Ajuste
    @FXML
    private VBox overlayAjuste;
    @FXML
    private Label lblTituloModal;
    @FXML
    private DatePicker dpRecebimento;
    @FXML
    private TextField txtValorPago;
    @FXML
    private CheckBox cbContestada;
    @FXML
    private ComboBox<String> comboStatus;
    @FXML
    private Label lblTotalPendente;
    @FXML
    private Label lblTotalRecebido;

    private final ObservableList<Comissao> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ComissoesController(ComissaoRepository repository,
            ComissaoViewModel viewModel) {
        this.repository = repository;
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltroReativo();
        configurarBindingsModal();
        recarregarDados();
    }

    private void configurarTabela() {
        // Coluna de Data (Previsão)
        colData.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDataPrevisaoPagamento() != null ? d.getValue().getDataPrevisaoPagamento().format(df)
                        : "-"));

        // Coluna de Cliente (Vindo da Proposta)
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProposta().getProponente().getNomeCompleto()));

        // Coluna de Banco (Vindo da Proposta)
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProposta().getBanco().getNome()));

        // Coluna de Valor (Com formatação monetária)
        colValorBruto.setCellValueFactory(d -> new SimpleStringProperty(
                "R$ " + d.getValue().getValorBrutoComissao().toString()));

        // Coluna de Status com Estilo Visual (Sinalização de Saúde Financeira)
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Comissao c = getTableRow().getItem();
                    setText(c.getStatusPagamento().toUpperCase());

                    // Sinalização por cores
                    if ("Pago".equalsIgnoreCase(c.getStatusPagamento())) {
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: bold;");
                    } else if (c.isContestada()) {
                        setTextFill(Color.RED);
                        setText("⚠️ CONTESTADA");
                    } else {
                        setTextFill(Color.ORANGE);
                    }
                }
            }
        });

        // Clique duplo para abrir "cirurgia" de ajuste
        tableComissoes.setRowFactory(tv -> {
            TableRow<Comissao> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    prepararAjuste(row.getItem());
                }
            });
            return row;
        });
    }

    private void configurarFiltroReativo() {
        FilteredList<Comissao> filteredData = new FilteredList<>(masterData, p -> true);
        txtBusca.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(comissao -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();

                return comissao.getProposta().getProponente().getNomeCompleto().toLowerCase().contains(lowerCaseFilter)
                        ||
                        comissao.getProposta().getBanco().getNome().toLowerCase().contains(lowerCaseFilter) ||
                        comissao.getStatusPagamento().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<Comissao> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableComissoes.comparatorProperty());
        tableComissoes.setItems(sortedData);
    }

    private void configurarBindingsModal() {
        // CORREÇÃO DO COMBOBOX: de textProperty() para valueProperty()
        comboStatus.valueProperty().bindBidirectional(viewModel.statusPagamentoProperty());
        comboStatus.getItems().setAll("Pendente", "Pago", "Estornado");

        // BINDING DE VALOR: (Já estava correto, mantendo o padrão)
        txtValorPago.textProperty().bindBidirectional(viewModel.valorPagoPelaPoderProperty(),
                new javafx.util.converter.BigDecimalStringConverter());

        // BINDING DE CHECKBOX:
        cbContestada.selectedProperty().bindBidirectional(viewModel.contestadaProperty());

        // CORREÇÃO DO DATEPICKER: Sincronia manual entre LocalDate e LocalDateTime
        // Quando a data mudar no DatePicker, atualiza o ViewModel com "início do dia"
        dpRecebimento.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                viewModel.dataRecebimentoProperty().set(newDate.atStartOfDay());
            } else {
                viewModel.dataRecebimentoProperty().set(null);
            }
        });

        // Quando o ViewModel mudar (vinda do banco), atualiza o DatePicker
        viewModel.dataRecebimentoProperty().addListener((obs, oldDateTime, newDateTime) -> {
            if (newDateTime != null) {
                dpRecebimento.setValue(newDateTime.toLocalDate());
            } else {
                dpRecebimento.setValue(null);
            }
        });
    }

    public void recarregarDados() {
        // 1. Usa a nova query que traz os dados completos (Evita tabela vazia)
        List<Comissao> comissoesCompletas = repository.findAllComDetalhes();

        // 2. Joga na tabela
        masterData.setAll(comissoesCompletas);

        // 3. Atualiza os monitores de sinais vitais (Cards do topo)
        atualizarCardsResumo(comissoesCompletas);
    }

    private void atualizarCardsResumo(List<Comissao> comissoes) {
        BigDecimal totalPendente = BigDecimal.ZERO;
        BigDecimal totalRecebido = BigDecimal.ZERO;

        for (Comissao c : comissoes) {
            if ("Pago".equalsIgnoreCase(c.getStatusPagamento())) {
                // Se já pagou, soma o valor que a Poder Financeiro realmente recebeu
                totalRecebido = totalRecebido.add(
                    c.getValorPagoPelaPoder() != null ? c.getValorPagoPelaPoder() : BigDecimal.ZERO
                );
            } else {
                // Se está pendente, soma a expectativa (Valor Bruto)
                totalPendente = totalPendente.add(
                    c.getValorBrutoComissao() != null ? c.getValorBrutoComissao() : BigDecimal.ZERO
                );
            }
        }

        // Formata e joga na tela
        lblTotalPendente.setText(String.format("R$ %,.2f", totalPendente));
        lblTotalRecebido.setText(String.format("R$ %,.2f", totalRecebido));
    }

    private void prepararAjuste(Comissao comissao) {
        viewModel.loadFromModel(comissao);
        lblTituloModal.setText("Ajuste de Comissão - " + comissao.getProposta().getProponente().getNomeCompleto());
        overlayAjuste.setVisible(true);
    }

    @FXML
    private void salvarAjuste() {
        if (viewModel.isDirty()) {
            Comissao atualizada = viewModel.atualizarModel(new Comissao());
            repository.save(atualizada);
            recarregarDados();
        }
        fecharModal();
    }

    @FXML
    private void fecharModal() {
        overlayAjuste.setVisible(false);
        viewModel.reset();
    }
}