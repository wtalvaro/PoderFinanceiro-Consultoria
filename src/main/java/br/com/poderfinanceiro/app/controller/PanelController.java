package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

@Component
public class PanelController {

    private final MainController mainController;

    @FXML
    private Button btnDashboard, btnPlaybook, btnBaseClientes, btnPropostas,
            btnPendencias, btnBancosConvenios, btnTabelasJuros,
            btnTabelaComissoes, btnLinks;

    public PanelController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        configurarDicasRicas();
    }

    /**
     * Instala os Tooltips personalizados com Título e Descrição
     */
    private void configurarDicasRicas() {
        aplicarDica(btnDashboard, "Visão Geral", "Acompanhe as métricas vitais da corretora.");
        aplicarDica(btnPlaybook, "Playbook de Vendas", "Roteiros de abordagem e contorno de objeções.");
        aplicarDica(btnBaseClientes, "Base de Contatos", "Gestão e anamnese dos seus clientes.");
        aplicarDica(btnPropostas, "Esteira de Crédito", "Acompanhe propostas e simulações em andamento.");
        aplicarDica(btnPendencias, "Pendências (UTI)", "Atenção: Propostas travadas no banco.");
        aplicarDica(btnBancosConvenios, "Bancos e Convênios", "Consulte a carteira de parceiros.");
        aplicarDica(btnTabelasJuros, "Tabelas de Juros", "Taxas de comissão e limites atualizados.");
        aplicarDica(btnTabelaComissoes, "Repasses (RV)", "Gestão da sua remuneração.");
        aplicarDica(btnLinks, "Links Úteis", "Acesso rápido a portais externos.");
    }

    /**
     * Constrói um Tooltip "Premium" com alto contraste.
     */
    private void aplicarDica(Button botao, String titulo, String descricao) {
        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(javafx.util.Duration.millis(300));

        VBox conteudo = new VBox(3);

        Label lblTitulo = new Label(titulo);
        // Removido o branco. Agora usa um chumbo escuro e negrito.
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333333;");

        Label lblDesc = new Label(descricao);
        // Removido o cinza claro. Agora usa um tom de grafite para leitura fácil.
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");

        conteudo.getChildren().addAll(lblTitulo, lblDesc);

        tooltip.setGraphic(conteudo);
        tooltip.setText(null);

        Tooltip.install(botao, tooltip);
    }

    // ==========================================================
    // MÉTODOS DE NAVEGAÇÃO
    // ==========================================================

    @FXML
    private void abrirWorkspace() {
        destacarBotaoAtivo(btnDashboard);
        mainController.abrirDashboard();;
    }

    @FXML
    private void abrirPlaybook() {
        destacarBotaoAtivo(btnPlaybook);
        mainController.abrirPlaybook();;
    }

    @FXML
    private void abrirTelaBaseClientes() {
        destacarBotaoAtivo(btnBaseClientes);
        mainController.abrirClientes();;
    }

    @FXML
    private void abrirPropostasList() {
        destacarBotaoAtivo(btnPropostas);
        mainController.irParaPropostas();
    }

    @FXML
    private void abrirPendenciasList() {
        destacarBotaoAtivo(btnPendencias);
        mainController.irParaPendencias();
    }

    @FXML
    private void abrirBancosConvenios() {
        destacarBotaoAtivo(btnBancosConvenios);
        mainController.irParaBancosConvenios();
    }

    @FXML
    private void abrirTabelasJuros() {
        destacarBotaoAtivo(btnTabelasJuros);
        mainController.irParaTabelasJuros();
    }

    @FXML
    private void abrirTabelaComissoes() {
        destacarBotaoAtivo(btnTabelaComissoes);
        mainController.irParaTabelaComissoes();
    }

    @FXML
    private void abrirLinksUteis() {
        destacarBotaoAtivo(btnLinks);
        mainController.irParaLinksUteis();
    }

    // ==========================================================
    // MÉTODOS AUXILIARES DE UI
    // ==========================================================

    private void destacarBotaoAtivo(Button botaoClicado) {
        Button[] todosBotoes = { btnDashboard, btnPlaybook, btnBaseClientes, btnPropostas, btnPendencias,
                btnBancosConvenios, btnTabelasJuros, btnTabelaComissoes, btnLinks };
        for (Button btn : todosBotoes) {
            if (btn != null)
                btn.getStyleClass().remove("accent");
        }
        if (botaoClicado != null && !botaoClicado.getStyleClass().contains("accent")) {
            botaoClicado.getStyleClass().add("accent");
        }
    }
}