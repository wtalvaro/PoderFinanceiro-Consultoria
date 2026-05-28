package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.facade.IAuthFacade;
import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
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
 * consultores. Implementa o padrão <b>Humble Object</b>, delegando a
 * persistência e regras de negócio para a {@link IAuthFacade}.
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
    @FXML private TextField txtNome;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtSenha;
    @FXML private PasswordField txtConfirmarSenha;
    @FXML private Label lblMensagem;
    @FXML private Button btnCadastrar;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtGeminiApiKey;

    public CadastroController(IAuthFacade authFacade, Navigator navigator, HostServices hostServices) {
        this.authFacade = authFacade;
        this.navigator = navigator;
        this.hostServices = hostServices;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: FLUXO PRINCIPAL DE CADASTRO
    // ==========================================================================================
    @FXML private void handleCadastrar() {
        String username = txtUsername.getText().trim();
        log.info("{} [TELEMETRIA] Tentativa de cadastro iniciada. Username: '{}'", LOG_PREFIX, username);

        if (!validarEntradas()) {
            return; // A validação já loga o motivo e exibe o erro na UI
        }

        setLoading(true);

        AsyncUtils.executarTaskAsync(() -> authFacade.cadastrarUsuario(txtNome.getText().trim(), username,
                txtEmail.getText().toLowerCase().trim(), txtSenha.getText(), txtGeminiApiKey.getText().trim()), sucesso -> {
                    log.info("{} [AUDITORIA] Usuário '{}' cadastrado com sucesso. Redirecionando para login.", LOG_PREFIX, username);
                    navigator.navegarPara("/fxml/login.fxml", false);
                }, erro -> {
                    log.error("{} [AUDITORIA] Falha no cadastro do usuário '{}': {}", LOG_PREFIX, username, erro.getMessage());
                    setLoading(false);
                    exibirErro(erro.getMessage());
                });
    }

    // ==========================================================================================
    // MÓDULO 5: VALIDAÇÃO DE ENTRADAS (Regras de UI)
    // ==========================================================================================
    private boolean validarEntradas() {
        log.trace("{} [UI] Validando campos do formulário de cadastro.", LOG_PREFIX);

        if (txtNome.getText().isBlank() || txtUsername.getText().isBlank() || txtEmail.getText().isBlank()) {
            log.warn("{} [NEGOCIO] Validação falhou: Campos obrigatórios em branco.", LOG_PREFIX);
            exibirErro("Todos os campos são obrigatórios.");
            return false;
        }

        if (txtUsername.getText().contains(" ")) {
            log.warn("{} [NEGOCIO] Validação falhou: Username contém espaços.", LOG_PREFIX);
            exibirErro("O nome de usuário não pode conter espaços.");
            return false;
        }

        if (!txtEmail.getText().contains("@")) {
            log.warn("{} [NEGOCIO] Validação falhou: E-mail inválido (sem '@').", LOG_PREFIX);
            exibirErro("Insira um e-mail válido.");
            return false;
        }

        if (txtSenha.getText().length() < 8) {
            log.warn("{} [NEGOCIO] Validação falhou: Senha muito curta.", LOG_PREFIX);
            exibirErro("A senha deve ter pelo menos 8 caracteres.");
            return false;
        }

        if (!txtSenha.getText().equals(txtConfirmarSenha.getText())) {
            log.warn("{} [NEGOCIO] Validação falhou: Senhas não coincidem.", LOG_PREFIX);
            exibirErro("As senhas informadas não coincidem.");
            return false;
        }

        if (txtGeminiApiKey.getText() == null || txtGeminiApiKey.getText().trim().isBlank()) {
            log.warn("{} [NEGOCIO] Validação falhou: API Key do Gemini ausente.", LOG_PREFIX);
            exibirErro("A chave de API do Gemini é obrigatória para o suporte analítico.");
            return false;
        }

        log.debug("{} [UI] Validação do formulário aprovada.", LOG_PREFIX);
        return true;
    }

    // ==========================================================================================
    // MÓDULO 6: NAVEGAÇÃO E INTEGRAÇÕES
    // ==========================================================================================
    @FXML private void handleVoltarLogin() {
        log.info("{} [TELEMETRIA] Usuário cancelou o cadastro e retornou ao login.", LOG_PREFIX);
        navigator.navegarPara("/fxml/login.fxml", false);
    }

    @FXML private void abrirGoogleAIStudio() {
        log.info("{} [TELEMETRIA] Solicitando abertura do Google AI Studio no navegador.", LOG_PREFIX);
        if (hostServices != null) {
            hostServices.showDocument(URL_AI_STUDIO);
        } else {
            log.error("{} [SISTEMA] HostServices indisponível. Impossível abrir link externo.", LOG_PREFIX);
        }
    }

    // ==========================================================================================
    // MÓDULO 7: UTILITÁRIOS DE UI
    // ==========================================================================================
    private void setLoading(boolean loading) {
        log.trace("{} [UI] Alterando estado de loading para: {}", LOG_PREFIX, loading);
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
        log.trace("{} [UI] Exibindo mensagem de erro: {}", LOG_PREFIX, msg);
        Platform.runLater(() -> {
            lblMensagem.setText(msg);
            lblMensagem.setVisible(true);
        });
    }
}
