package br.com.poderfinanceiro.app.presentation.controller.suporte;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.facade.IAjudaChatFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.presentation.ui.state.IAModelRegistry;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <h1>AjudaChatController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pelo Chat com a IA Gemini.
 * Implementa o padrão <b>Humble Object</b>, utilizando o IAModelRegistry para
 * padronização de modelos e AsyncUtils para orquestração de I/O em Virtual
 * Threads (Java 25).
 * </p>
 */
@Component
@Scope("prototype")
public class AjudaChatController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(AjudaChatController.class);
    private static final String LOG_PREFIX = "[AjudaChatController]";

    private static final String CAMINHO_CHAT_HTML = "/html/chat.html";
    private static final String NOME_ARQUIVO_CHAT = "chat.html";
    private static final String URL_BLANK = "about:blank";
    private static final String URL_AI_STUDIO = "https://aistudio.google.com/";

    private static final String JS_MOSTRAR_CARREGAMENTO = "mostrarCarregamentoJS();";
    private static final String JS_REMOVER_CARREGAMENTO = "removerCarregamentoJS();";
    private static final String JS_ADICIONAR_BALAO = "adicionarBalaoJS('%s', %b);";

    private static final String MSG_PADRAO_DOCUMENTO = "📄 Analise este documento.";
    private static final String MSG_ERRO_SISTEMA = "Desculpe, ocorreu uma falha técnica ao consultar o sistema.";
    private static final int MAX_TURNOS = 20;

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IAjudaChatFacade chatFacade;
    private final Navigator navigator;
    private final HostServices hostServices;
    private final IAModelRegistry modelRegistry;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
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
    @FXML
    private VBox paneSidebarChats;
    @FXML
    private VBox listaChatsRecentes;
    @FXML
    private HBox overlaySidebar;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private WebEngine webEngine;
    private boolean paginaCarregada = false;
    private File ficheiroAnexado = null;
    private String arquivoSessaoAtual = null;
    private final List<Runnable> filaMensagensPendentes = new ArrayList<>();
    private final List<GeminiRequest.Content> historicoConversa = Collections.synchronizedList(new ArrayList<>());

    public AjudaChatController(IAjudaChatFacade chatFacade,
            Navigator navigator,
            HostServices hostServices,
            IAModelRegistry modelRegistry) {
        this.chatFacade = chatFacade;
        this.navigator = navigator;
        this.hostServices = hostServices;
        this.modelRegistry = modelRegistry;
        log.info("{} [SISTEMA] Controlador do Chat instanciado com suporte a Registry Global.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E WEBVIEW
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface do Chat Cognitivo.", LOG_PREFIX);
        configurarWebView();
        configurarModelosIA();
        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos do controlador de Chat.", LOG_PREFIX);
        historicoConversa.clear();
        filaMensagensPendentes.clear();
        ficheiroAnexado = null;
    }

    private void configurarModelosIA() {
        log.debug("{} [SISTEMA] Vinculando ComboBox ao Registro Global de Modelos.", LOG_PREFIX);
        cmbModelo.setItems(modelRegistry.getModelosDisponiveis());

        // Seleção inicial baseada na especialidade de conversação
        String padrao = modelRegistry.getModeloPadrao();
        cmbModelo.getSelectionModel().select(padrao);

        modelRegistry.carregarModelos();
    }

    private void configurarWebView() {
        log.trace("{} [SISTEMA] Configurando motor de renderização HTML (WebEngine).", LOG_PREFIX);
        webEngine = webViewChat.getEngine();
        URL url = getClass().getResource(CAMINHO_CHAT_HTML);

        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            log.error("{} [SISTEMA] Falha crítica: Recurso '{}' não encontrado.", LOG_PREFIX, CAMINHO_CHAT_HTML);
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                paginaCarregada = true;
                log.info("{} [SISTEMA] Página HTML do chat carregada. Despachando fila de mensagens.", LOG_PREFIX);
                filaMensagensPendentes.forEach(Platform::runLater);
                filaMensagensPendentes.clear();
            } else if (newState == Worker.State.FAILED) {
                log.error("{} [SISTEMA] Falha ao carregar interface HTML do chat.", LOG_PREFIX);
            }
        });

        webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> lidarComNavegacaoExterna(newLoc));
    }

    private void lidarComNavegacaoExterna(String newLocation) {
        if (newLocation == null || newLocation.isEmpty() || newLocation.equals(URL_BLANK)
                || newLocation.contains(NOME_ARQUIVO_CHAT)) {
            return;
        }
        log.info("{} [TELEMETRIA] Interceptada navegação externa: {}. Redirecionando.", LOG_PREFIX, newLocation);
        Platform.runLater(() -> webEngine.getLoadWorker().cancel());
        abrirLinkExterno(newLocation);
    }

    // ==========================================================================================
    // MÓDULO 6: AÇÕES DE CHAT E IA
    // ==========================================================================================
    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();
        if ((texto == null || texto.trim().isEmpty()) && ficheiroAnexado == null) {
            log.warn("{} [NEGOCIO] Tentativa de envio bloqueada: Mensagem e anexo vazios.", LOG_PREFIX);
            return;
        }

        String modeloAtual = cmbModelo.getValue();
        String mensagemExibida = (texto == null || texto.trim().isEmpty()) ? MSG_PADRAO_DOCUMENTO : texto;
        File file = ficheiroAnexado;

        log.info("{} [TELEMETRIA] Enviando turno de conversa. Modelo: {}", LOG_PREFIX, modeloAtual);

        prepararInterfaceParaEnvio(mensagemExibida);
        removerAnexo();

        GeminiRequest.Content userContent = new GeminiRequest.Content("user",
                List.of(GeminiRequest.Part.ofText(mensagemExibida)));
        final String sessaoNoMomentoDoEnvio = arquivoSessaoAtual;
        List<GeminiRequest.Content> historicoAtual = obterHistoricoTruncado();

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade para processamento de IA.", LOG_PREFIX);
                    return chatFacade.enviarMensagemParaIA(mensagemExibida, file, modeloAtual, historicoAtual);
                },
                resposta -> {
                    if (!Objects.equals(arquivoSessaoAtual, sessaoNoMomentoDoEnvio)) {
                        log.warn("{} [NEGOCIO] Sessão alterada durante o processamento. Resposta descartada.",
                                LOG_PREFIX);
                        finalizarEnvioInterface(resposta);
                        return;
                    }
                    log.info("{} [AUDITORIA] Resposta da IA recebida com sucesso.", LOG_PREFIX);
                    historicoConversa.add(userContent);
                    historicoConversa
                            .add(new GeminiRequest.Content("model", List.of(GeminiRequest.Part.ofText(resposta))));
                    finalizarEnvioInterface(resposta);
                },
                erro -> {
                    log.error("{} [AUDITORIA] Falha na comunicação com o Gemini: {}", LOG_PREFIX, erro.getMessage());
                    finalizarEnvioInterface(MSG_ERRO_SISTEMA);
                });
    }

    private List<GeminiRequest.Content> obterHistoricoTruncado() {
        synchronized (historicoConversa) {
            int tamanhoTotal = historicoConversa.size();
            if (tamanhoTotal > MAX_TURNOS) {
                log.debug("{} [NEGOCIO] Truncando histórico para os últimos {} turnos.", LOG_PREFIX, MAX_TURNOS);
                return new ArrayList<>(historicoConversa.subList(tamanhoTotal - MAX_TURNOS, tamanhoTotal));
            }
            return new ArrayList<>(historicoConversa);
        }
    }

    private void prepararInterfaceParaEnvio(String mensagem) {
        txtMensagem.clear();
        adicionarBalao(mensagem, true);
        executarScriptSeguro(JS_MOSTRAR_CARREGAMENTO);
    }

    private void finalizarEnvioInterface(String mensagemIA) {
        executarScriptSeguro(JS_REMOVER_CARREGAMENTO);
        adicionarBalao(mensagemIA, false);
        salvarSessaoAtualAutomatica();
    }

    // ==========================================================================================
    // MÓDULO 7: GESTÃO DE SESSÕES (HISTÓRICO)
    // ==========================================================================================
    @FXML
    private void toggleSidebarChats() {
        boolean visivel = !overlaySidebar.isVisible();
        log.trace("{} [UI] Alternando visibilidade da sidebar de histórico: {}", LOG_PREFIX, visivel);
        setVisibilidadeElemento(overlaySidebar, visivel);
        if (visivel) {
            atualizarListaRecentes();
        }
    }

    @FXML
    private void iniciarNovaConversa() {
        log.info("{} [TELEMETRIA] Reiniciando contexto de conversa.", LOG_PREFIX);
        historicoConversa.clear();
        filaMensagensPendentes.clear();
        arquivoSessaoAtual = null;
        paginaCarregada = false;
        webEngine.reload();
        setVisibilidadeElemento(overlaySidebar, false);
    }

    private void salvarSessaoAtualAutomatica() {
        if (historicoConversa.isEmpty())
            return;

        if (arquivoSessaoAtual == null) {
            arquivoSessaoAtual = "chat_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".json";
        }

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [SISTEMA] Iniciando persistência da sessão no disco.", LOG_PREFIX);
                    chatFacade.salvarSessao(arquivoSessaoAtual, new ArrayList<>(historicoConversa));
                    return null;
                },
                sucesso -> {
                    log.debug("{} [AUDITORIA] Sessão persistida com sucesso: {}", LOG_PREFIX, arquivoSessaoAtual);
                    if (overlaySidebar.isVisible())
                        atualizarListaRecentes();
                },
                erro -> log.error("{} [AUDITORIA] Falha ao salvar sessão no disco: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void atualizarListaRecentes() {
        log.trace("{} [TELEMETRIA] Atualizando lista de conversas recentes.", LOG_PREFIX);
        listaChatsRecentes.getChildren().clear();

        AsyncUtils.executarTaskAsync(
                chatFacade::listarSessoesRecentes,
                sessoes -> {
                    if (sessoes.isEmpty()) {
                        Label lblVazio = new Label("Nenhuma conversa recente.");
                        lblVazio.setStyle("-fx-text-fill: #9e9e9e; -fx-font-style: italic;");
                        listaChatsRecentes.getChildren().add(lblVazio);
                        return;
                    }

                    for (IAjudaChatFacade.SessaoChatDTO sessao : sessoes) {
                        Button btnSessao = criarBotaoSessao(sessao);
                        listaChatsRecentes.getChildren().add(btnSessao);
                    }
                },
                erro -> log.error("{} [SISTEMA] Erro ao listar históricos de chat: {}", LOG_PREFIX, erro.getMessage()));
    }

    private Button criarBotaoSessao(IAjudaChatFacade.SessaoChatDTO sessao) {
        Button btn = new Button(sessao.tituloPreview());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setTextOverrun(OverrunStyle.ELLIPSIS);
        btn.setTooltip(new Tooltip(sessao.tituloPreview()));

        String styleBase = "-fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;";
        if (sessao.arquivo().getName().equals(arquivoSessaoAtual)) {
            btn.setStyle(styleBase
                    + "-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-font-weight: bold; -fx-border-color: #bbdefb; -fx-border-radius: 6;");
        } else {
            btn.setStyle(styleBase + "-fx-background-color: transparent; -fx-text-fill: #424242;");
        }

        btn.setOnAction(e -> abrirSessaoHistorica(sessao.arquivo()));
        return btn;
    }

    private void abrirSessaoHistorica(File file) {
        log.info("{} [TELEMETRIA] Carregando sessão histórica: {}", LOG_PREFIX, file.getName());
        AsyncUtils.executarTaskAsync(
                () -> chatFacade.carregarSessao(file),
                historicoCarregado -> {
                    historicoConversa.clear();
                    historicoConversa.addAll(historicoCarregado);
                    arquivoSessaoAtual = file.getName();
                    paginaCarregada = false;
                    webEngine.reload();

                    for (GeminiRequest.Content content : historicoCarregado) {
                        boolean isUsuario = "user".equals(content.role());
                        String texto = (content.parts() != null && !content.parts().isEmpty())
                                ? content.parts().get(0).text()
                                : "[Documento]";
                        adicionarBalao(texto, isUsuario);
                    }

                    atualizarListaRecentes();
                    setVisibilidadeElemento(overlaySidebar, false);
                    log.info("{} [AUDITORIA] Histórico restaurado com sucesso.", LOG_PREFIX);
                },
                erro -> log.error("{} [AUDITORIA] Falha ao carregar histórico: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 8: CONFIGURAÇÃO DE API KEY E MODELOS
    // ==========================================================================================
    @FXML
    private void toggleConfigChave() {
        boolean visivel = !paneConfigChave.isVisible();
        log.trace("{} [UI] Alternando painel de configuração de API Key.", LOG_PREFIX);
        setVisibilidadeElemento(paneConfigChave, visivel);

        if (visivel && chatFacade.isApiKeyConfigurada()) {
            txtNovaApiKey.setText(chatFacade.getApiKeyAtual());
        }
    }

    @FXML
    private void salvarNovaChave() {
        String chave = txtNovaApiKey.getText();
        if (chave == null || chave.trim().isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de salvar chave vazia abortada.", LOG_PREFIX);
            return;
        }

        log.info("{} [AUDITORIA] Solicitando atualização de API Key do Gemini.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(
                () -> {
                    chatFacade.atualizarApiKey(chave.trim());
                    return null;
                },
                sucesso -> {
                    log.info("{} [AUDITORIA] Chave atualizada. Reiniciando registro de modelos.", LOG_PREFIX);
                    modelRegistry.forçarAtualizacao();
                    setVisibilidadeElemento(paneConfigChave, false);
                    adicionarBalao("✅ Chave de API atualizada com sucesso!", false);
                },
                erro -> log.error("{} [SISTEMA] Erro ao persistir nova chave: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 9: UTILITÁRIOS DE UI E JS BRIDGE
    // ==========================================================================================
    @FXML
    private void escolherFicheiro() {
        log.trace("{} [UI] Abrindo seletor de arquivos para anexo.", LOG_PREFIX);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Anexar Documento ou Holerite");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos e Imagens", "*.pdf", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtMensagem.getScene().getWindow());
        if (file != null) {
            ficheiroAnexado = file;
            lblNomeFicheiro.setText(file.getName());
            setVisibilidadeElemento(boxAnexo, true);
            log.info("{} [TELEMETRIA] Arquivo anexado: {}", LOG_PREFIX, file.getName());
        }
    }

    @FXML
    private void removerAnexo() {
        if (ficheiroAnexado != null)
            log.debug("{} [TELEMETRIA] Anexo removido.", LOG_PREFIX);
        ficheiroAnexado = null;
        setVisibilidadeElemento(boxAnexo, false);
    }

    @FXML
    private void fecharPainel() {
        log.trace("{} [UI] Solicitando fechamento do painel lateral.", LOG_PREFIX);
        navigator.alternarPainelIA();
    }

    @FXML
    private void abrirGoogleAIStudioChat() {
        log.info("{} [TELEMETRIA] Redirecionando para Google AI Studio.", LOG_PREFIX);
        abrirLinkExterno(URL_AI_STUDIO);
    }

    public void solicitarFoco() {
        Platform.runLater(() -> {
            if (txtMensagem != null) {
                txtMensagem.requestFocus();
                txtMensagem.positionCaret(txtMensagem.getText().length());
            }
        });
    }

    private void adicionarBalao(String texto, boolean isUsuario) {
        if (texto == null)
            return;

        Runnable rotinaInjecao = () -> {
            try {
                // Codificação Base64 para evitar quebra de caracteres no motor JS
                String textoB64 = Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
                webEngine.executeScript(String.format(JS_ADICIONAR_BALAO, textoB64, isUsuario));
            } catch (Exception e) {
                log.error("{} [SISTEMA] Erro ao injetar mensagem no WebView: {}", LOG_PREFIX, e.getMessage());
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
                log.warn("{} [SISTEMA] Falha silenciada ao executar script JS.", LOG_PREFIX);
            }
        });
    }

    private void abrirLinkExterno(String url) {
        if (hostServices != null)
            hostServices.showDocument(url);
        else
            log.error("{} [SISTEMA] HostServices indisponível para abrir: {}", LOG_PREFIX, url);
    }

    private void setVisibilidadeElemento(Node elemento, boolean visivel) {
        elemento.setVisible(visivel);
        elemento.setManaged(visivel);
    }
}
