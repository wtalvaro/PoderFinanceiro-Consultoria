package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import br.com.poderfinanceiro.app.service.AuthService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component
public class CadastroController {

    private final AuthService authService;
    private final StageInitializer stageInitializer;

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

    public CadastroController(AuthService authService, StageInitializer stageInitializer) {
        this.authService = authService;
        this.stageInitializer = stageInitializer;
    }

    @FXML
    private void handleCadastrar() {
        if (!validarEntradas())
            return;

        setLoading(true);

        // Task para garantir que o salvamento no Postgres e o Hash BCrypt
        // ocorram fora da UI Thread
        Task<Void> cadastroTask = new Task<>() {
            @Override
            protected Void call() {
                // O service já lida com o @Transactional e o PasswordEncoder
                authService.cadastrar(
                        txtNome.getText().trim(),
                        txtEmail.getText().toLowerCase().trim(),
                        txtSenha.getText());
                return null;
            }
        };

        cadastroTask.setOnSucceeded(e -> {
            // Como o cadastro no AuthService já seta o 'usuarioLogado',
            // podemos ir direto para a tela principal
            stageInitializer.showScene("/fxml/main.fxml", "Poder Financeiro - Consultoria");
        });

        cadastroTask.setOnFailed(e -> {
            setLoading(false);
            // Captura a exceção de e-mail duplicado ou erro de banco
            Throwable exception = cadastroTask.getException();
            exibirErro(exception.getMessage());
        });

        new Thread(cadastroTask).start();
    }

    @FXML
    private void handleVoltarLogin() {
        stageInitializer.showScene("/fxml/login.fxml", "Poder Financeiro - Acesso");
    }

    private boolean validarEntradas() {
        if (txtNome.getText().isBlank() || txtEmail.getText().isBlank()) {
            exibirErro("Todos os campos são obrigatórios.");
            return false;
        }

        // Validação de E-mail simples antes de ir ao banco
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

        return true;
    }

    private void setLoading(boolean loading) {
        btnCadastrar.setDisable(loading);
        txtNome.setDisable(loading);
        txtEmail.setDisable(loading);
        txtSenha.setDisable(loading);
        txtConfirmarSenha.setDisable(loading);
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