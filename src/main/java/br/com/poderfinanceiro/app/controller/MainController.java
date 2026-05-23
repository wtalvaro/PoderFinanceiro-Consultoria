package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;
import javafx.application.HostServices;
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

@Component
public class MainController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String FXML_WORKSPACE = "/fxml/workspace.fxml";
    private static final String FXML_LOGIN = "/fxml/login.fxml";

    private static final double CHAT_LARGURA_MINIMA = 320.0;
    private static final double CHAT_LARGURA_MAXIMA = 750.0;
    private static final String STYLE_DRAG_HOVER = "-fx-cursor: h-resize; -fx-background-color: rgba(30, 64, 175, 0.15);";
    private static final String STYLE_DRAG_DEFAULT = "-fx-cursor: default; -fx-background-color: rgba(0, 0, 0, 0.03);";

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
    private final HostServices hostServices;
    private final ApplicationContext context;
    private final PropostaService propostaService;
    private final Map<String, ViewPair> cacheDeViews = new HashMap<>();

    private double mouseStartX = 0;
    private double chatStartWidth = 0;
    private String telaAtual = "";

    // Padrão moderno (Java 14+) para classes de puro transporte de dados
    private record ViewPair(Node view, Object controller) {
    }

    public MainController(ApplicationContext context, HostServices hostServices, PropostaService propostaService) {
        this.context = context;
        this.hostServices = hostServices;
        this.propostaService = propostaService;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    // =========================================================================
    // INICIALIZAÇÃO E UX
    // =========================================================================
    @FXML
    public void initialize() {
        configurarArrastoChat();
        configurarFechamentoChat();
    }

    private void configurarArrastoChat() {
        if (dragHandleChat == null)
            return;

        dragHandleChat.setOnMousePressed(event -> {
            mouseStartX = event.getSceneX();
            if (painelChat != null) {
                chatStartWidth = painelChat.getWidth();
            }
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
                alternarPainelIA();
                event.consume();
            }
        });
    }

    public void abrirCopilotoSimulacao(Node anchorNode) {
        // O anchorNode não é mais necessário pois não usaremos PopOver
        executarNoWorkspace(ws -> {
            ws.admitirAbaSimples(
                    RotaAba.COPILOTO, // 🚀 CORREÇÃO: Substituído a String pelo Enum
                    "✨ Copiloto de Vendas",
                    "/fxml/copiloto_simulacao.fxml");
        });
    }

    // 🚀 MÉTODO 2 CORRIGIDO: Executa a conversão do Rascunho para a Aba de Clientes
    // (Lead)
    public void iniciarConversaoCopiloto(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado, ProponenteModel cliente) {
        try {
            // Criação da Nova Proposta
            PropostaModel novaProposta = new PropostaModel();
            
            // Vinculação Obrigatória
            novaProposta.setProponente(cliente);
            novaProposta.setBanco(resultado.tabela().getBanco());
            novaProposta.setTabelaId(resultado.tabela().getId());
            
            // Dados da Simulação
            novaProposta.setValorSolicitado(rascunho.valorDesejado());
            novaProposta.setPrazoDesejado(rascunho.prazoDesejado());
            novaProposta.setConvenioOrgao(TipoConvenioModel.valueOf(rascunho.tipoConvenio()));
            
            // Status Inicial
            novaProposta.setStatus(StatusPropostaModel.DIGITADA);
            novaProposta.setObservacoes("✨ Proposta originada automaticamente via Copiloto de Vendas.");
            
            // Persistência
            PropostaModel propostaSalva = propostaService.salvarProposta(novaProposta);

            // Abre a aba da proposta no Workspace
            abrirPropostaNoWorkspace(propostaSalva);

            notificarSucesso("Proposta gerada com sucesso para " + cliente.getNomeCompleto() + 
                             "\nBanco: " + resultado.tabela().getBanco().getNome());

        } catch (Exception e) {
            notificarAviso("Erro ao gerar proposta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // MOTOR DE NAVEGAÇÃO E CACHE
    // =========================================================================
    public void navegarPara(String fxmlPath, boolean mostrarEstrutura) {
        executarNavegacao(fxmlPath, mostrarEstrutura);
    }

    private void executarNavegacao(String fxmlPath, boolean mostrarEstrutura) {
        try {
            // Uso de computeIfAbsent elimina a necessidade de if/else manual para cache
            ViewPair pair = cacheDeViews.computeIfAbsent(fxmlPath, this::carregarNovaView);

            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);

            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);

            contentArea.getChildren().setAll(pair.view());
            this.telaAtual = fxmlPath;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar a tela: " + fxmlPath, e);
        }
    }

    private ViewPair carregarNovaView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Node view = loader.load();
            return new ViewPair(view, loader.getController());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao instanciar o FXML: " + fxmlPath, e);
        }
    }

    public void limparCacheDeTelas() {
        cacheDeViews.clear();
    }

    // =========================================================================
    // ROTAS DO WORKSPACE
    // =========================================================================
    private void executarNoWorkspace(Consumer<WorkspaceController> acao) {
        try {
            garantirWorkspaceVisivel();
            ViewPair pair = cacheDeViews.get(FXML_WORKSPACE);

            if (pair != null && pair.controller() instanceof WorkspaceController wsController) {
                acao.accept(wsController);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void garantirWorkspaceVisivel() {
        if (!FXML_WORKSPACE.equals(this.telaAtual)) {
            navegarPara(FXML_WORKSPACE, true);
        }
    }

    public void irParaNovoContato() {
        abrirClienteNoWorkspace(null);
    }

    public void abrirDashboard() {
        executarNoWorkspace(WorkspaceController::abrirAbaDashboard);
    }

    public void abrirPlaybook() {
        executarNoWorkspace(WorkspaceController::abrirAbaPlaybook);
    }

    public void abrirClientes() {
        executarNoWorkspace(WorkspaceController::abrirAbaClientes);
    }

    public void irParaLinksUteis() {
        executarNoWorkspace(WorkspaceController::abrirAbaLinks);
    }

    public void irParaTabelasJuros() {
        executarNoWorkspace(WorkspaceController::abrirAbaTabelasJuros);
    }

    public void irParaBancosConvenios() {
        executarNoWorkspace(WorkspaceController::abrirAbaBancosConvenios);
    }

    public void irParaTabelaComissoes() {
        executarNoWorkspace(WorkspaceController::abrirAbaComissoes);
    }

    public void irParaPropostas() {
        executarNoWorkspace(ws -> ws.abrirAbaPropostas(null));
    }

    public void irParaImportadorTabelas() {
        executarNoWorkspace(WorkspaceController::abrirAbaImportadorTabelas);
    }

    public void abrirClienteNoWorkspace(ProponenteModel proponente) {
        executarNoWorkspace(ws -> ws.abrirOuFocarAba(proponente));
    }

    public void abrirPropostaNoWorkspace(PropostaModel proposta) {
        if (proposta == null || proposta.getProponente() == null)
            return;
        executarNoWorkspace(ws -> ws.abrirOuFocarAbaComProposta(proposta.getProponente(), proposta.getId()));
    }

    // =========================================================================
    // CONTROLES DE OVERLAY (LOADING & SAIR & IA)
    // =========================================================================
    public void mostrarLoading(String mensagem) {
        Platform.runLater(() -> {
            lblLoadingTexto.setText(mensagem);
            overlayLoading.setVisible(true);
        });
    }

    public void ocultarLoading() {
        Platform.runLater(() -> overlayLoading.setVisible(false));
    }

    public void mostrarOverlaySair() {
        overlaySair.setVisible(true);
    }

    @FXML
    private void cancelarLogout() {
        overlaySair.setVisible(false);
    }

    @FXML
    private void confirmarLogout() {
        overlaySair.setVisible(false);
        context.getBean(AuthService.class).logout();
        limparCacheDeTelas();
        navegarPara(FXML_LOGIN, false);
    }

    @FXML
    public void alternarPainelIA() {
        boolean estaAberto = overlayChatIA.isVisible();
        overlayChatIA.setVisible(!estaAberto);

        if (!estaAberto && painelChatController != null) {
            painelChatController.setMainController(this);
        }
    }

    // 🚀 MÉTODO: Exibe o overlay (substitui o Alert)
    private void exibirOverlayMensagem(String titulo, String mensagem) {
        Platform.runLater(() -> {
            lblMsgTitulo.setText(titulo);
            lblMsgTexto.setText(mensagem);
            overlayMensagem.setVisible(true);
            overlayMensagem.toFront(); // Garante que ele fique sobre tudo
        });
    }

    // 🚀 MÉTODO: Oculta o overlay (chamado pelo botão do FXML)
    @FXML
    private void ocultarOverlayMensagem() {
        overlayMensagem.setVisible(false);
    }

    // =========================================================================
    // NOTIFICAÇÕES GLOBAIS (DRY)
    // =========================================================================
    public void notificarSucesso(String mensagem) {
        exibirOverlayMensagem("Sucesso", mensagem);
    }

    public void notificarAviso(String mensagem) {
        exibirOverlayMensagem("Atenção", mensagem);
    }

}