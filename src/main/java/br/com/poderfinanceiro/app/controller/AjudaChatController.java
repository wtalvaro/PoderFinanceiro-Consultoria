package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.dto.GeminiRequest;
import br.com.poderfinanceiro.app.facade.IAjudaChatFacade;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
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
 * Controlador de Interface (UI) responsável pelo Chat com a IA. Implementa o
 * padrão <b>Humble Object</b>, delegando a gestão de arquivos, montagem de
 * contexto e comunicação com a IA para a {@link IAjudaChatFacade}.
 * </p>
 */
@Component @Scope("prototype")
public class AjudaChatController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(AjudaChatController.class);
    private static final String LOG_PREFIX = "[AjudaChatController]";

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
    private static final int MAX_TURNOS = 20;

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    @Autowired private IAjudaChatFacade chatFacade;
    @Autowired private Navigator navigator;
    @Autowired private HostServices hostServices;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TextField txtMensagem;
    @FXML private VBox paneConfigChave;
    @FXML private PasswordField txtNovaApiKey;
    @FXML private HBox boxAnexo;
    @FXML private Label lblNomeFicheiro;
    @FXML private WebView webViewChat;
    @FXML private ComboBox<String> cmbModelo;
    @FXML private VBox paneSidebarChats;
    @FXML private VBox listaChatsRecentes;
    @FXML private HBox overlaySidebar;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private boolean modelosCarregados = false;
    private boolean paginaCarregada = false;
    private WebEngine webEngine;
    private File ficheiroAnexado = null;
    private String arquivoSessaoAtual = null;
    private final List<Runnable> filaMensagensPendentes = new ArrayList<>();
    private final List<GeminiRequest.Content> historicoConversa = Collections.synchronizedList(new ArrayList<>());

    public AjudaChatController() {
        // Construtor vazio exigido pelo JavaFX FXML Loader
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E WEBVIEW
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface do Chat...", LOG_PREFIX);
        configurarWebView();
        carregarModelosIniciais();
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    private void carregarModelosIniciais() {
        log.info("{} [TELEMETRIA] Iniciando carregamento de modelos de IA.", LOG_PREFIX);
        carregarModelosDisponiveis();
    }

    private void configurarWebView() {
        log.trace("{} [UI] Configurando WebView do Chat.", LOG_PREFIX);
        webEngine = webViewChat.getEngine();
        URL url = getClass().getResource(CAMINHO_CHAT_HTML);

        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            log.error("{} [SISTEMA] CRÍTICO: chat.html não encontrado no classpath.", LOG_PREFIX);
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                paginaCarregada = true;
                log.info("{} [UI] Página HTML carregada com sucesso. Despachando fila pendente.", LOG_PREFIX);
                filaMensagensPendentes.forEach(Platform::runLater);
                filaMensagensPendentes.clear();
            } else if (newState == Worker.State.FAILED) {
                log.error("{} [SISTEMA] FALHA ao carregar página HTML do chat.", LOG_PREFIX);
            }
        });

        webEngine.setCreatePopupHandler(popupFeatures -> webEngine);
        webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> lidarComNavegacaoExterna(newLocation));
    }

    private void lidarComNavegacaoExterna(String newLocation) {
        if (newLocation == null || newLocation.isEmpty() || newLocation.equals(URL_BLANK) || newLocation.contains(NOME_ARQUIVO_CHAT)) {
            return;
        }
        log.info("{} [UI] Interceptada navegação externa. Redirecionando para browser nativo: {}", LOG_PREFIX, newLocation);
        Platform.runLater(() -> webEngine.getLoadWorker().cancel());
        abrirLinkExterno(newLocation);
    }

    // ==========================================================================================
    // MÓDULO 6: AÇÕES DE CHAT E IA
    // ==========================================================================================
    @FXML private void enviarMensagem() {
        String texto = txtMensagem.getText();

        if ((texto == null || texto.trim().isEmpty()) && ficheiroAnexado == null) {
            log.warn("{} [NEGOCIO] Tentativa de envio bloqueada: mensagem vazia e sem anexo.", LOG_PREFIX);
            return;
        }

        String modeloAtual = cmbModelo.getValue() != null ? cmbModelo.getValue() : MODELO_PADRAO;
        String mensagemExibida = (texto == null || texto.trim().isEmpty()) ? MSG_PADRAO_DOCUMENTO : texto;
        File file = ficheiroAnexado;

        log.info("{} [TELEMETRIA] Enviando mensagem. Modelo: {}, Anexo: {}", LOG_PREFIX, modeloAtual, file != null);

        prepararInterfaceParaEnvio(mensagemExibida);
        removerAnexo();

        GeminiRequest.Content userContent = new GeminiRequest.Content("user", List.of(GeminiRequest.Part.ofText(mensagemExibida)));
        final String sessaoNoMomentoDoEnvio = arquivoSessaoAtual;
        List<GeminiRequest.Content> historicoAtual = obterHistoricoTruncado();

        AsyncUtils.executarTaskAsync(() -> chatFacade.enviarMensagemParaIA(mensagemExibida, file, modeloAtual, historicoAtual),
                (resposta) -> {
                    if (!Objects.equals(arquivoSessaoAtual, sessaoNoMomentoDoEnvio)) {
                        log.warn("{} [NEGOCIO] Race condition evitada: usuário trocou de sessão durante o processamento.", LOG_PREFIX);
                        finalizarEnvioInterface(resposta);
                        return;
                    }
                    log.info("{} [AUDITORIA] Resposta da IA recebida com sucesso.", LOG_PREFIX);
                    historicoConversa.add(userContent);
                    historicoConversa.add(new GeminiRequest.Content("model", List.of(GeminiRequest.Part.ofText(resposta))));
                    finalizarEnvioInterface(resposta);
                }, (erro) -> {
                    log.error("{} [AUDITORIA] Falha na comunicação com a IA: {}", LOG_PREFIX, erro.getMessage());
                    finalizarEnvioInterface(MSG_ERRO_SISTEMA);
                });
    }

    private List<GeminiRequest.Content> obterHistoricoTruncado() {
        synchronized (historicoConversa) {
            int tamanhoTotal = historicoConversa.size();
            if (tamanhoTotal > MAX_TURNOS) {
                log.debug("{} [NEGOCIO] Histórico truncado para os últimos {} turnos.", LOG_PREFIX, MAX_TURNOS);
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
    @FXML private void toggleSidebarChats() {
        boolean visivel = !overlaySidebar.isVisible();
        log.trace("{} [UI] Alternando visibilidade da sidebar de histórico: {}", LOG_PREFIX, visivel);
        setVisibilidadeElemento(overlaySidebar, visivel);
        if (visivel)
            atualizarListaRecentes();
    }

    @FXML private void iniciarNovaConversa() {
        log.info("{} [TELEMETRIA] Iniciando nova conversa. Limpando memória.", LOG_PREFIX);
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
            arquivoSessaoAtual = "chat_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        }

        AsyncUtils.executarTaskAsync(() -> {
            chatFacade.salvarSessao(arquivoSessaoAtual, historicoConversa);
            return null;
        }, sucesso -> {
            log.debug("{} [SISTEMA] Sessão salva em background: {}", LOG_PREFIX, arquivoSessaoAtual);
            if (paneSidebarChats.isVisible())
                atualizarListaRecentes();
        }, erro -> log.error("{} [AUDITORIA] Falha ao persistir sessão: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void atualizarListaRecentes() {
        log.trace("{} [UI] Atualizando lista de conversas recentes.", LOG_PREFIX);
        listaChatsRecentes.getChildren().clear();

        AsyncUtils.executarTaskAsync(chatFacade::listarSessoesRecentes, sessoes -> {
            if (sessoes.isEmpty()) {
                Label lblVazio = new Label("Nenhuma conversa recente.");
                lblVazio.setStyle("-fx-text-fill: #9e9e9e; -fx-font-style: italic;");
                listaChatsRecentes.getChildren().add(lblVazio);
                return;
            }

            for (IAjudaChatFacade.SessaoChatDTO sessao : sessoes) {
                Button btnSessao = new Button(sessao.tituloPreview());
                btnSessao.setMaxWidth(Double.MAX_VALUE);
                btnSessao.setAlignment(Pos.CENTER_LEFT);
                btnSessao.setTextOverrun(OverrunStyle.ELLIPSIS);
                btnSessao.setTooltip(new Tooltip(sessao.tituloPreview()));

                if (sessao.arquivo().getName().equals(arquivoSessaoAtual)) {
                    btnSessao.setStyle(
                            "-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6; -fx-border-color: #bbdefb; -fx-border-radius: 6;");
                } else {
                    btnSessao.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: #424242; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;");
                    btnSessao.setOnMouseEntered(e -> btnSessao.setStyle(
                            "-fx-background-color: #eeeeee; -fx-text-fill: #212121; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;"));
                    btnSessao.setOnMouseExited(e -> btnSessao.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: #424242; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;"));
                }

                btnSessao.setOnAction(e -> abrirSessaoHistorica(sessao.arquivo()));
                listaChatsRecentes.getChildren().add(btnSessao);
            }
        }, erro -> log.error("{} [SISTEMA] Erro ao listar sessões recentes: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void abrirSessaoHistorica(File file) {
        log.info("{} [TELEMETRIA] Solicitando abertura de sessão histórica: {}", LOG_PREFIX, file.getName());
        AsyncUtils.executarTaskAsync(() -> chatFacade.carregarSessao(file), historicoCarregado -> {
            historicoConversa.clear();
            historicoConversa.addAll(historicoCarregado);
            arquivoSessaoAtual = file.getName();

            paginaCarregada = false;
            webEngine.reload();

            for (GeminiRequest.Content content : historicoCarregado) {
                boolean isUsuario = "user".equals(content.role());
                String texto = (content.parts() != null && !content.parts().isEmpty()) ? content.parts().get(0).text()
                        : "[Documento Anexado]";

                Runnable rotinaInjecao = () -> {
                    String textoB64 = Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
                    webEngine.executeScript(String.format(JS_ADICIONAR_BALAO, textoB64, isUsuario));
                };
                filaMensagensPendentes.add(rotinaInjecao);
            }

            atualizarListaRecentes();
            setVisibilidadeElemento(overlaySidebar, false);
            log.info("{} [AUDITORIA] Sessão histórica carregada com sucesso.", LOG_PREFIX);
        }, erro -> {
            log.error("{} [AUDITORIA] Falha ao carregar sessão histórica: {}", LOG_PREFIX, erro.getMessage());
            adicionarBalao("❌ Erro interno ao ler histórico.", false);
        });
    }

    // ==========================================================================================
    // MÓDULO 8: CONFIGURAÇÃO DE API KEY E MODELOS
    // ==========================================================================================
    @FXML private void toggleConfigChave() {
        boolean visivel = !paneConfigChave.isVisible();
        log.trace("{} [UI] Alternando painel de configuração de API Key: {}", LOG_PREFIX, visivel);
        setVisibilidadeElemento(paneConfigChave, visivel);

        if (visivel && chatFacade.isApiKeyConfigurada()) {
            txtNovaApiKey.setText(chatFacade.getApiKeyAtual());
        }
    }

    private void carregarModelosDisponiveis() {
        if (modelosCarregados)
            return;

        AsyncUtils.executarTaskAsync(chatFacade::listarModelosIADisponiveis, modelos -> {
            String modeloAnterior = cmbModelo.getValue();
            cmbModelo.getItems().setAll(modelos);

            if (modeloAnterior != null && modelos.contains(modeloAnterior)) {
                cmbModelo.getSelectionModel().select(modeloAnterior);
            } else if (modelos.contains(MODELO_PADRAO)) {
                cmbModelo.getSelectionModel().select(MODELO_PADRAO);
            } else {
                cmbModelo.getSelectionModel().selectFirst();
            }

            modelosCarregados = true;
            log.info("{} [TELEMETRIA] {} modelos de IA carregados. Selecionado: {}", LOG_PREFIX, modelos.size(), cmbModelo.getValue());
        }, erro -> log.error("{} [SISTEMA] Falha ao carregar modelos da API Gemini: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML private void salvarNovaChave() {
        String chaveDigitada = txtNovaApiKey.getText();
        if (chaveDigitada == null || chaveDigitada.trim().isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de salvar API Key em branco bloqueada.", LOG_PREFIX);
            adicionarBalao("⚠️ A chave de API não pode estar em branco.", false);
            return;
        }

        String chaveTrim = chaveDigitada.trim();
        log.info("{} [AUDITORIA] Solicitando atualização de API Key.", LOG_PREFIX);

        AsyncUtils.executarTaskAsync(() -> {
            chatFacade.atualizarApiKey(chaveTrim);
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] API Key atualizada com sucesso.", LOG_PREFIX);
            modelosCarregados = false;
            carregarModelosDisponiveis();
            setVisibilidadeElemento(paneConfigChave, false);
            adicionarBalao("✅ Sua chave de API foi atualizada com sucesso!", false);
        }, erro -> {
            log.error("{} [AUDITORIA] Erro ao persistir nova chave: {}", LOG_PREFIX, erro.getMessage());
            adicionarBalao("⚠️ Erro ao persistir nova chave: " + erro.getMessage(), false);
        });
    }

    // ==========================================================================================
    // MÓDULO 9: UTILITÁRIOS DE UI E JS BRIDGE
    // ==========================================================================================
    @FXML private void escolherFicheiro() {
        log.trace("{} [UI] Abrindo seletor de arquivo para anexo.", LOG_PREFIX);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Anexar Documento ou Holerite");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos e Imagens", "*.pdf", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtMensagem.getScene().getWindow());
        if (file != null) {
            ficheiroAnexado = file;
            lblNomeFicheiro.setText(file.getName());
            setVisibilidadeElemento(boxAnexo, true);
            log.info("{} [TELEMETRIA] Arquivo anexado ao chat: {}", LOG_PREFIX, file.getName());
        }
    }

    @FXML private void removerAnexo() {
        if (ficheiroAnexado != null)
            log.info("{} [TELEMETRIA] Anexo removido pelo usuário.", LOG_PREFIX);
        ficheiroAnexado = null;
        setVisibilidadeElemento(boxAnexo, false);
    }

    @FXML private void fecharPainel() {
        log.trace("{} [UI] Fechando painel lateral do Chat.", LOG_PREFIX);
        navigator.alternarPainelIA();
    }

    @FXML private void abrirGoogleAIStudioChat() {
        log.info("{} [TELEMETRIA] Usuário abriu o link do Google AI Studio.", LOG_PREFIX);
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
                String textoB64 = Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
                webEngine.executeScript(String.format(JS_ADICIONAR_BALAO, textoB64, isUsuario));
            } catch (Exception e) {
                log.error("{} [SISTEMA] Falha ao injetar balão no WebView: {}", LOG_PREFIX, e.getMessage());
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
                log.warn("{} [SISTEMA] Falha silenciada ao executar script JS: {}", LOG_PREFIX, e.getMessage());
            }
        });
    }

    private void abrirLinkExterno(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
        } else {
            log.error("{} [SISTEMA] HostServices não configurado. Impossível abrir URL: {}", LOG_PREFIX, url);
        }
    }

    private void setVisibilidadeElemento(Node elemento, boolean visivel) {
        elemento.setVisible(visivel);
        elemento.setManaged(visivel);
    }
}
