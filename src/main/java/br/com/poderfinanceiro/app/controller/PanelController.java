package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;

@Component
public class PanelController {

    // 1. Declaramos a variável que estava faltando
    private final MainController mainController;

    // 2. Criamos o construtor para o Spring injetar o "Maestro" do sistema
    public PanelController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // Lógica da barra lateral de navegação e da busca rápida
        System.out.println("PanelController carregado com sucesso!");
    }

    @FXML
    private void abrirTelaBaseClientes() {
        // Agora o Java sabe exatamente o que é o mainController
        mainController.navegarPara("/fxml/clientes_list.fxml", true);
    }

    @FXML
    private void abrirWorkspace() {
        // Seguindo o padrão de navegação do seu sistema
        // O caminho deve ser o arquivo que contém o TabPane (workspace.fxml)
        mainController.navegarPara("/fxml/workspace.fxml", true);
    }
}