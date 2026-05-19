package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.service.AtendimentoContextService.TipoTelaFocada;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.application.Platform;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class WorkspaceController {

    @FXML
    private TabPane tabPanePrincipal;

    private final ApplicationContext context;
    // 🎯 INJEÇÃO DO DIRECIONADOR DE CONTEXTO DA IA
    private final AtendimentoContextService contextoService;

    public WorkspaceController(ApplicationContext context, AtendimentoContextService contextoService) {
        this.context = context;
        this.contextoService = contextoService;
    }

    @FXML
    public void initialize() {
        configurarScrollHorizontal();

        // 🚀 O RASTREADOR ATIVO DE ABAS (Multi-Tenant de Tela)
        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            sincronizarContextoComIA(newTab);
        });
    }

    /**
     * 🧠 Analisa a aba focada pelo operador e atualiza os sinais vitais do chat em
     * background.
     */
    private void sincronizarContextoComIA(Tab abaFocada) {
        if (abaFocada == null) {
            contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
            return;
        }

        Object userData = abaFocada.getUserData();

        // 1. Mapeamento de Abas Estáticas de Gestão
        if (userData instanceof String idAba) {
            switch (idAba) {
                case "ABA_DASHBOARD":
                    contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
                    return;
                case "ABA_CLIENTES":
                    contextoService.atualizarFocoInterface(null, TipoTelaFocada.LISTA_CLIENTES);
                    return;
                case "ABA_LINKS":
                    contextoService.atualizarFocoInterface(null, TipoTelaFocada.LINKS_UTEIS);
                    return;
                case "ABA_JUROS":
                    contextoService.atualizarFocoInterface(null, TipoTelaFocada.TABELAS_JUROS);
                    return;
                case "ABA_COMISSOES": // 🎯 INTERCEPTADO: Captura o foco na tela de liquidação RV
                    contextoService.atualizarFocoInterface(null, TipoTelaFocada.GESTAO_COMISSOES);
                    return;
                default:
                    // Outras abas administrativas (Bancos, Comissões, Playbook ou Propostas)
                    if (idAba.startsWith("ABA_")) {
                        contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
                        return;
                    }
                    break;
            }
        }

        // Mapeamento de Aba de Atendimento Ativa (Hub de Cliente)
        Object controller = abaFocada.getProperties().get("controller");
        if (controller instanceof AtendimentoHubController hub) {
            if (hub.getLeadController() != null && hub.getLeadController().getViewModel() != null) {
                // 🎯 CONSERTADO: Agora coleta o modelo completo mantendo o relacionamento de
                // endereços!
                contextoService.atualizarFocoInterface(
                        hub.getProponenteComCamposDaTela(),
                        TipoTelaFocada.CADASTRO_CLIENTE);
            } else {
                contextoService.atualizarFocoInterface(null, TipoTelaFocada.CADASTRO_CLIENTE);
            }
        }
    }

    // ========================================================================
    // PROTOCOLO DE ADMISSÃO (Motor de Abas)
    // ========================================================================

    private void admitirAbaSimples(String id, String titulo, String fxmlPath) {
        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (id.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
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

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========================================================================
    // MÉTODOS PÚBLICOS (Ordens de Serviço)
    // ========================================================================

    public void abrirAbaDashboard() {
        admitirAbaSimples("ABA_DASHBOARD", "📊 Visão Geral", "/fxml/dashboard.fxml");
    }

    public void abrirAbaPlaybook() {
        admitirAbaSimples("ABA_PLAYBOOK", "📚 Playbook", "/fxml/playbook.fxml");
    }

    public void abrirAbaClientes() {
        admitirAbaSimples("ABA_CLIENTES", "👥 Clientes", "/fxml/clientes_list.fxml");
    }

    public void abrirAbaLinks() {
        admitirAbaSimples("ABA_LINKS", "🔗 Links Úteis", "/fxml/links_uteis.fxml");
    }

    public void abrirAbaTabelasJuros() {
        admitirAbaSimples("ABA_JUROS", "📈 Tabelas de Juros", "/fxml/tabelas_juros.fxml");
    }

    public void abrirAbaBancosConvenios() {
        admitirAbaSimples("ABA_BANCOS", "🏦 Bancos e Convênios", "/fxml/bancos_convenios.fxml");
    }

    public void abrirAbaComissoes() {
        admitirAbaSimples("ABA_COMISSOES", "💰 Gestão de Repasses (RV)", "/fxml/comissoes.fxml");
    }

    public void abrirAbaPropostas(String filtroInicial) {
        boolean isEmergencia = filtroInicial != null && !filtroInicial.trim().isEmpty();
        String idAba = isEmergencia ? "ABA_PENDENCIAS" : "ABA_PROPOSTAS";
        String tituloAba = isEmergencia ? "🚨 UTI: Pendências" : "📄 Esteira de Propostas";

        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (idAba.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/propostas_list.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            PropostasListController controller = loader.getController();
            if (isEmergencia) {
                controller.aplicarFiltroExterno(filtroInicial);
            }

            Tab novaAba = new Tab(tituloAba);
            novaAba.setContent(root);
            novaAba.setUserData(idAba);
            novaAba.setClosable(true);

            if (isEmergencia) {
                novaAba.setStyle("-fx-text-base-color: #c62828; -fx-font-weight: bold;");
            }

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void abrirOuFocarAba(ProponenteModel proponente) {
        abrirOuFocarAbaComProposta(proponente, null);
    }

    // =====================================================================
    // 🚀 PATCH: LIMPEZA DO REPASSE DE PROPOSTAS PARA O HUB DO CLIENTE
    // =====================================================================
    public void abrirOuFocarAbaComProposta(ProponenteModel proponente, Long propostaIdAlvo) {
        String idBuscado;

        if (proponente != null && proponente.getId() != null) {
            idBuscado = String.valueOf(proponente.getId());

            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (idBuscado.equals(String.valueOf(tab.getUserData()))) {
                    tabPanePrincipal.getSelectionModel().select(tab);

                    if (tab.getProperties().get("controller") instanceof AtendimentoHubController) {
                        AtendimentoHubController hubExistente = (AtendimentoHubController) tab.getProperties()
                                .get("controller");
                        // ❌ ANTES: hubExistente.inicializarAtendimento(proponente, propostaIdAlvo);
                        // ✅ AGORA: O Hub só foca no Cliente.
                        hubExistente.inicializarAtendimento(proponente);
                    }
                    return;
                }
            }
        } else {
            idBuscado = "NOVO_" + UUID.randomUUID().toString();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            AtendimentoHubController hub = loader.getController();

            if (proponente != null && proponente.getId() != null) {
                // ❌ ANTES: Tinha if/else verificando propostaIdAlvo
                // ✅ AGORA: Apenas inicializa o proponente
                hub.inicializarAtendimento(proponente);
            } else {
                hub.prepararNovoAtendimento();
            }

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setUserData(idBuscado);

            // Armazena a referência para resgate em tempo de execução
            novaAba.getProperties().put("controller", hub);

            hub.setTabPertencente(novaAba);

            configurarTituloReativoLead(novaAba, hub);

            novaAba.setOnCloseRequest(event -> {
                if (hub.temAlteracoesNaoSalvas()) {
                    event.consume();
                    hub.solicitarFechamento(() -> tabPanePrincipal.getTabs().remove(novaAba));
                } else {
                    hub.limparRecursos();
                }
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========================================================================
    // UTILITÁRIOS INTERNOS (Suporte Vital)
    // ========================================================================

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
            }
        });
    }
}