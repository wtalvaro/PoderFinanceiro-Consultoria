package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.IMainFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.presentation.controller.suporte.AjudaChatController;
import br.com.poderfinanceiro.app.presentation.event.UIEvent;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.presentation.ui.navigation.enums.AppRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <h1>MainController</h1>
 * <p>
 * Controlador Mestre (Mediador). Gerencia a infraestrutura global da UI,
 * overlays e orquestração de navegação via Registry (AppRoute).
 * Implementa o padrão Observer para reagir a eventos de sistema.
 * </p>
 */
@Component
public class MainController implements Navigator {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final String LOG_PREFIX = "[MainController]";

    private static final double CHAT_LARGURA_MINIMA = 320.0;
    private static final double CHAT_LARGURA_MAXIMA = 750.0;

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
    // MÓDULO 4: ESTADO INTERNO (UI STATE)
    // ==========================================================================================
    private final Map<AppRoute, ViewPair> cacheDeViews = new HashMap<>();
    private double mouseStartX = 0;
    private double chatStartWidth = 0;
    private AppRoute rotaAtiva;
    private Runnable acaoConfirmacaoPendente;

    private record ViewPair(Node view, Object controller) {
    }

    public MainController(ApplicationContext context, IMainFacade mainFacade) {
        this.context = context;
        this.mainFacade = mainFacade;
        log.info("{} [SISTEMA] Orquestrador Mestre instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando motor de interface e listeners de arrasto.", LOG_PREFIX);
        configurarArrastoChat();
        configurarFechamentoChat();
        log.debug("{} [SISTEMA] Layout mestre pronto para navegação.", LOG_PREFIX);
    }

    private void configurarArrastoChat() {
        if (dragHandleChat == null)
            return;

        dragHandleChat.setOnMousePressed(event -> {
            mouseStartX = event.getSceneX();
            chatStartWidth = painelChat.getWidth();
        });

        dragHandleChat.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseStartX;
            double novaLargura = Math.clamp(chatStartWidth - deltaX, CHAT_LARGURA_MINIMA, CHAT_LARGURA_MAXIMA);
            painelChat.setPrefWidth(novaLargura);
            painelChat.setMinWidth(novaLargura);
        });

        dragHandleChat.setOnMouseEntered(
                e -> dragHandleChat.setStyle("-fx-cursor: h-resize; -fx-background-color: rgba(30, 64, 175, 0.15);"));
        dragHandleChat.setOnMouseExited(
                e -> dragHandleChat.setStyle("-fx-cursor: default; -fx-background-color: transparent;"));
    }

    private void configurarFechamentoChat() {
        overlayChatIA.setOnMouseClicked(event -> {
            if (event.getTarget() == overlayChatIA) {
                log.trace("{} [TELEMETRIA] Fechando painel IA por clique no overlay.", LOG_PREFIX);
                alternarPainelIA();
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 6: OBSERVER (LISTENERS DE EVENTOS SPRING)
    // ==========================================================================================
    @EventListener
    public void handleLoadingEvent(UIEvent.Loading event) {
        log.trace("{} [TELEMETRIA] Evento de Loading recebido: visivel={}", LOG_PREFIX, event.isVisivel());
        if (event.isVisivel())
            mostrarLoading(event.getMensagem());
        else
            ocultarLoading();
    }

    @EventListener
    public void handleNotificacaoEvent(UIEvent.Notificacao event) {
        log.trace("{} [TELEMETRIA] Evento de Notificação recebido: {}", LOG_PREFIX, event.getTipo());
        UIEvent.Notificacao.Tipo tipo = event.getTipo();
        if (tipo == UIEvent.Notificacao.Tipo.SUCESSO) {
            notificarSucesso(event.getMensagem());
        } else {
            notificarAviso(event.getMensagem());
        }
    }

    @EventListener
    public void handleNavegacaoEvent(UIEvent.Navegacao event) {
        log.info("{} [TELEMETRIA] Evento de Navegação recebido para: {}", LOG_PREFIX, event.getRota());
        navegarPara(event.getRota());
    }

    // ==========================================================================================
    // MÓDULO 7: MOTOR DE NAVEGAÇÃO (REGISTRY PATTERN)
    // ==========================================================================================
    @Override
    public void navegarPara(AppRoute rota) {
        if (rota == null) {
            log.warn("{} [SISTEMA] Tentativa de navegação para rota nula.", LOG_PREFIX);
            return;
        }
        if (rota.isAba()) {
            log.debug("{} [SISTEMA] Rota '{}' identificada como aba. Delegando ao Workspace.", LOG_PREFIX, rota);
            executarNoWorkspace(ws -> ws.admitirAbaSimples(
                    rota, // Passamos o objeto AppRoute diretamente
                    rota.getTitulo(),
                    rota.getFxmlPath()));
            return;
        }
        executarNavegacaoMestre(rota);
    }

    private void executarNavegacaoMestre(AppRoute rota) {
        log.info("{} [TELEMETRIA] Executando navegação mestre para: {}", LOG_PREFIX, rota);
        try {
            ViewPair pair = cacheDeViews.computeIfAbsent(rota, this::carregarView);

            Platform.runLater(() -> {
                boolean estrutura = rota.isExibirEstruturaMestre();
                topBar.setVisible(estrutura);
                topBar.setManaged(estrutura);
                bottomBar.setVisible(estrutura);
                bottomBar.setManaged(estrutura);

                contentArea.getChildren().setAll(pair.view());
                this.rotaAtiva = rota;
                log.info("{} [AUDITORIA] Navegação mestre concluída: {}", LOG_PREFIX, rota.name());
            });
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica ao navegar para {}: {}", LOG_PREFIX, rota, e.getMessage());
        }
    }

    private ViewPair carregarView(AppRoute rota) {
        try {
            log.debug("{} [SISTEMA] Carregando recurso FXML: {}", LOG_PREFIX, rota.getFxmlPath());
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rota.getFxmlPath()));
            loader.setControllerFactory(context::getBean);
            return new ViewPair(loader.load(), loader.getController());
        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro de I/O ao carregar view {}: {}", LOG_PREFIX, rota.name(), e.getMessage());
            throw new RuntimeException("Erro ao carregar FXML: " + rota.getFxmlPath(), e);
        }
    }

    private void executarNoWorkspace(Consumer<WorkspaceController> acao) {
        if (this.rotaAtiva != AppRoute.WORKSPACE) {
            log.debug("{} [SISTEMA] Workspace não ativo. Forçando transição.", LOG_PREFIX);
            navegarPara(AppRoute.WORKSPACE);
        }

        ViewPair pair = cacheDeViews.get(AppRoute.WORKSPACE);
        if (pair != null && pair.controller() instanceof WorkspaceController ws) {
            acao.accept(ws);
        } else {
            log.error("{} [SISTEMA] Falha ao recuperar controlador do Workspace.", LOG_PREFIX);
        }
    }

    // ==========================================================================================
    // MÓDULO 8: OVERLAYS E FEEDBACK (IMPLEMENTAÇÃO NAVIGATOR)
    // ==========================================================================================
    @Override
    public void mostrarLoading(String mensagem) {
        Platform.runLater(() -> {
            lblLoadingTexto.setText(ValidationUtils.isPreenchido(mensagem) ? mensagem : "Processando...");
            overlayLoading.setVisible(true);
            overlayLoading.toFront();
        });
    }

    @Override
    public void ocultarLoading() {
        Platform.runLater(() -> overlayLoading.setVisible(false));
    }

    @Override
    public void notificarSucesso(String msg) {
        exibirOverlayMensagem("✅ Sucesso", msg, true);
    }

    @Override
    public void notificarAviso(String msg) {
        exibirOverlayMensagem("⚠️ Atenção", msg, false);
    }

    private void exibirOverlayMensagem(String titulo, String msg, boolean sucesso) {
        Platform.runLater(() -> {
            lblMsgTitulo.setText(titulo);
            lblMsgTexto.setText(msg);
            lblMsgTitulo.setStyle(
                    sucesso ? "-fx-text-fill: -color-success-emphasis;" : "-fx-text-fill: -color-danger-emphasis;");
            overlayMensagem.setVisible(true);
            overlayMensagem.toFront();
            log.debug("{} [SISTEMA] Overlay de mensagem exibido: {}", LOG_PREFIX, titulo);
        });
    }

    @FXML
    public void ocultarOverlayMensagem() {
        overlayMensagem.setVisible(false);
    }

    @Override
    public void solicitarConfirmacao(String titulo, String msg, String txtBtn, String cor, Runnable acao) {
        Platform.runLater(() -> {
            lblConfirmacaoTitulo.setText(titulo);
            lblConfirmacaoTexto.setText(msg);
            btnConfirmarGlobal.setText(txtBtn);
            btnConfirmarGlobal
                    .setStyle("-fx-background-color: " + cor + "; -fx-text-fill: white; -fx-font-weight: bold;");
            this.acaoConfirmacaoPendente = acao;
            overlayConfirmacaoGlobal.setVisible(true);
            overlayConfirmacaoGlobal.toFront();
            log.info("{} [SISTEMA] Diálogo de confirmação solicitado: {}", LOG_PREFIX, titulo);
        });
    }

    @FXML
    public void executarConfirmacaoGlobal() {
        overlayConfirmacaoGlobal.setVisible(false);
        if (acaoConfirmacaoPendente != null) {
            log.info("{} [NEGOCIO] Executando ação confirmada pelo usuário.", LOG_PREFIX);
            acaoConfirmacaoPendente.run();
            acaoConfirmacaoPendente = null;
        }
    }

    @FXML
    public void cancelarConfirmacaoGlobal() {
        overlayConfirmacaoGlobal.setVisible(false);
        acaoConfirmacaoPendente = null;
    }

    // ==========================================================================================
    // MÓDULO 9: LÓGICA DE NEGÓCIO E IA (CONCORRÊNCIA MODERNA)
    // ==========================================================================================
    public void iniciarConversaoCopiloto(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado,
            ProponenteModel cliente) {
        log.info("{} [TELEMETRIA] Iniciando orquestração de conversão via Copiloto.", LOG_PREFIX);
        mostrarLoading("IA gerando proposta...");

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade para conversão de rascunho.", LOG_PREFIX);
                    return mainFacade.converterRascunhoParaProposta(rascunho, resultado, cliente);
                },
                proposta -> {
                    ocultarLoading();
                    log.info("{} [AUDITORIA] Proposta convertida com sucesso pela IA.", LOG_PREFIX);
                    executarNoWorkspace(ws -> ws.abrirOuFocarAbaComPropostaEmMemoria(proposta));
                },
                erro -> {
                    ocultarLoading();
                    log.error("{} [AUDITORIA] Falha na conversão da IA: {}", LOG_PREFIX, erro.getMessage());
                    notificarAviso("Erro na IA: " + erro.getMessage());
                });
    }

    @FXML
    @Override
    public void alternarPainelIA() {
        boolean visivel = !overlayChatIA.isVisible();
        log.info("{} [TELEMETRIA] Alternando visibilidade do painel IA para: {}", LOG_PREFIX, visivel);
        overlayChatIA.setVisible(visivel);
        if (visivel && painelChatController != null) {
            painelChatController.solicitarFoco();
        }
    }

    @FXML
    public void confirmarLogout() {
        log.info("{} [AUDITORIA] Logout solicitado e confirmado pelo usuário.", LOG_PREFIX);
        mainFacade.realizarLogout();
        limparCacheDeTelas();
        navegarPara(AppRoute.LOGIN);
        overlaySair.setVisible(false);
    }

    @FXML
    public void mostrarOverlaySair() {
        overlaySair.setVisible(true);
        overlaySair.toFront();
    }

    @FXML
    public void cancelarLogout() {
        overlaySair.setVisible(false);
    }

    @Override
    public void abrirClienteNoWorkspace(ProponenteModel p) {
        log.debug("{} [NEGOCIO] Abrindo cliente no Workspace.", LOG_PREFIX);
        executarNoWorkspace(ws -> ws.abrirOuFocarAba(p));
    }

    @Override
    public void abrirPropostaNoWorkspace(PropostaModel p) {
        if (p != null && p.getProponente() != null) {
            log.debug("{} [NEGOCIO] Abrindo proposta ID {} no Workspace.", LOG_PREFIX, p.getId());
            executarNoWorkspace(ws -> ws.abrirOuFocarAbaComProposta(p.getProponente(), p.getId()));
        }
    }

    @Override
    public void abrirCopilotoSimulacao(Node anchor) {
        log.info("{} [TELEMETRIA] Abrindo Copiloto de Simulação.", LOG_PREFIX);
        navegarPara(AppRoute.COPILOTO);
    }

    @Override
    public void limparCacheDeTelas() {
        log.info("{} [SISTEMA] Limpando cache de visualização.", LOG_PREFIX);
        cacheDeViews.clear();
    }
}
