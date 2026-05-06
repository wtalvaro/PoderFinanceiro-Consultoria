package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;

@Component
public class PanelController {

    private final MainController mainController;

    public PanelController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        System.out.println("PanelController carregado com sucesso!");
    }

    @FXML
    private void abrirWorkspace() {
        mainController.focarAbaDashboard();
    }

    @FXML
    private void abrirPlaybook() {
        mainController.focarAbaPlaybook();
    }

    @FXML
    private void abrirTelaBaseClientes() {
        mainController.focarAbaClientes();
    }
}