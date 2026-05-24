package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;


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
    private final Navigator navigator;
    private final StatusBarController statusBarController;

    public LoginController(AuthService authService, Navigator navigator,
            StatusBarController statusBarController) {
        this.authService = authService;
        this.navigator = navigator;
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

    // 🚀 PATCH: Refatoração do fluxo de login para o AsyncUtils
    private void executarLoginAssincrono(String email, String senha) {
        AsyncUtils.executarTaskAsync(
                () -> authService.login(email, senha),
                this::processarResultadoLogin,
                this::processarErroLogin);
    }

    private void processarResultadoLogin(Boolean loginBemSucedido) {
        alternarEstadoCarregamento(false);

        if (loginBemSucedido != null && loginBemSucedido) {
            statusBarController.atualizarStatusUsuario();
            txtSenha.clear(); // Limpa a senha por segurança
            navigator.navegarPara(ROTA_WORKSPACE, true);
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
        navigator.navegarPara(ROTA_CADASTRO, false);
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