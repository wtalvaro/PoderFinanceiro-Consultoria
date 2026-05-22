package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.TipoTelaFocada;
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
    private final AtendimentoContextService contextoService;

    public WorkspaceController(ApplicationContext context, AtendimentoContextService contextoService) {
        this.context = context;
        this.contextoService = contextoService;
    }

    @FXML
    public void initialize() {
        configurarScrollHorizontal();

        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            sincronizarContextoComIA(newTab);
        });
    }

    private void sincronizarContextoComIA(Tab abaFocada) {
        if (abaFocada == null) {
            contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
            return;
        }

        Object userData = abaFocada.getUserData();

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
                case "ABA_COMISSOES":
                    contextoService.atualizarFocoInterface(null, TipoTelaFocada.GESTAO_COMISSOES);
                    return;
                case "ABA_PROPOSTAS": // 🚀 NOVO: Mantém o contexto do Chat ciente da Esteira
                    contextoService.setTelaAtualFocada(TipoTelaFocada.ESTEIRA_PROPOSTAS);
                    return;
                default:
                    if (idAba.startsWith("ABA_")) {
                        contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
                        return;
                    }
                    break;
            }
        }

        Object controller = abaFocada.getProperties().get("controller");
        if (controller instanceof AtendimentoHubController hub) {
            if (hub.getLeadController() != null && hub.getLeadController().getViewModel() != null) {
                contextoService.atualizarFocoInterface(hub.getProponenteComCamposDaTela(),
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

            // 🚀 A CURA: Salva a referência do Controller na aba para uso futuro
            novaAba.getProperties().put("controller", loader.getController());

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
        admitirAbaSimples("ABA_PROPOSTAS", "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");
    }

    // Adicione junto aos outros métodos "abrirAba..."
    public void abrirAbaImportadorTabelas() {
        admitirAbaSimples("ABA_IMPORTADOR_TABELAS", "🤖 Importador IA", "/fxml/importador_tabelas.fxml");
    }

    // =====================================================================
    // ROTEAMENTO: HUB DE CLIENTE
    // =====================================================================
    public void abrirOuFocarAba(ProponenteModel proponente) {
        String idBuscado = (proponente != null && proponente.getId() != null) ? String.valueOf(proponente.getId())
                : "NOVO_" + UUID.randomUUID().toString();

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

    // =====================================================================
    // 🚀 NOVO COMPORTAMENTO: ROTEAMENTO DIRETO PARA A ESTEIRA DE PROPOSTAS
    // =====================================================================
    public void abrirOuFocarAbaComProposta(ProponenteModel proponente, Long propostaIdAlvo) {
        if (propostaIdAlvo != null) {
            // 1. Garante que a aba da esteira está aberta e focada
            admitirAbaSimples("ABA_PROPOSTAS", "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");

            // 2. Busca o controlador e manda ele selecionar a proposta
            for (Tab tab : tabPanePrincipal.getTabs()) {
                if ("ABA_PROPOSTAS".equals(tab.getUserData())) {
                    Object controller = tab.getProperties().get("controller");
                    if (controller instanceof EsteiraPropostasController esteira) {
                        Platform.runLater(() -> esteira.selecionarPropostaPorId(propostaIdAlvo));
                    }
                    break;
                }
            }
        } else {
            // Fallback de segurança: Se não tiver ID de proposta, abre o Hub do Cliente
            abrirOuFocarAba(proponente);
        }
    }

    // ========================================================================
    // UTILITÁRIOS INTERNOS
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