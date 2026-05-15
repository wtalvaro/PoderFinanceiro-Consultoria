package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.model.ComissaoModel;
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

    private final String[] FRASES_MOTIVACIONAIS = {
            "O sucesso é a soma de pequenos esforços repetidos dia após dia.",
            "Bons negócios não caem do céu, são construídos. Ótimo dia de vendas!",
            "Cada 'não' te deixa mais perto do próximo 'sim'. Vamos em frente!",
            "Consultoria de excelência gera clientes para a vida toda.",
            "Acredite no seu potencial. O fechamento perfeito começa agora!",
            "O único lugar onde o sucesso vem antes do trabalho é no dicionário."
    };

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

    // ✅ NOVOS LABELS: Expectativa x Realidade
    @FXML
    private Label lblComissaoPendente;
    @FXML
    private Label lblComissaoPaga;

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
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValorSolicitado;
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValor;
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
        colCliente.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco().getNome()));
        colConvenio.setCellValueFactory(data -> {
            var convenio = data.getValue().getConvenioOrgao();
            return new SimpleStringProperty(convenio != null ? convenio.getLabel() : "Sem Convênio");
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        configurarColunaMoeda(colValorSolicitado, "valorSolicitado");
        configurarColunaMoeda(colValor, "valorAprovado");
        configurarColunaMoeda(colComissao, "comissaoEstimada");

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("📂 Abrir");
            {
                btnAbrir.getStyleClass().addAll("flat", "accent");
                btnAbrir.setCursor(javafx.scene.Cursor.HAND);
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
                List<ComissaoModel> comissoes = comissaoRepository.findAll();

                // 1. O que está travado na esteira
                long aguardando = propostas.stream()
                        .filter(p -> p.getStatus() == StatusPropostaModel.DIGITADA
                                || p.getStatus() == StatusPropostaModel.PENDENTE)
                        .count();

                // 2. Produção Aprovada Total
                BigDecimal volumeAprovado = propostas.stream()
                        .filter(p -> p.getStatus() == StatusPropostaModel.PAGO)
                        .map(p -> p.getValorFinalCliente() != null ? p.getValorFinalCliente() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 3. EXPECTATIVA: Comissões que não estão pagas (Ciclo Atual/Semanal)
                BigDecimal comissaoPendente = comissoes.stream()
                        .filter(c -> !"Pago".equalsIgnoreCase(c.getStatusPagamento())
                                && !"Liquidado".equalsIgnoreCase(c.getStatusPagamento()))
                        .map(c -> c.getValorBrutoComissao() != null ? c.getValorBrutoComissao() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 4. REALIDADE: Comissões já pagas ao consultor
                BigDecimal comissaoPaga = comissoes.stream()
                        .filter(c -> "Pago".equalsIgnoreCase(c.getStatusPagamento())
                                || "Liquidado".equalsIgnoreCase(c.getStatusPagamento()))
                        .map(c -> c.getValorPagoPelaPoder() != null ? c.getValorPagoPelaPoder() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new ResultadoDashboard(propostas, aguardando, volumeAprovado, comissaoPendente, comissaoPaga);
            }
        };

        taskBusca.setOnSucceeded(event -> {
            // 1. Atualiza os dados por trás dos panos (o usuário ainda não está vendo)
            ResultadoDashboard res = taskBusca.getValue();
            masterData.setAll(res.propostas());

            lblQtdAguardando.setText(String.valueOf(res.qtdAguardando()));
            lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(res.volumeAprovado()));
            lblComissaoPendente.setText(FinanceiroUtils.formatarParaExibicao(res.comissaoPendente()));
            lblComissaoPaga.setText(FinanceiroUtils.formatarParaExibicao(res.comissaoPaga()));

            // 2. Sorteia uma frase motivacional
            int indiceSorteado = new java.util.Random().nextInt(FRASES_MOTIVACIONAIS.length);
            String fraseMotivacional = "💡 " + FRASES_MOTIVACIONAIS[indiceSorteado];

            // 3. Muda o texto do Loading atual para a frase
            mainController.mostrarLoading(fraseMotivacional);

            // 4. Cria o "Delay Artificial" de 2.5 segundos antes de liberar a tela
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(3.5));
            delay.setOnFinished(e -> mainController.ocultarLoading());
            delay.play();
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

    // Record atualizado para refletir a separação
    private record ResultadoDashboard(
            List<PropostaModel> propostas,
            long qtdAguardando,
            BigDecimal volumeAprovado,
            BigDecimal comissaoPendente,
            BigDecimal comissaoPaga) {
    }
}