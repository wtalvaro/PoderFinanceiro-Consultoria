package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
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

    public WorkspaceController(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    public void initialize() {
        configurarScrollHorizontal();
    }

    // ========================================================================
    // PROTOCOLO DE ADMISSÃO (Motor de Abas)
    // ========================================================================

    /**
     * Protocolo Padrão para abas de suporte/gestão (Links, Juros, Bancos,
     * Comissões).
     * Evita a duplicação de lógica de carregamento de FXML e criação de Tab.
     */
    private void admitirAbaSimples(String id, String titulo, String fxmlPath) {
        // 1. Verifica se a "ficha" já está aberta (aba existente)
        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (id.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                return;
            }
        }

        // 2. Se for uma nova admissão, carrega o equipamento (FXML)
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

    public void focarAbaFixa(int index) {
        if (tabPanePrincipal != null && index >= 0 && index < tabPanePrincipal.getTabs().size()) {
            tabPanePrincipal.getSelectionModel().select(index);
        }
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
        admitirAbaSimples("ABA_COMISSOES", "💰 Comissões", "/fxml/comissoes.fxml");
    }

    /**
     * Admissão Especial: Hub de Atendimento.
     * Diferente das simples, esta requer injeção de dados do Proponente e Bindings
     * de título.
     */
    public void abrirOuFocarAba(Proponente proponente) {
        String idBuscado;

        // 1. Triagem Direta: Verificamos se o paciente existe e tem registro (ID)
        if (proponente != null && proponente.getId() != null) {
            idBuscado = String.valueOf(proponente.getId());

            // Busca se o leito (aba) já está ocupado por este paciente
            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (idBuscado.equals(String.valueOf(tab.getUserData()))) {
                    tabPanePrincipal.getSelectionModel().select(tab);
                    return;
                }
            }
        } else {
            // Se for um novo atendimento, geramos o código de entrada único
            idBuscado = "NOVO_" + UUID.randomUUID().toString();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            AtendimentoHubController hub = loader.getController();

            // 2. Aqui o compilador fica calmo: checamos o objeto proponente diretamente
            if (proponente != null && proponente.getId() != null) {
                hub.inicializarAtendimento(proponente);
            } else {
                hub.prepararNovoAtendimento();
            }

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setUserData(idBuscado);
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