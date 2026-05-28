package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.application.facade.IMenuFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <h1>MenuController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela barra de menus superior. Atua
 * como um <b>Humble Object</b>, roteando cliques para o {@link Navigator} e
 * delegando a lógica de atualização para a {@link IMenuFacade}.
 * </p>
 */
@Component
public class MenuController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(MenuController.class);
    private static final String LOG_PREFIX = "[MenuController]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final Navigator navigator;
    private final IMenuFacade menuFacade;

    public MenuController(Navigator navigator, IMenuFacade menuFacade) {
        this.navigator = navigator;
        this.menuFacade = menuFacade;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: GRUPO PODER FINANCEIRO (Geral & Sistema)
    // ==========================================================================================
    @FXML private void handleDashboard() {
        log.trace("{} [UI] Usuário clicou em 'Dashboard'.", LOG_PREFIX);
        navigator.abrirDashboard();
    }

    @FXML private void handleNovoContato() {
        log.trace("{} [UI] Usuário clicou em 'Novo Contato'.", LOG_PREFIX);
        navigator.irParaNovoContato();
    }

    @FXML private void handleLogout() {
        log.info("{} [TELEMETRIA] Usuário solicitou Logout via Menu.", LOG_PREFIX);
        navigator.mostrarOverlaySair();
    }

    @FXML private void handleSair() {
        log.warn("{} [TELEMETRIA] Usuário solicitou encerramento forçado da aplicação.", LOG_PREFIX);
        Platform.exit();
        System.exit(0);
    }

    // ==========================================================================================
    // MÓDULO 4: GRUPO OPERACIONAL (Esteira de Vendas)
    // ==========================================================================================
    @FXML private void handleClientes() {
        log.trace("{} [UI] Usuário clicou em 'Clientes'.", LOG_PREFIX);
        navigator.abrirClientes();
    }

    @FXML private void handlePropostas() {
        log.trace("{} [UI] Usuário clicou em 'Propostas'.", LOG_PREFIX);
        navigator.irParaPropostas();
    }

    // ==========================================================================================
    // MÓDULO 5: GRUPO FINANCEIRO
    // ==========================================================================================
    @FXML private void handleComissoes() {
        log.trace("{} [UI] Usuário clicou em 'Comissões'.", LOG_PREFIX);
        navigator.irParaTabelaComissoes();
    }

    @FXML private void handleJuros() {
        log.trace("{} [UI] Usuário clicou em 'Juros'.", LOG_PREFIX);
        navigator.irParaTabelasJuros();
    }

    @FXML private void handleImportarTabelas() {
        log.trace("{} [UI] Usuário clicou em 'Importar Tabelas'.", LOG_PREFIX);
        navigator.irParaImportadorTabelas();
    }

    // ==========================================================================================
    // MÓDULO 6: GRUPO FERRAMENTAS & SUPORTE
    // ==========================================================================================
    @FXML private void handlePlaybook() {
        log.trace("{} [UI] Usuário clicou em 'Playbook'.", LOG_PREFIX);
        navigator.abrirPlaybook();
    }

    @FXML private void handleBancos() {
        log.trace("{} [UI] Usuário clicou em 'Bancos e Convênios'.", LOG_PREFIX);
        navigator.irParaBancosConvenios();
    }

    @FXML private void handleLinks() {
        log.trace("{} [UI] Usuário clicou em 'Links Úteis'.", LOG_PREFIX);
        navigator.irParaLinksUteis();
    }

    @FXML private void handleLimparCache() {
        log.info("{} [TELEMETRIA] Usuário solicitou limpeza de cache de telas.", LOG_PREFIX);
        navigator.limparCacheDeTelas();
    }

    @FXML public void handleAbrirIA() {
        log.trace("{} [UI] Usuário clicou em 'Abrir IA'.", LOG_PREFIX);
        navigator.alternarPainelIA();
    }

    @FXML private void handleAbrirCopiloto() {
        log.trace("{} [UI] Usuário clicou em 'Abrir Copiloto de Vendas'.", LOG_PREFIX);
        navigator.abrirCopilotoSimulacao(null);
    }

    // ==========================================================================================
    // MÓDULO 7: AUTO-UPDATE
    // ==========================================================================================
    @FXML private void handleVerificarAtualizacoes() {
        log.info("{} [TELEMETRIA] Usuário solicitou verificação de atualizações.", LOG_PREFIX);
        navigator.mostrarLoading("Buscando atualizações...");

        AsyncUtils.executarTaskAsync(menuFacade::checarNovaVersao, novaTag -> {
            navigator.ocultarLoading();
            if (novaTag != null) {
                navigator.solicitarConfirmacao("🎉 Nova Atualização Encontrada", "A versão " + novaTag
                        + " está disponível e pronta para ser instalada. A aplicação precisará ser reiniciada.\n\nDeseja atualizar agora?",
                        "Atualizar Agora", "#00a884", () -> iniciarProcessoDeDownload(novaTag));
            } else {
                navigator.notificarSucesso("Você já possui a versão mais recente instalada.");
            }
        }, erro -> {
            log.error("{} [SISTEMA] Erro ao verificar atualizações: {}", LOG_PREFIX, erro.getMessage());
            navigator.ocultarLoading();
            navigator.notificarAviso(erro.getMessage());
        });
    }

    private void iniciarProcessoDeDownload(String tag) {
        log.warn("{} [AUDITORIA] Iniciando processo de download da versão: {}", LOG_PREFIX, tag);
        navigator.mostrarLoading("Baixando atualização...\nPor favor, não feche o sistema.");

        AsyncUtils.executarTaskAsync(() -> {
            menuFacade.baixarEExecutarAtualizacao(tag);
            return null;
        }, sucesso -> {
            // O app será morto pelo UpdateService antes de chegar aqui
        }, erro -> {
            log.error("{} [SISTEMA] Erro crítico durante o download da atualização: {}", LOG_PREFIX, erro.getMessage());
            navigator.ocultarLoading();
            navigator.notificarAviso(erro.getMessage());
        });
    }
}
