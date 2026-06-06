package br.com.poderfinanceiro.app.presentation.controller.layout;

import br.com.poderfinanceiro.app.application.facade.IWorkspaceFacade;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.presentation.controller.atendimento.AtendimentoHubController;
import br.com.poderfinanceiro.app.presentation.controller.proposta.EsteiraPropostasController;
import br.com.poderfinanceiro.app.presentation.ui.navigation.enums.AppRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * <h1>WorkspaceController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar as abas (Tabs) do
 * sistema.
 * Atua como um <b>Humble Object</b>, utilizando ValidationUtils para sanidade
 * de rotas
 * e delegando a gestão do contexto global para a {@link IWorkspaceFacade}.
 * </p>
 */
@Component
public class WorkspaceController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);
    private static final String LOG_PREFIX = "[WorkspaceController]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ApplicationContext context;
    private final IWorkspaceFacade workspaceFacade;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private TabPane tabPanePrincipal;

    public WorkspaceController(ApplicationContext context, IWorkspaceFacade workspaceFacade) {
        this.context = context;
        this.workspaceFacade = workspaceFacade;
        log.info("{} [SISTEMA] Motor de Workspace instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando motor de abas e listeners de contexto.", LOG_PREFIX);
        configurarScrollHorizontal();

        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            String titulo = (newTab != null) ? newTab.getText() : "Nenhuma";
            log.trace("{} [TELEMETRIA] Foco de aba alterado para: '{}'", LOG_PREFIX, titulo);
            sincronizarContextoComIA(newTab);
        });

        log.debug("{} [SISTEMA] Inicialização do Workspace concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: GESTÃO DE CONTEXTO GLOBAL (IA COGNITIVA)
    // ==========================================================================================
    private void sincronizarContextoComIA(Tab abaFocada) {
        if (abaFocada == null) {
            log.debug("{} [NEGOCIO] Nenhuma aba ativa. Resetando contexto para Dashboard.", LOG_PREFIX);
            workspaceFacade.resetarContextoParaDashboard();
            return;
        }

        Object userData = abaFocada.getUserData();

        // Sincronização via AppRoute (Single Source of Truth)
        if (userData instanceof String idRota) {
            AppRoute.fromName(idRota).ifPresentOrElse(
                    rota -> {
                        log.trace("{} [NEGOCIO] Sincronizando contexto para rota estática: {}", LOG_PREFIX, rota);
                        workspaceFacade.atualizarContextoParaRota(rota);
                    },
                    () -> {
                        log.debug("{} [NEGOCIO] Aba com ID '{}' não mapeada como rota estática.", LOG_PREFIX, idRota);
                    });
            return;
        }

        Object controller = abaFocada.getProperties().get("controller");
        if (controller instanceof AtendimentoHubController hub) {
            log.trace("{} [NEGOCIO] Sincronizando contexto para atendimento dinâmico.", LOG_PREFIX);
            if (hub.getLeadController() != null && hub.getLeadController().getViewModel() != null) {
                workspaceFacade.atualizarContextoParaAtendimento(hub.getProponenteComCamposDaTela());
            } else {
                workspaceFacade.atualizarContextoParaAtendimento(null);
            }
        }
    }

    // ==========================================================================================
    // MÓDULO 6: MOTOR DE ADMISSÃO DE ABAS
    // ==========================================================================================
    public void admitirAbaSimples(AppRoute rota, String titulo, String fxmlPath) {
        if (!ValidationUtils.isPreenchido(fxmlPath) || !ValidationUtils.isPreenchido(titulo)) {
            log.error("{} [SISTEMA] Falha na admissão: Título ou FXML inválidos.", LOG_PREFIX);
            return;
        }

        String id = rota.name();
        log.info("{} [TELEMETRIA] Solicitando abertura de aba: '{}' (Rota: {})", LOG_PREFIX, titulo, rota);

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (id.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                log.debug("{} [UI] Aba '{}' já existente. Foco transferido.", LOG_PREFIX, titulo);
                return;
            }
        }

        try {
            log.debug("{} [SISTEMA] Carregando novo recurso visual: {}", LOG_PREFIX, fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Tab novaAba = new Tab(titulo);
            novaAba.setContent(root);
            novaAba.setUserData(id);
            novaAba.setClosable(true);

            Object controller = loader.getController();
            novaAba.getProperties().put("controller", controller);

            novaAba.setOnCloseRequest(event -> {
                log.info("{} [SISTEMA] Encerrando aba '{}' e liberando recursos.", LOG_PREFIX, titulo);
                tentarDisposar(controller);
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
            log.info("{} [AUDITORIA] Aba '{}' admitida com sucesso no Workspace.", LOG_PREFIX, titulo);

        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro crítico ao carregar aba '{}': {}", LOG_PREFIX, titulo, e.getMessage());
        }
    }

    public void abrirOuFocarAbaComPropostaEmMemoria(PropostaModel proposta) {
        log.info("{} [TELEMETRIA] Orquestrando abertura de proposta volátil (Copiloto).", LOG_PREFIX);

        admitirAbaSimples(AppRoute.ESTEIRA_PROPOSTAS, "📄 Esteira de Propostas",
                AppRoute.ESTEIRA_PROPOSTAS.getFxmlPath());

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (AppRoute.ESTEIRA_PROPOSTAS.name().equals(tab.getUserData())) {
                Object controller = tab.getProperties().get("controller");
                if (controller instanceof EsteiraPropostasController esteira) {
                    log.debug("{} [NEGOCIO] Injetando proposta em memória no controlador da esteira.", LOG_PREFIX);
                    esteira.abrirFormularioComPropostaEmMemoria(proposta);
                }
                break;
            }
        }
    }

    // ==========================================================================================
    // MÓDULO 7: ROTAS DIRETAS
    // ==========================================================================================
    public void abrirAbaDashboard() {
        admitirAbaSimples(AppRoute.DASHBOARD, "📊 Visão Geral", AppRoute.DASHBOARD.getFxmlPath());
    }

    public void abrirAbaClientes() {
        admitirAbaSimples(AppRoute.CLIENTES, "👥 Clientes", AppRoute.CLIENTES.getFxmlPath());
    }

    public void abrirAbaLinks() {
        admitirAbaSimples(AppRoute.LINKS_UTEIS, "🔗 Links Úteis", AppRoute.LINKS_UTEIS.getFxmlPath());
    }

    public void abrirAbaTabelasJuros() {
        admitirAbaSimples(AppRoute.TABELAS_JUROS, "📈 Tabelas de Juros", AppRoute.TABELAS_JUROS.getFxmlPath());
    }

    public void abrirAbaBancosConvenios() {
        admitirAbaSimples(AppRoute.BANCOS_CONVENIOS, "🏦 Bancos e Convênios", AppRoute.BANCOS_CONVENIOS.getFxmlPath());
    }

    public void abrirAbaComissoes() {
        admitirAbaSimples(AppRoute.COMISSOES, "💰 Gestão de Repasses (RV)", AppRoute.COMISSOES.getFxmlPath());
    }

    public void abrirAbaImportadorTabelas() {
        admitirAbaSimples(AppRoute.IMPORTADOR_TABELAS, "📥 Importador IA", AppRoute.IMPORTADOR_TABELAS.getFxmlPath());
    }

    public void abrirAbaPropostas(String filtroInicial) {
        admitirAbaSimples(AppRoute.ESTEIRA_PROPOSTAS, "📄 Esteira de Propostas",
                AppRoute.ESTEIRA_PROPOSTAS.getFxmlPath());
    }

    public void abrirAbaPlaybook() {
        admitirAbaSimples(AppRoute.PLAYBOOK, "📚 Playbook", AppRoute.PLAYBOOK.getFxmlPath());
    }

    // ==========================================================================================
    // MÓDULO 8: ROTEAMENTO DE ATENDIMENTO (HUB)
    // ==========================================================================================
    public void abrirOuFocarAba(ProponenteModel proponente) {
        String idBuscado = (proponente != null && proponente.getId() != null)
                ? String.valueOf(proponente.getId())
                : "NOVO_" + UUID.randomUUID().toString();

        log.info("{} [TELEMETRIA] Solicitando Hub de Atendimento para ID: {}", LOG_PREFIX, idBuscado);

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (idBuscado.equals(String.valueOf(tab.getUserData()))) {
                tabPanePrincipal.getSelectionModel().select(tab);
                if (tab.getProperties().get("controller") instanceof AtendimentoHubController hubExistente) {
                    hubExistente.inicializarAtendimento(proponente);
                }
                log.debug("{} [UI] Hub de atendimento já aberto. Foco transferido.", LOG_PREFIX);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            AtendimentoHubController hub = loader.getController();

            if (proponente != null && proponente.getId() != null) {
                hub.inicializarAtendimento(proponente);
            } else {
                hub.prepararNovoAtendimento();
            }

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setUserData(idBuscado);
            novaAba.getProperties().put("controller", hub);
            hub.setTabPertencente(novaAba);
            configurarTituloReativoLead(novaAba, hub);

            novaAba.setOnCloseRequest(event -> {
                log.debug("{} [SISTEMA] Solicitando fechamento seguro da aba de atendimento.", LOG_PREFIX);
                event.consume();
                hub.solicitarFechamento(() -> {
                    hub.limparRecursos();
                    tentarDisposar(hub);
                    tabPanePrincipal.getTabs().remove(novaAba);
                    log.info("{} [AUDITORIA] Aba de atendimento ID {} encerrada.", LOG_PREFIX, idBuscado);
                });
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);

        } catch (IOException e) {
            log.error("{} [SISTEMA] Falha ao criar Hub de Atendimento: {}", LOG_PREFIX, e.getMessage());
        }
    }

    public void abrirOuFocarAbaComProposta(ProponenteModel proponente, Long propostaIdAlvo) {
        log.info("{} [TELEMETRIA] Roteando para proposta específica ID: {}", LOG_PREFIX, propostaIdAlvo);

        if (propostaIdAlvo != null) {
            admitirAbaSimples(AppRoute.ESTEIRA_PROPOSTAS, "📄 Esteira de Propostas",
                    AppRoute.ESTEIRA_PROPOSTAS.getFxmlPath());

            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (AppRoute.ESTEIRA_PROPOSTAS.name().equals(tab.getUserData())) {
                    Object controller = tab.getProperties().get("controller");
                    if (controller instanceof EsteiraPropostasController esteira) {
                        Platform.runLater(() -> {
                            log.debug("{} [NEGOCIO] Selecionando proposta ID {} na esteira.", LOG_PREFIX,
                                    propostaIdAlvo);
                            esteira.selecionarPropostaPorId(propostaIdAlvo);
                        });
                    }
                    break;
                }
            }
        } else {
            abrirOuFocarAba(proponente);
        }
    }

    // ==========================================================================================
    // MÓDULO 9: UTILITÁRIOS DE INTERFACE
    // ==========================================================================================
    private void configurarTituloReativoLead(Tab aba, AtendimentoHubController hub) {
        aba.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String nome = hub.getLeadController().getViewModel().nomeProperty().get();
            if (!ValidationUtils.isPreenchido(nome))
                return "Novo Atendimento";
            return nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;
        }, hub.getLeadController().getViewModel().nomeProperty()));

        Label icone = new Label("👤");
        icone.setStyle("-fx-font-size: 14px; -fx-padding: 0 0 0 5;");
        aba.setGraphic(icone);
    }

    private void configurarScrollHorizontal() {
        tabPanePrincipal.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null)
                aplicarScrollNoHeader();
        });
        if (tabPanePrincipal.getSkin() != null)
            aplicarScrollNoHeader();
    }

    private void aplicarScrollNoHeader() {
        tabPanePrincipal.applyCss();
        tabPanePrincipal.layout();

        Node header = tabPanePrincipal.lookup(".tab-header-area");
        if (header != null) {
            header.setOnScroll(event -> {
                if (event.getDeltaY() < 0 || event.getDeltaX() < 0)
                    tabPanePrincipal.getSelectionModel().selectNext();
                else
                    tabPanePrincipal.getSelectionModel().selectPrevious();
                event.consume();
            });
        }
    }

    private void tentarDisposar(Object controller) {
        if (controller instanceof Disposable disposable) {
            log.info("{} [SISTEMA] Invocando limpeza de recursos para: {}", LOG_PREFIX,
                    controller.getClass().getSimpleName());
            disposable.dispose();
        }
    }
}
