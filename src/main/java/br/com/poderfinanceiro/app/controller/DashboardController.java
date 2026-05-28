package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ComissaoUIEventHub;
import br.com.poderfinanceiro.app.domain.event.PropostaUIEventHub;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.facade.IDashboardFacade;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

/**
 * <h1>DashboardController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela tela inicial (Dashboard).
 * Implementa o padrão <b>Humble Object</b>, delegando cálculos de métricas,
 * filtros e consultas para a {@link IDashboardFacade}.
 * </p>
 */
@Component
public class DashboardController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final String LOG_PREFIX = "[DashboardController]";

    private static final String MSG_SEM_CONVENIO = "Sem Convênio";
    private static final String MSG_CARREGANDO = "Calculando métricas do Dashboard...";

    private static final String[] FRASES_MOTIVACIONAIS = { "O sucesso é a soma de pequenos esforços repetidos dia após dia.",
            "Bons negócios não caem do céu, são construídos. Ótimo dia de vendas!",
            "Cada 'não' te deixa mais perto do próximo 'sim'. Vamos em frente!",
            "Consultoria de excelência gera clientes para a vida toda.", "Acredite no seu potencial. O fechamento perfeito começa agora!",
            "O único lugar onde o sucesso vem antes do trabalho é no dicionário." };

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IDashboardFacade dashboardFacade;
    private final Navigator navigator;
    private final PropostaUIEventHub propostaEventHub;
    private final ComissaoUIEventHub comissaoEventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private Label lblNomeConsultor, lblQtdAguardando, lblVolumeAprovado, lblComissaoPendente, lblComissaoPaga;
    @FXML private TextField txtBuscaPropostas;
    @FXML private TableView<PropostaModel> tabelaPropostas;
    @FXML private TableColumn<PropostaModel, String> colCliente, colBanco, colConvenio;
    @FXML private TableColumn<PropostaModel, BigDecimal> colValorSolicitado, colValor, colComissao;
    @FXML private TableColumn<PropostaModel, StatusPropostaModel> colStatus;
    @FXML private TableColumn<PropostaModel, Void> colAcoes;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<PropostaModel> masterData = FXCollections.observableArrayList();
    private final ObservableList<PropostaModel> filteredData = FXCollections.observableArrayList();
    private final Random randomGenerator = new Random();
    private boolean isPrimeiraCarga = true;

    public DashboardController(IDashboardFacade dashboardFacade, Navigator navigator, PropostaUIEventHub propostaEventHub,
            ComissaoUIEventHub comissaoEventHub) {
        this.dashboardFacade = dashboardFacade;
        this.navigator = navigator;
        this.propostaEventHub = propostaEventHub;
        this.comissaoEventHub = comissaoEventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface do Dashboard...", LOG_PREFIX);

        propostaEventHub.inscrever(this::recarregarDadosSilencioso);
        comissaoEventHub.inscrever(this::recarregarDadosSilencioso);

        carregarNomeConsultor();
        configurarTabela();
        configurarBuscaReativa();
        carregarDadosReais();

        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d registro(s)", Bindings.size(filteredData)));
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo dos hubs de eventos.", LOG_PREFIX);
        propostaEventHub.desinscrever(this::recarregarDadosSilencioso);
        comissaoEventHub.desinscrever(this::recarregarDadosSilencioso);
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void carregarNomeConsultor() {
        String nome = dashboardFacade.obterNomeConsultorLogado();
        lblNomeConsultor.setText(nome);
        log.trace("{} [UI] Nome do consultor atualizado: {}", LOG_PREFIX, nome);
    }

    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da tabela de propostas.", LOG_PREFIX);
        tabelaPropostas.setItems(filteredData);

        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBanco().getNome()));
        colConvenio.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getConvenioOrgao() != null ? d.getValue().getConvenioOrgao().getLabel() : MSG_SEM_CONVENIO));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        configurarColunaMoeda(colValorSolicitado, "valorSolicitado");
        configurarColunaMoeda(colValor, "valorAprovado");
        configurarColunaMoeda(colComissao, "comissaoEstimada");

        configurarColunaAcoes();
    }

    private void configurarColunaMoeda(TableColumn<PropostaModel, BigDecimal> coluna, String propertyName) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "-" : FinanceiroUtils.formatarParaExibicao(item));
            }
        });
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("📂 Abrir");
            {
                btnAbrir.getStyleClass().addAll("flat", "accent");
                btnAbrir.setCursor(Cursor.HAND);
                btnAbrir.setOnAction(event -> {
                    PropostaModel proposta = getTableView().getItems().get(getIndex());
                    if (proposta != null && proposta.getProponente() != null) {
                        log.info("{} [TELEMETRIA] Abrindo proposta ID={} do cliente {}", LOG_PREFIX, proposta.getId(),
                                proposta.getProponente().getNomeCompleto());
                        navigator.abrirPropostaNoWorkspace(proposta);
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnAbrir);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void configurarBuscaReativa() {
        log.trace("{} [UI] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBuscaPropostas.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("{} [UI] Termo de busca alterado: '{}'", LOG_PREFIX, newVal);
            aplicarFiltro(newVal);
        });
    }

    private void aplicarFiltro(String termo) {
        AsyncUtils.executarTaskAsync(() -> dashboardFacade.filtrarPropostas(masterData, termo), resultado -> filteredData.setAll(resultado),
                erro -> log.error("{} [SISTEMA] Erro ao filtrar propostas: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: LÓGICA DE DADOS (ASYNC) E MÉTRICAS
    // ==========================================================================================
    public void recarregarDadosSilencioso() {
        executarCargaDeDados(false);
    }

    @FXML public void carregarDadosReais() {
        executarCargaDeDados(true);
    }

    private void executarCargaDeDados(boolean exibirLoading) {
        log.trace("{} [SISTEMA] Executando carga de dados. Exibir Loading: {}", LOG_PREFIX, exibirLoading);
        if (exibirLoading)
            navigator.mostrarLoading(MSG_CARREGANDO);

        AsyncUtils.executarTaskAsync(dashboardFacade::calcularMetricasGerais, metricas -> {
            atualizarInterfaceDoDashboard(metricas);
            if (exibirLoading) {
                if (isPrimeiraCarga) {
                    exibirLoadingMotivacional();
                    isPrimeiraCarga = false;
                } else {
                    navigator.ocultarLoading();
                }
            }
        }, erro -> {
            if (exibirLoading)
                navigator.ocultarLoading();
            log.error("{} [SISTEMA] Erro ao carregar métricas do Dashboard: {}", LOG_PREFIX, erro.getMessage());
        });
    }

    private void atualizarInterfaceDoDashboard(IDashboardFacade.MetricasDashboardDTO metricas) {
        log.trace("{} [UI] Atualizando componentes visuais com novas métricas.", LOG_PREFIX);
        masterData.setAll(metricas.propostas());
        aplicarFiltro(txtBuscaPropostas.getText());

        lblQtdAguardando.setText(String.valueOf(metricas.qtdAguardando()));
        lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(metricas.volumeAprovado()));
        lblComissaoPendente.setText(FinanceiroUtils.formatarParaExibicao(metricas.comissaoPendente()));
        lblComissaoPaga.setText(FinanceiroUtils.formatarParaExibicao(metricas.comissaoPaga()));

        log.info("{} [TELEMETRIA] Dashboard atualizado. {} propostas carregadas.", LOG_PREFIX, metricas.propostas().size());
    }

    private void exibirLoadingMotivacional() {
        String fraseSorteada = FRASES_MOTIVACIONAIS[randomGenerator.nextInt(FRASES_MOTIVACIONAIS.length)];
        log.trace("{} [UI] Exibindo frase motivacional: '{}'", LOG_PREFIX, fraseSorteada);
        navigator.mostrarLoading("💡 " + fraseSorteada);

        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
        delay.setOnFinished(e -> navigator.ocultarLoading());
        delay.play();
    }

    @FXML private void simularNovo() {
        log.info("{} [TELEMETRIA] Usuário iniciou nova simulação via Dashboard.", LOG_PREFIX);
        txtBuscaPropostas.clear();
        navigator.abrirClienteNoWorkspace(null);
    }
}
