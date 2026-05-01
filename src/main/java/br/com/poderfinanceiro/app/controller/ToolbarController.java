package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class ToolbarController {

    private final StageInitializer stageInitializer;

    public ToolbarController(StageInitializer stageInitializer) {
        this.stageInitializer = stageInitializer;
    }

    @FXML
    private void handleLogout() {
        stageInitializer.logout(); // Chama a lógica centralizada
    }
}