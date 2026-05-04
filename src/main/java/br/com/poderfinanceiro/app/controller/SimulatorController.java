package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class SimulatorController {

    // --- ELEMENTOS DE INTERFACE (Mapeados do simulator.fxml) ---

    @FXML
    private ComboBox<String> comboProponente;
    @FXML
    private ComboBox<String> comboConvenio;
    @FXML
    private ComboBox<String> comboOperacao;

    @FXML
    private ComboBox<String> comboBanco;
    @FXML
    private ComboBox<String> comboTabela;

    @FXML
    private TextField txtTaxaJuros;
    @FXML
    private TextField txtCoeficiente;
    @FXML
    private Spinner<Integer> spinnerParcelas;
    @FXML
    private TextField txtValorBase;
    @FXML
    private TextField txtSaldoDevedor;

    @FXML
    private CheckBox chkPrestamista;
    @FXML
    private CheckBox chkCarencia;

    @FXML
    private Label lblResultadoParcela;
    @FXML
    private Label lblResultadoLiquido;
    @FXML
    private Label lblResultadoBruto;
    @FXML
    private Label lblMensagem;

    @FXML
    private Button btnSalvarLead;
    @FXML
    private Button btnEfetivar;

    // Overlays
    @FXML
    private VBox overlayResumoSimulacao;
    @FXML
    private Label lblSimulacaoPreview;

    @FXML
    public void initialize() {
        // Inicialização básica para evitar null pointers.
        // Nas próximas etapas, podemos popular as listas de bancos e operações aqui.
        comboOperacao.getItems().addAll("Margem Livre", "Refinanciamento", "Portabilidade", "Cartão RMC/RCC");

        // Listener simples para habilitar/desabilitar o Saldo Devedor dependendo da
        // operação
        comboOperacao.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isRefinOuPort = newVal != null
                    && (newVal.equals("Refinanciamento") || newVal.equals("Portabilidade"));
            txtSaldoDevedor.setDisable(!isRefinOuPort);
        });
    }

    // ========================================================================
    // MÉTODOS EXIGIDOS PELO FXML (Botões da ToolBar)
    // ========================================================================

    @FXML
    public void limparSimulacao() {
        // Limpa os campos da tela
        comboProponente.getSelectionModel().clearSelection();
        comboConvenio.getSelectionModel().clearSelection();
        comboOperacao.getSelectionModel().clearSelection();
        comboBanco.getSelectionModel().clearSelection();
        comboTabela.getSelectionModel().clearSelection();

        txtTaxaJuros.clear();
        txtCoeficiente.clear();
        txtValorBase.clear();
        txtSaldoDevedor.clear();

        chkPrestamista.setSelected(true);
        chkCarencia.setSelected(false);

        lblResultadoParcela.setText("R$ 0,00");
        lblResultadoLiquido.setText("R$ 0,00");
        lblResultadoBruto.setText("R$ 0,00");

        exibirMensagem("Simulação limpa com sucesso.", true);
    }

    @FXML
    public void abrirPreviewSimulacao() {
        // Constrói o texto do WhatsApp baseado no que está na tela
        String banco = comboBanco.getValue() != null ? comboBanco.getValue() : "A definir";
        String operacao = comboOperacao.getValue() != null ? comboOperacao.getValue() : "A definir";
        String liquido = lblResultadoLiquido.getText();
        String parcela = lblResultadoParcela.getText();
        // Se houver spinner configurado no futuro, pegar o valor, senão usa um
        // placeholder
        String prazo = spinnerParcelas.getValue() != null ? spinnerParcelas.getValue().toString() : "00";

        String textoPreview = "*SIMULAÇÃO DE CRÉDITO - Poder Financeiro*\n" +
                "————————————————————————————\n" +
                "🏦 *Banco:* " + banco + "\n" +
                "⚙️ *Operação:* " + operacao + "\n\n" +
                "💰 *Valor Liberado:* " + liquido + "\n" +
                "📆 *Parcela:* " + prazo + "x " + parcela + "\n" +
                "————————————————————————————\n" +
                "_Simulação sujeita a análise de crédito e averbação._";

        lblSimulacaoPreview.setText(textoPreview);
        overlayResumoSimulacao.setVisible(true);
    }

    @FXML
    public void fecharPreviewSimulacao() {
        overlayResumoSimulacao.setVisible(false);
    }

    @FXML
    public void confirmarCopiaSimulacao() {
        final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(lblSimulacaoPreview.getText());
        clipboard.setContent(content);

        fecharPreviewSimulacao();
        exibirMensagem("✅ Simulação copiada para o WhatsApp!", true);
    }

    // ========================================================================
    // UTILITÁRIOS DA TELA
    // ========================================================================

    private void exibirMensagem(String texto, boolean sucesso) {
        lblMensagem.setText(texto);
        lblMensagem.setVisible(true);
        lblMensagem.setManaged(true);

        String color = sucesso ? "#e8f5e9" : "#ffebee";
        String text = sucesso ? "#2e7d32" : "#c62828";
        lblMensagem.setStyle("-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5; -fx-background-color: "
                + color + "; -fx-text-fill: " + text + ";");

        // O ideal é implementar aqui a mesma PauseTransition de 5 segundos que fizemos
        // na LeadController
        javafx.animation.PauseTransition timerMensagem = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(5));
        timerMensagem.setOnFinished(event -> {
            lblMensagem.setVisible(false);
            lblMensagem.setManaged(false);
        });
        timerMensagem.play();
    }
}