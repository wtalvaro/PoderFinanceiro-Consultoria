package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.io.IOException;
import javafx.application.HostServices;

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Node view = loader.load();

            // Oculta ou mostra barras (Menu, Sidebar, Rodapé)
            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);

            sideBar.setVisible(mostrarEstrutura);
            sideBar.setManaged(mostrarEstrutura);

            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);

            // Injeta a nova tela no centro
            contentArea.getChildren().setAll(view);

            // Atualiza o registro de onde o consultor está agora
            this.telaAtual = fxmlPath;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar a tela: " + fxmlPath, e);
        }
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