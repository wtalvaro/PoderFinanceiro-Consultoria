package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.io.IOException;

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

    private final ApplicationContext context;

    public MainController(ApplicationContext context) {
        this.context = context;
    }

    /**
     * O Cérebro da Navegação: Troca o conteúdo central e ajusta a estrutura.
     */
    public void navegarPara(String fxmlPath, boolean mostrarEstrutura) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Node view = loader.load();

            // Oculta ou mostra barras (Menu, Sidebar, Rodapé)
            // O setManaged(false) faz com que a área libere espaço físico para a tela (ex:
            // Login ficar no meio)
            topBar.setVisible(mostrarEstrutura);
            topBar.setManaged(mostrarEstrutura);

            sideBar.setVisible(mostrarEstrutura);
            sideBar.setManaged(mostrarEstrutura);

            bottomBar.setVisible(mostrarEstrutura);
            bottomBar.setManaged(mostrarEstrutura);

            // Injeta a nova tela no centro
            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar a tela: " + fxmlPath, e);
        }
    }

    // No MainController.java

    public void irParaNovoContato() {
        // Pegamos o bean de forma preguiçosa (lazy) para evitar dependência circular
        context.getBean(LeadController.class).prepararNovoContato();

        // Executamos a navegação
        navegarPara("/fxml/lead.fxml", true);
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
        // Redireciona para o login e oculta as barras de navegação!
        navegarPara("/fxml/login.fxml", false);
        overlaySair.setVisible(false);
    }
}