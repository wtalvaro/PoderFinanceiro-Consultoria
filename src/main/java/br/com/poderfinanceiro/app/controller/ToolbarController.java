package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class ToolbarController {

    private final StageInitializer stageInitializer;
    private final MainController mainController;

    public ToolbarController(StageInitializer stageInitializer, MainController mainController) {
        this.stageInitializer = stageInitializer;
        this.mainController = mainController;
    }

    // ==========================================
    // AÇÕES GLOBAIS
    // ==========================================

    @FXML
    private void handleNovoContato() {
        mainController.irParaNovoContato();
    }

    // ==========================================
    // NAVEGAÇÃO PRINCIPAL (Transplantada)
    // ==========================================

    @FXML
    private void abrirWorkspace() {
        mainController.abrirDashboard();
    }

    @FXML
    private void abrirPlaybook() {
        mainController.abrirPlaybook();
    }

    @FXML
    private void abrirTelaBaseClientes() {
        mainController.abrirClientes();
    }

    @FXML
    private void abrirPropostasList() {
        mainController.irParaPropostas();
    }

    @FXML
    private void abrirPendenciasList() {
        mainController.irParaPendencias();
    }

    // ==========================================
    // CONFIGURAÇÕES E AUXILIARES
    // ==========================================

    @FXML
    private void abrirBancosConvenios() {
        mainController.irParaBancosConvenios();
    }

    @FXML
    private void abrirTabelasJuros() {
        mainController.irParaTabelasJuros();
    }

    @FXML
    private void abrirTabelaComissoes() {
        mainController.irParaTabelaComissoes();
    }

    @FXML
    private void handleLinksUteis() {
        mainController.irParaLinksUteis();
    }

    // ==========================================
    // SISTEMA
    // ==========================================

    @FXML
    private void handleLogout() {
        stageInitializer.logout(); // Chama a lógica centralizada
    }
}