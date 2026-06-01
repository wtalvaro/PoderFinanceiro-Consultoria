package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.domain.service.UpdateService;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <h1>MenuController</h1>
 * <p>
 * Controlador de Interface responsável pela barra de menus superior.
 * Gerencia a navegação e o fluxo de atualização manual assistida via navegador.
 * Implementa o padrão <b>Humble Object</b>.
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
    private final UpdateService updateService;
    private final HostServices hostServices;

    public MenuController(Navigator navigator,
            UpdateService updateService,
            HostServices hostServices) {
        this.navigator = navigator;
        this.updateService = updateService;
        this.hostServices = hostServices;
        log.info("{} [SISTEMA] Controlador de Menu instanciado com suporte a HostServices.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: GRUPO PODER FINANCEIRO (Geral & Sistema)
    // ==========================================================================================
    @FXML
    private void handleDashboard() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Dashboard.", LOG_PREFIX);
        navigator.abrirDashboard();
    }

    @FXML
    private void handleNovoContato() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Novo Contato.", LOG_PREFIX);
        navigator.irParaNovoContato();
    }

    @FXML
    private void handleLogout() {
        log.info("{} [TELEMETRIA] Usuário solicitou Logout.", LOG_PREFIX);
        navigator.mostrarOverlaySair();
    }

    @FXML
    private void handleSair() {
        log.warn("{} [SISTEMA] Encerramento da aplicação solicitado pelo usuário.", LOG_PREFIX);
        Platform.exit();
        System.exit(0);
    }

    // ==========================================================================================
    // MÓDULO 4: GRUPO OPERACIONAL (Esteira de Vendas)
    // ==========================================================================================
    @FXML
    private void handleClientes() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Listagem de Clientes.", LOG_PREFIX);
        navigator.abrirClientes();
    }

    @FXML
    private void handlePropostas() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Esteira de Propostas.", LOG_PREFIX);
        navigator.irParaPropostas();
    }

    // ==========================================================================================
    // MÓDULO 5: GRUPO FINANCEIRO
    // ==========================================================================================
    @FXML
    private void handleComissoes() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Gestão de Comissões.", LOG_PREFIX);
        navigator.irParaTabelaComissoes();
    }

    @FXML
    private void handleJuros() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Tabelas de Juros.", LOG_PREFIX);
        navigator.irParaTabelasJuros();
    }

    @FXML
    private void handleImportarTabelas() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Importador de Tabelas.", LOG_PREFIX);
        navigator.irParaImportadorTabelas();
    }

    // ==========================================================================================
    // MÓDULO 6: FERRAMENTAS & SUPORTE
    // ==========================================================================================
    @FXML
    private void handlePlaybook() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Playbook Cognitivo.", LOG_PREFIX);
        navigator.abrirPlaybook();
    }

    @FXML
    private void handleBancos() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Mural de Bancos.", LOG_PREFIX);
        navigator.irParaBancosConvenios();
    }

    @FXML
    private void handleLinks() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Links Úteis.", LOG_PREFIX);
        navigator.irParaLinksUteis();
    }

    @FXML
    private void handleLimparCache() {
        log.info("{} [SISTEMA] Solicitação de limpeza de cache de visualização.", LOG_PREFIX);
        navigator.limparCacheDeTelas();
    }

    @FXML
    public void handleAbrirIA() {
        log.trace("{} [TELEMETRIA] Alternando visibilidade do painel IA.", LOG_PREFIX);
        navigator.alternarPainelIA();
    }

    @FXML
    private void handleAbrirCopiloto() {
        log.trace("{} [TELEMETRIA] Navegação solicitada: Copiloto de Simulação.", LOG_PREFIX);
        navigator.abrirCopilotoSimulacao(null);
    }

    // ==========================================================================================
    // MÓDULO 7: AUTO-UPDATE (MODO MANUAL ASSISTIDO)
    // ==========================================================================================
    @FXML
    private void handleVerificarAtualizacoes() {
        log.info("{} [TELEMETRIA] Iniciando verificação manual de atualizações.", LOG_PREFIX);
        navigator.mostrarLoading("Verificando novidades no servidor...");

        AsyncUtils.executarTaskAsync(
                updateService::checarNovaVersao,
                optRelease -> {
                    navigator.ocultarLoading();
                    if (optRelease.isPresent()) {
                        GitHubReleaseDTO release = optRelease.get();
                        log.info("{} [NEGOCIO] Nova versão detectada: {}", LOG_PREFIX, release.tagName());

                        navigator.solicitarConfirmacao(
                                "🚀 Nova Versão Disponível",
                                "A versão " + release.tagName()
                                        + " foi lançada com melhorias!\n\nDeseja abrir a página de download para atualizar manualmente?",
                                "Ir para Download",
                                "-color-success-emphasis",
                                () -> {
                                    log.info("{} [TELEMETRIA] Redirecionando usuário para o GitHub Releases.",
                                            LOG_PREFIX);
                                    hostServices.showDocument(
                                            "https://github.com/wtalvaro/PoderFinanceiro-Consultoria/releases/latest");
                                });
                    } else {
                        log.info("{} [NEGOCIO] Sistema já está na versão mais recente.", LOG_PREFIX);
                        navigator.notificarSucesso("Você já possui a versão mais recente instalada.");
                    }
                },
                erro -> {
                    log.error("{} [SISTEMA] Falha na checagem de versão: {}", LOG_PREFIX, erro.getMessage());
                    navigator.ocultarLoading();
                    navigator.notificarAviso("Não foi possível conectar ao servidor de atualizações.");
                });
    }
}
