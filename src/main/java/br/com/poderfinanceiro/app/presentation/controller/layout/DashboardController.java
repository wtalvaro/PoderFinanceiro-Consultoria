package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.application.facade.IDashboardFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.domain.event.ComissaoUIEventHub;
import br.com.poderfinanceiro.app.domain.event.PropostaUIEventHub;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
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
 * Implementa o padrão <b>Humble Object</b>, utilizando utilitários Gold
 * Standard
 * para garantir integridade de dados e fluidez na UI Thread via Virtual
 * Threads.
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

    private static final String[] FRASES_MOTIVACIONAIS = {
            "O sucesso é a soma de pequenos esforços repetidos dia após dia.",
            "Bons negócios não caem do céu, são construídos. Ótimo dia de vendas!",
            "Cada 'não' te deixa mais perto do próximo 'sim'. Vamos em frente!",
            "Consultoria de excelência gera clientes para a vida toda.",
            "Acredite no seu potencial. O fechamento perfeito começa agora!",
            "O único lugar onde o sucesso vem antes do trabalho é no dicionário."
    };

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
    @FXML
    private Label lblNomeConsultor, lblQtdAguardando, lblVolumeAprovado, lblComissaoPendente, lblComissaoPaga;
    @FXML
    private TextField txtBuscaPropostas;
    @FXML
    private TableView<PropostaModel> tabelaPropostas;
    @FXML
    private TableColumn<PropostaModel, String> colCliente, colBanco, colConvenio;
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValorSolicitado, colValor, colComissao;
    @FXML
    private TableColumn<PropostaModel, StatusPropostaModel> colStatus;
    @FXML
    private TableColumn<PropostaModel, Void> colAcoes;
    @FXML
    private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<PropostaModel> masterData = FXCollections.observableArrayList();
    private final ObservableList<PropostaModel> filteredData = FXCollections.observableArrayList();
    private final Random randomGenerator = new Random();
    private boolean isPrimeiraCarga = true;

    public DashboardController(IDashboardFacade dashboardFacade, Navigator navigator,
            PropostaUIEventHub propostaEventHub, ComissaoUIEventHub comissaoEventHub) {
        this.dashboardFacade = dashboardFacade;
        this.navigator = navigator;
        this.propostaEventHub = propostaEventHub;
        this.comissaoEventHub = comissaoEventHub;
        log.info("{} [SISTEMA] Controlador do Dashboard instanciado.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface e hubs de eventos.", LOG_PREFIX);

        propostaEventHub.inscrever(this::recarregarDadosSilencioso);
        comissaoEventHub.inscrever(this::recarregarDadosSilencioso);

        carregarNomeConsultor();
        configurarTabela();
        configurarBuscaReativa();
        carregarDadosReais();

        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d registro(s)", Bindings.size(filteredData)));
        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos e desinscrevendo de eventos.", LOG_PREFIX);
        propostaEventHub.desinscrever(this::recarregarDadosSilencioso);
        comissaoEventHub.desinscrever(this::recarregarDadosSilencioso);
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void carregarNomeConsultor() {
        String nome = dashboardFacade.obterNomeConsultorLogado();
        lblNomeConsultor.setText(nome);
        log.trace("{} [SISTEMA] Nome do consultor definido: {}", LOG_PREFIX, nome);
    }

    private void configurarTabela() {
        log.debug("{} [SISTEMA] Configurando colunas da TableView de propostas recentes.", LOG_PREFIX);
        tabelaPropostas.setItems(filteredData);

        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBanco().getNome()));
        colConvenio.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getConvenioOrgao() != null ? d.getValue().getConvenioOrgao().getLabel()
                        : MSG_SEM_CONVENIO));
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
            private final Button btnAbrir = new Button("📂 Abrir");
            {
                btnAbrir.getStyleClass().addAll("flat", "accent");
                btnAbrir.setCursor(Cursor.HAND);
                btnAbrir.setOnAction(event -> {
                    PropostaModel proposta = getTableView().getItems().get(getIndex());
                    if (proposta != null && proposta.getProponente() != null) {
                        log.info("{} [TELEMETRIA] Abertura de proposta solicitada. ID: {}", LOG_PREFIX,
                                proposta.getId());
                        navigator.abrirPropostaNoWorkspace(proposta);
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
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void configurarBuscaReativa() {
        log.trace("{} [SISTEMA] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBuscaPropostas.textProperty().addListener((obs, oldVal, newVal) -> {
            log.trace("{} [TELEMETRIA] Filtro de busca alterado para: '{}'", LOG_PREFIX, newVal);
            aplicarFiltro(newVal);
        });
    }

    private void aplicarFiltro(String termo) {
        // Uso do ValidationUtils para triagem de busca
        String termoSaneado = ValidationUtils.isPreenchido(termo) ? termo.trim() : "";

        AsyncUtils.executarTaskAsync(
                () -> dashboardFacade.filtrarPropostas(masterData, termoSaneado),
                resultado -> {
                    filteredData.setAll(resultado);
                    log.trace("{} [TELEMETRIA] Filtro aplicado. {} registros exibidos.", LOG_PREFIX, resultado.size());
                },
                erro -> log.error("{} [SISTEMA] Falha ao filtrar propostas: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: LÓGICA DE DADOS (ASYNC) E MÉTRICAS
    // ==========================================================================================
    public void recarregarDadosSilencioso() {
        log.info("{} [TELEMETRIA] Recarga silenciosa disparada por evento do Hub.", LOG_PREFIX);
        executarCargaDeDados(false);
    }

    @FXML
    public void carregarDadosReais() {
        log.info("{} [TELEMETRIA] Recarga manual solicitada pelo usuário.", LOG_PREFIX);
        executarCargaDeDados(true);
    }

    private void executarCargaDeDados(boolean exibirLoading) {
        if (exibirLoading) {
            navigator.mostrarLoading(MSG_CARREGANDO);
        }

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade para cálculo de métricas consolidadas.", LOG_PREFIX);
                    return dashboardFacade.calcularMetricasGerais();
                },
                metricas -> {
                    atualizarInterfaceDoDashboard(metricas);
                    if (exibirLoading) {
                        if (isPrimeiraCarga) {
                            exibirLoadingMotivacional();
                            isPrimeiraCarga = false;
                        } else {
                            navigator.ocultarLoading();
                        }
                    }
                    log.info("{} [AUDITORIA] Métricas do Dashboard atualizadas com sucesso.", LOG_PREFIX);
                },
                erro -> {
                    if (exibirLoading)
                        navigator.ocultarLoading();
                    log.error("{} [SISTEMA] Erro crítico ao carregar métricas: {}", LOG_PREFIX, erro.getMessage());
                });
    }

    private void atualizarInterfaceDoDashboard(IDashboardFacade.MetricasDashboardDTO metricas) {
        masterData.setAll(metricas.propostas());
        aplicarFiltro(txtBuscaPropostas.getText());

        lblQtdAguardando.setText(String.valueOf(metricas.qtdAguardando()));
        lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(metricas.volumeAprovado()));
        lblComissaoPendente.setText(FinanceiroUtils.formatarParaExibicao(metricas.comissaoPendente()));
        lblComissaoPaga.setText(FinanceiroUtils.formatarParaExibicao(metricas.comissaoPaga()));

        log.debug("{} [TELEMETRIA] UI atualizada com {} propostas e volume de R$ {}.",
                LOG_PREFIX, metricas.propostas().size(), metricas.volumeAprovado());
    }

    private void exibirLoadingMotivacional() {
        String fraseSorteada = FRASES_MOTIVACIONAIS[randomGenerator.nextInt(FRASES_MOTIVACIONAIS.length)];
        log.trace("{} [UI] Exibindo frase motivacional: '{}'", LOG_PREFIX, fraseSorteada);
        navigator.mostrarLoading("💡 " + fraseSorteada);

        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
        delay.setOnFinished(e -> navigator.ocultarLoading());
        delay.play();
    }

    @FXML
    private void simularNovo() {
        log.info("{} [TELEMETRIA] Redirecionando para nova simulação via Dashboard.", LOG_PREFIX);
        txtBuscaPropostas.clear();
        navigator.abrirClienteNoWorkspace(null);
    }
}
