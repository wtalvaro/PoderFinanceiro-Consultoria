package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WorkspaceController {

    @FXML
    private TabPane tabPanePrincipal;

    private final ApplicationContext context;

    public WorkspaceController(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Motor inteligente de abas: Localiza uma aba existente ou cria uma nova se
     * necessário.
     */
    public void abrirOuFocarAba(Proponente proponente) {
        // 1. Blindagem de Tipo: Normaliza IDs para String para evitar falsos negativos
        // na comparação
        String idBuscado = proponente.getId() != null ? String.valueOf(proponente.getId()) : "NOVO_CONTATO";

        // 2. BUSCA NA COLEÇÃO DE ABAS
        for (Tab tab : tabPanePrincipal.getTabs()) {
            String idNaAba = String.valueOf(tab.getUserData());

            if (idBuscado.equals(idNaAba)) {
                // A aba já existe! Apenas focamos nela e interrompemos a execução.
                tabPanePrincipal.getSelectionModel().select(tab);
                return;
            }
        }

        // 3. CRIAÇÃO ISOLADA DA NOVA ABA (SRP Respeitado)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            AtendimentoHubController hubController = loader.getController();
            hubController.inicializarAtendimento(proponente);

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setClosable(true);

            // O Segredo: Etiquetamos a aba fisicamente com a String normalizada
            novaAba.setUserData(idBuscado);

            Label iconeAba = new Label("👤");
            iconeAba.setStyle("-fx-font-size: 14px;");
            novaAba.setGraphic(iconeAba);

            // Binding reativo de título
            novaAba.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                String nome = hubController.getLeadController().getViewModel().nomeProperty().get();
                if (nome == null || nome.trim().isEmpty()) {
                    return "Novo Atendimento";
                }
                return nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;
            }, hubController.getLeadController().getViewModel().nomeProperty()));

            // Proteção contra vazamento de memória e fechamento acidental
            novaAba.setOnCloseRequest(event -> {
                Runnable acaoMatarAba = () -> {
                    tabPanePrincipal.getTabs().remove(novaAba);
                    hubController.limparRecursos();
                };

                if (hubController.temAlteracoesNaoSalvas()) {
                    event.consume();
                    hubController.solicitarFechamento(acaoMatarAba);
                } else {
                    hubController.limparRecursos();
                }
            });

            tabPanePrincipal.getTabs().add(novaAba);
            tabPanePrincipal.getSelectionModel().select(novaAba);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}