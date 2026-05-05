package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

        // 2. Localiza o TabPane dentro da tela atual (o Workspace)
        // O id no seu arquivo workspace.fxml deve ser "tabPanePrincipal"
        Node centro = contentArea.getChildren().get(0);
        TabPane tabPane = (TabPane) centro.lookup("#tabPanePrincipal");

        if (tabPane != null) {
            Long idBuscado = proponente.getId(); // ID vindo do banco

            // 1. BUSCA INTELIGENTE: Só tenta localizar se o proponente já tiver um ID
            if (idBuscado != null) {
                for (Tab tab : tabPane.getTabs()) {
                    Object idNaAba = tab.getUserData();

                    // Comparação segura de Longs para evitar duplicatas[cite: 4, 8]
                    if (idNaAba instanceof Long && idNaAba.equals(idBuscado)) {
                        tabPane.getSelectionModel().select(tab);
                        return; // Cliente já está aberto, apenas focamos a aba
                    }
                }
            } else {
                // OPCIONAL: Evitar abrir múltiplas abas de "Novo Contato" vazias
                for (Tab tab : tabPane.getTabs()) {
                    if ("NOVO_CONTATO".equals(tab.getUserData())) {
                        tabPane.getSelectionModel().select(tab);
                        return;
                    }
                }
            }

            // 3. Cria a nova aba manualmente
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
                loader.setControllerFactory(context::getBean);
                Parent root = loader.load();

                AtendimentoHubController hubController = loader.getController();
                hubController.inicializarAtendimento(proponente);

                // 1. Criamos a aba apenas com o conteúdo (root)
                Tab novaAba = new Tab();
                novaAba.setContent(root);
                novaAba.setClosable(true);

                Label iconeAba = new Label("👤");
                iconeAba.setStyle("-fx-font-size: 14px;");
                novaAba.setGraphic(iconeAba);

                // 3. CONFIGURAÇÃO DO TÍTULO DINÂMICO (Binding reativo)
                // O título vai observar a propriedade 'nome' lá no LeadViewModel
                novaAba.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                    String nome = hubController.getLeadController().getViewModel().nomeProperty().get();

                    if (nome == null || nome.trim().isEmpty()) {
                        return "Novo Atendimento";
                    }

                    // Opcional: Se o nome for muito longo, você pode dar um substring para não
                    // quebrar a aba
                    return nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;

                }, hubController.getLeadController().getViewModel().nomeProperty()));

                // --- 🛡️ IMPLEMENTAÇÃO DA INTERCEPTAÇÃO E MEMORY LEAK ---
                novaAba.setOnCloseRequest(event -> {
                    // Ação real que vai destruir a aba e limpar a memória
                    Runnable acaoMatarAba = () -> {
                        tabPane.getTabs().remove(novaAba); // Remove a aba visualmente
                        hubController.limparRecursos(); // Evita o Memory Leak
                    };

                    if (hubController.temAlteracoesNaoSalvas()) {
                        // 1. OBRIGATÓRIO: Cancela o fechamento automático do JavaFX
                        event.consume();

                        // 2. Aciona o seu overlay. Se o usuário clicar em "Descartar" ou "Salvar",
                        // o seu LeadController vai executar o 'acaoMatarAba' passado acima.
                        hubController.solicitarFechamento(acaoMatarAba);
                    } else {
                        // Se não tem alterações, deixa o JavaFX fechar a aba normalmente,
                        // mas garante que o Timer será desligado.
                        hubController.limparRecursos();
                    }
                });
                // --------------------------------------------------------

                tabPane.getTabs().add(novaAba);
                tabPane.getSelectionModel().select(novaAba);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Erro Crítico: Não foi possível localizar o TabPane no Workspace!");
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