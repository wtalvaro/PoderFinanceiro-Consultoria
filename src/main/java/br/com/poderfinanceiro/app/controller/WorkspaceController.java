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
import java.util.UUID; // <-- IMPORTANTE: Adicionado para gerar IDs únicos

@Component
public class WorkspaceController {

    @FXML
    private TabPane tabPanePrincipal;

    private final ApplicationContext context;

    public WorkspaceController(ApplicationContext context) {
        this.context = context;
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
        String idBuscado;
        boolean isContatoExistente = false;

        // 1 e 2. Avaliação explícita: A IDE agora é forçada a reconhecer a proteção.
        if (proponente != null && proponente.getId() != null) {
            idBuscado = String.valueOf(proponente.getId());
            isContatoExistente = true;
        } else {
            // Nova operação garantida e isolada
            idBuscado = "NOVO_CONTATO_" + UUID.randomUUID().toString();
        }

        // 3. BUSCA NA COLEÇÃO DE ABAS (Executado apenas para clientes existentes)
        if (isContatoExistente) {
            for (Tab tab : tabPanePrincipal.getTabs()) {
                String idNaAba = String.valueOf(tab.getUserData());

                if (idBuscado.equals(idNaAba)) {
                    // A aba do cliente já existe! Focamos nela e interrompemos a execução.
                    tabPanePrincipal.getSelectionModel().select(tab);
                    return;
                }
            }
        }

        // 4. CRIAÇÃO DA NOVA ABA HUB
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/atendimento_hub.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            AtendimentoHubController hubController = loader.getController();

            // Inicializa os dados do cliente ou prepara um cadastro vazio
            if (proponente != null) {
                hubController.inicializarAtendimento(proponente);
            } else {
                hubController.getLeadController().prepararNovoContato();
            }

            Tab novaAba = new Tab();
            novaAba.setContent(root);
            novaAba.setClosable(true);

            // Etiquetamos a aba fisicamente com a String (ID do banco ou UUID do novo
            // contato)
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