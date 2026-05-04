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
    private void handleNovoContato() { // Ou o nome que você deu ao método no XML
        mainController.irParaNovoContato();
    }

    @FXML
    private void handleNovaSimulacao() {
        // Direciona o centro do sistema para o simulador
        // O segundo parâmetro 'true' garante que a moldura (Sidebar/Menu) permaneça
        // visível
        mainController.navegarPara("/fxml/simulator.fxml", true);
    }

    @FXML
    private void handleMenuSair() {
        Platform.exit();
        System.exit(0);
    }
}