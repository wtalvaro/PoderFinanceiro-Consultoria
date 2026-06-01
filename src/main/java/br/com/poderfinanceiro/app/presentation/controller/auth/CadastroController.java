package br.com.poderfinanceiro.app.presentation.controller.auth;

import br.com.poderfinanceiro.app.application.facade.IAuthFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <h1>CadastroController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela tela de registro de novos
 * consultores.
 * Implementa o padrão <b>Humble Object</b>, utilizando ValidationUtils para
 * garantir a integridade dos dados antes da persistência via Virtual Threads.
 * </p>
 */
@Component
public class CadastroController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(CadastroController.class);
    private static final String LOG_PREFIX = "[CadastroController]";
    private static final String URL_AI_STUDIO = "https://aistudio.google.com/";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IAuthFacade authFacade;
    private final Navigator navigator;
    private final HostServices hostServices;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
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

    public CadastroController(IAuthFacade authFacade, Navigator navigator, HostServices hostServices) {
        this.authFacade = authFacade;
        this.navigator = navigator;
        this.hostServices = hostServices;
        log.info("{} [SISTEMA] Controlador de cadastro instanciado com suporte a ValidationUtils.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: FLUXO PRINCIPAL DE CADASTRO
    // ==========================================================================================
    @FXML
    private void handleCadastrar() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().toLowerCase().trim();
        String nome = txtNome.getText().trim();
        String senha = txtSenha.getText();
        String apiKey = txtGeminiApiKey.getText().trim();

        log.info("{} [TELEMETRIA] Iniciando orquestração de novo cadastro. Username: '{}'", LOG_PREFIX, username);

        if (!validarEntradas()) {
            return;
        }

        setLoading(true);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade de autenticação para persistência.", LOG_PREFIX);
                    return authFacade.cadastrarUsuario(nome, username, email, senha, apiKey);
                },
                sucesso -> {
                    log.info("{} [AUDITORIA] Usuário '{}' registrado com sucesso no PostgreSQL.", LOG_PREFIX, username);
                    navigator.navegarPara("/fxml/login.fxml", false);
                },
                erro -> {
                    log.error("{} [AUDITORIA] Falha crítica no cadastro do usuário '{}': {}", LOG_PREFIX, username,
                            erro.getMessage());
                    setLoading(false);
                    exibirErro(erro.getMessage());
                });
    }

    // ==========================================================================================
    // MÓDULO 5: VALIDAÇÃO DE ENTRADAS (Regras de UI via ValidationUtils)
    // ==========================================================================================
    private boolean validarEntradas() {
        log.trace("{} [UI] Iniciando triagem de validação do formulário.", LOG_PREFIX);

        if (txtNome.getText().isBlank() || txtUsername.getText().isBlank() || txtEmail.getText().isBlank()) {
            log.warn("{} [NEGOCIO] Validação abortada: Campos obrigatórios vazios.", LOG_PREFIX);
            exibirErro("Todos os campos são obrigatórios.");
            return false;
        }

        if (!ValidationUtils.isUsernameValido(txtUsername.getText())) {
            log.warn("{} [NEGOCIO] Validação abortada: Username inválido ou com espaços.", LOG_PREFIX);
            exibirErro("O nome de usuário deve ter 3+ caracteres e não conter espaços.");
            return false;
        }

        if (!ValidationUtils.isEmailValido(txtEmail.getText())) {
            log.warn("{} [NEGOCIO] Validação abortada: Formato de e-mail inválido.", LOG_PREFIX);
            exibirErro("Por favor, insira um e-mail válido.");
            return false;
        }

        if (!ValidationUtils.isSenhaForte(txtSenha.getText())) {
            log.warn("{} [NEGOCIO] Validação abortada: Senha abaixo do padrão de segurança.", LOG_PREFIX);
            exibirErro("A senha deve ter pelo menos 8 caracteres.");
            return false;
        }

        if (!txtSenha.getText().equals(txtConfirmarSenha.getText())) {
            log.warn("{} [NEGOCIO] Validação abortada: Divergência na confirmação de senha.", LOG_PREFIX);
            exibirErro("As senhas informadas não coincidem.");
            return false;
        }

        if (txtGeminiApiKey.getText() == null || txtGeminiApiKey.getText().trim().isBlank()) {
            log.warn("{} [NEGOCIO] Validação abortada: API Key do Gemini não fornecida.", LOG_PREFIX);
            exibirErro("A chave de API do Gemini é obrigatória para habilitar a IA.");
            return false;
        }

        log.debug("{} [UI] Formulário validado com sucesso.", LOG_PREFIX);
        return true;
    }

    // ==========================================================================================
    // MÓDULO 6: NAVEGAÇÃO E INTEGRAÇÕES
    // ==========================================================================================
    @FXML
    private void handleVoltarLogin() {
        log.info("{} [TELEMETRIA] Retornando à tela de login por solicitação do usuário.", LOG_PREFIX);
        navigator.navegarPara("/fxml/login.fxml", false);
    }

    @FXML
    private void abrirGoogleAIStudio() {
        log.info("{} [TELEMETRIA] Invocando navegador externo para Google AI Studio.", LOG_PREFIX);
        if (hostServices != null) {
            hostServices.showDocument(URL_AI_STUDIO);
        } else {
            log.error("{} [SISTEMA] Falha ao abrir link: HostServices nulo.", LOG_PREFIX);
        }
    }

    // ==========================================================================================
    // MÓDULO 7: UTILITÁRIOS DE INTERFACE
    // ==========================================================================================
    private void setLoading(boolean loading) {
        log.trace("{} [UI] Atualizando estado de bloqueio da interface: {}", LOG_PREFIX, loading);
        btnCadastrar.setDisable(loading);
        txtNome.setDisable(loading);
        txtUsername.setDisable(loading);
        txtEmail.setDisable(loading);
        txtSenha.setDisable(loading);
        txtConfirmarSenha.setDisable(loading);
        txtGeminiApiKey.setDisable(loading);

        if (loading) {
            lblMensagem.setVisible(false);
        }
    }

    private void exibirErro(String msg) {
        log.debug("{} [UI] Exibindo feedback de erro ao usuário: {}", LOG_PREFIX, msg);
        Platform.runLater(() -> {
            lblMensagem.setText(msg);
            lblMensagem.setVisible(true);
        });
    }
}
