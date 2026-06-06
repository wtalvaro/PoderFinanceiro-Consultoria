package br.com.poderfinanceiro.app.presentation.controller.auth;

import br.com.poderfinanceiro.app.application.facade.IAuthFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.presentation.controller.layout.StatusBarController;
import br.com.poderfinanceiro.app.presentation.ui.navigation.AppRoute;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
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
 * Refatorado para utilizar o padrão Registry (AppRoute) via Navigator.
 * Implementa o padrão <b>Humble Object</b> e orquestração via Virtual Threads.
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

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IAuthFacade authFacade;
    private final Navigator navigator;
    private final StatusBarController statusBarController;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtSenha;
    @FXML
    private TextField txtSenhaRevelada;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnMostrarSenha;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Label lblMensagem;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private boolean isSenhaVisivel = false;

    public LoginController(IAuthFacade authFacade, Navigator navigator, StatusBarController statusBarController) {
        this.authFacade = authFacade;
        this.navigator = navigator;
        this.statusBarController = statusBarController;
        log.info("{} [SISTEMA] Controlador de login instanciado e sincronizado com Navigator Registry.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando componentes da tela de autenticação.", LOG_PREFIX);
        lblMensagem.setVisible(false);

        // Sincronização bidirecional para o recurso de "mostrar senha"
        txtSenhaRevelada.textProperty().bindBidirectional(txtSenha.textProperty());

        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 6: FLUXO DE AUTENTICAÇÃO
    // ==========================================================================================
    @FXML
    private void handleLogin() {
        String identificador = txtEmail.getText().trim();
        String senha = txtSenha.getText();

        log.info("{} [TELEMETRIA] Iniciando tentativa de login. Identificador: '{}'", LOG_PREFIX, identificador);

        if (!validarInputs(identificador, senha)) {
            return;
        }

        alternarEstadoCarregamento(true);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade para validação de credenciais.", LOG_PREFIX);
                    return authFacade.realizarLogin(identificador, senha);
                },
                this::processarResultadoLogin,
                this::processarErroLogin);
    }

    private boolean validarInputs(String identificador, String senha) {
        if (identificador.isBlank() || senha == null || senha.isBlank()) {
            log.warn("{} [NEGOCIO] Login abortado: Campos obrigatórios vazios.", LOG_PREFIX);
            exibirErro(MSG_ERRO_CAMPOS_VAZIOS);
            return false;
        }

        if (!ValidationUtils.isUsernameValido(identificador) && !ValidationUtils.isEmailValido(identificador)) {
            log.warn("{} [NEGOCIO] Login abortado: Identificador '{}' com formato inválido.", LOG_PREFIX,
                    identificador);
            exibirErro("Identificador inválido.");
            return false;
        }

        return true;
    }

    private void processarResultadoLogin(Boolean loginBemSucedido) {
        alternarEstadoCarregamento(false);

        if (Boolean.TRUE.equals(loginBemSucedido)) {
            log.info("{} [AUDITORIA] Autenticação bem-sucedida para: '{}'", LOG_PREFIX, txtEmail.getText());

            statusBarController.atualizarStatusUsuario();
            txtSenha.clear();

            // CORREÇÃO: Utilizando AppRoute em vez de String/Boolean
            navigator.navegarPara(AppRoute.WORKSPACE);
        } else {
            log.warn("{} [AUDITORIA] Falha na autenticação: Credenciais incorretas para: '{}'", LOG_PREFIX,
                    txtEmail.getText());
            exibirErro(MSG_ERRO_CREDENCIAIS);
        }
    }

    private void processarErroLogin(Throwable excecao) {
        alternarEstadoCarregamento(false);
        log.error("{} [SISTEMA] Erro crítico durante o processo de login: {}", LOG_PREFIX, excecao.getMessage());
        exibirErro(MSG_ERRO_CONEXAO);
    }

    // ==========================================================================================
    // MÓDULO 7: UTILITÁRIOS DE UI E NAVEGAÇÃO
    // ==========================================================================================
    @FXML
    private void handleToggleSenha() {
        isSenhaVisivel = !isSenhaVisivel;
        log.trace("{} [UI] Alternando visibilidade da senha. Visível: {}", LOG_PREFIX, isSenhaVisivel);

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

    @FXML
    private void handleIrParaCadastro() {
        log.info("{} [TELEMETRIA] Redirecionando usuário para o fluxo de cadastro via Registry.", LOG_PREFIX);
        // CORREÇÃO: Utilizando AppRoute em vez de String/Boolean
        navigator.navegarPara(AppRoute.CADASTRO_USUARIO);
    }

    private void alternarEstadoCarregamento(boolean isCarregando) {
        log.trace("{} [UI] Atualizando estado de carregamento: {}", LOG_PREFIX, isCarregando);
        progress.setVisible(isCarregando);
        btnLogin.setDisable(isCarregando);
        txtEmail.setDisable(isCarregando);
        txtSenha.setDisable(isCarregando);

        if (isCarregando) {
            lblMensagem.setVisible(false);
        }
    }

    private void exibirErro(String mensagem) {
        log.debug("{} [UI] Exibindo feedback de erro: {}", LOG_PREFIX, mensagem);
        lblMensagem.setText(mensagem);
        lblMensagem.setVisible(true);
    }
}
