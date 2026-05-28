package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.facade.IAuthFacade;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <h1>LoginController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela tela de autenticação.
 * Implementa o padrão <b>Humble Object</b>, delegando a validação de
 * credenciais e gestão de sessão para a {@link IAuthFacade}.
 * </p>
 */
@Component
public class LoginController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String LOG_PREFIX = "[LoginController]";

    private static final String MSG_ERRO_CAMPOS_VAZIOS = "Por favor, preencha todos os campos.";
    private static final String MSG_ERRO_CREDENCIAIS = "Nome de Usuário ou senha incorretos.";
    private static final String MSG_ERRO_CONEXAO = "Erro ao conectar ao banco de dados.";

    private static final String ROTA_WORKSPACE = "/fxml/workspace.fxml";
    private static final String ROTA_CADASTRO = "/fxml/cadastro.fxml";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IAuthFacade authFacade;
    private final Navigator navigator;
    private final StatusBarController statusBarController;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtSenha;
    @FXML private TextField txtSenhaRevelada;
    @FXML private Button btnLogin;
    @FXML private Button btnMostrarSenha;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblMensagem;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private boolean isSenhaVisivel = false;

    public LoginController(IAuthFacade authFacade, Navigator navigator, StatusBarController statusBarController) {
        this.authFacade = authFacade;
        this.navigator = navigator;
        this.statusBarController = statusBarController;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando tela de Login...", LOG_PREFIX);
        lblMensagem.setVisible(false);
        txtSenhaRevelada.textProperty().bindBidirectional(txtSenha.textProperty());
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 6: FLUXO DE AUTENTICAÇÃO
    // ==========================================================================================
    @FXML private void handleLogin() {
        String username = txtEmail.getText();
        String senha = txtSenha.getText();
        log.info("{} [TELEMETRIA] Tentativa de login iniciada. Username: '{}'", LOG_PREFIX, username);

        if (isInputInvalido(username, senha)) {
            log.warn("{} [NEGOCIO] Login bloqueado: Campos obrigatórios vazios.", LOG_PREFIX);
            exibirErro(MSG_ERRO_CAMPOS_VAZIOS);
            return;
        }

        alternarEstadoCarregamento(true);

        AsyncUtils.executarTaskAsync(() -> authFacade.realizarLogin(username, senha), this::processarResultadoLogin,
                this::processarErroLogin);
    }

    private void processarResultadoLogin(Boolean loginBemSucedido) {
        alternarEstadoCarregamento(false);

        if (Boolean.TRUE.equals(loginBemSucedido)) {
            log.info("{} [AUDITORIA] Autenticação bem-sucedida para o username: '{}'", LOG_PREFIX, txtEmail.getText());
            statusBarController.atualizarStatusUsuario();
            txtSenha.clear();
            navigator.navegarPara(ROTA_WORKSPACE, true);
        } else {
            log.warn("{} [AUDITORIA] Falha na autenticação: Credenciais inválidas para o username: '{}'", LOG_PREFIX, txtEmail.getText());
            exibirErro(MSG_ERRO_CREDENCIAIS);
        }
    }

    private void processarErroLogin(Throwable excecao) {
        alternarEstadoCarregamento(false);
        log.error("{} [SISTEMA] Erro técnico durante o login: {}", LOG_PREFIX, excecao.getMessage(), excecao);
        exibirErro(MSG_ERRO_CONEXAO);
    }

    private boolean isInputInvalido(String username, String senha) {
        return username == null || username.isBlank() || senha == null || senha.isBlank();
    }

    // ==========================================================================================
    // MÓDULO 7: UTILITÁRIOS DE UI E NAVEGAÇÃO
    // ==========================================================================================
    @FXML private void handleToggleSenha() {
        isSenhaVisivel = !isSenhaVisivel;
        log.trace("{} [UI] Alternando visibilidade da senha: {}", LOG_PREFIX, isSenhaVisivel);

        if (isSenhaVisivel) {
            btnMostrarSenha.setText("🔒");
            txtSenhaRevelada.setVisible(true);
            txtSenha.setVisible(false);
            txtSenhaRevelada.requestFocus();
            Platform.runLater(txtSenhaRevelada::selectEnd);
        } else {
            btnMostrarSenha.setText("👁️");
            txtSenha.setVisible(true);
            txtSenhaRevelada.setVisible(false);
            txtSenha.requestFocus();
            Platform.runLater(txtSenha::selectEnd);
        }
    }

    @FXML private void handleIrParaCadastro() {
        log.info("{} [TELEMETRIA] Usuário solicitou navegação para a tela de Cadastro.", LOG_PREFIX);
        navigator.navegarPara(ROTA_CADASTRO, false);
    }

    private void alternarEstadoCarregamento(boolean isCarregando) {
        log.trace("{} [UI] Alterando estado de carregamento para: {}", LOG_PREFIX, isCarregando);
        progress.setVisible(isCarregando);
        btnLogin.setDisable(isCarregando);
        txtEmail.setDisable(isCarregando);
        txtSenha.setDisable(isCarregando);

        if (isCarregando) {
            lblMensagem.setVisible(false);
        }
    }

    private void exibirErro(String mensagem) {
        log.trace("{} [UI] Exibindo mensagem de erro: {}", LOG_PREFIX, mensagem);
        lblMensagem.setText(mensagem);
        lblMensagem.setVisible(true);
    }
}
