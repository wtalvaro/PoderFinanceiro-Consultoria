package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import br.com.poderfinanceiro.app.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private final AuthService authService;
    private final StageInitializer stageInitializer;

    @FXML
    private VBox overlaySair;

    // Injeção via construtor para o Spring gerenciar os beans
    public MainController(AuthService authService, StageInitializer stageInitializer) {
        this.authService = authService;
        this.stageInitializer = stageInitializer;
    }

    @FXML
    public void initialize() {
        System.out.println("MainController carregado com sucesso!");
    }

    /**
     * Exibe o diálogo interno, evitando o redimensionamento do Wayland
     */
    public void mostrarOverlaySair() {
        overlaySair.setVisible(true);
    }

    @FXML
    private void cancelarLogout() {
        overlaySair.setVisible(false);
    }

    @FXML
    private void confirmarLogout() {
        authService.logout(); // Limpa o usuário logado
        overlaySair.setVisible(false);

        // Retorna ao login mantendo o Stage estável
        stageInitializer.showScene("/fxml/login.fxml", "Poder Financeiro - Acesso");
    }
}