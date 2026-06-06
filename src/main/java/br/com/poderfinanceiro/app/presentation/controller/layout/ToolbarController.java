package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.presentation.ui.navigation.AppRoute;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <h1>ToolbarController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela barra de ferramentas (ícones
 * de atalho).
 * Refatorado para utilizar o padrão Registry (AppRoute) via Navigator.
 * Atua como um <b>Humble Object</b> puro.
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
        log.debug("{} [SISTEMA] Controlador de Toolbar instanciado e sincronizado com Navigator.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: AÇÕES GLOBAIS
    // ==========================================================================================
    @FXML
    private void handleNovoContato() {
        log.trace("{} [UI] Atalho acionado: Novo Contato.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.CADASTRO_PROPONENTE);
    }

    // ==========================================================================================
    // MÓDULO 4: NAVEGAÇÃO PRINCIPAL
    // ==========================================================================================
    @FXML
    private void abrirWorkspace() {
        log.trace("{} [UI] Atalho acionado: Dashboard.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.DASHBOARD);
    }

    @FXML
    private void abrirPlaybook() {
        log.trace("{} [UI] Atalho acionado: Playbook.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.PLAYBOOK);
    }

    @FXML
    private void abrirTelaBaseClientes() {
        log.trace("{} [UI] Atalho acionado: Listagem de Clientes.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.CLIENTES);
    }

    @FXML
    private void abrirPropostasList() {
        log.trace("{} [UI] Atalho acionado: Esteira de Propostas.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.ESTEIRA_PROPOSTAS);
    }

    // ==========================================================================================
    // MÓDULO 5: CONFIGURAÇÕES E AUXILIARES
    // ==========================================================================================
    @FXML
    private void abrirImportadorTabelas() {
        log.trace("{} [UI] Atalho acionado: Importador de Tabelas.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.IMPORTADOR_TABELAS);
    }

    @FXML
    private void abrirBancosConvenios() {
        log.trace("{} [UI] Atalho acionado: Bancos e Convênios.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.BANCOS_CONVENIOS);
    }

    @FXML
    private void abrirTabelasJuros() {
        log.trace("{} [UI] Atalho acionado: Tabelas de Juros.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.TABELAS_JUROS);
    }

    @FXML
    private void abrirTabelaComissoes() {
        log.trace("{} [UI] Atalho acionado: Gestão de Comissões.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.COMISSOES);
    }

    @FXML
    private void handleLinksUteis() {
        log.trace("{} [UI] Atalho acionado: Links Úteis.", LOG_PREFIX);
        navigator.navegarPara(AppRoute.LINKS_UTEIS);
    }

    // ==========================================================================================
    // MÓDULO 6: INTELIGÊNCIA ARTIFICIAL
    // ==========================================================================================
    @FXML
    public void handleAbrirIA() {
        log.trace("{} [UI] Atalho acionado: Alternar Painel IA.", LOG_PREFIX);
        navigator.alternarPainelIA();
    }

    @FXML
    public void handleAbrirCopiloto(ActionEvent event) {
        log.trace("{} [UI] Atalho acionado: Copiloto de Simulação.", LOG_PREFIX);
        Node source = (Node) event.getSource();
        // Mantemos a chamada específica pois o Copiloto pode exigir o Node para
        // posicionamento de PopOver
        navigator.abrirCopilotoSimulacao(source);
    }

    // ==========================================================================================
    // MÓDULO 7: SISTEMA
    // ==========================================================================================
    @FXML
    private void handleLogout() {
        log.info("{} [TELEMETRIA] Usuário solicitou logout via Toolbar.", LOG_PREFIX);
        navigator.mostrarOverlaySair();
    }
}
