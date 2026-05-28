package br.com.poderfinanceiro.app.presentation.controller.proposta;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.common.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * <h1>CopilotoCardController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por renderizar um único card de
 * resultado na lista de simulações do Copiloto. Implementa o padrão <b>Humble
 * Object</b>, delegando ações de interface global para o {@link Navigator}.
 * </p>
 */
@Component
@Scope("prototype")
public class CopilotoCardController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E ESTILOS (UI DESIGN SYSTEM)
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(CopilotoCardController.class);
    private static final String LOG_PREFIX = "[CopilotoCardController]";

    private static final String STYLE_RANK_1 = "-fx-background-color: #fef08a; -fx-text-fill: #854d0e; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;";
    private static final String STYLE_CARD_1 = "-fx-background-color: #fffbeb; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #f59e0b; -fx-border-width: 2;";

    private static final String STYLE_RANK_DEFAULT = "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;";
    private static final String STYLE_CARD_DEFAULT = "-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1;";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final Navigator navigator;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private VBox cardContainer;
    @FXML private Label lblRecomendado;
    @FXML private Label lblBanco;
    @FXML private Label lblParcela;
    @FXML private Label lblComissao;
    @FXML private Button btnAproveitar;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO
    // ==========================================================================================
    private ResultadoSimulacaoDTO resultado;
    private Consumer<ResultadoSimulacaoDTO> onGerarProposta;

    public CopilotoCardController(Navigator navigator) {
        this.navigator = navigator;
        log.debug("{} [SISTEMA] Componente de Card instanciado via Spring Context.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: POPULAÇÃO DE DADOS
    // ==========================================================================================

    /**
     * Configura os dados financeiros do card e define o callback de ação.
     */
    public void setDados(ResultadoSimulacaoDTO resultado, Consumer<ResultadoSimulacaoDTO> onGerarProposta) {
        if (resultado == null) {
            log.error("{} [SISTEMA] Falha ao renderizar card: Resultado da simulação é nulo.", LOG_PREFIX);
            return;
        }

        this.resultado = resultado;
        this.onGerarProposta = onGerarProposta;

        lblBanco.setText("🏦 " + resultado.tabela().getBanco().getNome() + " - " + resultado.tabela().getNomeTabela());
        lblParcela.setText("Parcela: R$ " + FinanceiroUtils.formatarParaExibicao(resultado.valorParcela()));
        lblComissao.setText(String.format("💰 Comissão: R$ %s (%s%%)",
                FinanceiroUtils.formatarParaExibicao(resultado.comissaoEstimada()),
                resultado.tabela().getComissaoPercentual()));

        log.trace("{} [UI] Card populado com sucesso para o banco: {}", LOG_PREFIX,
                resultado.tabela().getBanco().getNome());
    }

    /**
     * Aplica o destaque visual de recomendação da IA baseado no ranking.
     * 
     * @param rankIA Posição no ranking (1, 2 ou 3). 0 para não recomendado.
     */
    public void setRecomendadoIA(int rankIA) {
        boolean recomendado = rankIA > 0 && rankIA <= 3;
        lblRecomendado.setVisible(recomendado);
        lblRecomendado.setManaged(recomendado);

        if (!recomendado) {
            cardContainer.setStyle(STYLE_CARD_DEFAULT);
            return;
        }

        log.trace("{} [UI] Aplicando destaque visual de recomendação Rank {}", LOG_PREFIX, rankIA);

        if (rankIA == 1) {
            lblRecomendado.setText("🏆 1ª Escolha da IA");
            lblRecomendado.setStyle(STYLE_RANK_1);
            cardContainer.setStyle(STYLE_CARD_1);
        } else {
            lblRecomendado.setText(rankIA + "ª Escolha");
            lblRecomendado.setStyle(STYLE_RANK_DEFAULT);
            cardContainer.setStyle(STYLE_CARD_DEFAULT);
        }
    }

    // ==========================================================================================
    // MÓDULO 6: AÇÕES E OVERLAYS (NAVIGATOR)
    // ==========================================================================================

    /**
     * Aciona o fluxo de geração de proposta, utilizando o overlay global para
     * confirmação.
     */
    @FXML private void handleGerarProposta() {
        if (resultado == null || onGerarProposta == null) {
            log.warn("{} [NEGOCIO] Ação 'Aproveitar' ignorada: Estado do card é inválido.", LOG_PREFIX);
            return;
        }

        log.info("{} [TELEMETRIA] Usuário solicitou aproveitamento da simulação via card.", LOG_PREFIX);

        // Padronização: Uso da overlay do MainController via Navigator para
        // confirmação global
        navigator.solicitarConfirmacao(
                "✨ Gerar Proposta", "Deseja converter esta simulação do banco "
                        + resultado.tabela().getBanco().getNome() + " em uma proposta oficial para o cliente?",
                "Sim, Gerar Proposta", "-color-accent-fg", () -> {
                    log.info("{} [AUDITORIA] Confirmação recebida. Iniciando conversão de rascunho.", LOG_PREFIX);
                    onGerarProposta.accept(resultado);
                });
    }

    public Button getBtnAproveitar() {
        return btnAproveitar;
    }
}
