package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.model.ComissaoModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
public class DashboardController {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String STATUS_PAGO_COMISSAO = "Pago";
    private static final String STATUS_LIQUIDADO_COMISSAO = "Liquidado";
    private static final String MSG_OFFLINE = "Consultor Offline";
    private static final String MSG_SEM_CONVENIO = "Sem Convênio";
    private static final String MSG_CARREGANDO = "Calculando métricas do Dashboard...";

    private static final String[] FRASES_MOTIVACIONAIS = {
            "O sucesso é a soma de pequenos esforços repetidos dia após dia.",
            "Bons negócios não caem do céu, são construídos. Ótimo dia de vendas!",
            "Cada 'não' te deixa mais perto do próximo 'sim'. Vamos em frente!",
            "Consultoria de excelência gera clientes para a vida toda.",
            "Acredite no seu potencial. O fechamento perfeito começa agora!",
            "O único lugar onde o sucesso vem antes do trabalho é no dicionário."
    };

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final PropostaRepository propostaRepository;
    private final ComissaoRepository comissaoRepository;
    private final MainController mainController;
    private final AuthService authService;

    // =========================================================================
    // COMPONENTES UI (FXML)
    // =========================================================================
    @FXML
    private Label lblNomeConsultor;
    @FXML
    private Label lblQtdAguardando;
    @FXML
    private Label lblVolumeAprovado;
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
    private final Random randomGenerator = new Random();

    public DashboardController(PropostaRepository propostaRepository, ComissaoRepository comissaoRepository,
            MainController mainController, AuthService authService) {
        this.propostaRepository = propostaRepository;
        this.comissaoRepository = comissaoRepository;
        this.mainController = mainController;
        this.authService = authService;
    }

    // =========================================================================
    // INICIALIZAÇÃO E CONFIGURAÇÃO DE UI
    // =========================================================================
    @FXML
    public void initialize() {
        carregarNomeConsultor();
        configurarTabela();
        configurarBuscaReativa();
        carregarDadosReais();
    }

    private void carregarNomeConsultor() {
        UsuarioModel usuario = authService.getUsuarioLogado();
        lblNomeConsultor.setText(usuario != null ? usuario.getNome() : MSG_OFFLINE);
    }

    private void configurarTabela() {
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBanco().getNome()));
        colConvenio.setCellValueFactory(d -> {
            var convenio = d.getValue().getConvenioOrgao();
            return new SimpleStringProperty(convenio != null ? convenio.getLabel() : MSG_SEM_CONVENIO);
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        configurarColunaMoeda(colValorSolicitado, "valorSolicitado");
        configurarColunaMoeda(colValor, "valorAprovado");
        configurarColunaMoeda(colComissao, "comissaoEstimada");

        configurarColunaAcoes();
    }

    private void configurarColunaMoeda(TableColumn<PropostaModel, BigDecimal> coluna, String propertyName) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "-" : FinanceiroUtils.formatarParaExibicao(item));
            }
        });
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotaoAbrirProposta();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnAbrir);
                    setAlignment(Pos.CENTER);
                }
            }

            private Button criarBotaoAbrirProposta() {
                Button btn = new Button("📂 Abrir");
                btn.getStyleClass().addAll("flat", "accent");
                btn.setCursor(Cursor.HAND);
                btn.setOnAction(event -> {
                    PropostaModel proposta = getTableView().getItems().get(getIndex());
                    if (proposta != null && proposta.getProponente() != null) {
                        mainController.abrirPropostaNoWorkspace(proposta);
                    }
                });
                return btn;
            }
        });
    }

    private void configurarBuscaReativa() {
        FilteredList<PropostaModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscaPropostas.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(proposta -> atendeFiltroDeBusca(proposta, newVal));
        });

        SortedList<PropostaModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tabelaPropostas.comparatorProperty());
        tabelaPropostas.setItems(sortedData);
    }

    private boolean atendeFiltroDeBusca(PropostaModel proposta, String termo) {
        if (termo == null || termo.isBlank())
            return true;

        String filtro = termo.toLowerCase();
        String nome = proposta.getProponente().getNomeCompleto().toLowerCase();
        String cpf = proposta.getProponente().getCpf().replaceAll("[^0-9]", "");
        String banco = proposta.getBanco().getNome().toLowerCase();

        return nome.contains(filtro) || cpf.contains(filtro) || banco.contains(filtro);
    }

    // =========================================================================
    // LÓGICA DE DADOS (ASYNC) E MÉTRICAS
    // =========================================================================
    @FXML
    public void carregarDadosReais() {
        mainController.mostrarLoading(MSG_CARREGANDO);

        Task<ResultadoDashboard> taskBusca = new Task<>() {
            @Override
            protected ResultadoDashboard call() {
                List<PropostaModel> propostas = propostaRepository.findAllComDetalhes();
                List<ComissaoModel> comissoes = comissaoRepository.findAll();
                return calcularMetricasDoDashboard(propostas, comissoes);
            }
        };

        taskBusca.setOnSucceeded(event -> atualizarInterfaceDoDashboard(taskBusca.getValue()));
        taskBusca.setOnFailed(event -> {
            mainController.ocultarLoading();
            taskBusca.getException().printStackTrace();
        });

        Thread thread = new Thread(taskBusca);
        thread.setDaemon(true);
        thread.start();
    }

    private ResultadoDashboard calcularMetricasDoDashboard(List<PropostaModel> propostas,
            List<ComissaoModel> comissoes) {
        long aguardando = propostas.stream().filter(this::isPropostaAguardando).count();
        BigDecimal volumeAprovado = somarVolumeAprovado(propostas);
        BigDecimal comissaoPendente = somarComissoes(comissoes, false);
        BigDecimal comissaoPaga = somarComissoes(comissoes, true);

        return new ResultadoDashboard(propostas, aguardando, volumeAprovado, comissaoPendente, comissaoPaga);
    }

    private boolean isPropostaAguardando(PropostaModel p) {
        return p.getStatus() == StatusPropostaModel.DIGITADA || p.getStatus() == StatusPropostaModel.PENDENTE;
    }

    private BigDecimal somarVolumeAprovado(List<PropostaModel> propostas) {
        return propostas.stream()
                .filter(p -> p.getStatus() == StatusPropostaModel.PAGO)
                .map(p -> p.getValorFinalCliente() != null ? p.getValorFinalCliente() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarComissoes(List<ComissaoModel> comissoes, boolean isPaga) {
        return comissoes.stream()
                .filter(c -> isComissaoPaga(c) == isPaga)
                .map(c -> {
                    BigDecimal valor = isPaga ? c.getValorPagoPelaPoder() : c.getValorBrutoComissao();
                    return valor != null ? valor : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isComissaoPaga(ComissaoModel c) {
        String status = c.getStatusPagamento();
        return STATUS_PAGO_COMISSAO.equalsIgnoreCase(status) || STATUS_LIQUIDADO_COMISSAO.equalsIgnoreCase(status);
    }

    // =========================================================================
    // ATUALIZAÇÃO DA UI E EXPERIÊNCIA DO USUÁRIO
    // =========================================================================
    private void atualizarInterfaceDoDashboard(ResultadoDashboard res) {
        masterData.setAll(res.propostas());

        lblQtdAguardando.setText(String.valueOf(res.qtdAguardando()));
        lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(res.volumeAprovado()));
        lblComissaoPendente.setText(FinanceiroUtils.formatarParaExibicao(res.comissaoPendente()));
        lblComissaoPaga.setText(FinanceiroUtils.formatarParaExibicao(res.comissaoPaga()));

        exibirLoadingMotivacional();
    }

    private void exibirLoadingMotivacional() {
        String fraseSorteada = FRASES_MOTIVACIONAIS[randomGenerator.nextInt(FRASES_MOTIVACIONAIS.length)];
        mainController.mostrarLoading("💡 " + fraseSorteada);

        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
        delay.setOnFinished(e -> mainController.ocultarLoading());
        delay.play();
    }

    @FXML
    private void simularNovo() {
        txtBuscaPropostas.clear();
        mainController.abrirClienteNoWorkspace(null);
    }

    // =========================================================================
    // RECORDS AUXILIARES
    // =========================================================================
    private record ResultadoDashboard(
            List<PropostaModel> propostas,
            long qtdAguardando,
            BigDecimal volumeAprovado,
            BigDecimal comissaoPendente,
            BigDecimal comissaoPaga) {
    }
}