package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.TipoTelaFocada;
import br.com.poderfinanceiro.app.util.Disposable;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WorkspaceController {

    @FXML
    private TabPane tabPanePrincipal;

    private final ApplicationContext context;
    private final AtendimentoContextService contextoService;

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    public WorkspaceController(ApplicationContext context, AtendimentoContextService contextoService) {
        this.context = context;
        this.contextoService = contextoService;
        log.debug("[WORKSPACE] Construtor: Controller instanciado");
    }

    @FXML
    public void initialize() {
        log.debug("[WORKSPACE] initialize: Configurando abas e eventos");
        configurarScrollHorizontal();

        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            log.debug("[WORKSPACE] Seleção de aba alterada: {} -> {}",
                    oldTab != null ? oldTab.getText() : "null",
                    newTab != null ? newTab.getText() : "null");
            sincronizarContextoComIA(newTab);
        });
        log.info("[WORKSPACE] initialize: Workspace configurado com sucesso");
    }

    private void sincronizarContextoComIA(Tab abaFocada) {
        log.debug("[WORKSPACE] sincronizarContextoComIA: Focando aba '{}'",
                abaFocada != null ? abaFocada.getText() : "null");
        if (abaFocada == null) {
            contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
            log.trace("[WORKSPACE] Nenhuma aba focada, contexto resetado para DASHBOARD");
            return;
        }

        Object userData = abaFocada.getUserData();
        log.trace("[WORKSPACE] userData da aba: {}", userData);

        if (userData instanceof String idAba) {
            RotaAba rota = RotaAba.fromId(idAba);

            if (rota != null && rota.getTipoTelaFocada() != null) {
                if (rota == RotaAba.PROPOSTAS) {
                    contextoService.setTelaAtualFocada(rota.getTipoTelaFocada());
                    log.debug("[WORKSPACE] Contexto atualizado: tela focada = PROPOSTAS (sem limpar proponente)");
                } else {
                    contextoService.atualizarFocoInterface(null, rota.getTipoTelaFocada());
                    log.debug("[WORKSPACE] Contexto atualizado: tela focada = {}, proponente limpo",
                            rota.getTipoTelaFocada());
                }
                return;
            } else if (idAba.startsWith("ABA_")) {
                contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
                log.debug("[WORKSPACE] Aba genérica (ABA_*), contexto resetado para DASHBOARD");
                return;
            }
        }

        Object controller = abaFocada.getProperties().get("controller");
        if (controller instanceof AtendimentoHubController hub) {
            if (hub.getLeadController() != null && hub.getLeadController().getViewModel() != null) {
                contextoService.atualizarFocoInterface(hub.getProponenteComCamposDaTela(),
                        TipoTelaFocada.CADASTRO_CLIENTE);
                log.debug("[WORKSPACE] Contexto atualizado: CADASTRO_CLIENTE com proponente ativo");
            } else {
                contextoService.atualizarFocoInterface(null, TipoTelaFocada.CADASTRO_CLIENTE);
                log.debug("[WORKSPACE] Contexto atualizado: CADASTRO_CLIENTE sem proponente");
            }
        } else {
            log.trace("[WORKSPACE] Controller da aba não é do tipo esperado, nenhuma ação tomada");
        }
    }

    // ========================================================================
    // PROTOCOLO DE ADMISSÃO (Motor de Abas)
    // ========================================================================

    public void admitirAbaSimples(RotaAba rota, String titulo, String fxmlPath) {
        String id = rota.getId();
        log.debug("[WORKSPACE] admitirAbaSimples: rota={}, titulo='{}'", rota, titulo);

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (id.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                log.debug("[WORKSPACE] Aba '{}' já existe, apenas selecionada", titulo);
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
                log.debug("[WORKSPACE] Fechando aba '{}' (rota={})", titulo, rota);
                tentarDisposar(controller);
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
            log.info("[WORKSPACE] Nova aba '{}' criada e selecionada (rota={})", titulo, rota);
        } catch (IOException e) {
            log.error("[WORKSPACE] Erro ao carregar aba '{}' com FXML '{}'", titulo, fxmlPath, e);
        }
    }

    // ========================================================================
    // MÉTODOS PÚBLICOS (Ordens de Serviço)
    // ========================================================================

    public void abrirAbaDashboard() {
        log.info("[WORKSPACE] abrirAbaDashboard: Solicitado");
        admitirAbaSimples(RotaAba.DASHBOARD, "📊 Visão Geral", "/fxml/dashboard.fxml");
    }

    public void abrirAbaPlaybook() {
        log.info("[WORKSPACE] abrirAbaPlaybook: Solicitado");
        admitirAbaSimples(RotaAba.fromId("ABA_PLAYBOOK") != null ? RotaAba.fromId("ABA_PLAYBOOK") : RotaAba.DASHBOARD,
                "📚 Playbook", "/fxml/playbook.fxml");
    }

    public void abrirAbaClientes() {
        log.info("[WORKSPACE] abrirAbaClientes: Solicitado");
        admitirAbaSimples(RotaAba.CLIENTES, "👥 Clientes", "/fxml/proponente_list.fxml");
    }

    public void abrirAbaLinks() {
        log.info("[WORKSPACE] abrirAbaLinks: Solicitado");
        admitirAbaSimples(RotaAba.LINKS, "🔗 Links Úteis", "/fxml/links_uteis.fxml");
    }

    public void abrirAbaTabelasJuros() {
        log.info("[WORKSPACE] abrirAbaTabelasJuros: Solicitado");
        admitirAbaSimples(RotaAba.JUROS, "📈 Tabelas de Juros", "/fxml/tabelas_juros.fxml");
    }

    public void abrirAbaBancosConvenios() {
        log.info("[WORKSPACE] abrirAbaBancosConvenios: Solicitado");
        admitirAbaSimples(RotaAba.BANCOS, "🏦 Bancos e Convênios", "/fxml/bancos_convenios.fxml");
    }

    public void abrirAbaComissoes() {
        log.info("[WORKSPACE] abrirAbaComissoes: Solicitado");
        admitirAbaSimples(RotaAba.COMISSOES, "💰 Gestão de Repasses (RV)", "/fxml/comissoes.fxml");
    }

    public void abrirAbaPropostas(String filtroInicial) {
        log.info("[WORKSPACE] abrirAbaPropostas: Solicitado (filtro='{}')", filtroInicial);
        admitirAbaSimples(RotaAba.PROPOSTAS, "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");
    }

    public void abrirAbaImportadorTabelas() {
        log.info("[WORKSPACE] abrirAbaImportadorTabelas: Solicitado");
        admitirAbaSimples(RotaAba.IMPORTADOR_TABELAS, "📥 Importador IA", "/fxml/importador_tabelas.fxml");
    }

    // =====================================================================
    // ROTEAMENTO: HUB DE CLIENTE
    // =====================================================================
    public void abrirOuFocarAba(ProponenteModel proponente) {
        String idBuscado = (proponente != null && proponente.getId() != null) ? String.valueOf(proponente.getId())
                : "NOVO_" + UUID.randomUUID().toString();
        log.info("[WORKSPACE] abrirOuFocarAba: Cliente '{}' (ID={})",
                proponente != null ? proponente.getNomeCompleto() : "novo", idBuscado);

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (idBuscado.equals(String.valueOf(tab.getUserData()))) {
                tabPanePrincipal.getSelectionModel().select(tab);
                log.debug("[WORKSPACE] Aba existente encontrada e selecionada para ID={}", idBuscado);
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
                log.debug("[WORKSPACE] AtendimentoHub inicializado com cliente existente ID={}", proponente.getId());
            } else {
                hub.prepararNovoAtendimento();
                log.debug("[WORKSPACE] AtendimentoHub preparado para novo atendimento");
            }

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setUserData(idBuscado);
            novaAba.getProperties().put("controller", hub);
            hub.setTabPertencente(novaAba);
            configurarTituloReativoLead(novaAba, hub);

            novaAba.setOnCloseRequest(event -> {
                log.debug("[WORKSPACE] Tentativa de fechar aba de atendimento ID={}", idBuscado);
                if (hub.temAlteracoesNaoSalvas()) {
                    log.warn("[WORKSPACE] Aba com alterações não salvas, fechamento bloqueado até confirmação");
                    event.consume();
                    hub.solicitarFechamento(() -> {
                        hub.limparRecursos();
                        tentarDisposar(hub);
                        tabPanePrincipal.getTabs().remove(novaAba);
                        log.info("[WORKSPACE] Aba de atendimento ID={} fechada após confirmação", idBuscado);
                    });
                } else {
                    hub.limparRecursos();
                    tentarDisposar(hub);
                    log.info("[WORKSPACE] Aba de atendimento ID={} fechada (sem pendências)", idBuscado);
                }
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
            log.info("[WORKSPACE] Nova aba de atendimento criada e selecionada para ID={}", idBuscado);
        } catch (IOException e) {
            log.error("[WORKSPACE] Erro ao criar aba de atendimento para ID={}", idBuscado, e);
        }
    }

    // DEPOIS:
    public void abrirOuFocarAbaComProposta(ProponenteModel proponente, Long propostaIdAlvo) {
        log.info("[WORKSPACE] abrirOuFocarAbaComProposta: proponente={}, propostaId={}",
                proponente != null ? proponente.getNomeCompleto() : "null", propostaIdAlvo);
        if (propostaIdAlvo != null) {
            admitirAbaSimples(RotaAba.PROPOSTAS, "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");
            log.debug("[WORKSPACE] Esteira de propostas garantida, buscando aba para selecionar proposta ID={}",
                    propostaIdAlvo);

            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (RotaAba.PROPOSTAS.getId().equals(tab.getUserData())) {
                    Object controller = tab.getProperties().get("controller");
                    if (controller instanceof EsteiraPropostasController esteira) {
                        esteira.selecionarPropostaPorId(propostaIdAlvo);
                        log.debug("[WORKSPACE] Proposta ID={} solicitada à esteira", propostaIdAlvo);
                    }
                    break;
                }
            }
        } else {
            log.debug("[WORKSPACE] Nenhuma proposta alvo, abrindo apenas aba de cliente");
            abrirOuFocarAba(proponente);
        }
    }

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
        log.trace("[WORKSPACE] Título reativo configurado para aba de atendimento");
    }

    private void configurarScrollHorizontal() {
        log.debug("[WORKSPACE] configurarScrollHorizontal: Aplicando rolagem horizontal com scroll do mouse");
        Platform.runLater(() -> {
            Node header = tabPanePrincipal.lookup(".tab-header-area");
            if (header != null) {
                header.setOnScroll(event -> {
                    if (event.getDeltaY() < 0 || event.getDeltaX() < 0)
                        tabPanePrincipal.getSelectionModel().selectNext();
                    else
                        tabPanePrincipal.getSelectionModel().selectPrevious();
                    event.consume();
                });
                log.trace("[WORKSPACE] Scroll horizontal configurado");
            } else {
                log.warn("[WORKSPACE] Não foi possível localizar .tab-header-area para configurar scroll horizontal");
            }
        });
    }

    private void tentarDisposar(Object controller) {
        if (controller instanceof Disposable disposable) {
            disposable.dispose();
            log.debug("[WORKSPACE] Recurso do controller {} foi liberado (dispose)",
                    controller.getClass().getSimpleName());
        } else {
            log.trace("[WORKSPACE] Controller {} não implementa Disposable, nenhuma ação necessária",
                    controller != null ? controller.getClass().getSimpleName() : "null");
        }
    }
}