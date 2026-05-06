package br.com.poderfinanceiro.app.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class MenuController {

    private final MainController mainController;

    // Injeta o MainController via construtor
    public MenuController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleNovoContato() {
        mainController.irParaNovoContato(); // Agora criará uma aba Hub "Novo Atendimento"
    }

    @FXML
    private void handlePlaybook() {
        mainController.focarAbaPlaybook();
    }

    @FXML
    private void handleMenuSair() {
        Platform.exit();
        System.exit(0);
    }
}