package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class CopilotoCardController {

    @FXML
    private VBox cardContainer;
    @FXML
    private Label lblRecomendado;
    @FXML
    private Label lblBanco;
    @FXML
    private Label lblParcela;
    @FXML
    private Label lblComissao;
    @FXML
    private Button btnAproveitar;

    private ResultadoSimulacaoDTO resultado;
    private Consumer<ResultadoSimulacaoDTO> onGerarProposta;

    public void setDados(ResultadoSimulacaoDTO resultado, Consumer<ResultadoSimulacaoDTO> onGerarProposta) {
        this.resultado = resultado;
        this.onGerarProposta = onGerarProposta;

        lblBanco.setText("🏦 " + resultado.tabela().getBanco().getNome() + " - " + resultado.tabela().getNomeTabela());
        lblParcela.setText("Parcela: R$ " + FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()));
        lblComissao.setText(String.format("💰 Comissão: R$ %s (%s%%)",
                FinanceiroUtils.formatarParaExibicao(resultado.comissaoEstimada()),
                resultado.tabela().getComissaoPercentual()));
    }

    // Método responsável por iluminar o cartão escolhido
    public void setRecomendadoIA(boolean recomendado) {
        lblRecomendado.setVisible(recomendado);
        lblRecomendado.setManaged(recomendado);

        if (recomendado) {
            // Estilo Dourado/Amarelo para destaque
            cardContainer.setStyle(
                    "-fx-background-color: #fffbeb; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #f59e0b; -fx-border-width: 2;");
        } else {
            // Estilo padrão cinza
            cardContainer.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1;");
        }
    }

    public Button getBtnAproveitar() {
        return btnAproveitar;
    }

    @FXML
    private void handleGerarProposta() {
        if (onGerarProposta != null && resultado != null) {
            onGerarProposta.accept(resultado);
        }
    }
}