package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.AuthService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component
public class CadastroController {

    private final AuthService authService;
    private final MainController mainController;

    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtSenha;
    @FXML
    private PasswordField txtConfirmarSenha;
    @FXML
    private Label lblMensagem;
    @FXML
    private Button btnCadastrar;
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtGeminiApiKey;

    public CadastroController(AuthService authService, MainController mainController) {
        this.authService = authService;
        this.mainController = mainController;
    }

    @FXML
    private void handleCadastrar() {
        if (!validarEntradas())
            return;

        setLoading(true);

        Task<Void> cadastroTask = new Task<>() {
            @Override
            protected Void call() {
                authService.cadastrar(
                        txtNome.getText().trim(),
                        txtUsername.getText().trim(),
                        txtEmail.getText().toLowerCase().trim(),
                        txtSenha.getText(),
                        txtGeminiApiKey.getText().trim());
                return null;
            }
        };

        cadastroTask.setOnSucceeded(e -> {
            // 🎯 CORRIGIDO: Método ajustado de navigatePara para navegarPara
            mainController.navegarPara("/fxml/login.fxml", false);
        });

        cadastroTask.setOnFailed(e -> {
            setLoading(false);
            Throwable exception = cadastroTask.getException();
            exibirErro(exception.getMessage());
        });

        new Thread(cadastroTask).start();
    }

    @FXML
    private void handleVoltarLogin() {
        // 🎯 CORRIGIDO: Método ajustado de navigatePara para navegarPara
        mainController.navegarPara("/fxml/login.fxml", false);
    }

    @FXML
    private void abrirGoogleAIStudio() {
        if (mainController != null && mainController.getHostServices() != null) {
            mainController.getHostServices().showDocument("https://aistudio.google.com/");
        } else {
            System.out.println("Erro técnico: HostServices indisponível no momento.");
        }
    }

    private boolean validarEntradas() {
        if (txtNome.getText().isBlank() || txtUsername.getText().isBlank() || txtEmail.getText().isBlank()) {
            exibirErro("Todos os campos são obrigatórios.");
            return false;
        }

        if (txtUsername.getText().contains(" ")) {
            exibirErro("O nome de usuário não pode conter espaços.");
            return false;
        }

        if (!txtEmail.getText().contains("@")) {
            exibirErro("Insira um e-mail válido.");
            return false;
        }

        if (txtSenha.getText().length() < 8) {
            exibirErro("A senha deve ter pelo menos 8 caracteres.");
            return false;
        }

        if (!txtSenha.getText().equals(txtConfirmarSenha.getText())) {
            exibirErro("As senhas informadas não coincidem.");
            return false;
        }

        if (txtGeminiApiKey.getText() == null || txtGeminiApiKey.getText().trim().isBlank()) {
            exibirErro("A chave de API do Gemini é obrigatória para o suporte analítico.");
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        btnCadastrar.setDisable(loading);
        txtNome.setDisable(loading);
        txtUsername.setDisable(loading);
        txtEmail.setDisable(loading);
        txtSenha.setDisable(loading);
        txtConfirmarSenha.setDisable(loading);
        txtGeminiApiKey.setDisable(loading);

        if (loading)
            lblMensagem.setVisible(false);
    }

    private void exibirErro(String msg) {
        Platform.runLater(() -> {
            lblMensagem.setText(msg);
            lblMensagem.setVisible(true);
        });
    }
}