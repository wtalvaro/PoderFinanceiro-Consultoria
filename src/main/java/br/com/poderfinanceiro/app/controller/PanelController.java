package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

@Component
public class PanelController {

    private final MainController mainController;

    // Injetando todos os botões do painel (definidos no panel.fxml)
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnPlaybook;
    @FXML
    private Button btnBaseClientes;
    @FXML
    private Button btnPropostas;
    @FXML
    private Button btnPendencias;
    @FXML
    private Button btnBancosConvenios;
    @FXML
    private Button btnTabelasJuros;
    @FXML
    private Button btnTabelaComissoes;
    @FXML
    private Button btnLinks;

    public PanelController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        System.out.println("PanelController carregado com sucesso!");
    }

    // ==========================================================
    // MÉTODOS DE NAVEGAÇÃO
    // ==========================================================

    @FXML
    private void abrirWorkspace() {
        destacarBotaoAtivo(btnDashboard);
        mainController.focarAbaDashboard();
    }

    @FXML
    private void abrirPlaybook() {
        destacarBotaoAtivo(btnPlaybook);
        mainController.focarAbaPlaybook();
    }

    @FXML
    private void abrirTelaBaseClientes() {
        destacarBotaoAtivo(btnBaseClientes);
        mainController.focarAbaClientes();
    }

    @FXML
    private void abrirLinksUteis() {
        destacarBotaoAtivo(btnLinks);
        mainController.irParaLinksUteis();
    }

    @FXML
    private void abrirTabelasJuros() {
        destacarBotaoAtivo(btnTabelasJuros);
        mainController.irParaTabelasJuros();
    }

    @FXML
    private void abrirBancosConvenios() {
        destacarBotaoAtivo(btnBancosConvenios);
        mainController.irParaBancosConvenios();;
    }

    @FXML
    private void abrirTabelaComissoes() {
        destacarBotaoAtivo(btnTabelaComissoes);
        mainController.irParaTabelaComissoes();
    }

    // ==========================================================
    // MÉTODOS AUXILIARES DE UI
    // ==========================================================

    /**
     * Remove o destaque de todos os botões e aplica apenas no botão clicado.
     */
    private void destacarBotaoAtivo(Button botaoClicado) {
        removerDestaqueBotoes();
        if (botaoClicado != null && !botaoClicado.getStyleClass().contains("accent")) {
            botaoClicado.getStyleClass().add("accent");
        }
    }

    /**
     * Limpa a classe CSS 'accent' de todos os botões do painel lateral.
     */
    private void removerDestaqueBotoes() {
        Button[] todosBotoes = {
                btnDashboard, btnPlaybook, btnBaseClientes, btnPropostas,
                btnPendencias, btnBancosConvenios, btnTabelasJuros,
                btnTabelaComissoes, btnLinks
        };

        for (Button btn : todosBotoes) {
            if (btn != null) {
                btn.getStyleClass().remove("accent");
            }
        }
    }
}