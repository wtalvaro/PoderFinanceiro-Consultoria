package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

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
        log.debug("[LOGIN] Construtor: Controller instanciado");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[LOGIN] initialize: Iniciando configuração da tela de login");
        lblMensagem.setVisible(false);
        log.info("[LOGIN] initialize: Configuração concluída");
    }

    // =========================================================================
    // FLUXO DE LOGIN (SRP Aplicado)
    // =========================================================================
    @FXML
    private void handleLogin() {
        String email = txtEmail.getText();
        String senha = txtSenha.getText();
        log.debug("[LOGIN] handleLogin: Tentativa de login para email='{}'", email);

        if (isInputInvalido(email, senha)) {
            log.warn("[LOGIN] handleLogin: Campos de login vazios (email vazio? {}, senha vazia? {})",
                    email == null || email.isBlank(),
                    senha == null || senha.isBlank());
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
        log.debug("[LOGIN] executarLoginAssincrono: Iniciando chamada assíncrona para autenticação");
        AsyncUtils.executarTaskAsync(
                () -> authService.login(email, senha),
                this::processarResultadoLogin,
                this::processarErroLogin);
    }

    private void processarResultadoLogin(Boolean loginBemSucedido) {
        alternarEstadoCarregamento(false);
        log.debug("[LOGIN] processarResultadoLogin: Resultado recebido = {}", loginBemSucedido);

        if (loginBemSucedido != null && loginBemSucedido) {
            log.info("[LOGIN] Usuário autenticado com sucesso: email='{}'", txtEmail.getText());
            statusBarController.atualizarStatusUsuario();
            txtSenha.clear();
            navigator.navegarPara(ROTA_WORKSPACE, true);
        } else {
            log.warn("[LOGIN] Falha na autenticação: credenciais inválidas para email='{}'", txtEmail.getText());
            exibirErro(MSG_ERRO_CREDENCIAIS);
        }
    }

    private void processarErroLogin(Throwable excecao) {
        alternarEstadoCarregamento(false);
        log.error("[LOGIN] processarErroLogin: {}", MSG_ERRO_CONEXAO, excecao);
        exibirErro(MSG_ERRO_CONEXAO);

        if (excecao != null) {
            log.error("[LOGIN][ERRO] Erro: {}", excecao.getMessage(), excecao);
        }
    }

    // =========================================================================
    // NAVEGAÇÃO E UTILITÁRIOS DE UI
    // =========================================================================
    @FXML
    private void handleIrParaCadastro() {
        log.info("[LOGIN] Navegando para tela de cadastro");
        navigator.navegarPara(ROTA_CADASTRO, false);
    }

    private void alternarEstadoCarregamento(boolean isCarregando) {
        log.trace("[LOGIN] alternarEstadoCarregamento: isCarregando={}", isCarregando);
        progress.setVisible(isCarregando);
        btnLogin.setDisable(isCarregando);
        txtEmail.setDisable(isCarregando);
        txtSenha.setDisable(isCarregando);

        if (isCarregando) {
            lblMensagem.setVisible(false);
        }
    }

    private void exibirErro(String mensagem) {
        log.debug("[LOGIN] exibirErro: {}", mensagem);
        lblMensagem.setText(mensagem);
        lblMensagem.setVisible(true);
    }
}