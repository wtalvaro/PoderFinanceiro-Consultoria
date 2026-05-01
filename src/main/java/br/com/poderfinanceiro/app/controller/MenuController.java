package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class MenuController {

    private final StageInitializer stageInitializer;

    public MenuController(StageInitializer stageInitializer) {
        this.stageInitializer = stageInitializer;
    }

    @FXML
    private void handleMenuSair() {
        stageInitializer.logout(); // Usa a mesma lógica centralizada
    }
}