package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.application.Platform;

@Component
public class MenuController {

    private final MainController mainController;

    public MenuController(MainController mainController) {
        this.mainController = mainController;
    }

    // --- GRUPO: PODER FINANCEIRO ---
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

    // --- GRUPO: OPERACIONAL ---
    @FXML
    private void handleClientes() {
        mainController.abrirClientes();
    }

    @FXML
    private void handlePropostas() {
        mainController.irParaPropostas();
    }

    @FXML
    private void handlePendencias() {
        mainController.irParaPendencias();
    }

    // --- GRUPO: FINANCEIRO ---
    @FXML
    private void handleComissoes() {
        mainController.irParaTabelaComissoes();
    }

    @FXML
    private void handleJuros() {
        mainController.irParaTabelasJuros();
    }

    // --- GRUPO: FERRAMENTAS ---
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
        // "Chefe (MainController), o usuário clicou no botão! Abre o painel de IA aí!"
        mainController.alternarPainelIA();
    }
}