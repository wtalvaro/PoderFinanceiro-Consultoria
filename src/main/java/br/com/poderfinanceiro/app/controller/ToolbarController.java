package br.com.poderfinanceiro.app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;

/**
 * <h1>ToolbarController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela barra de ferramentas (ícones
 * de atalho). Atua como um <b>Humble Object</b> puro, roteando cliques
 * exclusivamente para o {@link Navigator}.
 * </p>
 */
@Component
public class ToolbarController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(ToolbarController.class);
    private static final String LOG_PREFIX = "[ToolbarController]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final Navigator navigator;

    public ToolbarController(Navigator navigator) {
        this.navigator = navigator;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: AÇÕES GLOBAIS
    // ==========================================================================================
    @FXML private void handleNovoContato() {
        log.trace("{} [UI] Usuário clicou no atalho 'Novo Contato'.", LOG_PREFIX);
        navigator.irParaNovoContato();
    }

    // ==========================================================================================
    // MÓDULO 4: NAVEGAÇÃO PRINCIPAL
    // ==========================================================================================
    @FXML private void abrirWorkspace() {
        log.trace("{} [UI] Usuário clicou no atalho 'Dashboard'.", LOG_PREFIX);
        navigator.abrirDashboard();
    }

    @FXML private void abrirPlaybook() {
        log.trace("{} [UI] Usuário clicou no atalho 'Playbook'.", LOG_PREFIX);
        navigator.abrirPlaybook();
    }

    @FXML private void abrirTelaBaseClientes() {
        log.trace("{} [UI] Usuário clicou no atalho 'Clientes'.", LOG_PREFIX);
        navigator.abrirClientes();
    }

    @FXML private void abrirPropostasList() {
        log.trace("{} [UI] Usuário clicou no atalho 'Lista de Propostas'.", LOG_PREFIX);
        navigator.irParaPropostas();
    }

    // ==========================================================================================
    // MÓDULO 5: CONFIGURAÇÕES E AUXILIARES
    // ==========================================================================================
    @FXML private void abrirImportadorTabelas() {
        log.trace("{} [UI] Usuário clicou no atalho 'Importador de Tabelas'.", LOG_PREFIX);
        navigator.irParaImportadorTabelas();
    }

    @FXML private void abrirBancosConvenios() {
        log.trace("{} [UI] Usuário clicou no atalho 'Bancos e Convênios'.", LOG_PREFIX);
        navigator.irParaBancosConvenios();
    }

    @FXML private void abrirTabelasJuros() {
        log.trace("{} [UI] Usuário clicou no atalho 'Tabelas de Juros'.", LOG_PREFIX);
        navigator.irParaTabelasJuros();
    }

    @FXML private void abrirTabelaComissoes() {
        log.trace("{} [UI] Usuário clicou no atalho 'Tabela de Comissões'.", LOG_PREFIX);
        navigator.irParaTabelaComissoes();
    }

    @FXML private void handleLinksUteis() {
        log.trace("{} [UI] Usuário clicou no atalho 'Links Úteis'.", LOG_PREFIX);
        navigator.irParaLinksUteis();
    }

    // ==========================================================================================
    // MÓDULO 6: INTELIGÊNCIA ARTIFICIAL
    // ==========================================================================================
    @FXML public void handleAbrirIA() {
        log.trace("{} [UI] Usuário clicou no atalho 'Abrir IA'.", LOG_PREFIX);
        navigator.alternarPainelIA();
    }

    @FXML public void handleAbrirCopiloto(ActionEvent event) {
        log.trace("{} [UI] Usuário clicou no atalho 'Abrir Copiloto'.", LOG_PREFIX);
        Node source = (Node) event.getSource();
        navigator.abrirCopilotoSimulacao(source);
    }

    // ==========================================================================================
    // MÓDULO 7: SISTEMA
    // ==========================================================================================
    @FXML private void handleLogout() {
        log.info("{} [TELEMETRIA] Usuário solicitou logout via Toolbar.", LOG_PREFIX);
        // Padronizado com o MenuController: O Navigator exibe o overlay de
        // confirmação
        navigator.mostrarOverlaySair();
    }
}
