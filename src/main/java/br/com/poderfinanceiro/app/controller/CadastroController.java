package br.com.poderfinanceiro.app.controller;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CadastroController {

    private static final Logger log = LoggerFactory.getLogger(CadastroController.class);

    private final AuthService authService;
    private final Navigator navigator;
    private final HostServices hostServices;

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

    public CadastroController(AuthService authService, Navigator navigator, HostServices hostServices) {
        this.authService = authService;
        this.navigator = navigator;
        this.hostServices = hostServices;
    }

    // =========================================================================
    // FLUXO PRINCIPAL DE CADASTRO
    // =========================================================================
    @FXML
    private void handleCadastrar() {
        log.info("[CADASTRO] Tentativa de cadastro iniciada. username='{}'",
                txtUsername.getText().trim());

        if (!validarEntradas()) {
            // A validação já loga o motivo específico da falha
            return;
        }

        log.info("[CADASTRO] Validação aprovada. Disparando cadastro assíncrono. username='{}'",
                txtUsername.getText().trim());

        setLoading(true);
        long inicio = System.currentTimeMillis();

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("[CADASTRO] Chamando AuthService.cadastrar na thread '{}'.",
                            Thread.currentThread().getName());
                    authService.cadastrar(
                            txtNome.getText().trim(),
                            txtUsername.getText().trim(),
                            txtEmail.getText().toLowerCase().trim(),
                            txtSenha.getText(),
                            txtGeminiApiKey.getText().trim());
                    return null;
                },
                sucesso -> {
                    long tempo = System.currentTimeMillis() - inicio;
                    // Username no log de sucesso permite rastrear qual conta foi criada
                    log.info("[CADASTRO] Usuário '{}' cadastrado com sucesso em {}ms. Redirecionando para login.",
                            txtUsername.getText().trim(), tempo);
                    navigator.navegarPara("/fxml/login.fxml", false);
                },
                erro -> {
                    long tempo = System.currentTimeMillis() - inicio;
                    // Distingue erros de negócio (ex: username já existe) de falhas técnicas
                    if (erro instanceof IllegalArgumentException || erro instanceof IllegalStateException) {
                        log.warn(
                                "[CADASTRO] Cadastro rejeitado por regra de negócio após {}ms. username='{}' | Motivo: {}",
                                tempo, txtUsername.getText().trim(), erro.getMessage());
                    } else {
                        log.error("[CADASTRO] FALHA técnica no cadastro após {}ms. username='{}' | Erro: {}",
                                tempo, txtUsername.getText().trim(), erro.getMessage(), erro);
                    }
                    setLoading(false);
                    exibirErro(erro.getMessage());
                });
    }

    // =========================================================================
    // VALIDAÇÃO DE ENTRADAS
    // =========================================================================
    private boolean validarEntradas() {
        // Campos obrigatórios — sem logar os valores (LGPD)
        if (txtNome.getText().isBlank() || txtUsername.getText().isBlank() || txtEmail.getText().isBlank()) {
            log.warn("[CADASTRO][VALIDAÇÃO] Falhou: campos obrigatórios em branco. " +
                    "nome={} | username={} | email={}",
                    !txtNome.getText().isBlank(),
                    !txtUsername.getText().isBlank(),
                    !txtEmail.getText().isBlank());
            exibirErro("Todos os campos são obrigatórios.");
            return false;
        }

        if (txtUsername.getText().contains(" ")) {
            log.warn("[CADASTRO][VALIDAÇÃO] Falhou: username '{}' contém espaços.",
                    txtUsername.getText().trim());
            exibirErro("O nome de usuário não pode conter espaços.");
            return false;
        }

        if (!txtEmail.getText().contains("@")) {
            // E-mail mascarado: não expõe o valor completo no log
            log.warn("[CADASTRO][VALIDAÇÃO] Falhou: e-mail informado não contém '@'. " +
                    "Tamanho do valor: {} chars.", txtEmail.getText().length());
            exibirErro("Insira um e-mail válido.");
            return false;
        }

        if (txtSenha.getText().length() < 8) {
            // NUNCA logar senha — apenas o comprimento para diagnóstico mínimo
            log.warn("[CADASTRO][VALIDAÇÃO] Falhou: senha com {} caracteres (mínimo: 8).",
                    txtSenha.getText().length());
            exibirErro("A senha deve ter pelo menos 8 caracteres.");
            return false;
        }

        if (!txtSenha.getText().equals(txtConfirmarSenha.getText())) {
            log.warn("[CADASTRO][VALIDAÇÃO] Falhou: senha e confirmação não coincidem.");
            exibirErro("As senhas informadas não coincidem.");
            return false;
        }

        if (txtGeminiApiKey.getText() == null || txtGeminiApiKey.getText().trim().isBlank()) {
            log.warn("[CADASTRO][VALIDAÇÃO] Falhou: API Key do Gemini não preenchida.");
            exibirErro("A chave de API do Gemini é obrigatória para o suporte analítico.");
            return false;
        }

        // Loga prefixo da key apenas para confirmar que não foi colada uma chave errada
        String chaveTrim = txtGeminiApiKey.getText().trim();
        log.debug("[CADASTRO][VALIDAÇÃO] Todas as regras aprovadas. username='{}' | " +
                "API Key: {} chars, prefixo='{}'",
                txtUsername.getText().trim(),
                chaveTrim.length(),
                chaveTrim.length() > 6 ? chaveTrim.substring(0, 6) + "..." : "***");

        return true;
    }

    // =========================================================================
    // NAVEGAÇÃO E INTEGRAÇÕES
    // =========================================================================
    @FXML
    private void handleVoltarLogin() {
        log.info("[CADASTRO] Usuário retornou para a tela de login sem concluir o cadastro.");
        navigator.navegarPara("/fxml/login.fxml", false);
    }

    @FXML
    private void abrirGoogleAIStudio() {
        if (hostServices != null) {
            log.info("[CADASTRO] Abrindo Google AI Studio no browser.");
            hostServices.showDocument("https://aistudio.google.com/");
        } else {
            log.error("[CADASTRO] CRÍTICO: HostServices indisponível. Impossível abrir link externo.");
        }
    }

    // =========================================================================
    // UTILITÁRIOS DE UI
    // =========================================================================
    private void setLoading(boolean loading) {
        log.debug("[CADASTRO][UI] Estado de loading: {}.", loading ? "ATIVADO" : "DESATIVADO");
        btnCadastrar.setDisable(loading);
        txtNome.setDisable(loading);
        txtUsername.setDisable(loading);
        txtEmail.setDisable(loading);
        txtSenha.setDisable(loading);
        txtConfirmarSenha.setDisable(loading);
        txtGeminiApiKey.setDisable(loading);

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