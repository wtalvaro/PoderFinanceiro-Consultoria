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
    private final Map<String, Node> telasEmCache = new HashMap<>();

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
     * O motor real de navegação que faz o carregamento do FXML e atualiza a UI.
     */
    private void executarNavegacao(String fxmlPath, boolean mostrarEstrutura) {
        try {
            Node view;

            // Verifica se a tela já foi carregada antes
            if (telasEmCache.containsKey(fxmlPath)) {
                // Recupera a tela com todo o seu estado (como as abas abertas)
                view = telasEmCache.get(fxmlPath);
            } else {
                // Se for a primeira vez, carrega o FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(context::getBean);
                view = loader.load();

                // Salva no cache para uso futuro
                telasEmCache.put(fxmlPath, view);
            }

            // Oculta ou mostra barras (Menu, Sidebar, Rodapé)
            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);

            sideBar.setVisible(mostrarEstrutura);
            sideBar.setManaged(mostrarEstrutura);

            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);

            // Injeta a tela recuperada ou recém-criada no centro
            contentArea.getChildren().setAll(view);

            this.telaAtual = fxmlPath;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar a tela: " + fxmlPath, e);
        }
    }

    public void limparCacheDeTelas() {
        telasEmCache.clear();
    }

    /**
     * Método acionado pelo menu lateral para iniciar um novo atendimento
     */
    public void irParaNovoContato() {
        Runnable acaoNovoContato = () -> {
            context.getBean(LeadController.class).prepararNovoContato();
            executarNavegacao("/fxml/lead.fxml", true);
        };

        // Se já estivermos editando uma Lead e clicarmos no botão "Novo Contato",
        // também precisamos proteger os dados antes de limpar a tela!
        if ("/fxml/lead.fxml".equals(this.telaAtual)) {
            context.getBean(LeadController.class).tentarNavegar(acaoNovoContato);
        } else {
            acaoNovoContato.run();
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
        // 3. Cria a nova aba manualmente
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            AtendimentoHubController hubController = loader.getController();
            hubController.inicializarAtendimento(proponente);

            Tab novaAba = new Tab(proponente.getNomeCompleto(), root);
            novaAba.setClosable(true);

            Label iconeAba = new Label("👤");
            iconeAba.setStyle("-fx-font-size: 14px;");
            novaAba.setGraphic(iconeAba);

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

        // Chamamos a navegação padrão. Se houver edição de Lead em andamento,
        // o sistema vai "pausar" o logout, abrir a tela de salvar e só depois efetuar o
        // logout!
        navegarPara("/fxml/login.fxml", false);
    }
}