package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.dto.GeminiRequest;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import javafx.application.HostServices;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Arrays;
import java.util.Comparator;
import javafx.scene.control.Button;
import javafx.geometry.Pos;

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

    private static final Logger log = LoggerFactory.getLogger(AjudaChatController.class);

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
    @FXML
    private VBox paneSidebarChats;
    @FXML
    private VBox listaChatsRecentes;
    @FXML
    private HBox overlaySidebar;

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private boolean modelosCarregados = false;
    private boolean paginaCarregada = false;
    private WebEngine webEngine;
    private File ficheiroAnexado = null;
    private String arquivoSessaoAtual = null;
    final String sessaoNoEnvio = arquivoSessaoAtual;

    private final List<Runnable> filaMensagensPendentes = new ArrayList<>();
    private final List<GeminiRequest.Content> historicoConversa = Collections.synchronizedList(new ArrayList<>());

    private static final int MAX_TURNOS = 20;

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final GeminiService geminiService;
    private final AuthService authService;
    private final AtendimentoContextService contextoService;
    private final TabelaJurosService tabelaService;
    private final LinkUtilRepository linkRepository;
    private final Navigator navigator;
    private final HostServices hostServices;

    public AjudaChatController(GeminiService geminiService, AuthService authService,
            AtendimentoContextService contextoService, TabelaJurosService tabelaService,
            LinkUtilRepository linkRepository, Navigator navigator, HostServices hostServices) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.contextoService = contextoService;
        this.tabelaService = tabelaService;
        this.linkRepository = linkRepository;
        this.navigator = navigator;
        this.hostServices = hostServices;
        log.debug("[CHAT] Controller instanciado (prototype). hashCode={}", System.identityHashCode(this));
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.info("[CHAT] Inicializando AjudaChatController...");
        configurarWebView();
        carregarModelosIniciais();
        obterDiretorioChats();
        log.info("[CHAT] Inicialização concluída.");
    }

    private void carregarModelosIniciais() {
        boolean temApiKey = authService.getUsuarioLogado() != null
                && authService.getUsuarioLogado().getGeminiApiKey() != null;
        log.info("[CHAT][MODELOS] Iniciando carregamento de modelos. Usuário logado: {} | API Key presente: {}",
                (authService.getUsuarioLogado() != null),
                temApiKey);

        String token = temApiKey ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        carregarModelosDisponiveis(token);
    }

    private void configurarWebView() {
        log.debug("[CHAT][WEBVIEW] Configurando WebView...");
        webEngine = webViewChat.getEngine();
        URL url = getClass().getResource(CAMINHO_CHAT_HTML);

        if (url != null) {
            log.info("[CHAT][WEBVIEW] Recurso chat.html localizado. Carregando: {}", url.toExternalForm());
            webEngine.load(url.toExternalForm());
        } else {
            // ERRO crítico: sem o HTML a interface de chat é inutilizável
            log.error("[CHAT][WEBVIEW] CRÍTICO: chat.html não encontrado no classpath em '{}'. " +
                    "O painel de chat estará inoperante.", CAMINHO_CHAT_HTML);
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            log.debug("[CHAT][WEBVIEW] Transição de estado: {} → {}", oldState, newState);

            if (newState == Worker.State.SUCCEEDED) {
                paginaCarregada = true;
                int mensagensPendentes = filaMensagensPendentes.size();
                log.info("[CHAT][WEBVIEW] Página carregada com sucesso. Mensagens pendentes na fila: {}",
                        mensagensPendentes);

                if (mensagensPendentes > 0) {
                    log.debug("[CHAT][WEBVIEW] Despachando {} mensagem(ns) enfileirada(s)...", mensagensPendentes);
                    filaMensagensPendentes.forEach(Platform::runLater);
                    filaMensagensPendentes.clear();
                    log.debug("[CHAT][WEBVIEW] Fila de mensagens pendentes liberada.");
                }
            } else if (newState == Worker.State.FAILED) {
                log.error("[CHAT][WEBVIEW] FALHA ao carregar página. Estado anterior: {}. " +
                        "Verifique se o arquivo '{}' está no classpath.", oldState, CAMINHO_CHAT_HTML);
            }
        });

        webEngine.setCreatePopupHandler(popupFeatures -> webEngine);
        webEngine.locationProperty()
                .addListener((obs, oldLocation, newLocation) -> lidarComNavegacaoExterna(newLocation));
    }

    private void lidarComNavegacaoExterna(String newLocation) {
        if (newLocation == null || newLocation.isEmpty() || newLocation.equals(URL_BLANK)
                || newLocation.contains(NOME_ARQUIVO_CHAT)) {
            return;
        }
        // Log importante: indica que o WebView tentou navegar para fora,
        // o que significaria sair da tela do chat
        log.info("[CHAT][WEBVIEW] Interceptada navegação externa. URL destino: '{}'. Redirecionando para browser.",
                newLocation);
        Platform.runLater(() -> webEngine.getLoadWorker().cancel());
        abrirLinkExterno(newLocation);
    }

    // =========================================================================
    // GESTÃO DE ANEXOS E PAINÉIS
    // =========================================================================
    @FXML
    private void escolherFicheiro() {
        log.debug("[CHAT][ANEXO] Abrindo seletor de arquivo...");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Anexar Documento ou Holerite");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documentos e Imagens", "*.pdf", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtMensagem.getScene().getWindow());
        if (file != null) {
            ficheiroAnexado = file;
            lblNomeFicheiro.setText(file.getName());
            setVisibilidadeElemento(boxAnexo, true);
            // Nome e tamanho do arquivo ajudam a rastrear problemas de upload
            log.info("[CHAT][ANEXO] Arquivo selecionado: '{}' | Tamanho: {} KB",
                    file.getName(), file.length() / 1024);
        } else {
            log.debug("[CHAT][ANEXO] Seleção de arquivo cancelada pelo usuário.");
        }
    }

    @FXML
    private void removerAnexo() {
        if (ficheiroAnexado != null) {
            log.info("[CHAT][ANEXO] Anexo removido pelo usuário: '{}'", ficheiroAnexado.getName());
        }
        ficheiroAnexado = null;
        setVisibilidadeElemento(boxAnexo, false);
    }

    @FXML
    private void fecharPainel() {
        log.debug("[CHAT] Painel de IA fechado pelo usuário.");
        navigator.alternarPainelIA();
    }

    @FXML
    private void abrirGoogleAIStudioChat() {
        log.info("[CHAT] Usuário abriu o Google AI Studio.");
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

    // =========================================================================
    // LÓGICA DE ENVIO DE MENSAGENS (CORE DA IA)
    // =========================================================================
    @FXML
    private void enviarMensagem() {
        String texto = txtMensagem.getText();

        if (isEntradaInvalida(texto)) {
            log.warn("[CHAT][ENVIO] Tentativa de envio bloqueada: mensagem vazia e sem anexo.");
            return;
        }

        String modeloAtual = cmbModelo.getValue() != null ? cmbModelo.getValue() : MODELO_PADRAO;
        String telaFocada = contextoService.getTelaAtualFocada() != null
                ? contextoService.getTelaAtualFocada().name()
                : "NENHUMA";
        boolean temAnexo = ficheiroAnexado != null;

        // Log de negócio: contexto completo da requisição que vai à IA
        log.info("[CHAT][ENVIO] Iniciando envio. Modelo='{}' | Tela focada='{}' | Anexo presente={} | " +
                "Tamanho da mensagem={} chars | Turnos no histórico={}",
                modeloAtual, telaFocada, temAnexo,
                (texto != null ? texto.length() : 0),
                historicoConversa.size());

        if (temAnexo) {
            log.debug("[CHAT][ENVIO] Arquivo a ser enviado: '{}' ({} KB)",
                    ficheiroAnexado.getName(), ficheiroAnexado.length() / 1024);
        }

        String mensagemExibida = (texto == null || texto.trim().isEmpty()) ? MSG_PADRAO_DOCUMENTO : texto;
        prepararInterfaceParaEnvio(mensagemExibida);

        GeminiRequest.Content userContent = new GeminiRequest.Content("user",
                List.of(GeminiRequest.Part.ofText(mensagemExibida)));

        File file = ficheiroAnexado;
        removerAnexo();

        // Captura a identidade da sessão no momento exato do disparo.
        // Isso é a âncora que impede um callback tardio de contaminar
        // uma sessão diferente caso o usuário troque de conversa enquanto a IA
        // processa.
        final String sessaoNoMomentoDoEnvio = arquivoSessaoAtual;
        log.debug("[CHAT][ENVIO] Sessão de referência capturada para guarda de integridade: '{}'",
                sessaoNoMomentoDoEnvio != null ? sessaoNoMomentoDoEnvio : "<nova conversa>");

        long inicioEnvio = System.currentTimeMillis();

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("[CHAT][IA] Task assíncrona iniciada na thread '{}'.",
                            Thread.currentThread().getName());
                    return processarChamadaIA(mensagemExibida, file);
                },
                (resposta) -> {
                    long tempoTotal = System.currentTimeMillis() - inicioEnvio;

                    // Verifica se a sessão ativa no momento do callback ainda é a mesma
                    // que estava ativa quando o envio foi disparado.
                    // Divergência indica que o usuário trocou ou criou uma nova conversa
                    // durante o processamento — o turno deve ser descartado para evitar
                    // corromper o histórico da sessão atualmente ativa.
                    if (!Objects.equals(arquivoSessaoAtual, sessaoNoMomentoDoEnvio)) {
                        log.warn("[CHAT][IA] Race condition detectada: sessão trocada durante processamento. " +
                                "Sessão no envio='{}' | Sessão atual='{}'. " +
                                "Turno descartado para preservar integridade do histórico. Tempo total={}ms",
                                sessaoNoMomentoDoEnvio != null ? sessaoNoMomentoDoEnvio : "<nova conversa>",
                                arquivoSessaoAtual != null ? arquivoSessaoAtual : "<nova conversa>",
                                tempoTotal);
                        finalizarEnvioInterface(resposta);
                        return;
                    }

                    // Tempo de resposta é o indicador mais importante de performance da integração
                    // IA
                    log.info("[CHAT][IA] Resposta recebida com sucesso. Tempo total={}ms | " +
                            "Tamanho da resposta={} chars | Histórico após atualização={} turnos",
                            tempoTotal,
                            (resposta != null ? resposta.length() : 0),
                            historicoConversa.size() + 2); // +2 pois ainda não adicionou

                    historicoConversa.add(userContent);
                    historicoConversa.add(new GeminiRequest.Content("model",
                            List.of(GeminiRequest.Part.ofText(resposta))));
                    finalizarEnvioInterface(resposta);
                },
                (erro) -> {
                    long tempoTotal = System.currentTimeMillis() - inicioEnvio;
                    // Histórico preservado em caso de erro — log deixa isso explícito
                    log.error("[CHAT][IA] FALHA na chamada à IA após {}ms. " +
                            "Histórico preservado ({} turnos). Sessão de referência='{}'. Erro: {}",
                            tempoTotal, historicoConversa.size(),
                            sessaoNoMomentoDoEnvio != null ? sessaoNoMomentoDoEnvio : "<nova conversa>",
                            erro.getMessage(), erro);
                    finalizarEnvioInterface(MSG_ERRO_SISTEMA);
                });
    }

    private String processarChamadaIA(String mensagem, File ficheiro) throws Exception {
        String modeloSelecionado = cmbModelo.getValue() != null ? cmbModelo.getValue() : MODELO_PADRAO;
        boolean temToken = authService.getUsuarioLogado() != null
                && authService.getUsuarioLogado().getGeminiApiKey() != null;

        log.debug("[CHAT][IA] Preparando chamada ao GeminiService. Modelo='{}' | Token presente={}",
                modeloSelecionado, temToken);

        String token = temToken ? authService.getUsuarioLogado().getGeminiApiKey() : null;

        String jsonFoco = obterContextoPrincipal();
        String jsonComissoes = obterContextoComissoes();
        String jsonTabelas = SummaryGeneratorUtils.gerarJsonTabelasJuros(tabelaService.listarAtivas());
        String jsonLinks = SummaryGeneratorUtils.gerarJsonLinksUteis(linkRepository.findAll());

        List<GeminiRequest.Content> historicoAtual = obterHistoricoTruncado();

        // Permite auditar o que foi montado como contexto sem expor dados sensíveis
        log.debug("[CHAT][IA] Contexto montado: foco={} chars | comissões={} chars | " +
                "tabelas={} chars | links={} chars | histórico={} turnos (truncado de {})",
                jsonFoco != null ? jsonFoco.length() : 0,
                jsonComissoes != null ? jsonComissoes.length() : 0,
                jsonTabelas != null ? jsonTabelas.length() : 0,
                jsonLinks != null ? jsonLinks.length() : 0,
                historicoAtual.size(),
                historicoConversa.size());

        log.debug("[CHAT][IA] Disparando requisição ao GeminiService...");
        String resposta = geminiService.perguntarAoAssistente(
                mensagem, token, modeloSelecionado, ficheiro,
                jsonFoco, jsonTabelas, jsonLinks, jsonComissoes,
                historicoAtual);

        log.debug("[CHAT][IA] GeminiService retornou resposta. Tamanho: {} chars",
                resposta != null ? resposta.length() : 0);
        return resposta;
    }

    private List<GeminiRequest.Content> obterHistoricoTruncado() {
        synchronized (historicoConversa) {
            int tamanhoTotal = historicoConversa.size();
            if (tamanhoTotal > MAX_TURNOS) {
                int descartados = tamanhoTotal - MAX_TURNOS;
                log.debug("[CHAT][HISTÓRICO] Histórico truncado: {} turnos totais → mantendo últimos {}. " +
                        "Descartados: {} turnos antigos.",
                        tamanhoTotal, MAX_TURNOS, descartados);
                return new ArrayList<>(
                        historicoConversa.subList(tamanhoTotal - MAX_TURNOS, tamanhoTotal));
            }
            log.debug("[CHAT][HISTÓRICO] Histórico enviado completo: {} turnos.", tamanhoTotal);
            return new ArrayList<>(historicoConversa);
        }
    }

    private boolean isEntradaInvalida(String texto) {
        return (texto == null || texto.trim().isEmpty()) && ficheiroAnexado == null;
    }

    private void prepararInterfaceParaEnvio(String mensagem) {
        txtMensagem.clear();
        adicionarBalao(mensagem, true);
        executarScriptSeguro(JS_MOSTRAR_CARREGAMENTO);
        log.debug("[CHAT][UI] Interface preparada para aguardar resposta da IA.");
    }

    private void finalizarEnvioInterface(String mensagemIA) {
        executarScriptSeguro(JS_REMOVER_CARREGAMENTO);
        adicionarBalao(mensagemIA, false);
        salvarSessaoAtualAutomatica();
        log.debug("[CHAT][UI] Interface atualizada e salva com a resposta da IA.");
    }

    // =========================================================================
    // ENGENHARIA DE CONTEXTO (JSON BUILDERS)
    // =========================================================================
    private String obterContextoPrincipal() {
        AtendimentoContextService.TipoTelaFocada telaAtual = contextoService.getTelaAtualFocada();
        log.debug("[CHAT][CONTEXTO] Tela focada: {}", telaAtual);

        if (telaAtual == AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS) {
            boolean temProposta = contextoService.getPropostaAtiva() != null;
            log.debug("[CHAT][CONTEXTO] Gerando contexto de proposta. Proposta ativa presente: {}", temProposta);
            return SummaryGeneratorUtils.gerarJsonPropostaParaIA(contextoService.getPropostaAtiva());
        }

        boolean isAbaCliente = (telaAtual == AtendimentoContextService.TipoTelaFocada.CADASTRO_CLIENTE);
        boolean temLead = contextoService.getLeadAtivo() != null;
        log.debug("[CHAT][CONTEXTO] Gerando contexto de lead. Lead ativo presente: {} | isAbaCliente: {}",
                temLead, isAbaCliente);
        return SummaryGeneratorUtils.gerarJsonContextualParaIA(contextoService.getLeadAtivo(), isAbaCliente);
    }

    private String obterContextoComissoes() {
        if (contextoService.getTelaAtualFocada() == AtendimentoContextService.TipoTelaFocada.GESTAO_COMISSOES) {
            int qtdComissoes = contextoService.getComissoesAtivas() != null
                    ? contextoService.getComissoesAtivas().size()
                    : 0;
            log.debug("[CHAT][CONTEXTO] Gerando contexto de comissões. Quantidade: {}", qtdComissoes);
            return SummaryGeneratorUtils.gerarJsonComissoes(contextoService.getComissoesAtivas());
        }
        log.debug("[CHAT][CONTEXTO] Tela não é de comissões, contexto retornado vazio.");
        return "[]";
    }

    @FXML
    private void toggleSidebarChats() {
        boolean visivel = !overlaySidebar.isVisible();
        setVisibilidadeElemento(overlaySidebar, visivel);
        if (visivel) {
            atualizarListaRecentes();
        }
    }

    @FXML
    private void iniciarNovaConversa() {
        log.info("[CHAT][SESSION] Iniciando nova conversa. Limpando memória.");
        historicoConversa.clear();
        filaMensagensPendentes.clear();
        arquivoSessaoAtual = null;
        paginaCarregada = false;
        webEngine.reload();
        setVisibilidadeElemento(overlaySidebar, false);
    }

    private File obterDiretorioChats() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        String caminhoBase;

        if (os.contains("win")) {
            caminhoBase = System.getenv("APPDATA") + File.separator + "PoderFinanceiro";
        } else if (os.contains("mac")) {
            caminhoBase = home + File.separator + "Library" + File.separator + "Application Support" + File.separator
                    + "PoderFinanceiro";
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            caminhoBase = (xdgData != null && !xdgData.isEmpty())
                    ? xdgData + File.separator + "PoderFinanceiro"
                    : home + File.separator + ".local" + File.separator + "share" + File.separator + "PoderFinanceiro";
        }

        File pastaChats = new File(caminhoBase, "chats_gemini");

        if (!pastaChats.exists()) {
            boolean criado = pastaChats.mkdirs(); // mkdirs() cria pastas pai se não existirem
            if (criado) {
                log.info("[CHAT][DIR] 📁 Diretório criado com sucesso em: {}", pastaChats.getAbsolutePath());
            } else {
                log.error("[CHAT][DIR] ❌ Falha grave ao criar diretório. Permissão negada no caminho: {}",
                        pastaChats.getAbsolutePath());
            }
        } else {
            log.trace("[CHAT][DIR] Diretório de chat verificado e disponível.");
        }

        return pastaChats;
    }

    private void salvarSessaoAtualAutomatica() {
        if (historicoConversa.isEmpty())
            return;

        File diretorio = obterDiretorioChats();

        // Se for a primeira mensagem da conversa, cria um nome de arquivo
        if (arquivoSessaoAtual == null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            arquivoSessaoAtual = "chat_" + LocalDateTime.now().format(formatter) + ".json";
        }

        File arquivo = new File(diretorio, arquivoSessaoAtual);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(arquivo, historicoConversa);
            log.debug("[CHAT][AUTOSAVE] Sessão salva em background: {}", arquivo.getName());

            // Atualiza a lista lateral de forma reativa se estiver aberta
            if (paneSidebarChats.isVisible()) {
                Platform.runLater(this::atualizarListaRecentes);
            }
        } catch (Exception e) {
            log.error("[CHAT][AUTOSAVE] Falha ao persistir sessão: {}", e.getMessage(), e);
        }
    }

    private void atualizarListaRecentes() {
        listaChatsRecentes.getChildren().clear();
        File diretorio = obterDiretorioChats();
        File[] arquivos = diretorio.listFiles((dir, name) -> name.endsWith(".json"));

        if (arquivos == null || arquivos.length == 0) {
            Label lblVazio = new Label("Nenhuma conversa recente.");
            lblVazio.setStyle("-fx-text-fill: #9e9e9e; -fx-font-style: italic;");
            listaChatsRecentes.getChildren().add(lblVazio);
            return;
        }

        // Ordena do mais recente para o mais antigo
        Arrays.sort(arquivos, Comparator.comparingLong(File::lastModified).reversed());

        for (File file : arquivos) {
            String nomeAmigavel = extrairPrimeiraMensagem(file);
            Button btnSessao = new Button(nomeAmigavel);

            btnSessao.setMaxWidth(Double.MAX_VALUE);
            btnSessao.setAlignment(Pos.CENTER_LEFT);

            // NOVO: Adiciona reticências (...) se o botão ficar espremido
            btnSessao.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

            // NOVO: Exibe o nome completo quando o usuário passa o mouse por cima
            btnSessao.setTooltip(new javafx.scene.control.Tooltip(nomeAmigavel));

            // Destaque visual para a sessão que está ativa no momento
            if (file.getName().equals(arquivoSessaoAtual)) {
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

            btnSessao.setOnAction(e -> abrirSessaoHistorica(file));
            listaChatsRecentes.getChildren().add(btnSessao);
        }
    }

    private String formatarNomeArquivo(String nome) {
        // Converte "chat_20260526_224014.json" para algo legível
        try {
            String parteData = nome.substring(5, 13); // 20260526
            String ano = parteData.substring(0, 4);
            String mes = parteData.substring(4, 6);
            String dia = parteData.substring(6, 8);
            return "💬 Chat " + dia + "/" + mes + "/" + ano;
        } catch (Exception e) {
            return "💬 " + nome.replace(".json", "");
        }
    }

    private String extrairPrimeiraMensagem(File arquivo) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<GeminiRequest.Content> historico = mapper.readValue(
                    arquivo, new TypeReference<List<GeminiRequest.Content>>() {
                    });

            for (GeminiRequest.Content content : historico) {
                if ("user".equals(content.role())
                        && content.parts() != null
                        && !content.parts().isEmpty()) {
                    String texto = content.parts().get(0).text();
                    if (texto != null && !texto.isBlank()) {
                        return "💬 " + texto.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[CHAT][SIDEBAR] Falha ao extrair primeira mensagem de '{}': {}",
                    arquivo.getName(), e.getMessage());
        }
        // Fallback seguro: exibe a data se o JSON não tiver texto legível
        return formatarNomeArquivo(arquivo.getName());
    }

    private void abrirSessaoHistorica(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<GeminiRequest.Content> historicoCarregado = mapper.readValue(
                    file,
                    new TypeReference<List<GeminiRequest.Content>>() {
                    });

            historicoConversa.clear();
            historicoConversa.addAll(historicoCarregado);
            arquivoSessaoAtual = file.getName(); // Define esta como a sessão ativa

            paginaCarregada = false;
            webEngine.reload();

            for (GeminiRequest.Content content : historicoCarregado) {
                boolean isUsuario = "user".equals(content.role());
                String texto = "";
                if (content.parts() != null && !content.parts().isEmpty()) {
                    texto = content.parts().get(0).text();
                    if (texto == null)
                        texto = "[Documento Anexado]";
                }

                final String textoFinal = texto;
                Runnable rotinaInjecao = () -> {
                    String textoB64 = Base64.getEncoder().encodeToString(textoFinal.getBytes(StandardCharsets.UTF_8));
                    String script = String.format(JS_ADICIONAR_BALAO, textoB64, isUsuario);
                    webEngine.executeScript(script);
                };
                filaMensagensPendentes.add(rotinaInjecao);
            }

            atualizarListaRecentes();
            setVisibilidadeElemento(overlaySidebar, false); // Fecha o painel
            log.info("[CHAT][LOAD] Sessão carregada silenciosamente: {}", file.getName());

        } catch (Exception e) {
            log.error("[CHAT][LOAD] Falha ao carregar sessão.", e);
            adicionarBalao("❌ Erro interno ao ler histórico.", false);
        }
    }

    // =========================================================================
    // CONFIGURAÇÃO DE CHAVE DE API
    // =========================================================================
    @FXML
    private void toggleConfigChave() {
        boolean visivel = !paneConfigChave.isVisible();
        log.debug("[CHAT][CONFIG] Painel de configuração de API Key {} pelo usuário.",
                visivel ? "aberto" : "fechado");
        setVisibilidadeElemento(paneConfigChave, visivel);

        if (visivel && authService.getUsuarioLogado() != null) {
            // Não logar o valor da chave — apenas confirmar que foi preenchida
            boolean chaveExistente = authService.getUsuarioLogado().getGeminiApiKey() != null;
            log.debug("[CHAT][CONFIG] Campo de API Key preenchido com chave existente: {}", chaveExistente);
            txtNovaApiKey.setText(authService.getUsuarioLogado().getGeminiApiKey());
        }
    }

    private void carregarModelosDisponiveis(String token) {
        if (modelosCarregados) {
            log.debug("[CHAT][MODELOS] Modelos já carregados anteriormente. Ignorando nova carga.");
            return;
        }

        log.info("[CHAT][MODELOS] Iniciando busca assíncrona de modelos disponíveis. Token presente: {}",
                token != null);

        AsyncUtils.executarTaskAsync(
                () -> geminiService.listarModelosMultimodais(token),
                modelos -> {
                    String modeloAnterior = cmbModelo.getValue();
                    cmbModelo.getItems().setAll(modelos);

                    String modeloSelecionado;
                    if (modeloAnterior != null && modelos.contains(modeloAnterior)) {
                        cmbModelo.getSelectionModel().select(modeloAnterior);
                        modeloSelecionado = modeloAnterior;
                    } else if (modelos.contains(MODELO_PADRAO)) {
                        cmbModelo.getSelectionModel().select(MODELO_PADRAO);
                        modeloSelecionado = MODELO_PADRAO;
                    } else {
                        cmbModelo.getSelectionModel().selectFirst();
                        modeloSelecionado = cmbModelo.getValue();
                    }

                    modelosCarregados = true;
                    // Log de saúde: confirma quais modelos estão disponíveis e qual foi selecionado
                    log.info("[CHAT][MODELOS] {} modelo(s) carregado(s). Selecionado: '{}'. Lista: {}",
                            modelos.size(), modeloSelecionado, modelos);
                },
                erro -> {
                    // Falha aqui não impede o uso do chat, mas o usuário ficará sem opções no
                    // ComboBox
                    log.error("[CHAT][MODELOS] Falha ao carregar modelos da API Gemini. " +
                            "ComboBox pode estar vazio. Erro: {}",
                            (erro != null ? erro.getMessage() : "null"), erro);
                });
    }

    @FXML
    private void salvarNovaChave() {
        String chaveDigitada = txtNovaApiKey.getText();
        log.info("[CHAT][CONFIG] Solicitação de atualização de API Key recebida.");

        if (chaveDigitada == null || chaveDigitada.trim().isBlank()) {
            log.warn("[CHAT][CONFIG] Tentativa de salvar API Key em branco bloqueada.");
            adicionarBalao("⚠️ A chave de API não pode estar em branco.", false);
            return;
        }

        // Loga apenas o tamanho e prefixo para rastreio sem expor a chave completa
        String chaveTrim = chaveDigitada.trim();
        log.debug("[CHAT][CONFIG] Persistindo nova API Key. Tamanho={} chars | Prefixo='{}'",
                chaveTrim.length(),
                chaveTrim.length() > 6 ? chaveTrim.substring(0, 6) + "..." : "***");

        try {
            authService.atualizarGeminiApiKey(chaveTrim);
            log.info("[CHAT][CONFIG] API Key atualizada com sucesso. Recarregando lista de modelos...");
            modelosCarregados = false; // força recarregar com nova chave
            carregarModelosDisponiveis(chaveTrim);
            setVisibilidadeElemento(paneConfigChave, false);
            adicionarBalao(
                    "✅ Sua chave de API foi atualizada com sucesso! O Gemini já está operando com as novas credenciais.",
                    false);
        } catch (Exception e) {
            log.error("[CHAT][CONFIG] Falha ao persistir nova API Key no AuthService: {}", e.getMessage(), e);
            adicionarBalao("⚠️ Erro ao persistir nova chave: " + e.getMessage(), false);
        }
    }

    // =========================================================================
    // UTILITÁRIOS (DRY, INTEROP & ASYNC)
    // =========================================================================
    private void adicionarBalao(String texto, boolean isUsuario) {
        if (texto == null) {
            log.warn("[CHAT][UI] Tentativa de adicionar balão com texto null ignorada.");
            return;
        }

        Runnable rotinaInjecao = () -> {
            try {
                String textoB64 = Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
                String script = String.format(JS_ADICIONAR_BALAO, textoB64, isUsuario);
                webEngine.executeScript(script);
                log.debug("[CHAT][UI] Balão injetado no WebView. Origem={} | Tamanho={} chars",
                        isUsuario ? "USUÁRIO" : "IA", texto.length());
            } catch (Exception e) {
                // Falha de injeção de JS não pode ser silenciada sem log — quebraria a UI
                log.error("[CHAT][UI] Falha ao injetar balão no WebView. Origem={} | Erro: {}",
                        isUsuario ? "USUÁRIO" : "IA", e.getMessage(), e);
            }
        };

        if (paginaCarregada) {
            Platform.runLater(rotinaInjecao);
        } else {
            filaMensagensPendentes.add(rotinaInjecao);
            log.debug("[CHAT][UI] WebView não pronto. Mensagem enfileirada. Fila atual: {} item(ns).",
                    filaMensagensPendentes.size());
        }
    }

    private void executarScriptSeguro(String script) {
        Platform.runLater(() -> {
            try {
                if (paginaCarregada) {
                    webEngine.executeScript(script);
                } else {
                    log.warn("[CHAT][UI] Script '{}' ignorado: WebView ainda não está pronto.", script);
                }
            } catch (Exception e) {
                log.warn("[CHAT][UI] Falha silenciada ao executar script JS '{}': {}", script, e.getMessage());
            }
        });
    }

    private void abrirLinkExterno(String url) {
        if (hostServices != null) {
            log.info("[CHAT][NAV] Abrindo link externo no browser: '{}'", url);
            hostServices.showDocument(url);
        } else {
            log.error("[CHAT][NAV] HostServices não configurado. Impossível abrir URL externa: '{}'", url);
        }
    }

    private void setVisibilidadeElemento(Node elemento, boolean visivel) {
        elemento.setVisible(visivel);
        elemento.setManaged(visivel);
    }
}