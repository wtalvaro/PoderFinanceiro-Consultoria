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
import javafx.concurrent.Task; // 💉 IMPORTAÇÃO DO MAQUEIRO
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
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValor;
    @FXML
    private TableColumn<PropostaModel, StatusPropostaModel> colStatus;

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
        // 🧠 Triagem de Identidade: Busca o usuário logado no AuthService
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

        // 🚀 O AJUSTE: Buscando o Convênio direto da Proposta
        colConvenio.setCellValueFactory(data -> {
            var convenio = data.getValue().getConvenioOrgao();
            return new SimpleStringProperty(convenio != null ? convenio.getLabel() : "Sem Convênio");
        });

        colValor.setCellValueFactory(new PropertyValueFactory<>("valorAprovado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colValor.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else
                    setText(FinanceiroUtils.formatarParaExibicao(item));
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

    /**
     * 💉 Busca informações reais de forma assíncrona (Sem travar a tela)
     */
    @FXML
    public void carregarDadosReais() {
        // 1. Acende o aviso visual de carregamento
        mainController.mostrarLoading("Calculando métricas do Dashboard...");

        // 2. Prepara a Task (O Maqueiro vai processar tudo na Thread Secundária)
        Task<ResultadoDashboard> taskBusca = new Task<>() {
            @Override
            protected ResultadoDashboard call() throws Exception {
                // Consultas pesadas no banco
                List<PropostaModel> propostas = propostaRepository.findAllComDetalhes();

                // Processamento de matemática pesada FORA da tela!
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

                // Preenche a prancheta com todos os dados calculados e retorna
                return new ResultadoDashboard(propostas, aguardando, volumePago, comissaoTotal);
            }
        };

        // 3. Quando o maqueiro voltar com a prancheta pronta:
        taskBusca.setOnSucceeded(event -> {
            ResultadoDashboard res = taskBusca.getValue();

            // Atualiza a tabela
            masterData.setAll(res.propostas());

            // Atualiza os Monitores Vitais (KPIs)
            lblQtdAguardando.setText(String.valueOf(res.qtdAguardando()));
            lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(res.volumeAprovado()));
            lblComissaoEstimada.setText(FinanceiroUtils.formatarParaExibicao(res.comissoesPendentes()));

            // Apaga a luz da sala de espera
            mainController.ocultarLoading();
        });

        // 4. Se houver falha de banco de dados
        taskBusca.setOnFailed(event -> {
            mainController.ocultarLoading();
            taskBusca.getException().printStackTrace();
        });

        // 5. Dispara a Thread
        Thread thread = new Thread(taskBusca);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void simularNovo() {
        txtBuscaPropostas.clear();
        mainController.abrirClienteNoWorkspace(null);
    }

    // ==========================================================
    // DTO INTERNO (A "Prancheta de Exames")
    // ==========================================================
    /**
     * Um 'record' é perfeito aqui: ele apenas transporta os dados
     * da Thread secundária para a Thread da Interface de forma imutável e segura.
     */
    private record ResultadoDashboard(
            List<PropostaModel> propostas,
            long qtdAguardando,
            BigDecimal volumeAprovado,
            BigDecimal comissoesPendentes) {
    }
}