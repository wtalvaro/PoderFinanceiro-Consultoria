package br.com.poderfinanceiro.app.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.ui.navigation.Navigator;

@Component
public class MenuController {

    private final Navigator navigator;

    public MenuController(Navigator navigator) {
        this.navigator = navigator;
    }

    // =========================================================================
    // GRUPO: PODER FINANCEIRO (Geral & Sistema)
    // =========================================================================
    @FXML
    private void handleDashboard() {
        navigator.abrirDashboard();
    }

    @FXML
    private void handleNovoContato() {
        navigator.irParaNovoContato();
    }

    @FXML
    private void handleLogout() {
        navigator.mostrarOverlaySair();
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
        navigator.abrirClientes();
    }

    @FXML
    private void handlePropostas() {
        navigator.irParaPropostas();
    }

    // =========================================================================
    // GRUPO: FINANCEIRO
    // =========================================================================
    @FXML
    private void handleComissoes() {
        navigator.irParaTabelaComissoes();
    }

    @FXML
    private void handleJuros() {
        navigator.irParaTabelasJuros();
    }

    @FXML
    private void handleImportarTabelas() {
        navigator.irParaImportadorTabelas();
    }

    // =========================================================================
    // GRUPO: FERRAMENTAS & SUPORTE
    // =========================================================================
    @FXML
    private void handlePlaybook() {
        navigator.abrirPlaybook();
    }

    @FXML
    private void handleBancos() {
        navigator.irParaBancosConvenios();
    }

    @FXML
    private void handleLinks() {
        navigator.irParaLinksUteis();
    }

    @FXML
    private void handleLimparCache() {
        navigator.limparCacheDeTelas();
    }

    @FXML
    public void handleAbrirIA() {
        navigator.alternarPainelIA();
    }

    @FXML
    private void handleAbrirCopiloto() {
        // Usa null porque o menu não passa o nó de âncora (o AnchorNode foi removido na
        // nossa última refatoração)
        navigator.abrirCopilotoSimulacao(null);
    }
}