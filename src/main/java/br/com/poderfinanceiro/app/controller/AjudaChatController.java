package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.GeminiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AjudaChatController {

    @FXML
    private VBox containerMensagens;
    @FXML
    private TextField txtMensagem;
    @FXML
    private ScrollPane scrollChat;

    private final GeminiService geminiService;
    private MainController mainController; // Para fechar o painel

    public AjudaChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();
        if (texto == null || texto.trim().isEmpty())
            return;

        // 1. Limpa o campo e desenha a bolha do Consultor na tela (Lado Direito)
        txtMensagem.clear();
        adicionarBalao(texto, true);

        // 2. Adiciona o indicador de carregamento
        Label lblPensando = adicionarBalaoDeCarregamento();

        // 3. Dispara a chamada para a IA em Background
        Task<String> taskIA = new Task<>() {
            @Override
            protected String call() throws Exception {
                return geminiService.perguntarAoAssistente(texto);
            }
        };

        taskIA.setOnSucceeded(e -> {
            containerMensagens.getChildren().remove(lblPensando); // Remove o "Pensando..."
            adicionarBalao(taskIA.getValue(), false); // Adiciona a resposta (Lado Esquerdo)
        });

        taskIA.setOnFailed(e -> {
            containerMensagens.getChildren().remove(lblPensando);
            adicionarBalao("Desculpe, ocorreu uma falha técnica ao consultar o sistema.", false);
        });

        new Thread(taskIA).start();
    }

    private void adicionarBalao(String texto, boolean isUsuario) {
        Label balao = new Label(texto);
        balao.setWrapText(true);
        balao.setMaxWidth(320); // Alinhado com a largura do layout

        VBox wrapper = new VBox(balao);

        if (isUsuario) {
            // Estilo WhatsApp - Mensagem Enviada (Verde Claro, Texto Preto)
            balao.setStyle("-fx-font-size: 15px; "
                    + "-fx-padding: 10 14; "
                    + "-fx-font-family: 'Segoe UI', sans-serif; "
                    + "-fx-line-spacing: 2.5px; "
                    + "-fx-text-fill: #000000; "
                    + "-fx-background-color: #D9FDD3; "
                    + "-fx-background-radius: 12 12 0 12; "
                    + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.06), 2, 0, 0, 1);");
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // Estilo WhatsApp - Mensagem Recebida (Branco Puro, Texto Preto)
            balao.setStyle("-fx-font-size: 15px; "
                    + "-fx-padding: 10 14; "
                    + "-fx-font-family: 'Segoe UI', sans-serif; "
                    + "-fx-line-spacing: 2.5px; "
                    + "-fx-text-fill: #000000; "
                    + "-fx-background-color: #FFFFFF; "
                    + "-fx-background-radius: 12 12 12 0; "
                    + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.06), 2, 0, 0, 1);");
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }

        Platform.runLater(() -> {
            containerMensagens.getChildren().add(wrapper);
            // Garante que o scroll desça suavemente até o final da nova mensagem
            Platform.runLater(() -> scrollChat.setVvalue(1.0));
        });
    }

    private Label adicionarBalaoDeCarregamento() {
        Label lbl = new Label("Analisando cenário...");
        lbl.setStyle("-fx-text-fill: #999999; -fx-font-style: italic; -fx-font-size: 11px;");
        VBox wrapper = new VBox(lbl);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        Platform.runLater(() -> containerMensagens.getChildren().add(wrapper));
        return lbl;
    }

    @FXML
    private void fecharPainel() {
        if (mainController != null) {
            mainController.alternarPainelIA();
        }
    }
}