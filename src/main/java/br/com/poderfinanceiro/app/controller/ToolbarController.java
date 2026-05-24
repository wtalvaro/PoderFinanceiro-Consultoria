package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.ui.stage.StageInitializer;

@Component
public class ToolbarController {

    private final StageInitializer stageInitializer;
    private final Navigator navigator;

    public ToolbarController(StageInitializer stageInitializer, Navigator navigator) {
        this.stageInitializer = stageInitializer;
        this.navigator = navigator;
    }

    // ==========================================
    // AÇÕES GLOBAIS
    // ==========================================

    @FXML
    private void handleNovoContato() {
        navigator.irParaNovoContato();
    }

    // ==========================================
    // NAVEGAÇÃO PRINCIPAL (Transplantada)
    // ==========================================

    @FXML
    private void abrirWorkspace() {
        navigator.abrirDashboard();
    }

    @FXML
    private void abrirPlaybook() {
        navigator.abrirPlaybook();
    }

    @FXML
    private void abrirTelaBaseClientes() {
        navigator.abrirClientes();
    }

    @FXML
    private void abrirPropostasList() {
        navigator.irParaPropostas();
    }

    // Adicione no grupo CONFIGURAÇÕES E AUXILIARES
    @FXML
    private void abrirImportadorTabelas() {
        navigator.irParaImportadorTabelas();
    }
    
    // ==========================================
    // CONFIGURAÇÕES E AUXILIARES
    // ==========================================

    @FXML
    private void abrirBancosConvenios() {
        navigator.irParaBancosConvenios();
    }

    @FXML
    private void abrirTabelasJuros() {
        navigator.irParaTabelasJuros();
    }

    @FXML
    private void abrirTabelaComissoes() {
        navigator.irParaTabelaComissoes();
    }

    @FXML
    private void handleLinksUteis() {
        navigator.irParaLinksUteis();
    }

    @FXML
    public void handleAbrirIA() {
        // "Chefe (Navigator), o usuário clicou no botão! Abre o painel de IA aí!"
        navigator.alternarPainelIA();
    }

    @FXML
    public void handleAbrirCopiloto(javafx.event.ActionEvent event) {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        navigator.abrirCopilotoSimulacao(source);
    }

    // ==========================================
    // SISTEMA
    // ==========================================

    @FXML
    private void handleLogout() {
        stageInitializer.logout(); // Chama a lógica centralizada
    }
}