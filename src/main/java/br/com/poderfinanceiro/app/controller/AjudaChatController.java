package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@Scope("prototype")
public class AjudaChatController {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String MODELO_PADRAO = "gemini-3.5-flash";
    private static final String CAMINHO_CHAT_HTML = "/html/chat.html";
    private static final String NOME_ARQUIVO_CHAT = "chat.html";
    private static final String URL_BLANK = "about:blank";
    private static final String URL_AI_STUDIO = "https://aistudio.google.com/";

    private static final String JS_MOSTRAR_CARREGAMENTO = "mostrarCarregamentoJS();";
    private static final String JS_REMOVER_CARREGAMENTO = "removerCarregamentoJS();";
    private static final String JS_ADICIONAR_BALAO = "adicionarBalaoJS('%s', %b);";

    private static final String MSG_PADRAO_DOCUMENTO = "📄 Analise este documento.";
    private static final String MSG_ERRO_SISTEMA = "Desculpe, ocorreu uma falha técnica ao consultar o sistema.";

    // =========================================================================
    // COMPONENTES DE UI (FXML)
    // =========================================================================
    @FXML
    private TextField txtMensagem;
    @FXML
    private VBox paneConfigChave;
    @FXML
    private PasswordField txtNovaApiKey;
    @FXML
    private HBox boxAnexo;
    @FXML
    private Label lblNomeFicheiro;
    @FXML
    private WebView webViewChat;
    @FXML
    private ComboBox<String> cmbModelo;

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private boolean modelosCarregados = false;
    private boolean paginaCarregada = false;
    private WebEngine webEngine;
    private File ficheiroAnexado = null;
    private MainController mainController;
    private final List<Runnable> filaMensagensPendentes = new ArrayList<>();

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final GeminiService geminiService;
    private final AuthService authService;
    private final AtendimentoContextService contextoService;
    private final TabelaJurosService tabelaService;
    private final LinkUtilRepository linkRepository;

    public AjudaChatController(GeminiService geminiService, AuthService authService,
            AtendimentoContextService contextoService, TabelaJurosService tabelaService,
            LinkUtilRepository linkRepository) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.contextoService = contextoService;
        this.tabelaService = tabelaService;
        this.linkRepository = linkRepository;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarWebView();
        carregarModelosIniciais();
    }

    private void carregarModelosIniciais() {
        String token = (authService.getUsuarioLogado() != null)
                ? authService.getUsuarioLogado().getGeminiApiKey()
                : null;

        carregarModelosDisponiveis(token);
    }

    private void configurarWebView() {
        webEngine = webViewChat.getEngine();
        URL url = getClass().getResource(CAMINHO_CHAT_HTML);

        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("⚠️ Arquivo chat.html não encontrado no classpath!");
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                paginaCarregada = true;
                filaMensagensPendentes.forEach(Platform::runLater);
                filaMensagensPendentes.clear();
            }
        });

        webEngine.setCreatePopupHandler(popupFeatures -> webEngine);
        webEngine.locationProperty()
                .addListener((obs, oldLocation, newLocation) -> lidarComNavegacaoExterna(newLocation));
    }

    private void lidarComNavegacaoExterna(String newLocation) {
        // Early Return pattern: sai rápido se for uma URL irrelevante
        if (newLocation == null || newLocation.isEmpty() || newLocation.equals(URL_BLANK)
                || newLocation.contains(NOME_ARQUIVO_CHAT)) {
            return;
        }

        Platform.runLater(() -> webEngine.getLoadWorker().cancel());
        abrirLinkExterno(newLocation);
    }

    // =========================================================================
    // GESTÃO DE ANEXOS E PAINÉIS
    // =========================================================================
    @FXML
    private void escolherFicheiro() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Anexar Documento ou Holerite");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documentos e Imagens", "*.pdf", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtMensagem.getScene().getWindow());
        if (file != null) {
            ficheiroAnexado = file;
            lblNomeFicheiro.setText(file.getName());
            setVisibilidadeElemento(boxAnexo, true);
        }
    }

    @FXML
    private void removerAnexo() {
        ficheiroAnexado = null;
        setVisibilidadeElemento(boxAnexo, false);
    }

    @FXML
    private void fecharPainel() {
        if (mainController != null)
            mainController.alternarPainelIA();
    }

    @FXML
    private void abrirGoogleAIStudioChat() {
        abrirLinkExterno(URL_AI_STUDIO);
    }

    // =========================================================================
    // LÓGICA DE ENVIO DE MENSAGENS (CORE DA IA)
    // =========================================================================
    // 🚀 PATCH: AjudaChatController.java - Método enviarMensagem()
    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();
        if (isEntradaInvalida(texto))
            return;

        String mensagemExibida = texto.trim().isEmpty() ? MSG_PADRAO_DOCUMENTO : texto;

        prepararInterfaceParaEnvio(mensagemExibida);
        final File ficheiroParaEnvio = ficheiroAnexado;
        removerAnexo();

        // DELEGADO PARA O UTILS!
        AsyncUtils.executarTaskAsync(
                () -> processarChamadaIA(mensagemExibida, ficheiroParaEnvio),
                this::finalizarEnvioInterface,
                erro -> finalizarEnvioInterface(MSG_ERRO_SISTEMA));
    }

    private boolean isEntradaInvalida(String texto) {
        return (texto == null || texto.trim().isEmpty()) && ficheiroAnexado == null;
    }

    private void prepararInterfaceParaEnvio(String mensagem) {
        txtMensagem.clear();
        adicionarBalao(mensagem, true);
        executarScriptSeguro(JS_MOSTRAR_CARREGAMENTO);
    }

    private void finalizarEnvioInterface(String mensagemIA) {
        executarScriptSeguro(JS_REMOVER_CARREGAMENTO);
        adicionarBalao(mensagemIA, false);
    }

    private String processarChamadaIA(String mensagem, File ficheiro) throws Exception {
        String token = authService.getUsuarioLogado() != null ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        String modeloSelecionado = cmbModelo.getValue() != null ? cmbModelo.getValue() : MODELO_PADRAO;

        String jsonFoco = obterContextoPrincipal();
        String jsonComissoes = obterContextoComissoes();
        String jsonTabelas = SummaryGeneratorUtils.gerarJsonTabelasJuros(tabelaService.listarAtivas());
        String jsonLinks = SummaryGeneratorUtils.gerarJsonLinksUteis(linkRepository.findAll());

        return geminiService.perguntarAoAssistente(mensagem, token, modeloSelecionado, ficheiro, jsonFoco, jsonTabelas,
                jsonLinks, jsonComissoes);
    }

    // =========================================================================
    // ENGENHARIA DE CONTEXTO (JSON BUILDERS)
    // =========================================================================
    private String obterContextoPrincipal() {
        AtendimentoContextService.TipoTelaFocada telaAtual = contextoService.getTelaAtualFocada();

        if (telaAtual == AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS) {
            return SummaryGeneratorUtils.gerarJsonPropostaParaIA(contextoService.getPropostaAtiva());
        }

        boolean isAbaCliente = (telaAtual == AtendimentoContextService.TipoTelaFocada.CADASTRO_CLIENTE);
        return SummaryGeneratorUtils.gerarJsonContextualParaIA(contextoService.getLeadAtivo(), isAbaCliente);
    }

    private String obterContextoComissoes() {
        if (contextoService.getTelaAtualFocada() == AtendimentoContextService.TipoTelaFocada.GESTAO_COMISSOES) {
            return SummaryGeneratorUtils.gerarJsonComissoes(contextoService.getComissoesAtivas());
        }
        return "[]";
    }

    // =========================================================================
    // CONFIGURAÇÃO DE CHAVE DE API (ENGRENAGEM)
    // =========================================================================
    @FXML
    private void toggleConfigChave() {
        boolean visivel = !paneConfigChave.isVisible();
        setVisibilidadeElemento(paneConfigChave, visivel);

        if (visivel && authService.getUsuarioLogado() != null) {
            txtNovaApiKey.setText(authService.getUsuarioLogado().getGeminiApiKey());
        }
    }

    // 🚀 PATCH: AjudaChatController.java - Método carregarModelosDisponiveis()
    private void carregarModelosDisponiveis(String token) {
        if (modelosCarregados)
            return;

        // DELEGADO PARA O UTILS!
        AsyncUtils.executarTaskAsync(
                () -> geminiService.listarModelosMultimodais(token),
                modelos -> {
                    String atual = cmbModelo.getValue();
                    cmbModelo.getItems().setAll(modelos);

                    if (atual != null && modelos.contains(atual)) {
                        cmbModelo.getSelectionModel().select(atual);
                    } else if (modelos.contains(MODELO_PADRAO)) {
                        cmbModelo.getSelectionModel().select(MODELO_PADRAO);
                    } else {
                        cmbModelo.getSelectionModel().selectFirst();
                    }

                    modelosCarregados = true;
                },
                null);
    }

    @FXML
    private void salvarNovaChave() {
        String chaveDigitada = txtNovaApiKey.getText();
        if (chaveDigitada == null || chaveDigitada.trim().isBlank()) {
            adicionarBalao("⚠️ A chave de API não pode estar em branco.", false);
            return;
        }

        try {
            authService.atualizarGeminiApiKey(chaveDigitada.trim());
            setVisibilidadeElemento(paneConfigChave, false);
            adicionarBalao(
                    "✅ Sua chave de API foi atualizada com sucesso! O Gemini já está operando com as novas credenciais.",
                    false);
        } catch (Exception e) {
            adicionarBalao("⚠️ Erro ao persistir nova chave: " + e.getMessage(), false);
        }
    }

    // =========================================================================
    // UTILITÁRIOS (DRY, INTEROP & ASYNC)
    // =========================================================================
    private void adicionarBalao(String texto, boolean isUsuario) {
        if (texto == null)
            return;

        Runnable rotinaInjecao = () -> {
            try {
                String textoB64 = Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
                String script = String.format(JS_ADICIONAR_BALAO, textoB64, isUsuario);
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("Erro ao injetar script no WebView: " + e.getMessage());
            }
        };

        if (paginaCarregada)
            Platform.runLater(rotinaInjecao);
        else
            filaMensagensPendentes.add(rotinaInjecao);
    }

    private void executarScriptSeguro(String script) {
        Platform.runLater(() -> {
            try {
                if (paginaCarregada)
                    webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("Aviso: Falha silenciada ao executar JS visual: " + e.getMessage());
            }
        });
    }

    private void abrirLinkExterno(String url) {
        Platform.runLater(() -> {
            if (mainController != null && mainController.getHostServices() != null) {
                mainController.getHostServices().showDocument(url);
            } else {
                System.err.println("⚠️ MainController ou HostServices não configurados para o Chat!");
            }
        });
    }

    /**
     * Utilitário DRY para controle de visibilidade no layout do JavaFX
     */
    private void setVisibilidadeElemento(Node elemento, boolean visivel) {
        elemento.setVisible(visivel);
        elemento.setManaged(visivel);
    }
}