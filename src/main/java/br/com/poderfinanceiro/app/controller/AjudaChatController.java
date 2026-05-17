package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.AuthService; // 🚀 Importado
import br.com.poderfinanceiro.app.service.GeminiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
    @FXML
    private VBox paneConfigChave;
    @FXML
    private PasswordField txtNovaApiKey;

    private final GeminiService geminiService;
    private final AuthService authService; // 🚀 Injeção adicionada
    private MainController mainController;

    // Construtor atualizado para o Spring injetar o AuthService automaticamente
    public AjudaChatController(GeminiService geminiService, AuthService authService) {
        this.geminiService = geminiService;
        this.authService = authService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();
        if (texto == null || texto.trim().isEmpty())
            return;

        txtMensagem.clear();
        adicionarBalao(texto, true);

        Label lblPensando = adicionarBalaoDeCarregamento();

        // 3. Dispara a chamada para a IA em Background
        Task<String> taskIA = new Task<>() {
            @Override
            protected String call() throws Exception {
                // 🚀 Captura dinamicamente o token do consultor logado na sessão do sistema
                String tokenDoConsultor = (authService.getUsuarioLogado() != null)
                        ? authService.getUsuarioLogado().getGeminiApiKey()
                        : null;

                // Passa os dois parâmetros exigidos pelo novo motor BYOK
                return geminiService.perguntarAoAssistente(texto, tokenDoConsultor);
            }
        };

        taskIA.setOnSucceeded(e -> {
            containerMensagens.getChildren().remove(lblPensando);
            adicionarBalao(taskIA.getValue(), false);
        });

        taskIA.setOnFailed(e -> {
            containerMensagens.getChildren().remove(lblPensando);
            adicionarBalao("Desculpe, ocorreu uma falha técnica ao consultar o sistema.", false);
        });

        new Thread(taskIA).start();
    }

    // ⚙️ NOVO: Método que expande ou recolhe a aba de configuração
    @FXML
    private void toggleConfigChave() {
        boolean visivel = !paneConfigChave.isVisible();
        paneConfigChave.setVisible(visivel);
        paneConfigChave.setManaged(visivel); // Força o JavaFX a recalcular o espaço ocupado

        // Já preenche o campo com a chave atual se o usuário estiver ativo na sessão
        if (visivel && authService.getUsuarioLogado() != null) {
            txtNovaApiKey.setText(authService.getUsuarioLogado().getGeminiApiKey());
        }
    }

    // ⚙️ NOVO: Persiste a nova chave direto no banco e atualiza a sessão local
    @FXML
    private void salvarNovaChave() {
        String chaveDigitada = txtNovaApiKey.getText();
        if (chaveDigitada == null || chaveDigitada.trim().isBlank()) {
            adicionarBalao("⚠️ A chave de API não pode estar em branco.", false);
            return;
        }

        try {
            // Dispara o update na camada de serviço
            authService.atualizarGeminiApiKey(chaveDigitada.trim());

            // Recolhe o painel de forma suave
            paneConfigChave.setVisible(false);
            paneConfigChave.setManaged(false);

            adicionarBalao(
                    "✅ Sua chave de API foi atualizada com sucesso no banco de dados! O Copilot já está operando com as novas credenciais.",
                    false);
        } catch (Exception e) {
            adicionarBalao("⚠️ Erro ao persistir nova chave: " + e.getMessage(), false);
        }
    }

    // ⚙️ NOVO: Atalho para o consultor gerar um novo token sem sair do chat
    @FXML
    private void abrirGoogleAIStudioChat() {
        if (mainController != null && mainController.getHostServices() != null) {
            mainController.getHostServices().showDocument("https://aistudio.google.com/");
        }
    }

    private void adicionarBalao(String texto, boolean isUsuario) {
        Label balao = new Label(texto);
        balao.setWrapText(true);
        balao.setMaxWidth(320);

        VBox wrapper = new VBox(balao);

        if (isUsuario) {
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