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

/**
 * <h1>CopilotoCardController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por renderizar um único card de
 * resultado na lista de simulações do Copiloto. É um componente visual puro.
 * </p>
 */
public class CopilotoCardController {

    private static final Logger log = LoggerFactory.getLogger(CopilotoCardController.class);
    private static final String LOG_PREFIX = "[CopilotoCardController]";

    @FXML private VBox cardContainer;
    @FXML private Label lblRecomendado;
    @FXML private Label lblBanco;
    @FXML private Label lblParcela;
    @FXML private Label lblComissao;
    @FXML private Button btnAproveitar;

    private ResultadoSimulacaoDTO resultado;
    private Consumer<ResultadoSimulacaoDTO> onGerarProposta;

    public void setDados(ResultadoSimulacaoDTO resultado, Consumer<ResultadoSimulacaoDTO> onGerarProposta) {
        if (resultado == null) {
            log.error("{} [SISTEMA] setDados chamado com resultado null. Card não será renderizado.", LOG_PREFIX);
            return;
        }

        this.resultado = resultado;
        this.onGerarProposta = onGerarProposta;

        lblBanco.setText("🏦 " + resultado.tabela().getBanco().getNome() + " - " + resultado.tabela().getNomeTabela());
        lblParcela.setText("Parcela: R$ " + FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()));
        lblComissao.setText(String.format("💰 Comissão: R$ %s (%s%%)", FinanceiroUtils.formatarParaExibicao(resultado.comissaoEstimada()),
                resultado.tabela().getComissaoPercentual()));

        log.trace("{} [UI] Card populado. Banco: '{}', Parcela: {}", LOG_PREFIX, resultado.tabela().getBanco().getNome(),
                FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()));
    }

    public void setRecomendadoIA(int rankIA) {
        if (rankIA < 0 || rankIA > 3) {
            log.warn("{} [SISTEMA] rankIA={} fora do intervalo válido (0–3). Tratando como não recomendado.", LOG_PREFIX, rankIA);
            rankIA = 0;
        }

        boolean recomendado = rankIA > 0;
        lblRecomendado.setVisible(recomendado);
        lblRecomendado.setManaged(recomendado);

        if (recomendado) {
            log.trace("{} [UI] Aplicando estilo de recomendação. Rank: {}", LOG_PREFIX, rankIA);

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

    @FXML private void handleGerarProposta() {
        if (resultado == null || onGerarProposta == null) {
            log.warn("{} [SISTEMA] Clique em 'Aproveitar' ignorado. Estado inválido (resultado ou callback nulos).", LOG_PREFIX);
            return;
        }

        log.info("{} [TELEMETRIA] Usuário acionou 'Aproveitar' no card. Banco: '{}', Tabela: '{}'", LOG_PREFIX,
                resultado.tabela().getBanco().getNome(), resultado.tabela().getNomeTabela());

        onGerarProposta.accept(resultado);
    }
}
