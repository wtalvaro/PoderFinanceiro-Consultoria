package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private final AuthService authService;
    private final MainController mainController;
    // 1. Adicionado o StatusBarController aqui
    private final StatusBarController statusBarController;

    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtSenha;
    @FXML
    private Button btnLogin;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Label lblMensagem;

    // 2. Injetado no construtor
    public LoginController(AuthService authService, MainController mainController,
            StatusBarController statusBarController) {
        this.authService = authService;
        this.mainController = mainController;
        this.statusBarController = statusBarController; // Inicializou!
    }

    @FXML
    public void initialize() {
        lblMensagem.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String email = txtEmail.getText();
        String senha = txtSenha.getText();

        if (email.isEmpty() || senha.isEmpty()) {
            exibirErro("Por favor, preencha todos os campos.");
            return;
        }

        setLoading(true);

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return authService.login(email, senha);
            }
        };

        loginTask.setOnSucceeded(event -> {
            if (loginTask.getValue()) {
                // 3. O SEGREDO ESTÁ AQUI: Atualiza a barra ANTES de mostrar a tela
                statusBarController.atualizarStatusUsuario();

                // Redireciona para a cena principal
                mainController.navegarPara("/fxml/workspace.fxml", true);
            } else {
                setLoading(false);
                exibirErro("E-mail ou senha incorretos.");
            }
        });

        loginTask.setOnFailed(event -> {
            setLoading(false);
            exibirErro("Erro ao conectar ao banco de dados.");
            loginTask.getException().printStackTrace();
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleIrParaCadastro() {
        mainController.navegarPara("/fxml/cadastro.fxml", false);
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        btnLogin.setDisable(loading);
        txtEmail.setDisable(loading);
        txtSenha.setDisable(loading);
        if (loading)
            lblMensagem.setVisible(false);
    }

    private void exibirErro(String mensagem) {
        lblMensagem.setText(mensagem);
        lblMensagem.setVisible(true);
    }
}