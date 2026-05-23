package br.com.poderfinanceiro.app.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class MenuController {

    private final MainController mainController;

    public MenuController(MainController mainController) {
        this.mainController = mainController;
    }

    // =========================================================================
    // GRUPO: PODER FINANCEIRO (Geral & Sistema)
    // =========================================================================
    @FXML
    private void handleDashboard() {
        mainController.abrirDashboard();
    }

    @FXML
    private void handleNovoContato() {
        mainController.irParaNovoContato();
    }

    @FXML
    private void handleLogout() {
        mainController.mostrarOverlaySair();
    }

    @FXML
    private void handleSair() {
        Platform.exit();
        System.exit(0);
    }

    // =========================================================================
    // GRUPO: OPERACIONAL (Esteira de Vendas)
    // =========================================================================
    @FXML
    private void handleClientes() {
        mainController.abrirClientes();
    }

    @FXML
    private void handlePropostas() {
        mainController.irParaPropostas();
    }

    // =========================================================================
    // GRUPO: FINANCEIRO
    // =========================================================================
    @FXML
    private void handleComissoes() {
        mainController.irParaTabelaComissoes();
    }

    @FXML
    private void handleJuros() {
        mainController.irParaTabelasJuros();
    }

    @FXML
    private void handleImportarTabelas() {
        mainController.irParaImportadorTabelas();
    }

    // =========================================================================
    // GRUPO: FERRAMENTAS & SUPORTE
    // =========================================================================
    @FXML
    private void handlePlaybook() {
        mainController.abrirPlaybook();
    }

    @FXML
    private void handleBancos() {
        mainController.irParaBancosConvenios();
    }

    @FXML
    private void handleLinks() {
        mainController.irParaLinksUteis();
    }

    @FXML
    private void handleLimparCache() {
        mainController.limparCacheDeTelas();
    }

    @FXML
    public void handleAbrirIA() {
        mainController.alternarPainelIA();
    }

    @FXML
    private void handleAbrirCopiloto() {
        // Usa null porque o menu não passa o nó de âncora (o AnchorNode foi removido na
        // nossa última refatoração)
        mainController.abrirCopilotoSimulacao(null);
    }
}