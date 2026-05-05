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
    private void abrirTelaBaseClientes() {
        mainController.navegarPara("/fxml/clientes_list.fxml", true);
    }

    @FXML
    private void abrirWorkspace() {
        mainController.navegarPara("/fxml/workspace.fxml", true);
    }

    /**
     * Aciona a navegação para a tela que contém os scripts
     * e estratégias de venda da Poder Financeiro.
     */
    @FXML
    private void abrirPlaybook() {
        // Certifique-se de que o arquivo FXML da listagem de scripts
        // esteja neste caminho
        mainController.navegarPara("/fxml/playbook.fxml", true);
    }
}