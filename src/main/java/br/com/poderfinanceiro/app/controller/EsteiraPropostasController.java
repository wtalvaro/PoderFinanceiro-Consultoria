package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.event.PropostaUIEventHub;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.facade.IEsteiraPropostasFacade;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * <h1>EsteiraPropostasController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar a Esteira de
 * Propostas. Implementa o padrão <b>Humble Object</b>, delegando a
 * persistência, filtros e formatações para a {@link IEsteiraPropostasFacade}.
 * </p>
 */
@Component @Scope("prototype")
public class EsteiraPropostasController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(EsteiraPropostasController.class);
    private static final String LOG_PREFIX = "[EsteiraPropostasController]";

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String STYLE_BTN_BASE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 6;";
    private static final String STYLE_BTN_SUCCESS = "-fx-background-color: #2e7d32; " + STYLE_BTN_BASE;
    private static final String STYLE_BTN_DANGER = "-fx-background-color: #c62828; " + STYLE_BTN_BASE;
    private static final String STYLE_BTN_WARNING = "-fx-background-color: #f57c00; " + STYLE_BTN_BASE;

    private static final String STYLE_LBL_CONFIRM_TITULO = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;";
    private static final String STYLE_BTN_CONFIRM = "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;";

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

    @FXML private VBox overlayConfirmacao, overlayFeedback, overlayAlertaSimples;
    @FXML private Label lblConfirmacaoTitulo, lblConfirmacaoTexto;
    @FXML private Button btnConfirmarAcao;

    @FXML private Label lblFeedbackIcon, lblFeedbackTitle;
    @FXML private WebView webFeedback;
    @FXML private Button btnFeedbackAction;

    @FXML private Label lblAlertaIcone, lblAlertaTitulo, lblAlertaMensagem;
    @FXML private Button btnAlertaAcao;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<PropostaModel> listaPropostas = FXCollections.observableArrayList();
    private Runnable acaoConfirmacaoPendente;
    private Runnable acaoFeedbackPendente;
    private Runnable acaoAlertaPendente;
    private PropostaController formularioAtivo;
    private Node viewFormularioAtivo;
    private Long propostaIdPendenteSelecao = null;

    public EsteiraPropostasController(IEsteiraPropostasFacade esteiraFacade, ApplicationContext context, PropostaUIEventHub eventHub,
            ProponenteUIEventHub proponenteEventHub) {
        this.esteiraFacade = esteiraFacade;
        this.context = context;
        this.eventHub = eventHub;
        this.proponenteEventHub = proponenteEventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
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
        log.trace("{} [UI] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("{} [UI] Termo de busca alterado: '{}'", LOG_PREFIX, newVal);
            filtrarPropostas(newVal);
        });
    }

    private void filtrarPropostas(String termo) {
        AsyncUtils.executarTaskAsync(() -> esteiraFacade.filtrarPropostas(termo), propostasFiltradas -> {
            listaPropostas.setAll(propostasFiltradas);
            tablePropostas.setItems(listaPropostas);
            log.info("{} [TELEMETRIA] Tabela atualizada. {} registro(s) exibido(s).", LOG_PREFIX, propostasFiltradas.size());

            // CORREÇÃO: Se havia uma proposta aguardando para ser selecionada,
            // seleciona agora
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

        colCliente.setCellValueFactory(cell -> {
            String nome = cell.getValue().getProponente() != null ? cell.getValue().getProponente().getNomeCompleto() : "Sem Cliente";
            return new SimpleStringProperty(nome);
        });

        colBanco.setCellValueFactory(cell -> {
            String banco = cell.getValue().getBanco() != null ? cell.getValue().getBanco().getNome() : "-";
            return new SimpleStringProperty(banco);
        });

        colValorSol.setCellValueFactory(cell -> {
            BigDecimal valor = cell.getValue().getValorSolicitado();
            return new SimpleStringProperty(valor != null ? FinanceiroUtils.formatarParaExibicao(valor) : "0,00");
        });

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
                    switch (item) {
                    case "Aprovada", "Pago" -> setTextFill(Color.GREEN);
                    case "Reprovada", "Cancelado" -> setTextFill(Color.RED);
                    case "Pendente", "Aguardando Documentação" -> setTextFill(Color.ORANGE);
                    default -> setTextFill(Color.BLACK);
                    }
                }
            }
        });

        tablePropostas.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                log.info("{} [TELEMETRIA] Proposta selecionada na tabela. ID: {}", LOG_PREFIX, newSelection.getId());
                abrirFormularioComProposta(newSelection);
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 8: AÇÕES DE NEGÓCIO (CRUD)
    // ==========================================================================================
    @FXML private void criarNovaProposta() {
        log.info("{} [TELEMETRIA] Usuário solicitou criação de nova proposta.", LOG_PREFIX);

        AsyncUtils.executarTaskAsync(esteiraFacade::criarNovaPropostaEmBranco, novaProposta -> {
            log.info("{} [UI] Formulário aberto para nova proposta.", LOG_PREFIX);
            // Removemos o recarregarDados() daqui, pois a proposta ainda não
            // está no banco
            abrirFormularioComProposta(novaProposta);
        }, erro -> {
            log.error("{} [SISTEMA] Falha ao preparar nova proposta: {}", LOG_PREFIX, erro.getMessage());
            mostrarFeedback("❌", "Erro", "Não foi possível preparar a proposta: " + erro.getMessage(), null);
        });
    }

    public void selecionarPropostaPorId(Long id) {
        log.trace("{} [UI] Tentando selecionar proposta por ID na tabela: {}", LOG_PREFIX, id);

        if (listaPropostas.isEmpty()) {
            log.debug("{} [SISTEMA] Tabela ainda vazia. Guardando ID {} para seleção posterior.", LOG_PREFIX, id);
            this.propostaIdPendenteSelecao = id;
            return;
        }

        for (PropostaModel p : tablePropostas.getItems()) {
            if (p.getId().equals(id)) {
                tablePropostas.getSelectionModel().select(p);
                tablePropostas.scrollTo(p);
                this.propostaIdPendenteSelecao = null; // Limpa a pendência
                return;
            }
        }
        log.warn("{} [NEGOCIO] Proposta ID {} não encontrada na tabela atual.", LOG_PREFIX, id);
    }

    public void abrirFormularioComPropostaEmMemoria(PropostaModel proposta) {
        log.trace("{} [UI] Abrindo formulário com proposta gerada em memória.", LOG_PREFIX);
        if (formularioAtivo != null && formularioAtivo.getViewModel().isDirty()) {
            solicitarConfirmacao("⚠️ Descartar alterações?",
                    "A proposta atual tem alterações não salvas. Deseja descartá-las e abrir a nova?", "Descartar", "#c62828",
                    () -> carregarPropostaNoFormulario(proposta), null);
            return;
        }
        carregarPropostaNoFormulario(proposta);
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO FORMULÁRIO (MASTER-DETAIL)
    // ==========================================================================================
    private void abrirFormularioComProposta(PropostaModel proposta) {
        log.trace("{} [UI] Abrindo formulário para a proposta ID: {}", LOG_PREFIX, proposta.getId());
        if (formularioAtivo != null) {
            if (formularioAtivo.getViewModel().isDirty()) {
                log.debug("{} [TELEMETRIA] Formulário atual possui alterações não salvas. Solicitando confirmação.", LOG_PREFIX);
                solicitarConfirmacao("⚠️ Descartar alterações?",
                        "A proposta atual tem alterações não salvas. Deseja descartá-las e abrir a nova?", "Descartar", "#c62828",
                        () -> carregarPropostaNoFormulario(proposta), null);
                return;
            }
        }
        carregarPropostaNoFormulario(proposta);
    }

    private void carregarPropostaNoFormulario(PropostaModel proposta) {
        log.trace("{} [UI] Carregando proposta no formulário ativo.", LOG_PREFIX);
        if (formularioAtivo == null) {
            garantirFormularioCarregado();
        }
        if (formularioAtivo != null && viewFormularioAtivo != null) {
            formularioAtivo.carregarProposta(proposta);

            // Injeta a view diretamente no container
            if (!containerFormulario.getChildren().contains(viewFormularioAtivo)) {
                containerFormulario.getChildren().setAll(viewFormularioAtivo);
            }
        }
    }

    private void garantirFormularioCarregado() {
        if (formularioAtivo == null) {
            log.trace("{} [SISTEMA] Carregando FXML do formulário de proposta.", LOG_PREFIX);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proposta.fxml"));
                loader.setControllerFactory(context::getBean);

                // Guarda a view carregada na variável da classe
                viewFormularioAtivo = loader.load();

                formularioAtivo = loader.getController();
                formularioAtivo.setEsteiraController(this);
                formularioAtivo.setOnPropostaFechada(this::fecharFormulario);

                // REMOVA O LISTENER QUE ESTAVA AQUI!

            } catch (IOException e) {
                log.error("{} [SISTEMA] Falha ao carregar formulário de proposta: {}", LOG_PREFIX, e.getMessage(), e);
            }
        }
    }

    private void fecharFormulario() {
        log.trace("{} [UI] Fechando formulário e voltando para o painel vazio.", LOG_PREFIX);
        containerFormulario.getChildren().setAll(paneVazio);
        tablePropostas.getSelectionModel().clearSelection();
        formularioAtivo = null;
    }

    // ==========================================================================================
    // MÓDULO 10: OVERLAYS E FEEDBACKS
    // ==========================================================================================
    public void solicitarConfirmacao(String titulo, String mensagem, String textoBotaoConfirmar, String corHex, Runnable acaoConfirmar,
            Runnable acaoCancelar) {
        log.trace("{} [UI] Exibindo overlay de confirmação: {}", LOG_PREFIX, titulo);
        Platform.runLater(() -> {
            lblConfirmacaoTitulo.setText(titulo);
            lblConfirmacaoTitulo.setStyle(String.format(STYLE_LBL_CONFIRM_TITULO, corHex));
            lblConfirmacaoTexto.setText(mensagem);
            btnConfirmarAcao.setText(textoBotaoConfirmar);
            btnConfirmarAcao.setStyle(String.format(STYLE_BTN_CONFIRM, corHex));
            this.acaoConfirmacaoPendente = acaoConfirmar;
            overlayConfirmacao.setVisible(true);
        });
    }

    @FXML private void confirmarAcao() {
        log.trace("{} [UI] Ação confirmada pelo usuário.", LOG_PREFIX);
        overlayConfirmacao.setVisible(false);
        if (acaoConfirmacaoPendente != null) {
            acaoConfirmacaoPendente.run();
            acaoConfirmacaoPendente = null;
        }
    }

    @FXML private void cancelarAcaoBase() {
        log.trace("{} [UI] Ação cancelada pelo usuário.", LOG_PREFIX);
        overlayConfirmacao.setVisible(false);
        acaoConfirmacaoPendente = null;
    }

    public void mostrarFeedback(String icone, String titulo, String htmlContent, Runnable callback) {
        log.trace("{} [UI] Exibindo overlay de feedback (IA/WebView): {}", LOG_PREFIX, titulo);
        Platform.runLater(() -> {
            lblFeedbackIcon.setText(icone);
            lblFeedbackTitle.setText(titulo);
            WebEngine engine = webFeedback.getEngine();
            engine.loadContent(String.format(HTML_TEMPLATE_IA, htmlContent));
            this.acaoFeedbackPendente = callback;
            overlayFeedback.setVisible(true);
        });
    }

    @FXML private void fecharFeedback() {
        log.trace("{} [UI] Fechando overlay de feedback.", LOG_PREFIX);
        overlayFeedback.setVisible(false);
        if (acaoFeedbackPendente != null) {
            acaoFeedbackPendente.run();
            acaoFeedbackPendente = null;
        }
    }

    public void mostrarAlertaSimples(String icone, String titulo, String mensagem, String tipoBotao, Runnable callback) {
        log.trace("{} [UI] Exibindo alerta simples: {}", LOG_PREFIX, titulo);
        Platform.runLater(() -> {
            lblAlertaIcone.setText(icone);
            lblAlertaTitulo.setText(titulo);
            lblAlertaMensagem.setText(mensagem);

            switch (tipoBotao.toLowerCase()) {
            case "success" -> btnAlertaAcao.setStyle(STYLE_BTN_SUCCESS);
            case "danger" -> btnAlertaAcao.setStyle(STYLE_BTN_DANGER);
            case "warning" -> btnAlertaAcao.setStyle(STYLE_BTN_WARNING);
            default -> btnAlertaAcao.setStyle(STYLE_BTN_BASE + "-fx-background-color: #1976d2;");
            }

            this.acaoAlertaPendente = callback;
            overlayAlertaSimples.setVisible(true);
        });
    }

    @FXML private void fecharAlertaSimples() {
        log.trace("{} [UI] Fechando alerta simples.", LOG_PREFIX);
        overlayAlertaSimples.setVisible(false);
        if (acaoAlertaPendente != null) {
            acaoAlertaPendente.run();
            acaoAlertaPendente = null;
        }
    }
}
