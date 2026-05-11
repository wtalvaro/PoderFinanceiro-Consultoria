package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PropostasListController {

    private final PropostaRepository repository;
    private final MainController mainController;

    @FXML
    private TableView<Proposta> tablePropostas;
    @FXML
    private TableColumn<Proposta, String> colData, colCliente, colBanco, colValor, colStatus;
    @FXML
    private TableColumn<Proposta, Void> colAcoes;
    @FXML
    private TextField txtBusca;

    private final ObservableList<Proposta> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PropostasListController(PropostaRepository repository, MainController mainController) {
        this.repository = repository;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltroReativo();
        recarregarDados();
    }

    private void configurarTabela() {
        colData.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDataSolicitacao() != null ? d.getValue().getDataSolicitacao().format(df) : "-"));

        colCliente.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProponente().getNomeCompleto()));

        colBanco.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBanco().getNome()));

        colValor.setCellValueFactory(d -> {
            BigDecimal valor = d.getValue().getValorSolicitado();
            return new SimpleStringProperty(valor != null ? FinanceiroUtils.formatarParaExibicao(valor) : "R$ 0,00");
        });

        // Colorindo o Status para fácil visualização
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Proposta p = getTableRow().getItem();
                    setText(p.getStatus().name().replace("_", " "));

                    switch (p.getStatus()) {
                        case PAGO:
                            setTextFill(Color.GREEN);
                            setStyle("-fx-font-weight: bold;");
                            break;
                        case REPROVADA:
                        case CANCELADO:
                            setTextFill(Color.RED);
                            break;
                        case PENDENTE:
                        case AGUARDANDO_DOC:
                            setTextFill(Color.DARKORANGE);
                            break;
                        default:
                            setTextFill(Color.BLACK);
                            break;
                    }
                }
            }
        });

        // Adiciona um botão para abrir a ficha completa do cliente (Hub)
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("Abrir Ficha");
            {
                btnAbrir.getStyleClass().add("flat");
                btnAbrir.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold; -fx-font-size: 11px;");
                btnAbrir.setOnAction(event -> {
                    Proposta p = getTableView().getItems().get(getIndex());
                    mainController.abrirClienteNoWorkspace(p.getProponente());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(btnAbrir));
            }
        });
    }

    private void configurarFiltroReativo() {
        FilteredList<Proposta> filteredData = new FilteredList<>(masterData, p -> true);
        txtBusca.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(proposta -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();

                String nome = proposta.getProponente().getNomeCompleto().toLowerCase();
                String cpf = proposta.getProponente().getCpf().replaceAll("[^0-9]", "");
                String banco = proposta.getBanco().getNome().toLowerCase();
                String status = proposta.getStatus().name().toLowerCase();

                return nome.contains(lowerCaseFilter) || cpf.contains(lowerCaseFilter)
                        || banco.contains(lowerCaseFilter) || status.contains(lowerCaseFilter);
            });
        });

        SortedList<Proposta> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePropostas.comparatorProperty());
        tablePropostas.setItems(sortedData);
    }

    @FXML
    public void recarregarDados() {
        // Trazendo todas as propostas com os detalhes já carregados
        List<Proposta> propostas = repository.findAllComDetalhes();
        masterData.setAll(propostas);
    }
}