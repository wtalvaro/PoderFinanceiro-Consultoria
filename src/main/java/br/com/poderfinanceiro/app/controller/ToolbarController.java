package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.ui.stage.StageInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ToolbarController {

    private static final Logger log = LoggerFactory.getLogger(ToolbarController.class);

    private final StageInitializer stageInitializer;
    private final Navigator navigator;

    public ToolbarController(StageInitializer stageInitializer, Navigator navigator) {
        this.stageInitializer = stageInitializer;
        this.navigator = navigator;
        log.debug("[TOOLBAR] Construtor: Controller instanciado");
    }

    // ==========================================
    // AÇÕES GLOBAIS
    // ==========================================

    @FXML
    private void handleNovoContato() {
        log.info("[TOOLBAR] Usuário clicou em 'Novo Contato'");
        navigator.irParaNovoContato();
    }

    // ==========================================
    // NAVEGAÇÃO PRINCIPAL (Transplantada)
    // ==========================================

    @FXML
    private void abrirWorkspace() {
        log.info("[TOOLBAR] Usuário clicou em 'Workspace/Dashboard'");
        navigator.abrirDashboard();
    }

    @FXML
    private void abrirPlaybook() {
        log.info("[TOOLBAR] Usuário clicou em 'Playbook'");
        navigator.abrirPlaybook();
    }

    @FXML
    private void abrirTelaBaseClientes() {
        log.info("[TOOLBAR] Usuário clicou em 'Clientes'");
        navigator.abrirClientes();
    }

    @FXML
    private void abrirPropostasList() {
        log.info("[TOOLBAR] Usuário clicou em 'Lista de Propostas'");
        navigator.irParaPropostas();
    }

    // Adicione no grupo CONFIGURAÇÕES E AUXILIARES
    @FXML
    private void abrirImportadorTabelas() {
        log.info("[TOOLBAR] Usuário clicou em 'Importador de Tabelas'");
        navigator.irParaImportadorTabelas();
    }

    // ==========================================
    // CONFIGURAÇÕES E AUXILIARES
    // ==========================================

    @FXML
    private void abrirBancosConvenios() {
        log.info("[TOOLBAR] Usuário clicou em 'Bancos e Convênios'");
        navigator.irParaBancosConvenios();
    }

    @FXML
    private void abrirTabelasJuros() {
        log.info("[TOOLBAR] Usuário clicou em 'Tabelas de Juros'");
        navigator.irParaTabelasJuros();
    }

    @FXML
    private void abrirTabelaComissoes() {
        log.info("[TOOLBAR] Usuário clicou em 'Tabela de Comissões'");
        navigator.irParaTabelaComissoes();
    }

    @FXML
    private void handleLinksUteis() {
        log.info("[TOOLBAR] Usuário clicou em 'Links Úteis'");
        navigator.irParaLinksUteis();
    }

    @FXML
    public void handleAbrirIA() {
        log.info("[TOOLBAR] Usuário clicou em 'Abrir IA' (alternar painel)");
        navigator.alternarPainelIA();
    }

    @FXML
    public void handleAbrirCopiloto(javafx.event.ActionEvent event) {
        log.info("[TOOLBAR] Usuário clicou em 'Abrir Copiloto'");
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        navigator.abrirCopilotoSimulacao(source);
    }

    // ==========================================
    // SISTEMA
    // ==========================================

    @FXML
    private void handleLogout() {
        log.info("[TOOLBAR] Usuário solicitou logout");
        stageInitializer.logout(); // Chama a lógica centralizada
    }
}