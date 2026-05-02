package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class ToolbarController {

    private final StageInitializer stageInitializer;
    private final MainController mainController;
    
    public ToolbarController(StageInitializer stageInitializer, MainController mainController,
            LeadController leadController) {
        this.stageInitializer = stageInitializer;
        this.mainController = mainController;
    }

    @FXML
    private void handleLogout() {
        stageInitializer.logout(); // Chama a lógica centralizada
    }

    // Lembre-se de injetar o MainController no construtor do ToolbarController
    // primeiro!

    @FXML
    private void handleNovoContato() { // Ou o nome que você deu ao método no XML
        mainController.irParaNovoContato();
    }
}