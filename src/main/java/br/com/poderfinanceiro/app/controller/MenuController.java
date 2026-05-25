package br.com.poderfinanceiro.app.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);

    private final Navigator navigator;

    public MenuController(Navigator navigator) {
        this.navigator = navigator;
        log.debug("[MENU] Construtor: Controller instanciado");
    }

    // =========================================================================
    // GRUPO: PODER FINANCEIRO (Geral & Sistema)
    // =========================================================================
    @FXML
    private void handleDashboard() {
        log.info("[MENU] Usuário clicou em 'Dashboard'");
        navigator.abrirDashboard();
    }

    @FXML
    private void handleNovoContato() {
        log.info("[MENU] Usuário clicou em 'Novo Contato'");
        navigator.irParaNovoContato();
    }

    @FXML
    private void handleLogout() {
        log.info("[MENU] Usuário clicou em 'Logout'");
        navigator.mostrarOverlaySair();
    }

    @FXML
    private void handleSair() {
        log.info("[MENU] Usuário clicou em 'Sair' - encerrando aplicação");
        Platform.exit();
        System.exit(0);
    }

    // =========================================================================
    // GRUPO: OPERACIONAL (Esteira de Vendas)
    // =========================================================================
    @FXML
    private void handleClientes() {
        log.info("[MENU] Usuário clicou em 'Clientes'");
        navigator.abrirClientes();
    }

    @FXML
    private void handlePropostas() {
        log.info("[MENU] Usuário clicou em 'Propostas'");
        navigator.irParaPropostas();
    }

    // =========================================================================
    // GRUPO: FINANCEIRO
    // =========================================================================
    @FXML
    private void handleComissoes() {
        log.info("[MENU] Usuário clicou em 'Comissões'");
        navigator.irParaTabelaComissoes();
    }

    @FXML
    private void handleJuros() {
        log.info("[MENU] Usuário clicou em 'Juros'");
        navigator.irParaTabelasJuros();
    }

    @FXML
    private void handleImportarTabelas() {
        log.info("[MENU] Usuário clicou em 'Importar Tabelas'");
        navigator.irParaImportadorTabelas();
    }

    // =========================================================================
    // GRUPO: FERRAMENTAS & SUPORTE
    // =========================================================================
    @FXML
    private void handlePlaybook() {
        log.info("[MENU] Usuário clicou em 'Playbook'");
        navigator.abrirPlaybook();
    }

    @FXML
    private void handleBancos() {
        log.info("[MENU] Usuário clicou em 'Bancos e Convênios'");
        navigator.irParaBancosConvenios();
    }

    @FXML
    private void handleLinks() {
        log.info("[MENU] Usuário clicou em 'Links Úteis'");
        navigator.irParaLinksUteis();
    }

    @FXML
    private void handleLimparCache() {
        log.info("[MENU] Usuário clicou em 'Limpar Cache de Telas'");
        navigator.limparCacheDeTelas();
    }

    @FXML
    public void handleAbrirIA() {
        log.info("[MENU] Usuário clicou em 'Abrir IA' (alternar painel)");
        navigator.alternarPainelIA();
    }

    @FXML
    private void handleAbrirCopiloto() {
        log.info("[MENU] Usuário clicou em 'Abrir Copiloto de Vendas'");
        navigator.abrirCopilotoSimulacao(null);
    }
}