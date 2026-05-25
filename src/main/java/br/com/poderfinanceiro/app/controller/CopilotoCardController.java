package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class CopilotoCardController {

    private static final Logger log = LoggerFactory.getLogger(CopilotoCardController.class);

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
        // Dado nulo aqui causaria NPE silencioso na renderização do card
        if (resultado == null) {
            log.error("[COPILOTO_CARD] setDados chamado com resultado null. Card não será renderizado corretamente.");
            return;
        }

        this.resultado = resultado;
        this.onGerarProposta = onGerarProposta;

        lblBanco.setText("🏦 " + resultado.tabela().getBanco().getNome() + " - " + resultado.tabela().getNomeTabela());
        lblParcela.setText("Parcela: R$ " + FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()));
        lblComissao.setText(String.format("💰 Comissão: R$ %s (%s%%)",
                FinanceiroUtils.formatarParaExibicao(resultado.comissaoEstimada()),
                resultado.tabela().getComissaoPercentual()));

        log.debug("[COPILOTO_CARD] Card populado. Banco='{}' | Tabela='{}' | Parcela={} | Comissão={}",
                resultado.tabela().getBanco().getNome(),
                resultado.tabela().getNomeTabela(),
                FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()),
                FinanceiroUtils.formatarParaExibicao(resultado.comissaoEstimada()));
    }

    public void setRecomendadoIA(int rankIA) {
        // Rank fora do esperado (0–3) indica bug no código que monta os cards
        if (rankIA < 0 || rankIA > 3) {
            log.warn("[COPILOTO_CARD] rankIA={} fora do intervalo válido (0–3). Banco='{}'. " +
                    "Tratando como não recomendado.",
                    rankIA,
                    resultado != null ? resultado.tabela().getBanco().getNome() : "N/A");
            rankIA = 0;
        }

        boolean recomendado = rankIA > 0;
        lblRecomendado.setVisible(recomendado);
        lblRecomendado.setManaged(recomendado);

        if (recomendado) {
            log.debug("[COPILOTO_CARD] Card marcado como recomendado. Rank={} | Banco='{}'",
                    rankIA,
                    resultado != null ? resultado.tabela().getBanco().getNome() : "N/A");

            if (rankIA == 1) {
                lblRecomendado.setText("🏆 1ª Escolha da IA");
                lblRecomendado.setStyle(
                        "-fx-background-color: #fef08a; -fx-text-fill: #854d0e; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
                cardContainer.setStyle(
                        "-fx-background-color: #fffbeb; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #f59e0b; -fx-border-width: 2;");
            } else if (rankIA == 2) {
                lblRecomendado.setText("🥈 2ª Escolha");
                lblRecomendado.setStyle(
                        "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
                cardContainer.setStyle(
                        "-fx-background-color: #f8fafc; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #94a3b8; -fx-border-width: 2;");
            } else {
                lblRecomendado.setText("🥉 3ª Escolha");
                lblRecomendado.setStyle(
                        "-fx-background-color: #ffedd5; -fx-text-fill: #9a3412; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
                cardContainer.setStyle(
                        "-fx-background-color: #fff7ed; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #fdba74; -fx-border-width: 2;");
            }
        } else {
            cardContainer.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1;");
        }
    }

    public Button getBtnAproveitar() {
        return btnAproveitar;
    }

    @FXML
    private void handleGerarProposta() {
        if (resultado == null || onGerarProposta == null) {
            // Sem este log, o clique no botão simplesmente não faria nada — invisível para
            // debug
            log.warn("[COPILOTO_CARD] handleGerarProposta chamado em estado inválido. " +
                    "resultado={} | onGerarProposta={}",
                    resultado == null ? "NULL" : "ok",
                    onGerarProposta == null ? "NULL" : "ok");
            return;
        }

        log.info("[COPILOTO_CARD] Usuário acionou 'Aproveitar'. Banco='{}' | Tabela='{}' | Parcela={}",
                resultado.tabela().getBanco().getNome(),
                resultado.tabela().getNomeTabela(),
                FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()));

        onGerarProposta.accept(resultado);
    }
}