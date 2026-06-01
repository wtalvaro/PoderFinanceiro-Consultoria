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
 * Controlador de Interface (UI) responsável pela barra de menus superior.
 * Atua como um <b>Humble Object</b>, roteando cliques para o {@link Navigator}
 * e gerenciando o ciclo de vida de atualizações de forma resiliente.
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
        log.info("{} [SISTEMA] Controlador de Menu instanciado via Spring.", LOG_PREFIX);
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
    // MÓDULO 7: AUTO-UPDATE (RESILIENTE)
    // ==========================================================================================
    @FXML
    private void handleVerificarAtualizacoes() {
        log.info("{} [TELEMETRIA] Iniciando verificação manual de atualizações.", LOG_PREFIX);
        navigator.mostrarLoading("Buscando atualizações no servidor...");

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Consultando última release via Facade.", LOG_PREFIX);
                    return menuFacade.checarNovaVersao();
                },
                novaTag -> {
                    navigator.ocultarLoading();
                    if (novaTag != null) {
                        log.info("{} [NEGOCIO] Nova versão detectada: {}", LOG_PREFIX, novaTag);
                        navigator.solicitarConfirmacao(
                                "🎉 Nova Atualização Encontrada",
                                "A versão " + novaTag + " está disponível. O download será iniciado em segundo plano.",
                                "Baixar Agora",
                                "-color-success-emphasis",
                                () -> iniciarProcessoDeDownload(novaTag));
                    } else {
                        log.info("{} [NEGOCIO] O sistema já está na versão mais recente.", LOG_PREFIX);
                        navigator.notificarSucesso("Você já possui a versão mais recente instalada.");
                    }
                },
                erro -> {
                    log.error("{} [SISTEMA] Falha ao verificar atualizações: {}", LOG_PREFIX, erro.getMessage());
                    navigator.ocultarLoading();
                    navigator.notificarAviso("Não foi possível verificar atualizações: " + erro.getMessage());
                });
    }

    private void iniciarProcessoDeDownload(String tag) {
        log.info("{} [TELEMETRIA] Iniciando download da atualização v{}.", LOG_PREFIX, tag);
        navigator.mostrarLoading("Baixando atualização...\nO sistema será preparado para o reinício.");

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando download do binário JAR.", LOG_PREFIX);
                    menuFacade.baixarEExecutarAtualizacao(tag);
                    return true;
                },
                sucesso -> {
                    // FIX: Oculta o overlay antes de solicitar o reinício
                    navigator.ocultarLoading();
                    log.info("{} [AUDITORIA] Download concluído com sucesso. Solicitando reinício.", LOG_PREFIX);

                    navigator.solicitarConfirmacao(
                            "🚀 Download Concluído",
                            "A atualização v" + tag
                                    + " foi baixada. Deseja reiniciar o sistema agora para aplicar as mudanças?",
                            "Reiniciar Agora",
                            "-color-success-emphasis",
                            () -> {
                                log.warn("{} [SISTEMA] Encerrando aplicação para Hot Swap (Windows 11).", LOG_PREFIX);
                                Platform.exit();
                                System.exit(0); // Essencial para liberar o lock do arquivo no Windows
                            });
                },
                erro -> {
                    log.error("{} [SISTEMA] Erro crítico no download da atualização: {}", LOG_PREFIX,
                            erro.getMessage());
                    navigator.ocultarLoading();
                    navigator.notificarAviso("Falha no download: " + erro.getMessage());
                });
    }
}
