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
            RotaAba rota = RotaAba.fromId(idAba);

            // Tratamento dinâmico via Enum (adeus Switch-Case!)
            if (rota != null && rota.getTipoTelaFocada() != null) {
                // A esteira não limpa o proponente, apenas muda o foco da tela
                if (rota == RotaAba.PROPOSTAS) {
                    contextoService.setTelaAtualFocada(rota.getTipoTelaFocada());
                } else {
                    contextoService.atualizarFocoInterface(null, rota.getTipoTelaFocada());
                }
                return;
            } else if (idAba.startsWith("ABA_")) {
                contextoService.atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
                return;
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

    public void admitirAbaSimples(RotaAba rota, String titulo, String fxmlPath) {
        String id = rota.getId();

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

            Object controller = loader.getController();
            novaAba.getProperties().put("controller", controller);
            novaAba.setOnCloseRequest(event -> tentarDisposar(controller));
            
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
        admitirAbaSimples(RotaAba.DASHBOARD, "📊 Visão Geral", "/fxml/dashboard.fxml");
    }

    public void abrirAbaPlaybook() {
        // Fallback temporário para strings se não estiver no enum
        admitirAbaSimples(RotaAba.fromId("ABA_PLAYBOOK") != null ? RotaAba.fromId("ABA_PLAYBOOK") : RotaAba.DASHBOARD,
                "📚 Playbook", "/fxml/playbook.fxml");
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
                    hub.solicitarFechamento(() -> {
                        hub.limparRecursos();
                        tentarDisposar(hub); // Limpeza ao fechar
                        tabPanePrincipal.getTabs().remove(novaAba);
                    });
                } else {
                    hub.limparRecursos();
                    tentarDisposar(hub);
                }
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // DEPOIS:
    public void abrirOuFocarAbaComProposta(ProponenteModel proponente, Long propostaIdAlvo) {
        if (propostaIdAlvo != null) {
            admitirAbaSimples(RotaAba.PROPOSTAS, "📄 Esteira de Propostas", "/fxml/esteira_propostas.fxml");

            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (RotaAba.PROPOSTAS.getId().equals(tab.getUserData())) {
                    Object controller = tab.getProperties().get("controller");
                    if (controller instanceof EsteiraPropostasController esteira) {
                        // Sem Platform.runLater — selecionarPropostaPorId já é assíncrono internamente
                        esteira.selecionarPropostaPorId(propostaIdAlvo);
                    }
                    break;
                }
            }
        } else {
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

    private void tentarDisposar(Object controller) {
    if (controller instanceof Disposable disposable) {
        disposable.dispose();
    }
}
}