package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DashboardController {

    private final PropostaRepository propostaRepository;
    private final ComissaoRepository comissaoRepository;
    private final MainController mainController;
    private final AuthService authService;

    @FXML
    private Label lblNomeConsultor;
    @FXML
    private Label lblQtdAguardando;
    @FXML
    private Label lblVolumeAprovado;
    @FXML
    private Label lblComissaoEstimada;
    @FXML
    private TextField txtBuscaPropostas;

    @FXML
    private TableView<PropostaModel> tabelaPropostas;
    @FXML
    private TableColumn<PropostaModel, String> colCliente;
    @FXML
    private TableColumn<PropostaModel, String> colBanco;
    @FXML
    private TableColumn<PropostaModel, String> colConvenio;

    // Novas Colunas
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValorSolicitado;
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValor; // Vlr. Aprovado
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colComissao;
    @FXML
    private TableColumn<PropostaModel, StatusPropostaModel> colStatus;
    @FXML
    private TableColumn<PropostaModel, Void> colAcoes;

    private final ObservableList<PropostaModel> masterData = FXCollections.observableArrayList();

    public DashboardController(PropostaRepository propostaRepository,
            ComissaoRepository comissaoRepository,
            MainController mainController, AuthService authService) {
        this.propostaRepository = propostaRepository;
        this.comissaoRepository = comissaoRepository;
        this.mainController = mainController;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        UsuarioModel usuario = authService.getUsuarioLogado();

        if (usuario != null) {
            lblNomeConsultor.setText(usuario.getNome());
        } else {
            lblNomeConsultor.setText("Consultor Offline");
        }

        configurarTabela();
        configurarBuscaReativa();
        carregarDadosReais();
    }

    private void configurarTabela() {
        // Data Mappings padrão
        colCliente.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco().getNome()));

        colConvenio.setCellValueFactory(data -> {
            var convenio = data.getValue().getConvenioOrgao();
            return new SimpleStringProperty(convenio != null ? convenio.getLabel() : "Sem Convênio");
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Data Mappings de Valores Monetários com reaproveitamento de código (DRY)
        configurarColunaMoeda(colValorSolicitado, "valorSolicitado");
        configurarColunaMoeda(colValor, "valorAprovado");
        configurarColunaMoeda(colComissao, "comissaoEstimada");

        // CellFactory para injetar um UI Control (Button) na TableView
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("📂 Abrir");

            {
                btnAbrir.getStyleClass().addAll("flat", "accent");
                btnAbrir.setCursor(javafx.scene.Cursor.HAND);

                // Dispara o Roteamento de Tela enviando a entidade Proponente vinculada
                btnAbrir.setOnAction(event -> {
                    PropostaModel proposta = getTableView().getItems().get(getIndex());
                    if (proposta != null && proposta.getProponente() != null) {
                        mainController.abrirClienteNoWorkspace(proposta.getProponente());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnAbrir);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
    }

    /**
     * Utilitário para evitar código duplicado na formatação de colunas BigDecimal.
     */
    private void configurarColunaMoeda(TableColumn<PropostaModel, BigDecimal> coluna, String propertyName) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("-");
                } else {
                    setText(FinanceiroUtils.formatarParaExibicao(item));
                }
            }
        });
    }

    private void configurarBuscaReativa() {
        FilteredList<PropostaModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscaPropostas.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(proposta -> {
                if (newVal == null || newVal.isEmpty())
                    return true;

                String filtro = newVal.toLowerCase();
                String nome = proposta.getProponente().getNomeCompleto().toLowerCase();
                String cpf = proposta.getProponente().getCpf().replaceAll("[^0-9]", "");
                String banco = proposta.getBanco().getNome().toLowerCase();

                return nome.contains(filtro) || cpf.contains(filtro) || banco.contains(filtro);
            });
        });

        SortedList<PropostaModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tabelaPropostas.comparatorProperty());

        tabelaPropostas.setItems(sortedData);
    }

    @FXML
    public void carregarDadosReais() {
        mainController.mostrarLoading("Calculando métricas do Dashboard...");

        Task<ResultadoDashboard> taskBusca = new Task<>() {
            @Override
            protected ResultadoDashboard call() throws Exception {
                List<PropostaModel> propostas = propostaRepository.findAllComDetalhes();

                long aguardando = propostas.stream()
                        .filter(p -> p.getStatus() == StatusPropostaModel.DIGITADA
                                || p.getStatus() == StatusPropostaModel.PENDENTE)
                        .count();

                BigDecimal volumePago = propostas.stream()
                        .filter(p -> p.getStatus() == StatusPropostaModel.PAGO)
                        .map(p -> p.getValorFinalCliente() != null ? p.getValorFinalCliente() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal comissaoTotal = comissaoRepository.findAll().stream()
                        .filter(c -> "Pago".equalsIgnoreCase(c.getStatusPagamento()))
                        .map(c -> c.getValorBrutoComissao() != null ? c.getValorBrutoComissao() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new ResultadoDashboard(propostas, aguardando, volumePago, comissaoTotal);
            }
        };

        taskBusca.setOnSucceeded(event -> {
            ResultadoDashboard res = taskBusca.getValue();
            masterData.setAll(res.propostas());

            lblQtdAguardando.setText(String.valueOf(res.qtdAguardando()));
            lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(res.volumeAprovado()));
            lblComissaoEstimada.setText(FinanceiroUtils.formatarParaExibicao(res.comissoesPendentes()));

            mainController.ocultarLoading();
        });

        taskBusca.setOnFailed(event -> {
            mainController.ocultarLoading();
            taskBusca.getException().printStackTrace();
        });

        Thread thread = new Thread(taskBusca);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void simularNovo() {
        txtBuscaPropostas.clear();
        mainController.abrirClienteNoWorkspace(null);
    }

    private record ResultadoDashboard(
            List<PropostaModel> propostas,
            long qtdAguardando,
            BigDecimal volumeAprovado,
            BigDecimal comissoesPendentes) {
    }
}