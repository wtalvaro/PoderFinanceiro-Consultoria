package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.IMainFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;
import br.com.poderfinanceiro.app.presentation.controller.suporte.AjudaChatController;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <h1>MainController</h1>
 * <p>
 * Controlador de Interface (UI) raiz da aplicação. Implementa o
 * {@link Navigator}.
 * Gerencia a navegação entre telas, o motor de abas (Workspace) e provê a API
 * centralizada de overlays para toda a aplicação.
 * </p>
 */
@Component
public class MainController implements Navigator {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final String LOG_PREFIX = "[MainController]";

    private static final String FXML_WORKSPACE = "/fxml/workspace.fxml";
    private static final String FXML_LOGIN = "/fxml/login.fxml";

    private static final double CHAT_LARGURA_MINIMA = 320.0;
    private static final double CHAT_LARGURA_MAXIMA = 750.0;
    private static final String STYLE_DRAG_HOVER = "-fx-cursor: h-resize; -fx-background-color: rgba(30, 64, 175, 0.15);";
    private static final String STYLE_DRAG_DEFAULT = "-fx-cursor: default; -fx-background-color: rgba(0, 0, 0, 0.03);";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ApplicationContext context;
    private final IMainFacade mainFacade;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private StackPane rootStackPane, contentArea, bottomBar;
    @FXML
    private VBox topBar, overlaySair, overlayLoading, overlayMensagem, overlayConfirmacaoGlobal;
    @FXML
    private HBox overlayChatIA;
    @FXML
    private Region dragHandleChat, painelChat;
    @FXML
    private AjudaChatController painelChatController;
    @FXML
    private Label lblLoadingTexto, lblMsgTitulo, lblMsgTexto, lblConfirmacaoTitulo, lblConfirmacaoTexto;
    @FXML
    private Button btnConfirmarGlobal;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final Map<String, ViewPair> cacheDeViews = new HashMap<>();
    private double mouseStartX = 0;
    private double chatStartWidth = 0;
    private String telaAtual = "";
    private Runnable acaoConfirmacaoPendente;

    private record ViewPair(Node view, Object controller) {
    }

    public MainController(ApplicationContext context, IMainFacade mainFacade) {
        this.context = context;
        this.mainFacade = mainFacade;
        log.info("{} [SISTEMA] Controlador Mestre instanciado com suporte a Virtual Threads e ValidationUtils.",
                LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E UX
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando layout mestre da aplicação.", LOG_PREFIX);
        configurarArrastoChat();
        configurarFechamentoChat();
        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    private void configurarArrastoChat() {
        if (dragHandleChat == null)
            return;

        dragHandleChat.setOnMousePressed(event -> {
            mouseStartX = event.getSceneX();
            if (painelChat != null)
                chatStartWidth = painelChat.getWidth();
        });

        dragHandleChat.setOnMouseDragged(event -> {
            if (painelChat == null)
                return;
            double deltaX = event.getSceneX() - mouseStartX;
            double novaLargura = chatStartWidth - deltaX;

            if (novaLargura >= CHAT_LARGURA_MINIMA && novaLargura <= CHAT_LARGURA_MAXIMA) {
                painelChat.setPrefWidth(novaLargura);
                painelChat.setMinWidth(novaLargura);
                painelChat.setMaxWidth(novaLargura);
            }
        });

        dragHandleChat.setOnMouseEntered(e -> dragHandleChat.setStyle(STYLE_DRAG_HOVER));
        dragHandleChat.setOnMouseExited(e -> dragHandleChat.setStyle(STYLE_DRAG_DEFAULT));
    }

    private void configurarFechamentoChat() {
        if (overlayChatIA == null)
            return;
        overlayChatIA.setOnMouseClicked(event -> {
            if (event.getTarget() == overlayChatIA) {
                log.trace("{} [SISTEMA] Fechando painel IA por clique no overlay.", LOG_PREFIX);
                alternarPainelIA();
                event.consume();
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 6: REGRAS DE NEGÓCIO GLOBAIS (COPILOTO)
    // ==========================================================================================
    public void iniciarConversaoCopiloto(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado,
            ProponenteModel cliente) {
        log.info("{} [TELEMETRIA] Iniciando conversão de rascunho via Copiloto.", LOG_PREFIX);
        mostrarLoading("Gerando proposta...");

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade para conversão de rascunho em proposta.", LOG_PREFIX);
                    return mainFacade.converterRascunhoParaProposta(rascunho, resultado, cliente);
                },
                propostaEmMemoria -> {
                    ocultarLoading();
                    log.info("{} [AUDITORIA] Proposta gerada com sucesso via Copiloto.", LOG_PREFIX);
                    executarNoWorkspace(ws -> ws.abrirOuFocarAbaComPropostaEmMemoria(propostaEmMemoria));
                },
                erro -> {
                    ocultarLoading();
                    log.error("{} [AUDITORIA] Falha crítica na conversão do Copiloto: {}", LOG_PREFIX,
                            erro.getMessage());
                    notificarAviso("Erro ao gerar proposta: " + erro.getMessage());
                });
    }

    // ==========================================================================================
    // MÓDULO 7: MOTOR DE NAVEGAÇÃO E CACHE
    // ==========================================================================================
    private void executarNavegacao(String fxmlPath, boolean mostrarEstrutura) {
        // Uso do ValidationUtils para garantir integridade da rota
        if (!ValidationUtils.isPreenchido(fxmlPath)) {
            log.error("{} [SISTEMA] Tentativa de navegação para rota nula ou vazia.", LOG_PREFIX);
            return;
        }

        log.info("{} [TELEMETRIA] Solicitando navegação para: {}", LOG_PREFIX, fxmlPath);
        try {
            ViewPair pair = cacheDeViews.computeIfAbsent(fxmlPath, this::carregarNovaView);
            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);
            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);
            contentArea.getChildren().setAll(pair.view());
            this.telaAtual = fxmlPath;
            log.info("{} [TELEMETRIA] Navegação concluída com sucesso: {}", LOG_PREFIX, fxmlPath);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro fatal ao processar navegação para '{}': {}", LOG_PREFIX, fxmlPath,
                    e.getMessage());
            throw new RuntimeException("Erro de navegação", e);
        }
    }

    private ViewPair carregarNovaView(String fxmlPath) {
        log.debug("{} [SISTEMA] Carregando FXML e instanciando controlador: {}", LOG_PREFIX, fxmlPath);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            return new ViewPair(loader.load(), loader.getController());
        } catch (IOException e) {
            log.error("{} [SISTEMA] Falha crítica no carregamento do recurso FXML: {}", LOG_PREFIX, fxmlPath);
            throw new RuntimeException(e);
        }
    }

    private void executarNoWorkspace(Consumer<WorkspaceController> acao) {
        try {
            garantirWorkspaceVisivel();
            ViewPair pair = cacheDeViews.get(FXML_WORKSPACE);
            if (pair != null && pair.controller() instanceof WorkspaceController wsController) {
                acao.accept(wsController);
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro na orquestração de comandos do Workspace: {}", LOG_PREFIX, e.getMessage());
        }
    }

    private void garantirWorkspaceVisivel() {
        if (!FXML_WORKSPACE.equals(this.telaAtual)) {
            log.debug("{} [SISTEMA] Workspace não está visível. Forçando navegação.", LOG_PREFIX);
            navegarPara(FXML_WORKSPACE, true);
        }
    }

    // ==========================================================================================
    // MÓDULO 8: CONTROLES DE OVERLAY (LOADING, SAIR, MENSAGENS)
    // ==========================================================================================
    @FXML
    private void cancelarLogout() {
        overlaySair.setVisible(false);
    }

    @FXML
    private void confirmarLogout() {
        log.info("{} [AUDITORIA] Logout efetuado pelo usuário. Limpando sessão.", LOG_PREFIX);
        overlaySair.setVisible(false);
        mainFacade.realizarLogout();
        limparCacheDeTelas();
        navegarPara(FXML_LOGIN, false);
    }

    @FXML
    private void cancelarConfirmacaoGlobal() {
        overlayConfirmacaoGlobal.setVisible(false);
        acaoConfirmacaoPendente = null;
    }

    @FXML
    private void executarConfirmacaoGlobal() {
        overlayConfirmacaoGlobal.setVisible(false);
        if (acaoConfirmacaoPendente != null) {
            log.info("{} [SISTEMA] Executando ação confirmada pelo usuário.", LOG_PREFIX);
            acaoConfirmacaoPendente.run();
            acaoConfirmacaoPendente = null;
        }
    }

    private void exibirOverlayMensagemInterno(String titulo, String mensagem, boolean isSucesso) {
        // Uso do ValidationUtils para evitar overlays vazios
        String msgFinal = ValidationUtils.isPreenchido(mensagem) ? mensagem : "Operação concluída.";

        Platform.runLater(() -> {
            lblMsgTitulo.setText(titulo);
            lblMsgTexto.setText(msgFinal);
            lblMsgTitulo.setAlignment(Pos.CENTER);
            lblMsgTexto.setAlignment(Pos.CENTER);

            lblMsgTitulo.setStyle(
                    isSucesso ? "-fx-text-fill: -color-success-emphasis;" : "-fx-text-fill: -color-danger-emphasis;");

            overlayMensagem.setVisible(true);
            overlayMensagem.toFront();
            log.debug("{} [SISTEMA] Overlay de mensagem exibido: {}", LOG_PREFIX, titulo);
        });
    }

    @FXML
    private void ocultarOverlayMensagem() {
        overlayMensagem.setVisible(false);
    }

    // ==========================================================================================
    // MÓDULO 9: IMPLEMENTAÇÃO DO NAVIGATOR (API PÚBLICA DE UI)
    // ==========================================================================================
    @Override
    public void navegarPara(String fxmlPath, boolean mostrarEstrutura) {
        executarNavegacao(fxmlPath, mostrarEstrutura);
    }

    @Override
    public void abrirDashboard() {
        executarNoWorkspace(WorkspaceController::abrirAbaDashboard);
    }

    @Override
    public void abrirPlaybook() {
        executarNoWorkspace(WorkspaceController::abrirAbaPlaybook);
    }

    @Override
    public void abrirClientes() {
        executarNoWorkspace(WorkspaceController::abrirAbaClientes);
    }

    @Override
    public void abrirClienteNoWorkspace(ProponenteModel proponente) {
        executarNoWorkspace(ws -> ws.abrirOuFocarAba(proponente));
    }

    @Override
    public void abrirPropostaNoWorkspace(PropostaModel proposta) {
        if (proposta != null && proposta.getProponente() != null) {
            executarNoWorkspace(ws -> ws.abrirOuFocarAbaComProposta(proposta.getProponente(), proposta.getId()));
        }
    }

    @Override
    public void mostrarLoading(String mensagem) {
        String msgLoading = ValidationUtils.isPreenchido(mensagem) ? mensagem : "Processando...";
        Platform.runLater(() -> {
            lblLoadingTexto.setText(msgLoading);
            overlayLoading.setVisible(true);
            overlayLoading.toFront();
        });
    }

    @Override
    public void ocultarLoading() {
        Platform.runLater(() -> overlayLoading.setVisible(false));
    }

    @Override
    public void notificarSucesso(String mensagem) {
        exibirOverlayMensagemInterno("✅ Sucesso", mensagem, true);
    }

    @Override
    public void notificarAviso(String mensagem) {
        exibirOverlayMensagemInterno("⚠️ Atenção", mensagem, false);
    }

    @FXML
    @Override
    public void alternarPainelIA() {
        boolean estaAberto = overlayChatIA.isVisible();
        log.info("{} [TELEMETRIA] Alternando visibilidade do painel IA. Novo estado: {}", LOG_PREFIX, !estaAberto);
        overlayChatIA.setVisible(!estaAberto);
        if (!estaAberto && painelChatController != null) {
            painelChatController.solicitarFoco();
        }
    }

    @Override
    public void irParaNovoContato() {
        abrirClienteNoWorkspace(null);
    }

    @Override
    public void irParaPropostas() {
        executarNoWorkspace(ws -> ws.abrirAbaPropostas(null));
    }

    @Override
    public void irParaTabelaComissoes() {
        executarNoWorkspace(WorkspaceController::abrirAbaComissoes);
    }

    @Override
    public void irParaTabelasJuros() {
        executarNoWorkspace(WorkspaceController::abrirAbaTabelasJuros);
    }

    @Override
    public void irParaImportadorTabelas() {
        executarNoWorkspace(WorkspaceController::abrirAbaImportadorTabelas);
    }

    @Override
    public void irParaBancosConvenios() {
        executarNoWorkspace(WorkspaceController::abrirAbaBancosConvenios);
    }

    @Override
    public void irParaLinksUteis() {
        executarNoWorkspace(WorkspaceController::abrirAbaLinks);
    }

    @Override
    public void limparCacheDeTelas() {
        log.info("{} [SISTEMA] Limpando cache de visualização para liberação de memória.", LOG_PREFIX);
        cacheDeViews.clear();
    }

    @Override
    public void abrirCopilotoSimulacao(Node anchorNode) {
        log.info("{} [TELEMETRIA] Solicitando abertura do Copiloto de Simulação.", LOG_PREFIX);
        executarNoWorkspace(
                ws -> ws.admitirAbaSimples(RotaAba.COPILOTO, "✨ Copiloto de Vendas", "/fxml/copiloto_simulacao.fxml"));
    }

    @Override
    public void mostrarOverlaySair() {
        overlaySair.setVisible(true);
        overlaySair.toFront();
    }

    @Override
    public void solicitarConfirmacao(String titulo, String mensagem, String textoBotao, String estiloCor,
            Runnable acao) {
        if (!ValidationUtils.isPreenchido(titulo) || acao == null) {
            log.warn("{} [SISTEMA] Tentativa de exibir confirmação com dados incompletos.", LOG_PREFIX);
            return;
        }

        Platform.runLater(() -> {
            lblConfirmacaoTitulo.setText(titulo);
            lblConfirmacaoTexto.setText(mensagem);
            btnConfirmarGlobal.setText(textoBotao);

            String background = estiloCor.startsWith("-color") ? estiloCor : estiloCor;
            btnConfirmarGlobal
                    .setStyle("-fx-background-color: " + background + "; -fx-text-fill: white; -fx-font-weight: bold;");

            this.acaoConfirmacaoPendente = acao;
            overlayConfirmacaoGlobal.setVisible(true);
            overlayConfirmacaoGlobal.toFront();
            log.info("{} [SISTEMA] Diálogo de confirmação global exibido: {}", LOG_PREFIX, titulo);
        });
    }
}
