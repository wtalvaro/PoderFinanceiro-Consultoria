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
import java.util.UUID; // <-- IMPORTANTE: Adicionado para gerar IDs únicos

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
        Platform.runLater(() -> {
            Node headerArea = tabPanePrincipal.lookup(".tab-header-area");

            if (headerArea != null) {
                headerArea.setOnScroll(event -> {
                    double dy = event.getDeltaY();
                    double dx = event.getDeltaX();

                    // Prioriza o eixo com maior deslocamento para evitar trocas acidentais
                    if (Math.abs(dy) > Math.abs(dx)) {
                        // Scroll Vertical
                        if (dy < 0) {
                            tabPanePrincipal.getSelectionModel().selectNext();
                        } else if (dy > 0) {
                            tabPanePrincipal.getSelectionModel().selectPrevious();
                        }
                    } else if (Math.abs(dx) > 2) { // Pequena margem de tolerância (deadzone) para o X
                        // Scroll Horizontal
                        if (dx < 0) {
                            tabPanePrincipal.getSelectionModel().selectNext();
                        } else if (dx > 0) {
                            tabPanePrincipal.getSelectionModel().selectPrevious();
                        }
                    }

                    event.consume(); // Importante para o evento não subir para os controles dentro da aba
                });
            }
        });
    }

    /**
     * Foca em uma das abas fixas do sistema (0=Dashboard, 1=Playbook, 2=Clientes)
     */
    public void focarAbaFixa(int index) {
        if (tabPanePrincipal != null && index >= 0 && index < tabPanePrincipal.getTabs().size()) {
            tabPanePrincipal.getSelectionModel().select(index);
        }
    }

    /**
     * Motor inteligente: Se proponente for null, abre um Novo Contato ÚNICO.
     * Se existir, foca na aba já aberta ou cria uma nova aba Hub.
     */
    public void abrirOuFocarAba(Proponente proponente) {
        String idBuscado = null;
        boolean isExistente = (proponente != null && proponente.getId() != null);

        if (isExistente && proponente != null) {
            idBuscado = String.valueOf(proponente.getId());
            // Busca aba existente pelo ID do banco
            for (Tab tab : tabPanePrincipal.getTabs()) {
                if (idBuscado.equals(String.valueOf(tab.getUserData()))) {
                    tabPanePrincipal.getSelectionModel().select(tab);
                    return;
                }
            }
        } else {
            // Para novos, geramos um ID temporário único para permitir várias abas de
            // "Novo"
            idBuscado = "NOVO_" + UUID.randomUUID().toString();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            AtendimentoHubController hub = loader.getController();

            if (isExistente)
                hub.inicializarAtendimento(proponente);
            else
                hub.prepararNovoAtendimento();

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setUserData(idBuscado); // Identificador único da aba
            hub.setTabPertencente(novaAba);

            // Binding reativo: O título da aba "persegue" o nome no ViewModel
            novaAba.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                String nome = hub.getLeadController().getViewModel().nomeProperty().get();
                if (nome == null || nome.trim().isEmpty()) {
                    return "Novo Atendimento";
                }
                // Limita o tamanho para não quebrar o layout das abas
                return nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;
            }, hub.getLeadController().getViewModel().nomeProperty()));

            // Opcional: Adicionar o ícone que tínhamos antes
            Label iconeAba = new Label("👤");
            iconeAba.setStyle("-fx-font-size: 14px;");
            novaAba.setGraphic(iconeAba);

            // Configura o fechamento SEGURO
            novaAba.setOnCloseRequest(event -> {
                if (hub.temAlteracoesNaoSalvas()) {
                    event.consume(); // Para o fechamento automático
                    hub.solicitarFechamento(() -> {
                        tabPanePrincipal.getTabs().remove(novaAba);
                    });
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

    /**
     * Abre ou foca na aba de Links Úteis.
     * Diferente das abas fixas, esta é fechável.
     */
    public void abrirAbaLinks() {
        String idLinks = "ABA_LINKS_UTEIS";

        // 1. Verificar se a aba já está aberta
        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (idLinks.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                return;
            }
        }

        // 2. Se não estiver aberta, carregar o FXML
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/links_uteis.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Tab tabLinks = new Tab("🔗 Links Úteis");
            tabLinks.setContent(root);
            tabLinks.setUserData(idLinks);
            tabLinks.setClosable(true); // Esta aba o usuário pode fechar

            tabPanePrincipal.getTabs().add(tabLinks);
            tabPanePrincipal.getSelectionModel().select(tabLinks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Abre ou foca na aba de Gestão de Tabelas de Juros.
     * Segue a mesma arquitetura de aba fechável da tela de Links Úteis.
     */
    public void abrirAbaTabelasJuros() {
        String idTabelas = "ABA_TABELAS_JUROS";

        // 1. Verificar se a aba já está aberta
        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (idTabelas.equals(tab.getUserData())) {
                tabPanePrincipal.getSelectionModel().select(tab);
                return;
            }
        }

        // 2. Se não estiver aberta, carregar o FXML
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tabelas_juros.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Tab tabTabelas = new Tab("📈 Tabelas de Juros");
            tabTabelas.setContent(root);
            tabTabelas.setUserData(idTabelas);
            tabTabelas.setClosable(true); // Esta aba o usuário pode fechar

            tabPanePrincipal.getTabs().add(tabTabelas);
            tabPanePrincipal.getSelectionModel().select(tabTabelas);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}