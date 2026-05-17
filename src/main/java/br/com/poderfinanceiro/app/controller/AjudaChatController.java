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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.File;

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
    @FXML
    private HBox boxAnexo;
    @FXML
    private Label lblNomeFicheiro;

    private File ficheiroAnexado = null; // Guarda o ficheiro em memória

    private final GeminiService geminiService;
    private final AuthService authService; // 🚀 Injeção adicionada
    private MainController mainController;

    // Construtor atualizado para o Spring injetar o AuthService automaticamente
    public AjudaChatController(GeminiService geminiService, AuthService authService) {
        this.geminiService = geminiService;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        // 🚀 O SEGREDO: Fica observando o VBox crescer. Se cresceu, empurra o scroll
        // pro final!
        containerMensagens.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollChat.setVvalue(1.0);
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // 📎 NOVO: Método para escolher o ficheiro
    @FXML
    private void escolherFicheiro() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Anexar Documento ou Holerite");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos e Imagens", "*.pdf", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtMensagem.getScene().getWindow());
        if (file != null) {
            ficheiroAnexado = file;
            lblNomeFicheiro.setText(file.getName());
            boxAnexo.setVisible(true);
            boxAnexo.setManaged(true);
        }
    }

    // ✕ NOVO: Método para limpar o anexo
    @FXML
    private void removerAnexo() {
        ficheiroAnexado = null;
        boxAnexo.setVisible(false);
        boxAnexo.setManaged(false);
    }

    // 🚀 ATUALIZADO: O método enviarMensagem agora passa o ficheiro
    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();

        // Se não houver texto E não houver ficheiro, aborta
        if ((texto == null || texto.trim().isEmpty()) && ficheiroAnexado == null) {
            return;
        }

        // 🎯 A CORREÇÃO: Verificamos se o texto é null antes de verificar se está vazio
        String mensagemExibida = (texto == null || texto.trim().isEmpty()) ? "📄 Analise este documento." : texto;

        txtMensagem.clear();
        adicionarBalao(mensagemExibida, true);

        // Guarda a referência do ficheiro para a Task e limpa a UI
        File ficheiroParaEnvio = ficheiroAnexado;
        removerAnexo();

        Label lblPensando = adicionarBalaoDeCarregamento();

        Task<String> taskIA = new Task<>() {
            @Override
            protected String call() throws Exception {
                String tokenDoConsultor = (authService.getUsuarioLogado() != null)
                        ? authService.getUsuarioLogado().getGeminiApiKey()
                        : null;

                // Passa o texto, a chave e o ficheiro (se existir)
                return geminiService.perguntarAoAssistente(mensagemExibida, tokenDoConsultor, ficheiroParaEnvio);
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
                    "✅ Sua chave de API foi atualizada com sucesso no banco de dados! O Gemini já está operando com as novas credenciais.",
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

        // 1. 🖥️ CAPTURA A FONTE NATIVA DO SISTEMA OPERACIONAL (Win11, Mac ou Linux)
        String fonteSistema = javafx.scene.text.Font.getDefault().getFamily();

        TextArea balao = new TextArea(texto);
        balao.setWrapText(true);
        balao.setEditable(false);
        balao.setFocusTraversable(false);

        // 2. APLICA A FONTE REAL DO SO
        balao.setStyle("-fx-background-color: transparent; "
                + "-fx-control-inner-background: transparent; "
                + "-fx-background-insets: 0; "
                + "-fx-padding: 0; "
                + "-fx-border-color: transparent; "
                + "-fx-font-size: 15px; "
                + "-fx-font-family: '" + fonteSistema + "', sans-serif; "
                + "-fx-text-fill: #000000;");

        // 3. A FITA MÉTRICA (Agora usando a mesma fonte do SO para não errar o cálculo)
        javafx.scene.text.Text medidor = new javafx.scene.text.Text(texto);
        medidor.setFont(javafx.scene.text.Font.font(fonteSistema, 15));
        medidor.setWrappingWidth(300);

        // 4. O ANTÍDOTO DO SCROLL: +25px de margem de segurança!
        // O TextArea precisa desse "respiro" para o cursor invisível não ativar a barra
        // de rolagem.
        double larguraReal = medidor.getLayoutBounds().getWidth() + 15;
        double alturaReal = medidor.getLayoutBounds().getHeight() + 25;

        balao.setPrefWidth(larguraReal);
        balao.setMaxWidth(larguraReal);
        balao.setPrefHeight(alturaReal);
        balao.setMinHeight(alturaReal);
        balao.setMaxHeight(alturaReal);

        // VBox da Bolha Visual
        VBox bolhaVisual = new VBox(balao);
        // Reduzi ligeiramente o padding vertical do VBox para compensar os 25px extras
        // do TextArea
        bolhaVisual.setPadding(new javafx.geometry.Insets(6, 14, 6, 14));
        bolhaVisual.setMaxWidth(340);

        VBox wrapper = new VBox(bolhaVisual);

        if (isUsuario) {
            bolhaVisual.setStyle("-fx-background-color: #D9FDD3; "
                    + "-fx-background-radius: 12 12 0 12; "
                    + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.06), 2, 0, 0, 1);");
            wrapper.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        } else {
            bolhaVisual.setStyle("-fx-background-color: #FFFFFF; "
                    + "-fx-background-radius: 12 12 12 0; "
                    + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.06), 2, 0, 0, 1);");
            wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        javafx.application.Platform.runLater(() -> {
            containerMensagens.getChildren().add(wrapper);
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