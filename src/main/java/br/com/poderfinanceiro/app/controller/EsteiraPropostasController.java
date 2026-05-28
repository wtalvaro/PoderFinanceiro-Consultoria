package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.event.PropostaUIEventHub;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.facade.IEsteiraPropostasFacade;
import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * <h1>EsteiraPropostasController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar a Esteira de
 * Propostas. Implementa o padrão <b>Humble Object</b>, delegando a persistência
 * e filtros para a {@link IEsteiraPropostasFacade} e interações globais para o
 * {@link Navigator}.
 * </p>
 */
@Component
@Scope("prototype")
public class EsteiraPropostasController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(EsteiraPropostasController.class);
    private static final String LOG_PREFIX = "[EsteiraPropostasController]";

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String HTML_TEMPLATE_IA = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
                <style>
                    body { font-family: sans-serif; padding: 25px; box-sizing: border-box; color: #333; }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """;

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IEsteiraPropostasFacade esteiraFacade;
    private final ApplicationContext context;
    private final Navigator navigator;
    private final PropostaUIEventHub eventHub;
    private final ProponenteUIEventHub proponenteEventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TextField txtBusca;
    @FXML private TableView<PropostaModel> tablePropostas;
    @FXML private TableColumn<PropostaModel, String> colData, colCliente, colBanco, colValorSol, colStatus;
    @FXML private StackPane containerFormulario;
    @FXML private VBox paneVazio;

    @FXML private VBox overlayFeedback;
    @FXML private Label lblFeedbackIcon, lblFeedbackTitle;
    @FXML private WebView webFeedback;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<PropostaModel> listaPropostas = FXCollections.observableArrayList();
    private Runnable acaoFeedbackPendente;
    private PropostaController formularioAtivo;
    private Node viewFormularioAtivo;
    private Long propostaIdPendenteSelecao = null;

    public EsteiraPropostasController(IEsteiraPropostasFacade esteiraFacade, ApplicationContext context,
            Navigator navigator, PropostaUIEventHub eventHub, ProponenteUIEventHub proponenteEventHub) {
        this.esteiraFacade = esteiraFacade;
        this.context = context;
        this.navigator = navigator;
        this.eventHub = eventHub;
        this.proponenteEventHub = proponenteEventHub;
        log.info("{} [SISTEMA] Controlador da Esteira instanciado com suporte a Navigator.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface da Esteira de Propostas...", LOG_PREFIX);
        configurarTabela();
        configurarFiltroReativo();

        eventHub.inscrever(this::recarregarDados);
        proponenteEventHub.inscrever(this::recarregarDados);

        recarregarDados();

        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d proposta(s)", Bindings.size(listaPropostas)));
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo dos hubs de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::recarregarDados);
        proponenteEventHub.desinscrever(this::recarregarDados);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    @FXML public void recarregarDados() {
        log.trace("{} [TELEMETRIA] Recarregando lista de propostas.", LOG_PREFIX);
        filtrarPropostas(txtBusca.getText());
    }

    private void configurarFiltroReativo() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> filtrarPropostas(newVal));
    }

    private void filtrarPropostas(String termo) {
        AsyncUtils.executarTaskAsync(() -> esteiraFacade.filtrarPropostas(termo), propostasFiltradas -> {
            listaPropostas.setAll(propostasFiltradas);
            tablePropostas.setItems(listaPropostas);
            log.debug("{} [TELEMETRIA] Tabela atualizada: {} propostas.", LOG_PREFIX, propostasFiltradas.size());

            if (propostaIdPendenteSelecao != null) {
                selecionarPropostaPorId(propostaIdPendenteSelecao);
            }
        }, erro -> log.error("{} [SISTEMA] Erro ao filtrar propostas: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: CONFIGURAÇÃO DA TABELA (UI)
    // ==========================================================================================
    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da tabela.", LOG_PREFIX);
        colData.setCellValueFactory(cell -> {
            LocalDate data = cell.getValue().getDataSolicitacao();
            return new SimpleStringProperty(data != null ? data.format(FMT_DATA) : "-");
        });

        colCliente.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getProponente() != null ? cell.getValue().getProponente().getNomeCompleto() : "S/C"));
        colBanco.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getBanco() != null ? cell.getValue().getBanco().getNome() : "-"));
        colValorSol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getValorSolicitado() != null
                ? FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorSolicitado())
                : "0,00"));

        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().getLabel()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER;");
                    if (item.equals("Aprovada") || item.equals("Pago"))
                        setTextFill(Color.GREEN);
                    else if (item.equals("Reprovada") || item.equals("Cancelado"))
                        setTextFill(Color.RED);
                    else
                        setTextFill(Color.ORANGE);
                }
            }
        });

        tablePropostas.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                abrirFormularioComProposta(newSelection);
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 8: AÇÕES DE NEGÓCIO (CRUD)
    // ==========================================================================================
    @FXML private void criarNovaProposta() {
        log.info("{} [TELEMETRIA] Solicitando criação de nova proposta.", LOG_PREFIX);
        navigator.mostrarLoading("Preparando simulação...");

        AsyncUtils.executarTaskAsync(esteiraFacade::criarNovaPropostaEmBranco, novaProposta -> {
            navigator.ocultarLoading();
            abrirFormularioComProposta(novaProposta);
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [SISTEMA] Falha ao preparar proposta: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso("Não foi possível preparar a proposta: " + erro.getMessage());
        });
    }

    public void selecionarPropostaPorId(Long id) {
        if (listaPropostas.isEmpty()) {
            this.propostaIdPendenteSelecao = id;
            return;
        }
        for (PropostaModel p : tablePropostas.getItems()) {
            if (p.getId().equals(id)) {
                tablePropostas.getSelectionModel().select(p);
                tablePropostas.scrollTo(p);
                this.propostaIdPendenteSelecao = null;
                return;
            }
        }
    }

    public void abrirFormularioComPropostaEmMemoria(PropostaModel proposta) {
        if (formularioAtivo != null && formularioAtivo.getViewModel().isDirty()) {
            navigator.solicitarConfirmacao("⚠️ Descartar alterações?",
                    "A proposta atual tem alterações não salvas. Deseja descartá-las e abrir a nova?", "Descartar",
                    "#c62828", () -> carregarPropostaNoFormulario(proposta));
            return;
        }
        carregarPropostaNoFormulario(proposta);
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO FORMULÁRIO (MASTER-DETAIL)
    // ==========================================================================================
    private void abrirFormularioComProposta(PropostaModel proposta) {
        if (formularioAtivo != null && formularioAtivo.getViewModel().isDirty()) {
            navigator.solicitarConfirmacao("⚠️ Descartar alterações?",
                    "A proposta atual tem alterações não salvas. Deseja descartá-las e abrir a nova?", "Descartar",
                    "#c62828", () -> carregarPropostaNoFormulario(proposta));
            return;
        }
        carregarPropostaNoFormulario(proposta);
    }

    private void carregarPropostaNoFormulario(PropostaModel proposta) {
        log.trace("{} [UI] Carregando proposta no formulário. ID: {}", LOG_PREFIX, proposta.getId());
        if (formularioAtivo == null) {
            garantirFormularioCarregado();
        }
        if (formularioAtivo != null && viewFormularioAtivo != null) {
            formularioAtivo.carregarProposta(proposta);
            if (!containerFormulario.getChildren().contains(viewFormularioAtivo)) {
                containerFormulario.getChildren().setAll(viewFormularioAtivo);
            }
        }
    }

    private void garantirFormularioCarregado() {
        if (formularioAtivo == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proposta.fxml"));
                loader.setControllerFactory(context::getBean);
                viewFormularioAtivo = loader.load();
                formularioAtivo = loader.getController();
                formularioAtivo.setEsteiraController(this);
                formularioAtivo.setOnPropostaFechada(this::fecharFormulario);
            } catch (IOException e) {
                log.error("{} [SISTEMA] Falha ao carregar FXML de proposta: {}", LOG_PREFIX, e.getMessage());
            }
        }
    }

    private void fecharFormulario() {
        containerFormulario.getChildren().setAll(paneVazio);
        tablePropostas.getSelectionModel().clearSelection();
        formularioAtivo = null;
    }

    // ==========================================================================================
    // MÓDULO 10: OVERLAYS E FEEDBACKS
    // ==========================================================================================

    /**
     * Exibe o overlay de feedback específico para resultados da IA.
     */
    public void mostrarFeedback(String icone, String titulo, String htmlContent, Runnable callback) {
        log.info("{} [TELEMETRIA] Exibindo feedback de IA: {}", LOG_PREFIX, titulo);
        Platform.runLater(() -> {
            lblFeedbackIcon.setText(icone);
            lblFeedbackTitle.setText(titulo);
            WebEngine engine = webFeedback.getEngine();
            engine.loadContent(String.format(HTML_TEMPLATE_IA, htmlContent));
            this.acaoFeedbackPendente = callback;
            overlayFeedback.setVisible(true);
            overlayFeedback.toFront();
        });
    }

    @FXML private void fecharFeedback() {
        overlayFeedback.setVisible(false);
        if (acaoFeedbackPendente != null) {
            acaoFeedbackPendente.run();
            acaoFeedbackPendente = null;
        }
    }
}
