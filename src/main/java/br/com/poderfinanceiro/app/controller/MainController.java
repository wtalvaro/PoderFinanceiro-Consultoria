package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.model.Proponente;

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
    private VBox sideBar;
    @FXML
    private StackPane bottomBar;
    @FXML
    private VBox overlaySair;

    private final HostServices hostServices;
    private final ApplicationContext context;

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
            sideBar.setVisible(mostrarEstrutura);
            sideBar.setManaged(mostrarEstrutura);
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
        abrirClienteNoWorkspace(null); // Null indica "Novo Cadastro" para o Workspace
    }

    public void focarAbaDashboard() {
        acionarAbaFixaWorkspace(0);
    }

    public void focarAbaPlaybook() {
        acionarAbaFixaWorkspace(1);
    }

    public void focarAbaClientes() {
        acionarAbaFixaWorkspace(2);
    }

    private void acionarAbaFixaWorkspace(int index) {
        garantirWorkspaceVisivel();
        WorkspaceController ws = (WorkspaceController) cacheDeViews.get("/fxml/workspace.fxml").controller;
        ws.focarAbaFixa(index);
    }

    public void abrirClienteNoWorkspace(Proponente proponente) {
        garantirWorkspaceVisivel();
        WorkspaceController ws = (WorkspaceController) cacheDeViews.get("/fxml/workspace.fxml").controller;
        ws.abrirOuFocarAba(proponente);
    }

    private void garantirWorkspaceVisivel() {
        if (!"/fxml/workspace.fxml".equals(this.telaAtual)) {
            // Executa o carregamento real do FXML e Controller caso ainda não esteja na
            // tela
            navegarPara("/fxml/workspace.fxml", true);
        }
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
}