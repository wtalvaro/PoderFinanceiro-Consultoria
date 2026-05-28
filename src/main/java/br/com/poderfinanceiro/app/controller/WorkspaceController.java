package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;
import br.com.poderfinanceiro.app.facade.IWorkspaceFacade;
import br.com.poderfinanceiro.app.util.Disposable;
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
 * sistema. Atua como um <b>Humble Object</b>, delegando a gestão do estado
 * global (Contexto) para a {@link IWorkspaceFacade}.
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
    @FXML private TabPane tabPanePrincipal;

    public WorkspaceController(ApplicationContext context, IWorkspaceFacade workspaceFacade) {
        this.context = context;
        this.workspaceFacade = workspaceFacade;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando Workspace (Motor de Abas)...", LOG_PREFIX);
        configurarScrollHorizontal();

        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            log.trace("{} [UI] Seleção de aba alterada: {} -> {}", LOG_PREFIX, oldTab != null ? oldTab.getText() : "null",
                    newTab != null ? newTab.getText() : "null");
            sincronizarContextoComIA(newTab);
        });

        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: GESTÃO DE CONTEXTO GLOBAL
    // ==========================================================================================
    private void sincronizarContextoComIA(Tab abaFocada) {
        if (abaFocada == null) {
            workspaceFacade.resetarContextoParaDashboard();
            return;
        }

        Object userData = abaFocada.getUserData();

        if (userData instanceof String idAba) {
            RotaAba rota = RotaAba.fromId(idAba);
            if (rota != null) {
                workspaceFacade.atualizarContextoParaRota(rota);
                return;
            } else if (idAba.startsWith("ABA_")) {
                workspaceFacade.resetarContextoParaDashboard();
                return;
            }
        }

        Object controller = abaFocada.getProperties().get("controller");
        if (controller instanceof AtendimentoHubController hub) {
            if (hub.getLeadController() != null && hub.getLeadController().getViewModel() != null) {
                workspaceFacade.atualizarContextoParaAtendimento(hub.getProponenteComCamposDaTela());
            } else {
                workspaceFacade.atualizarContextoParaAtendimento(null);
            }
        }
    }

    // ==========================================================================================
    // MÓDULO 6: MOTOR DE ABAS (ADMISSÃO)
    // ==========================================================================================
    public void admitirAbaSimples(RotaAba rota, String titulo, String fxmlPath) {
        String id = rota.getId();
        log.info("{} [TELEMETRIA] Solicitando abertura de aba. Rota: {}, Título: '{}'", LOG_PREFIX, rota, titulo);

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (id.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                log.trace("{} [UI] Aba '{}' já existe. Foco transferido.", LOG_PREFIX, titulo);
                return;
            }
        }

        try {
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
                log.trace("{} [UI] Fechando aba '{}'.", LOG_PREFIX, titulo);
                tentarDisposar(controller);
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
            log.info("{} [UI] Nova aba '{}' criada e selecionada.", LOG_PREFIX, titulo);

        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro ao carregar aba '{}' com FXML '{}': {}", LOG_PREFIX, titulo, fxmlPath, e.getMessage());
        }
    }

        public void abrirOuFocarAbaComPropostaEmMemoria(PropostaModel proposta) {
        log.info("{} [TELEMETRIA] Solicitando abertura de proposta em memória (Copiloto).", LOG_PREFIX);
        
        admitirAbaSimples(RotaAba.PROPOSTAS, "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (RotaAba.PROPOSTAS.getId().equals(tab.getUserData())) {
                Object controller = tab.getProperties().get("controller");
                if (controller instanceof EsteiraPropostasController esteira) {
                    // Passa o objeto diretamente para o formulário da esteira
                    esteira.abrirFormularioComPropostaEmMemoria(proposta);
                }
                break;
            }
        }
    }


    // ==========================================================================================
    // MÓDULO 7: ROTAS DIRETAS (ORDENS DE SERVIÇO)
    // ==========================================================================================
    public void abrirAbaDashboard() {
        admitirAbaSimples(RotaAba.DASHBOARD, "📊 Visão Geral", "/fxml/dashboard.fxml");
    }

    public void abrirAbaPlaybook() {
        admitirAbaSimples(RotaAba.fromId("ABA_PLAYBOOK") != null ? RotaAba.fromId("ABA_PLAYBOOK") : RotaAba.DASHBOARD, "📚 Playbook",
                "/fxml/playbook.fxml");
    }

    public void abrirAbaClientes() {
        admitirAbaSimples(RotaAba.CLIENTES, "👥 Clientes", "/fxml/proponente_list.fxml");
    }

    public void abrirAbaLinks() {
        admitirAbaSimples(RotaAba.LINKS, "🔗 Links Úteis", "/fxml/links_uteis.fxml");
    }

    public void abrirAbaTabelasJuros() {
        admitirAbaSimples(RotaAba.JUROS, "📈 Tabelas de Juros", "/fxml/tabelas_juros.fxml");
    }

    public void abrirAbaBancosConvenios() {
        admitirAbaSimples(RotaAba.BANCOS, "🏦 Bancos e Convênios", "/fxml/bancos_convenios.fxml");
    }

    public void abrirAbaComissoes() {
        admitirAbaSimples(RotaAba.COMISSOES, "💰 Gestão de Repasses (RV)", "/fxml/comissoes.fxml");
    }

    public void abrirAbaPropostas(String filtroInicial) {
        admitirAbaSimples(RotaAba.PROPOSTAS, "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");
    }

    public void abrirAbaImportadorTabelas() {
        admitirAbaSimples(RotaAba.IMPORTADOR_TABELAS, "📥 Importador IA", "/fxml/importador_tabelas.fxml");
    }

    // ==========================================================================================
    // MÓDULO 8: ROTEAMENTO COMPLEXO (HUB DE CLIENTE)
    // ==========================================================================================
    public void abrirOuFocarAba(ProponenteModel proponente) {
        String idBuscado = (proponente != null && proponente.getId() != null) ? String.valueOf(proponente.getId())
                : "NOVO_" + UUID.randomUUID().toString();
        log.info("{} [TELEMETRIA] Solicitando abertura de Hub de Atendimento. Cliente ID: {}", LOG_PREFIX, idBuscado);

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (idBuscado.equals(String.valueOf(tab.getUserData()))) {
                tabPanePrincipal.getSelectionModel().select(tab);
                if (tab.getProperties().get("controller") instanceof AtendimentoHubController hubExistente) {
                    hubExistente.inicializarAtendimento(proponente);
                }
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
                log.trace("{} [UI] Tentativa de fechar aba de atendimento ID: {}", LOG_PREFIX, idBuscado);
                event.consume();
                hub.solicitarFechamento(() -> {
                    hub.limparRecursos();
                    tentarDisposar(hub);
                    tabPanePrincipal.getTabs().remove(novaAba);
                    log.info("{} [UI] Aba de atendimento ID {} fechada com sucesso.", LOG_PREFIX, idBuscado);
                });
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);

        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro ao criar aba de atendimento para ID {}: {}", LOG_PREFIX, idBuscado, e.getMessage());
        }
    }

    public void abrirOuFocarAbaComProposta(ProponenteModel proponente, Long propostaIdAlvo) {
        log.info("{} [TELEMETRIA] Solicitando abertura de proposta específica. Proposta ID: {}", LOG_PREFIX, propostaIdAlvo);

        if (propostaIdAlvo != null) {
            // 1. Garante que a aba da Esteira existe
            admitirAbaSimples(RotaAba.PROPOSTAS, "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");

            // 2. Procura a aba e passa a instrução
            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (RotaAba.PROPOSTAS.getId().equals(tab.getUserData())) {
                    Object controller = tab.getProperties().get("controller");
                    if (controller instanceof EsteiraPropostasController esteira) {
                        // Usamos Platform.runLater para dar tempo da UI
                        // respirar
                        javafx.application.Platform.runLater(() -> {
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
    // MÓDULO 9: UTILITÁRIOS DE UI
    // ==========================================================================================
    private void configurarTituloReativoLead(Tab aba, AtendimentoHubController hub) {
        aba.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String nome = hub.getLeadController().getViewModel().nomeProperty().get();
            if (nome == null || nome.trim().isEmpty())
                return "Novo Atendimento";
            return nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;
        }, hub.getLeadController().getViewModel().nomeProperty()));

        Label icone = new Label("👤");
        icone.setStyle("-fx-font-size: 14px;");
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
            disposable.dispose();
            log.trace("{} [SISTEMA] Recurso do controller {} liberado (dispose).", LOG_PREFIX, controller.getClass().getSimpleName());
        }
    }
}
