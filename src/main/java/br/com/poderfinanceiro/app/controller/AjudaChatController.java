package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.TabelaJurosService;
import br.com.poderfinanceiro.app.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.utils.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.GeminiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.File;

// 🚀 IMPORTS DO WEBVIEW ADICIONADOS AQUI
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

@Component
@Scope("prototype")
public class AjudaChatController {

    @FXML
    private TextField txtMensagem;
    @FXML
    private VBox paneConfigChave;
    @FXML
    private PasswordField txtNovaApiKey;
    @FXML
    private HBox boxAnexo;
    @FXML
    private Label lblNomeFicheiro;

    // 🚀 DECLARAÇÃO CORRIGIDA
    @FXML
    private WebView webViewChat;
    private WebEngine webEngine;

    private File ficheiroAnexado = null;

    private final GeminiService geminiService;
    private final AuthService authService;
    private final AtendimentoContextService contextoService;
    private final TabelaJurosService tabelaService;
    private final LinkUtilRepository linkRepository;

    private MainController mainController;

    public AjudaChatController(GeminiService geminiService, AuthService authService,
            AtendimentoContextService contextoService,
            TabelaJurosService tabelaService, LinkUtilRepository linkRepository) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.contextoService = contextoService;
        this.tabelaService = tabelaService;
        this.linkRepository = linkRepository;
    }

    @FXML
    public void initialize() {
        webEngine = webViewChat.getEngine();

        java.net.URL url = getClass().getResource("/html/chat.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("⚠️ Arquivo chat.html não encontrado!");
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

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

    @FXML
    private void removerAnexo() {
        ficheiroAnexado = null;
        boxAnexo.setVisible(false);
        boxAnexo.setManaged(false);
    }

    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();

        if ((texto == null || texto.trim().isEmpty()) && ficheiroAnexado == null) {
            return;
        }

        String mensagemExibida = (texto == null || texto.trim().isEmpty()) ? "📄 Analise este documento." : texto;

        txtMensagem.clear();
        adicionarBalao(mensagemExibida, true);

        final File ficheiroParaEnvio = ficheiroAnexado;
        removerAnexo();

        // 🚀 MOSTRA O CARREGAMENTO VIA JAVASCRIPT
        Platform.runLater(() -> {
            try {
                webEngine.executeScript("mostrarCarregamentoJS();");
            } catch (Exception e) {
            }
        });

        Task<String> taskIA = new Task<>() {
            @Override
            protected String call() throws Exception {
                String token = (authService.getUsuarioLogado() != null)
                        ? authService.getUsuarioLogado().getGeminiApiKey()
                        : null;

                String jsonCliente = SummaryGeneratorUtils.gerarJsonContextualParaIA(contextoService.getLeadAtivo());
                java.util.List<br.com.poderfinanceiro.app.model.TabelaJurosModel> listaTabelas = tabelaService
                        .listarAtivas();
                String jsonTabelas = SummaryGeneratorUtils.gerarJsonTabelasJuros(listaTabelas);
                var listaLinks = linkRepository.findAll();
                String jsonLinks = SummaryGeneratorUtils.gerarJsonLinksUteis(listaLinks);

                return geminiService.perguntarAoAssistente(mensagemExibida, token, ficheiroParaEnvio,
                        jsonCliente, jsonTabelas, jsonLinks);
            }
        };

        taskIA.setOnSucceeded(e -> {
            // 🚀 REMOVE O CARREGAMENTO VIA JAVASCRIPT E ADICIONA A RESPOSTA
            Platform.runLater(() -> {
                try {
                    webEngine.executeScript("removerCarregamentoJS();");
                } catch (Exception ex) {
                }
                adicionarBalao(taskIA.getValue(), false);
            });
        });

        taskIA.setOnFailed(e -> {
            // 🚀 REMOVE O CARREGAMENTO E MOSTRA O ERRO
            Platform.runLater(() -> {
                try {
                    webEngine.executeScript("removerCarregamentoJS();");
                } catch (Exception ex) {
                }
                adicionarBalao("Desculpe, ocorreu uma falha técnica ao consultar o sistema.", false);
            });
        });

        new Thread(taskIA).start();
    }

    @FXML
    private void toggleConfigChave() {
        boolean visivel = !paneConfigChave.isVisible();
        paneConfigChave.setVisible(visivel);
        paneConfigChave.setManaged(visivel);

        if (visivel && authService.getUsuarioLogado() != null) {
            txtNovaApiKey.setText(authService.getUsuarioLogado().getGeminiApiKey());
        }
    }

    @FXML
    private void salvarNovaChave() {
        String chaveDigitada = txtNovaApiKey.getText();
        if (chaveDigitada == null || chaveDigitada.trim().isBlank()) {
            adicionarBalao("⚠️ A chave de API não pode estar em branco.", false);
            return;
        }

        try {
            authService.atualizarGeminiApiKey(chaveDigitada.trim());
            paneConfigChave.setVisible(false);
            paneConfigChave.setManaged(false);
            adicionarBalao(
                    "✅ Sua chave de API foi atualizada com sucesso no banco de dados! O Gemini já está operando com as novas credenciais.",
                    false);
        } catch (Exception e) {
            adicionarBalao("⚠️ Erro ao persistir nova chave: " + e.getMessage(), false);
        }
    }

    @FXML
    private void abrirGoogleAIStudioChat() {
        if (mainController != null && mainController.getHostServices() != null) {
            mainController.getHostServices().showDocument("https://aistudio.google.com/");
        }
    }

    private void adicionarBalao(String texto, boolean isUsuario) {
        Platform.runLater(() -> {
            try {
                String textoB64 = java.util.Base64.getEncoder()
                        .encodeToString(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String script = String.format("adicionarBalaoJS('%s', %b);", textoB64, isUsuario);
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("Erro ao injetar script no WebView: " + e.getMessage());
            }
        });
    }

    @FXML
    private void fecharPainel() {
        if (mainController != null) {
            mainController.alternarPainelIA();
        }
    }
}