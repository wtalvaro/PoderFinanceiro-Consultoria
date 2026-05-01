package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.StageInitializer;
import br.com.poderfinanceiro.app.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private final AuthService authService;
    private final StageInitializer stageInitializer;

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

    public LoginController(AuthService authService, StageInitializer stageInitializer) {
        this.authService = authService;
        this.stageInitializer = stageInitializer;
    }

    @FXML
    public void initialize() {
        // Garante que a mensagem de erro comece escondida
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

        // Inicia o estado de carregamento (Loading State)
        setLoading(true);

        // Task para evitar que a UI trave durante a consulta ao Postgres
        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Simulação opcional de delay de 1s para o usuário perceber o loading
                // Thread.sleep(1000);
                return authService.login(email, senha);
            }
        };

        loginTask.setOnSucceeded(event -> {
            if (loginTask.getValue()) {
                // Redireciona para a cena principal se o login for sucesso
                stageInitializer.showScene("/fxml/main.fxml", "Poder Financeiro - Consultoria");
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
        // Redireciona para a tela de cadastro (implementaremos a seguir)
        stageInitializer.showScene("/fxml/cadastro.fxml", "Poder Financeiro - Novo Cadastro");
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