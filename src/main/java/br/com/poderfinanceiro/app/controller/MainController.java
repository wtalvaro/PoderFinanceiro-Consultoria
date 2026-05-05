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

    // Rastreamento da tela ativa para interceptar saídas acidentais
    private String telaAtual = "";

    public MainController(ApplicationContext context, HostServices hostServices) {
        this.context = context;
        this.hostServices = hostServices;
    }

    /**
     * Expondo o serviço para os outros controllers
     */
    public HostServices getHostServices() {
        return hostServices;
    }

    /**
     * O Cérebro da Navegação: Ponto de entrada para todas as trocas de tela.
     * Ele intercepta a ação caso o usuário esteja saindo da tela de Lead.
     */
    public void navegarPara(String fxmlPath, boolean mostrarEstrutura) {
        // Empacota a troca de tela em um "Callback" (Runnable)
        Runnable acaoNavegacao = () -> executarNavegacao(fxmlPath, mostrarEstrutura);

        // Se estivermos na tela de Lead e tentando ir para outra tela (ex: Dashboard ou
        // Login)
        if ("/fxml/lead.fxml".equals(this.telaAtual) && !"/fxml/lead.fxml".equals(fxmlPath)) {
            // Delega a responsabilidade para o LeadController perguntar sobre alterações
            LeadController leadController = context.getBean(LeadController.class);
            leadController.tentarNavegar(acaoNavegacao);
        } else {
            // Se estiver em qualquer outra tela, navega direto
            acaoNavegacao.run();
        }
    }

    /**
     * Motor de navegação atualizado para capturar e preservar o Controller.
     */
    private void executarNavegacao(String fxmlPath, boolean mostrarEstrutura) {
        try {
            ViewPair pair;

            if (cacheDeViews.containsKey(fxmlPath)) {
                pair = cacheDeViews.get(fxmlPath);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(context::getBean);
                Node view = loader.load();

                // Captura o controller REAL gerado pelo Spring para esta tela[cite: 4]
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

    /**
     * Versão corrigida: Agora fala com o Controller REAL da tela, não com um
     * novo.
     */
    public void irParaNovoContato() {
        // 1. Garante que a tela de Lead esteja carregada no cache e visível
        executarNavegacao("/fxml/lead.fxml", true);

        // 2. Recupera o par Visual/Controller do cache[cite: 4]
        ViewPair pair = cacheDeViews.get("/fxml/lead.fxml");

        if (pair != null && pair.controller instanceof LeadController leadController) {
            // Ação que será executada se for seguro prosseguir[cite: 4]
            Runnable acaoLimparFormulario = () -> leadController.prepararNovoContato();

            // 3. Se o usuário já estiver na tela de lead, verifica alterações
            // pendentes[cite: 4]
            // Se não estiver, executa o reset direto
            if ("/fxml/lead.fxml".equals(this.telaAtual)) {
                leadController.tentarNavegar(acaoLimparFormulario);
            } else {
                acaoLimparFormulario.run();
            }
        }
    }

    // No MainController.java
    public void irParaWorkspace() {
        navegarPara("/fxml/workspace.fxml", true); // Garante o carregamento do FXML
    }

    public void abrirClienteNoWorkspace(Proponente proponente) {
        // 1. Garante que o Workspace esteja visível no centro da tela
        if (!"/fxml/workspace.fxml".equals(this.telaAtual)) {
            navegarPara("/fxml/workspace.fxml", true);
        }

        try {
            WorkspaceController workspaceController;
            ViewPair pair = cacheDeViews.get("/fxml/workspace.fxml");

            // 2. Se já estiver no cache, usa ele. Se não, carrega a tela e o Controller
            // agora.
            if (pair != null && pair.controller instanceof WorkspaceController) {
                workspaceController = (WorkspaceController) pair.controller;
            } else {
                // Força o carregamento para garantir que o WorkspaceController exista
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/workspace.fxml"));
                loader.setControllerFactory(context::getBean);
                Node view = loader.load();
                workspaceController = loader.getController();
                cacheDeViews.put("/fxml/workspace.fxml", new ViewPair(view, workspaceController));
                contentArea.getChildren().setAll(view);
                this.telaAtual = "/fxml/workspace.fxml";
            }

            // 3. Delega a responsabilidade da aba para quem realmente detém o componente
            // TabPane
            workspaceController.abrirOuFocarAba(proponente);

        } catch (IOException e) {
            System.err.println("Erro ao carregar WorkspaceController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- LÓGICA DO OVERLAY DE SAÍDA ---
    public void mostrarOverlaySair() {
        overlaySair.setVisible(true);
    }

    @FXML
    private void cancelarLogout() {
        overlaySair.setVisible(false);
    }

    @FXML
    private void confirmarLogout() {
        // Primeiro ocultamos a janela modal de confirmação de saída
        overlaySair.setVisible(false);

        // ---> CORREÇÕES DE SEGURANÇA E UX AQUI <---
        // 1. Efetua o logout real no backend (anula o usuarioLogado)
        context.getBean(br.com.poderfinanceiro.app.service.AuthService.class).logout();

        // 2. Destrói o cache! Assim, o próximo usuário terá um Workspace novinho
        limparCacheDeTelas();
        // ------------------------------------------

        // Chamamos a navegação padrão.
        navegarPara("/fxml/login.fxml", false);
    }
}