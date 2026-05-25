package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MainController implements Navigator {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String FXML_WORKSPACE = "/fxml/workspace.fxml";
    private static final String FXML_LOGIN = "/fxml/login.fxml";

    private static final double CHAT_LARGURA_MINIMA = 320.0;
    private static final double CHAT_LARGURA_MAXIMA = 750.0;
    private static final String STYLE_DRAG_HOVER = "-fx-cursor: h-resize; -fx-background-color: rgba(30, 64, 175, 0.15);";
    private static final String STYLE_DRAG_DEFAULT = "-fx-cursor: default; -fx-background-color: rgba(0, 0, 0, 0.03);";

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox topBar;
    @FXML
    private StackPane bottomBar;
    @FXML
    private VBox overlaySair;
    @FXML
    private VBox overlayLoading;
    @FXML
    private Label lblLoadingTexto;
    @FXML
    private HBox overlayChatIA;

    @FXML
    private Region dragHandleChat;
    @FXML
    private Region painelChat;
    @FXML
    private AjudaChatController painelChatController;

    @FXML
    private VBox overlayMensagem;
    @FXML
    private Label lblMsgTitulo;
    @FXML
    private Label lblMsgTexto;

    // =========================================================================
    // ESTADO E INJEÇÕES
    // =========================================================================
    private final ApplicationContext context;
    private final PropostaService propostaService;
    private final Map<String, ViewPair> cacheDeViews = new HashMap<>();

    private double mouseStartX = 0;
    private double chatStartWidth = 0;
    private String telaAtual = "";

    // Padrão moderno (Java 14+) para classes de puro transporte de dados
    private record ViewPair(Node view, Object controller) {
    }

    public MainController(ApplicationContext context, PropostaService propostaService) {
        this.context = context;
        this.propostaService = propostaService;
        log.debug("[MAIN] Construtor: Controller instanciado");
    }

    // =========================================================================
    // INICIALIZAÇÃO E UX
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[MAIN] initialize: Configurando interações da interface principal");
        configurarArrastoChat();
        configurarFechamentoChat();
        log.info("[MAIN] initialize: Configuração concluída");
    }

    private void configurarArrastoChat() {
        if (dragHandleChat == null) {
            log.warn("[MAIN] configurarArrastoChat: dragHandleChat não encontrado, recurso de arrasto desabilitado");
            return;
        }

        dragHandleChat.setOnMousePressed(event -> {
            mouseStartX = event.getSceneX();
            if (painelChat != null) {
                chatStartWidth = painelChat.getWidth();
            }
            log.trace("[MAIN] configurarArrastoChat: mouse pressionado (x={}, largura inicial={})", mouseStartX,
                    chatStartWidth);
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
                log.trace("[MAIN] configurarArrastoChat: redimensionando chat para largura={}", novaLargura);
            }
        });

        dragHandleChat.setOnMouseEntered(e -> dragHandleChat.setStyle(STYLE_DRAG_HOVER));
        dragHandleChat.setOnMouseExited(e -> dragHandleChat.setStyle(STYLE_DRAG_DEFAULT));
    }

    private void configurarFechamentoChat() {
        if (overlayChatIA == null) {
            log.warn("[MAIN] configurarFechamentoChat: overlayChatIA não encontrado");
            return;
        }

        overlayChatIA.setOnMouseClicked(event -> {
            if (event.getTarget() == overlayChatIA) {
                log.debug("[MAIN] Fechando painel IA por clique no overlay");
                alternarPainelIA();
                event.consume();
            }
        });
    }

    // 🚀 MÉTODO 2 CORRIGIDO: Executa a conversão do Rascunho para a Aba de Clientes
    // (Lead)
    public void iniciarConversaoCopiloto(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado,
            ProponenteModel cliente) {
        log.info("[MAIN] iniciarConversaoCopiloto: Convertendo rascunho para proposta - cliente='{}', banco='{}'",
                cliente != null ? cliente.getNomeCompleto() : "N/A",
                resultado != null && resultado.tabela() != null ? resultado.tabela().getBanco().getNome() : "N/A");
        try {
            PropostaModel propostaSalva = propostaService.converterRascunhoParaProposta(rascunho, resultado, cliente);
            abrirPropostaNoWorkspace(propostaSalva);

            String clienteNome = cliente != null ? cliente.getNomeCompleto() : "N/A";
            String bancoNome = resultado != null && resultado.tabela() != null && resultado.tabela().getBanco() != null
                    ? resultado.tabela().getBanco().getNome()
                    : "N/A";

            notificarSucesso("Proposta gerada com sucesso" + (cliente != null ? " para " + clienteNome : "") +
                    "\nBanco: " + bancoNome);
            log.info("[MAIN] Proposta ID={} gerada com sucesso a partir do Copiloto", propostaSalva.getId());
        } catch (Exception e) {
            log.error("[MAIN][CONVERSACOPILOtO] Erro ao gerar proposta: {}", e.getMessage(), e);
            notificarAviso("Erro ao gerar proposta: " + e.getMessage());
        }
    }

    // =========================================================================
    // MOTOR DE NAVEGAÇÃO E CACHE
    // =========================================================================
    private void executarNavegacao(String fxmlPath, boolean mostrarEstrutura) {
        log.debug("[MAIN] executarNavegacao: navegando para '{}', mostrarEstrutura={}", fxmlPath, mostrarEstrutura);
        try {
            ViewPair pair = cacheDeViews.computeIfAbsent(fxmlPath, this::carregarNovaView);
            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);
            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);
            contentArea.getChildren().setAll(pair.view());
            this.telaAtual = fxmlPath;
            log.info("[MAIN] Navegação para '{}' concluída", fxmlPath);
        } catch (Exception e) {
            log.error("[MAIN] executarNavegacao: erro fatal ao carregar '{}'", fxmlPath, e);
            throw new RuntimeException("Erro ao carregar a tela: " + fxmlPath, e);
        }
    }

    private ViewPair carregarNovaView(String fxmlPath) {
        log.debug("[MAIN] carregarNovaView: carregando FXML '{}'", fxmlPath);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Node view = loader.load();
            log.info("[MAIN] FXML '{}' carregado com sucesso, controller={}", fxmlPath, loader.getController());
            return new ViewPair(view, loader.getController());
        } catch (IOException e) {
            log.error("[MAIN] Falha ao instanciar o FXML: {}", fxmlPath, e);
            throw new RuntimeException("Falha ao instanciar o FXML: " + fxmlPath, e);
        }
    }

    // =========================================================================
    // ROTAS DO WORKSPACE
    // =========================================================================
    private void executarNoWorkspace(Consumer<WorkspaceController> acao) {
        log.trace("[MAIN] executarNoWorkspace: tentando obter referência ao workspace");
        try {
            garantirWorkspaceVisivel();
            ViewPair pair = cacheDeViews.get(FXML_WORKSPACE);
            if (pair != null && pair.controller() instanceof WorkspaceController wsController) {
                acao.accept(wsController);
            } else {
                log.warn("[MAIN] executarNoWorkspace: workspace não encontrado no cache ou controller inválido");
            }
        } catch (Exception e) {
            log.error("[MAIN][EXECUTAWS] Erro: {}", e.getMessage(), e);
        }
    }

    private void garantirWorkspaceVisivel() {
        if (!FXML_WORKSPACE.equals(this.telaAtual)) {
            log.debug("[MAIN] garantindo que workspace está visível (navegando para ele)");
            navegarPara(FXML_WORKSPACE, true);
        }
    }

    // =========================================================================
    // CONTROLES DE OVERLAY (LOADING & SAIR & IA)
    // =========================================================================
    @FXML
    private void cancelarLogout() {
        log.debug("[MAIN] cancelarLogout: usuário cancelou o logout");
        overlaySair.setVisible(false);
    }

    @FXML
    private void confirmarLogout() {
        log.info("[MAIN] confirmarLogout: usuário confirmou logout");
        overlaySair.setVisible(false);
        context.getBean(AuthService.class).logout();
        limparCacheDeTelas();
        navegarPara(FXML_LOGIN, false);
    }

    // 🚀 MÉTODO: Exibe o overlay (substitui o Alert)
    private void exibirOverlayMensagem(String titulo, String mensagem) {
        log.debug("[MAIN] exibirOverlayMensagem: titulo='{}', mensagem='{}'", titulo, mensagem);
        Platform.runLater(() -> {
            lblMsgTitulo.setText(titulo);
            lblMsgTexto.setText(mensagem);
            overlayMensagem.setVisible(true);
            overlayMensagem.toFront();
        });
    }

    // 🚀 MÉTODO: Oculta o overlay (chamado pelo botão do FXML)
    @FXML
    private void ocultarOverlayMensagem() {
        log.trace("[MAIN] ocultarOverlayMensagem: fechando overlay de mensagem");
        overlayMensagem.setVisible(false);
    }

    // =========================================================================
    // NAVIGATOR
    // =========================================================================
    @Override
    public void navegarPara(String fxmlPath, boolean mostrarEstrutura) {
        log.debug("[MAIN] navegarPara: fxmlPath='{}', mostrarEstrutura={}", fxmlPath, mostrarEstrutura);
        executarNavegacao(fxmlPath, mostrarEstrutura);
    }

    @Override
    public void abrirDashboard() {
        log.debug("[MAIN] abrirDashboard");
        executarNoWorkspace(WorkspaceController::abrirAbaDashboard);
    }

    @Override
    public void abrirPlaybook() {
        log.debug("[MAIN] abrirPlaybook");
        executarNoWorkspace(WorkspaceController::abrirAbaPlaybook);
    }

    @Override
    public void abrirClientes() {
        log.debug("[MAIN] abrirClientes");
        executarNoWorkspace(WorkspaceController::abrirAbaClientes);
    }

    @Override
    public void abrirClienteNoWorkspace(ProponenteModel proponente) {
        log.debug("[MAIN] abrirClienteNoWorkspace: proponente={}",
                proponente != null ? proponente.getNomeCompleto() : "null");
        executarNoWorkspace(ws -> ws.abrirOuFocarAba(proponente));
    }

    @Override
    public void abrirPropostaNoWorkspace(PropostaModel proposta) {
        if (proposta == null || proposta.getProponente() == null) {
            log.warn("[MAIN] abrirPropostaNoWorkspace: proposta ou proponente nulo, ignorando");
            return;
        }
        log.info("[MAIN] abrirPropostaNoWorkspace: proposta ID={} do cliente {}", proposta.getId(),
                proposta.getProponente().getNomeCompleto());
        executarNoWorkspace(ws -> ws.abrirOuFocarAbaComProposta(proposta.getProponente(), proposta.getId()));
    }

    @Override
    public void mostrarLoading(String mensagem) {
        log.debug("[MAIN] mostrarLoading: '{}'", mensagem);
        Platform.runLater(() -> {
            lblLoadingTexto.setText(mensagem);
            overlayLoading.setVisible(true);
        });
    }

    @Override
    public void ocultarLoading() {
        log.trace("[MAIN] ocultarLoading");
        Platform.runLater(() -> overlayLoading.setVisible(false));
    }

    @Override
    public void notificarSucesso(String mensagem) {
        log.info("[MAIN] notificarSucesso: {}", mensagem);
        exibirOverlayMensagem("Sucesso", mensagem);
    }

    @Override
    public void notificarAviso(String mensagem) {
        log.warn("[MAIN] notificarAviso: {}", mensagem);
        exibirOverlayMensagem("Atenção", mensagem);
    }

    @FXML
    @Override
    public void alternarPainelIA() {
        boolean estaAberto = overlayChatIA.isVisible();
        log.debug("[MAIN] alternarPainelIA: estado atual aberto={}, alternando para {}", estaAberto, !estaAberto);
        overlayChatIA.setVisible(!estaAberto);
        if (!estaAberto && painelChatController != null) {
            painelChatController.solicitarFoco();
        }
    }

    @Override
    public void irParaNovoContato() {
        log.debug("[MAIN] irParaNovoContato");
        abrirClienteNoWorkspace(null);
    }

    @Override
    public void irParaPropostas() {
        log.debug("[MAIN] irParaPropostas");
        executarNoWorkspace(ws -> ws.abrirAbaPropostas(null));
    }

    @Override
    public void irParaTabelaComissoes() {
        log.debug("[MAIN] irParaTabelaComissoes");
        executarNoWorkspace(WorkspaceController::abrirAbaComissoes);
    }

    @Override
    public void irParaTabelasJuros() {
        log.debug("[MAIN] irParaTabelasJuros");
        executarNoWorkspace(WorkspaceController::abrirAbaTabelasJuros);
    }

    @Override
    public void irParaImportadorTabelas() {
        log.debug("[MAIN] irParaImportadorTabelas");
        executarNoWorkspace(WorkspaceController::abrirAbaImportadorTabelas);
    }

    @Override
    public void irParaBancosConvenios() {
        log.debug("[MAIN] irParaBancosConvenios");
        executarNoWorkspace(WorkspaceController::abrirAbaBancosConvenios);
    }

    @Override
    public void irParaLinksUteis() {
        log.debug("[MAIN] irParaLinksUteis");
        executarNoWorkspace(WorkspaceController::abrirAbaLinks);
    }

    @Override
    public void limparCacheDeTelas() {
        log.info("[MAIN] limparCacheDeTelas: removendo {} views em cache", cacheDeViews.size());
        cacheDeViews.clear();
    }

    @Override
    public void abrirCopilotoSimulacao(Node anchorNode) {
        log.debug("[MAIN] abrirCopilotoSimulacao: abrindo aba Copiloto (anchorNode ignorado)");
        executarNoWorkspace(ws -> {
            ws.admitirAbaSimples(
                    RotaAba.COPILOTO,
                    "✨ Copiloto de Vendas",
                    "/fxml/copiloto_simulacao.fxml");
        });
    }

    @Override
    public void mostrarOverlaySair() {
        log.debug("[MAIN] mostrarOverlaySair: exibindo confirmação de logout");
        overlaySair.setVisible(true);
    }
}