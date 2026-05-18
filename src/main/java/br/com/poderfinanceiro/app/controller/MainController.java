package br.com.poderfinanceiro.app.controller;

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

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;

import java.io.IOException;
import javafx.application.HostServices;

import java.util.HashMap;
import java.util.Map;

@Component
public class MainController {

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

    // 🎯 INJEÇÕES PARA REDIMENSIONAMENTO DINÂMICO
    @FXML
    private Region dragHandleChat; // Alça de arrasto mapeada no FXML
    @FXML
    private Region painelChat; // O nó raiz do ajuda_chat.fxml injetado pelo fx:id do include
    @FXML
    private AjudaChatController painelChatController;

    private final HostServices hostServices;
    private final ApplicationContext context;

    // 🚀 VARIÁVEIS DE CONTROLE FISICO DO MOUSE
    private double mouseStartX = 0;
    private double chatStartWidth = 0;

    private static class ViewPair {
        Node view;
        Object controller;

        ViewPair(Node view, Object controller) {
            this.view = view;
            this.controller = controller;
        }
    }

    private final Map<String, ViewPair> cacheDeViews = new HashMap<>();

    // Rastreamento da tela ativa para evitar recarregamentos desnecessários
    private String telaAtual = "";

    public MainController(ApplicationContext context, HostServices hostServices) {
        this.context = context;
        this.hostServices = hostServices;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    // ========================================================================
    // CICLO DE INICIALIZAÇÃO NATIVA (Controle das Alças do Mouse)
    // ========================================================================
    @FXML
    public void initialize() {
        // 🚀 CAPTURA DA FÍSICA DE ARRASTO DO OVERLAY FLUTUANTE
        if (dragHandleChat != null) {
            dragHandleChat.setOnMousePressed(event -> {
                mouseStartX = event.getSceneX();
                if (painelChat != null) {
                    chatStartWidth = painelChat.getWidth();
                }
            });

            dragHandleChat.setOnMouseDragged(event -> {
                if (painelChat == null)
                    return;

                // Calcula o deslocamento horizontal do mouse
                double deltaX = event.getSceneX() - mouseStartX;

                // Engenharia Reversa: Como o painel está colado na DIREITA, arrastar o mouse
                // para a ESQUERDA (deltaX negativo) aumenta o tamanho útil do chat.
                double novaLargura = chatStartWidth - deltaX;

                // Margens de segurança de UX para não quebrar as tabelas Bootstrap nem amassar
                // a tela
                double larguraMinima = 320.0;
                double larguraMaxima = 750.0;

                if (novaLargura >= larguraMinima && novaLargura <= larguraMaxima) {
                    painelChat.setPrefWidth(novaLargura);
                    painelChat.setMinWidth(novaLargura);
                    painelChat.setMaxWidth(novaLargura);
                }
            });

            // Feedback visual elegante ao passar o mouse sobre a alça de arrasto
            dragHandleChat.setOnMouseEntered(e -> dragHandleChat
                    .setStyle("-fx-cursor: h-resize; -fx-background-color: rgba(30, 64, 175, 0.15);"));
            dragHandleChat.setOnMouseExited(
                    e -> dragHandleChat.setStyle("-fx-cursor: default; -fx-background-color: rgba(0, 0, 0, 0.03);"));
        }

        // 🚀 O BOTÃO FECHAR INVISÍVEL (Clique fora para fechar)
        if (overlayChatIA != null) {
            overlayChatIA.setOnMouseClicked(event -> {
                // Se o alvo real do clique foi a área vazia do HBox, e não o chat ou a alça
                if (event.getTarget() == overlayChatIA) {
                    alternarPainelIA(); // Executa a sua lógica nativa de fechar/esconder
                    event.consume(); // Consome o evento para não propagar para os componentes de baixo
                }
            });
        }
    }

    /**
     * O Cérebro da Navegação: Ponto de entrada para todas as trocas de tela
     * principais.
     */
    public void navegarPara(String fxmlPath, boolean mostrarEstrutura) {
        executarNavegacao(fxmlPath, mostrarEstrutura);
    }

    private void executarNavegacao(String fxmlPath, boolean mostrarEstrutura) {
        try {
            ViewPair pair;

            if (cacheDeViews.containsKey(fxmlPath)) {
                pair = cacheDeViews.get(fxmlPath);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(context::getBean);
                Node view = loader.load();

                // Captura o controller REAL gerado pelo Spring para esta tela
                pair = new ViewPair(view, loader.getController());
                cacheDeViews.put(fxmlPath, pair);
            }

            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);

            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);

            contentArea.getChildren().setAll(pair.view);
            this.telaAtual = fxmlPath;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar a tela: " + fxmlPath, e);
        }
    }

    public void limparCacheDeTelas() {
        cacheDeViews.clear();
    }

    // ========================================================================
    // ROTEAMENTO DE ABAS NO WORKSPACE (Clean Architecture)
    // ========================================================================

    public void irParaNovoContato() {
        abrirClienteNoWorkspace(null);
    }

    // ========================================================================
    // MOTOR DE NAVEGAÇÃO INTERNA (O "Protocolo Padrão")
    // ========================================================================

    private void executarNoWorkspace(java.util.function.Consumer<WorkspaceController> acao) {
        try {
            garantirWorkspaceVisivel();
            ViewPair pair = cacheDeViews.get("/fxml/workspace.fxml");

            if (pair != null && pair.controller instanceof WorkspaceController) {
                acao.accept((WorkspaceController) pair.controller);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================================================================
    // MÉTODOS PÚBLICOS (As Ordens Diretas)
    // ========================================================================

    public void abrirDashboard() {
        executarNoWorkspace(ws -> ws.abrirAbaDashboard());
    }

    public void abrirPlaybook() {
        executarNoWorkspace(ws -> ws.abrirAbaPlaybook());
    }

    public void abrirClientes() {
        executarNoWorkspace(ws -> ws.abrirAbaClientes());
    }

    public void abrirClienteNoWorkspace(ProponenteModel proponente) {
        executarNoWorkspace(ws -> ws.abrirOuFocarAba(proponente));
    }

    public void abrirPropostaNoWorkspace(PropostaModel proposta) {
        if (proposta == null || proposta.getProponente() == null)
            return;
        executarNoWorkspace(ws -> ws.abrirOuFocarAbaComProposta(proposta.getProponente(), proposta.getId()));
    }

    public void irParaLinksUteis() {
        executarNoWorkspace(ws -> ws.abrirAbaLinks());
    }

    public void irParaTabelasJuros() {
        executarNoWorkspace(ws -> ws.abrirAbaTabelasJuros());
    }

    public void irParaBancosConvenios() {
        executarNoWorkspace(ws -> ws.abrirAbaBancosConvenios());
    }

    public void irParaTabelaComissoes() {
        executarNoWorkspace(ws -> ws.abrirAbaComissoes());
    }

    public void irParaPropostas() {
        executarNoWorkspace(ws -> ws.abrirAbaPropostas(null));
    }

    public void irParaPendencias() {
        executarNoWorkspace(ws -> ws.abrirAbaPropostas("PENDENTE"));
    }

    private void garantirWorkspaceVisivel() {
        if (!"/fxml/workspace.fxml".equals(this.telaAtual)) {
            navegarPara("/fxml/workspace.fxml", true);
        }
    }

    public void mostrarLoading(String mensagem) {
        javafx.application.Platform.runLater(() -> {
            lblLoadingTexto.setText(mensagem);
            overlayLoading.setVisible(true);
        });
    }

    public void ocultarLoading() {
        javafx.application.Platform.runLater(() -> {
            overlayLoading.setVisible(false);
        });
    }

    // ========================================================================
    // LÓGICA DO OVERLAY DE SAÍDA
    // ========================================================================

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
        context.getBean(br.com.poderfinanceiro.app.service.AuthService.class).logout();
        limparCacheDeTelas();
        navegarPara("/fxml/login.fxml", false);
    }

    // ========================================================================
    // SISTEMA DE NOTIFICAÇÕES GLOBAIS
    // ========================================================================

    public void notificarSucesso(String mensagem) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alerta.setTitle("Poder Financeiro");
            alerta.setHeaderText(null);
            alerta.setContentText(mensagem);
            alerta.showAndWait();
        });
    }

    public void notificarAviso(String mensagem) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alerta.setTitle("Atenção");
            alerta.setHeaderText(null);
            alerta.setContentText(mensagem);
            alerta.showAndWait();
        });
    }

    @FXML
    public void alternarPainelIA() {
        boolean estaAberto = overlayChatIA.isVisible();
        overlayChatIA.setVisible(!estaAberto);

        if (!estaAberto && painelChatController != null) {
            painelChatController.setMainController(this);
        }
    }
}