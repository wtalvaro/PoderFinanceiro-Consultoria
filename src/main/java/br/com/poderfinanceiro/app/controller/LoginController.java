package br.com.poderfinanceiro.app.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.domain.service.AuthService;

@Component
public class LoginController {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String MSG_ERRO_CAMPOS_VAZIOS = "Por favor, preencha todos os campos.";
    private static final String MSG_ERRO_CREDENCIAIS = "Nome de Usuário ou senha incorretos.";
    private static final String MSG_ERRO_CONEXAO = "Erro ao conectar ao banco de dados.";

    private static final String ROTA_WORKSPACE = "/fxml/workspace.fxml";
    private static final String ROTA_CADASTRO = "/fxml/cadastro.fxml";

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
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

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final AuthService authService;
    private final MainController mainController;
    private final StatusBarController statusBarController;

    public LoginController(AuthService authService, MainController mainController,
            StatusBarController statusBarController) {
        this.authService = authService;
        this.mainController = mainController;
        this.statusBarController = statusBarController;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        lblMensagem.setVisible(false);
    }

    // =========================================================================
    // FLUXO DE LOGIN (SRP Aplicado)
    // =========================================================================
    @FXML
    private void handleLogin() {
        String email = txtEmail.getText();
        String senha = txtSenha.getText();

        if (isInputInvalido(email, senha)) {
            exibirErro(MSG_ERRO_CAMPOS_VAZIOS);
            return;
        }

        alternarEstadoCarregamento(true);
        executarLoginAssincrono(email, senha);
    }

    private boolean isInputInvalido(String email, String senha) {
        return email == null || email.isBlank() || senha == null || senha.isBlank();
    }

    private void executarLoginAssincrono(String email, String senha) {
        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return authService.login(email, senha);
            }
        };

        loginTask.setOnSucceeded(event -> processarResultadoLogin(loginTask.getValue()));
        loginTask.setOnFailed(event -> processarErroLogin(loginTask.getException()));

        Thread thread = new Thread(loginTask);
        thread.setDaemon(true); // Garante que a JVM encerre mesmo se a Task estiver rodando
        thread.start();
    }

    private void processarResultadoLogin(boolean loginBemSucedido) {
        alternarEstadoCarregamento(false);

        if (loginBemSucedido) {
            statusBarController.atualizarStatusUsuario();
            txtSenha.clear(); // Limpa a senha por segurança
            mainController.navegarPara(ROTA_WORKSPACE, true);
        } else {
            exibirErro(MSG_ERRO_CREDENCIAIS);
        }
    }

    private void processarErroLogin(Throwable excecao) {
        alternarEstadoCarregamento(false);
        exibirErro(MSG_ERRO_CONEXAO);

        if (excecao != null) {
            excecao.printStackTrace();
        }
    }

    // =========================================================================
    // NAVEGAÇÃO E UTILITÁRIOS DE UI
    // =========================================================================
    @FXML
    private void handleIrParaCadastro() {
        mainController.navegarPara(ROTA_CADASTRO, false);
    }

    private void alternarEstadoCarregamento(boolean isCarregando) {
        progress.setVisible(isCarregando);
        btnLogin.setDisable(isCarregando);
        txtEmail.setDisable(isCarregando);
        txtSenha.setDisable(isCarregando);

        if (isCarregando) {
            lblMensagem.setVisible(false);
        }
    }

    private void exibirErro(String mensagem) {
        lblMensagem.setText(mensagem);
        lblMensagem.setVisible(true);
    }
}