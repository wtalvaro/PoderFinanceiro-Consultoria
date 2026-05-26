package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.domain.event.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.beans.binding.Bindings;

@Component
@Scope("prototype")
public class EsteiraPropostasController implements Disposable {

    // =========================================================================
    // CONSTANTES E TEMPLATES (Clean Code & DRY)
    // =========================================================================
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String STYLE_BTN_BASE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 6;";
    private static final String STYLE_BTN_SUCCESS = "-fx-background-color: #2e7d32; " + STYLE_BTN_BASE;
    private static final String STYLE_BTN_DANGER = "-fx-background-color: #c62828; " + STYLE_BTN_BASE;
    private static final String STYLE_BTN_WARNING = "-fx-background-color: #f57c00; " + STYLE_BTN_BASE;

    private static final String STYLE_LBL_CONFIRM_TITULO = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;";
    private static final String STYLE_BTN_CONFIRM = "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;";

    private static final String HTML_TEMPLATE_IA = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
                <style>
                    body { font-family: sans-serif; padding: 25px; box-sizing: border-box; color: #333; }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """;

    private static final Logger log = LoggerFactory.getLogger(EsteiraPropostasController.class);

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final PropostaRepository repository;
    private final PropostaService propostaService;
    private final ApplicationContext context;
    private final PropostaUIEventHub eventHub;
    private final ProponenteUIEventHub proponenteEventHub;

    // =========================================================================
    // COMPONENTES UI (FXML)
    // =========================================================================
    @FXML
    private TextField txtBusca;
    @FXML
    private TableView<PropostaModel> tablePropostas;
    @FXML
    private TableColumn<PropostaModel, String> colData, colCliente, colBanco, colValorSol, colStatus;
    @FXML
    private StackPane containerFormulario;
    @FXML
    private VBox paneVazio;

    @FXML
    private VBox overlayConfirmacao, overlayFeedback, overlayAlertaSimples;

    @FXML
    private Label lblConfirmacaoTitulo, lblConfirmacaoTexto;
    @FXML
    private Button btnConfirmarAcao;

    @FXML
    private Label lblFeedbackIcon, lblFeedbackTitle;
    @FXML
    private Button btnFeedbackAction;
    @FXML
    private WebView webFeedback;

    @FXML
    private Label lblAlertaIcone, lblAlertaTitulo, lblAlertaMensagem;
    @FXML
    private Button btnAlertaAcao;
    @FXML
    private Label lblTotalRegistros;

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private final ObservableList<PropostaModel> masterData = FXCollections.observableArrayList();
    private PropostaController formController;
    private boolean isRevertendoSelecao = false;

    private Runnable acaoPendente;
    private Runnable cancelPendente;
    private Runnable onFeedbackClose;

    public EsteiraPropostasController(PropostaRepository repository, PropostaService propostaService,
            ApplicationContext context, PropostaUIEventHub eventHub, ProponenteUIEventHub proponenteEventHub) {
        this.repository = repository;
        this.propostaService = propostaService;
        this.context = context;
        this.eventHub = eventHub;
        this.proponenteEventHub = proponenteEventHub;
        log.debug("[ESTEIRA] Construtor: Controller instanciado (escopo prototype)");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[ESTEIRA] initialize: Iniciando configuração da esteira de propostas");
        eventHub.inscrever(this::recarregarDados);
        proponenteEventHub.inscrever(this::recarregarDados);
        configurarTabela();
        configurarFiltroReativo();
        recarregarDados();
        lblTotalRegistros.textProperty().bind(
                Bindings.format("Total: %d proposta(s)", Bindings.size(tablePropostas.getItems())));
        log.info("[ESTEIRA] initialize: Configuração concluída e inscrição no evento realizada");
    }

    // =========================================================================
    // CONFIGURAÇÃO DA TABELA (SRP)
    // =========================================================================
    private void configurarTabela() {
        log.debug("[ESTEIRA] configurarTabela: Configurando colunas e eventos da tabela");
        configurarColunasDados();
        configurarColunaStatus();
        configurarEventosSelecao();
        log.trace("[ESTEIRA] configurarTabela: Tabela configurada");
    }

    private void configurarColunasDados() {
        colData.setCellValueFactory(d -> formatarData(d.getValue().getDataSolicitacao()));
        colCliente.setCellValueFactory(d -> textoOuHifen(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(
                d -> textoOuHifen(d.getValue().getBanco() != null ? d.getValue().getBanco().getNome() : null));
        colValorSol.setCellValueFactory(d -> formatarMoeda(d.getValue().getValorSolicitado()));
        log.trace("[ESTEIRA] configurarColunasDados: Colunas de dados configuradas");
    }

    private void configurarColunaStatus() {
        log.debug("[ESTEIRA] configurarColunaStatus: Configurando coluna de status com cores");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                PropostaModel p = getTableRow().getItem();
                setText(p.getStatus().name().replace("_", " "));
                setTextFill(corDoStatus(p.getStatus()));
                setStyle(p.getStatus() == StatusPropostaModel.PAGO ? "-fx-font-weight: bold;" : "");
                log.trace("[ESTEIRA] colunaStatus: Proposta ID={} status={}", p.getId(), p.getStatus());
            }
        });
    }

    private void configurarEventosSelecao() {
        log.debug("[ESTEIRA] configurarEventosSelecao: Adicionando listener de seleção da tabela");
        tablePropostas.getSelectionModel().selectedItemProperty().addListener((obs, old, novaProposta) -> {
            log.debug("[ESTEIRA] Seleção alterada: old={}, new={}",
                    old != null ? old.getId() : "null",
                    novaProposta != null ? novaProposta.getId() : "null");
            if (isRevertendoSelecao) {
                log.trace("[ESTEIRA] Revertendo seleção, ignorando evento");
                isRevertendoSelecao = false;
                return;
            }
            if (novaProposta != null) {
                abrirFormularioComProposta(novaProposta, old);
            }
        });
    }

    // =========================================================================
    // FILTROS E BUSCA
    // =========================================================================
    private void configurarFiltroReativo() {
        log.debug("[ESTEIRA] configurarFiltroReativo: Configurando filtro de busca reativo");
        FilteredList<PropostaModel> filteredData = new FilteredList<>(masterData, p -> true);
        txtBusca.textProperty()
                .addListener((obs, old, newValue) -> {
                    log.debug("[ESTEIRA] Busca alterada: '{}' -> '{}'", old, newValue);
                    filteredData.setPredicate(criarPredicadoBusca(newValue));
                });

        SortedList<PropostaModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePropostas.comparatorProperty());
        tablePropostas.setItems(sortedData);
    }

    private Predicate<PropostaModel> criarPredicadoBusca(String filtro) {
        if (filtro == null || filtro.isBlank())
            return p -> true;

        String termo = filtro.trim().toLowerCase();
        log.trace("[ESTEIRA] criarPredicadoBusca: termo='{}'", termo);

        // 1. Lógica para Busca Financeira (Valores)
        try {
            String valorLimpo = termo.replaceAll("[^0-9,.]", "").replace(',', '.');

            if (termo.contains("-")) {
                String[] partes = termo.split("-");
                BigDecimal min = new BigDecimal(partes[0].replaceAll("[^0-9,.]", "").replace(',', '.'));
                BigDecimal max = new BigDecimal(partes[1].replaceAll("[^0-9,.]", "").replace(',', '.'));
                log.trace("[ESTEIRA] Filtro por intervalo: min={}, max={}", min, max);
                return p -> p.getValorSolicitado().compareTo(min) >= 0 && p.getValorSolicitado().compareTo(max) <= 0;
            }

            if (termo.startsWith(">")) {
                BigDecimal valor = new BigDecimal(valorLimpo);
                log.trace("[ESTEIRA] Filtro maior que: valor={}", valor);
                return p -> p.getValorSolicitado().compareTo(valor) > 0;
            }

            if (termo.startsWith("<")) {
                BigDecimal valor = new BigDecimal(valorLimpo);
                log.trace("[ESTEIRA] Filtro menor que: valor={}", valor);
                return p -> p.getValorSolicitado().compareTo(valor) < 0;
            }
        } catch (Exception e) {
            log.trace("[ESTEIRA] Busca financeira não aplicável: {}", e.getMessage());
        }

        // 2. Lógica de Busca Textual
        return p -> {
            String nome = p.getProponente().getNomeCompleto().toLowerCase();
            String cpf = p.getProponente().getCpf().replaceAll("[^0-9]", "");
            String banco = p.getBanco() != null ? p.getBanco().getNome().toLowerCase() : "";
            String status = p.getStatus().name().toLowerCase().replace("_", " ");

            boolean matches = nome.contains(termo) || cpf.contains(termo) || banco.contains(termo)
                    || status.contains(termo);
            if (matches)
                log.trace("[ESTEIRA] Filtro textual match para termo '{}' na proposta ID={}", termo, p.getId());
            return matches;
        };
    }

    @FXML
    public void recarregarDados() {
        log.info("[ESTEIRA] recarregarDados: Iniciando recarga assíncrona dos dados");
        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("[ESTEIRA] recarregarDados: Buscando findAllComDetalhes");
                    return repository.findAllComDetalhes();
                },
                dados -> {
                    log.info("[ESTEIRA] recarregarDados: {} propostas carregadas", dados.size());
                    masterData.setAll(dados);
                    limparPainelSeExcluido();
                },
                erro -> log.error("[ESTEIRA] recarregarDados: Erro crítico ao recarregar dados", erro));
    }

    // =========================================================================
    // INTEGRAÇÃO COM FORMULÁRIO (PROPOSTA CONTROLLER)
    // =========================================================================
    @FXML
    public void criarNovaProposta() {
        log.info("[ESTEIRA] criarNovaProposta: Criando nova proposta (limpa formulário)");
        abrirFormularioComProposta(new PropostaModel(), null);
        tablePropostas.getSelectionModel().clearSelection();
    }

    private void abrirFormularioComProposta(PropostaModel proposta, PropostaModel oldSelection) {
        log.debug("[ESTEIRA] abrirFormularioComProposta: Proposta ID={}, oldSelection ID={}",
                proposta != null ? proposta.getId() : "null",
                oldSelection != null ? oldSelection.getId() : "null");
        if (formController != null && formController.getViewModel().isDirty()) {
            log.warn("[ESTEIRA] Formulário com alterações não salvas. Solicitando confirmação para descarte.");
            solicitarConfirmacao(
                    "⚠️ Alterações Não Salvas",
                    "Tem alterações não guardadas no formulário ativo. Deseja descartá-las para abrir este registo?",
                    "Descartar", "#c62828",
                    () -> carregarPropostaNoFormulario(proposta),
                    () -> reverterSelecao(oldSelection));
        } else {
            carregarPropostaNoFormulario(proposta);
        }
    }

    private void carregarPropostaNoFormulario(PropostaModel proposta) {
        log.debug("[ESTEIRA] carregarPropostaNoFormulario: Carregando proposta ID={}",
                proposta != null ? proposta.getId() : "null");
        try {
            garantirFormularioCarregado();

            if (proposta != null && proposta.getId() != null) {
                carregarPropostaExistenteAssincrono(proposta.getId());
            } else {
                formController.carregarProposta(proposta);
                exibirPainelFormulario();
                log.debug("[ESTEIRA] Nova proposta carregada no formulário");
            }
        } catch (IOException e) {
            log.error("[ESTEIRA] Falha ao carregar formulário de proposta", e);
            throw new RuntimeException("Falha ao carregar formulário de proposta", e);
        }
    }

    private void carregarPropostaExistenteAssincrono(Long id) {
        log.debug("[ESTEIRA] carregarPropostaExistenteAssincrono: Buscando proposta ID={}", id);
        AsyncUtils.executarTaskAsync(
                () -> propostaService.carregarPropostaDetalhada(id),
                this::processarSucessoCarregamentoProposta,
                erro -> log.error("[ESTEIRA] Falha ao carregar detalhamento da proposta ID={}", id, erro));
    }

    private void processarSucessoCarregamentoProposta(PropostaModel completa) {
        if (completa != null) {
            log.info("[ESTEIRA] Proposta ID={} carregada com sucesso", completa.getId());
            formController.carregarProposta(completa);
            exibirPainelFormulario();
        } else {
            log.warn("[ESTEIRA] Proposta não encontrada no banco de dados (retornou null)");
            mostrarErro("Proposta não encontrada no banco de dados.");
        }
    }

    private void garantirFormularioCarregado() throws IOException {
        if (formController != null) {
            log.trace("[ESTEIRA] Formulário já carregado");
            return;
        }

        log.debug("[ESTEIRA] Carregando formulário FXML /fxml/proposta.fxml");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proposta.fxml"));
        loader.setControllerFactory(context::getBean);
        Node view = loader.load();

        formController = loader.getController();
        formController.setEsteiraController(this);
        formController.setOnPropostaFechada(this::limparPainelDetail);

        containerFormulario.getChildren().add(view);
        log.info("[ESTEIRA] Formulário de proposta carregado e injetado");
    }

    private void exibirPainelFormulario() {
        log.trace("[ESTEIRA] Exibindo painel de formulário, ocultando pane vazio");
        paneVazio.setVisible(false);
        paneVazio.setManaged(false);
    }

    private void reverterSelecao(PropostaModel oldSelection) {
        log.debug("[ESTEIRA] Revertendo seleção para proposta ID={}",
                oldSelection != null ? oldSelection.getId() : "null");
        isRevertendoSelecao = true;
        Platform.runLater(() -> {
            if (oldSelection != null) {
                tablePropostas.getSelectionModel().select(oldSelection);
            } else {
                tablePropostas.getSelectionModel().clearSelection();
            }
        });
    }

    private void limparPainelDetail() {
        log.debug("[ESTEIRA] Limpando painel de detalhe do formulário");
        containerFormulario.getChildren().removeIf(node -> node != paneVazio);
        paneVazio.setVisible(true);
        paneVazio.setManaged(true);
        tablePropostas.getSelectionModel().clearSelection();
        formController = null;
        log.info("[ESTEIRA] Painel de detalhe limpo e formulário removido");
    }

    public void selecionarPropostaPorId(Long idTarget) {
        if (idTarget == null || tablePropostas == null) {
            log.warn("[ESTEIRA] selecionarPropostaPorId: idTarget nulo ou tabela nula, ignorando");
            return;
        }
        log.info("[ESTEIRA] selecionarPropostaPorId: Buscando e selecionando proposta ID={}", idTarget);
        AsyncUtils.executarTaskAsync(
                repository::findAllComDetalhes,
                dados -> {
                    if (txtBusca != null)
                        txtBusca.clear();
                    masterData.setAll(dados);
                    dados.stream()
                            .filter(p -> idTarget.equals(p.getId()))
                            .findFirst()
                            .ifPresent(proposta -> {
                                log.debug("[ESTEIRA] Proposta ID={} encontrada, selecionando na tabela", idTarget);
                                tablePropostas.getSelectionModel().select(proposta);
                                tablePropostas.scrollTo(proposta);
                            });
                },
                erro -> log.error("[ESTEIRA] Erro ao selecionar proposta por ID={}", idTarget, erro));
    }

    private void limparPainelSeExcluido() {
        if (formController != null) {
            Long idAtivo = formController.getViewModel().idProperty().get();
            if (idAtivo != null) {
                boolean aindaExiste = masterData.stream().anyMatch(p -> p.getId().equals(idAtivo));
                if (!aindaExiste) {
                    log.warn("[ESTEIRA] Proposta ID={} foi excluída, limpando painel", idAtivo);
                    limparPainelDetail();
                } else {
                    log.trace("[ESTEIRA] Proposta ID={} ainda existe, mantendo painel", idAtivo);
                }
            }
        }
    }

    // =========================================================================
    // API PUBLICA DE OVERLAYS (SISTEMA DE AVISOS E IA)
    // =========================================================================
    public void solicitarConfirmacao(String titulo, String mensagem, String txtBotao, String corHex, Runnable onConfirm,
            Runnable onCancel) {
        log.info("[ESTEIRA] solicitacaoConfirmacao: titulo='{}', botao='{}'", titulo, txtBotao);
        Platform.runLater(() -> {
            lblConfirmacaoTitulo.setText(titulo);
            lblConfirmacaoTitulo.setStyle(String.format(STYLE_LBL_CONFIRM_TITULO, corHex));
            lblConfirmacaoTexto.setText(mensagem);

            btnConfirmarAcao.setText(txtBotao);
            btnConfirmarAcao.setStyle(String.format(STYLE_BTN_CONFIRM, corHex));

            this.acaoPendente = onConfirm;
            this.cancelPendente = onCancel;
            overlayConfirmacao.setVisible(true);
        });
    }

    @FXML
    public void confirmarAcao() {
        log.debug("[ESTEIRA] confirmarAcao: Usuário confirmou ação");
        overlayConfirmacao.setVisible(false);
        if (acaoPendente != null) {
            acaoPendente.run();
            acaoPendente = null;
        }
    }

    @FXML
    public void cancelarAcaoBase() {
        log.debug("[ESTEIRA] cancelarAcaoBase: Usuário cancelou ação");
        overlayConfirmacao.setVisible(false);
        if (cancelPendente != null) {
            cancelPendente.run();
            cancelPendente = null;
        }
        acaoPendente = null;
    }

    public void mostrarFeedback(String icone, String titulo, String conteudo, Runnable callback) {
        log.info("[ESTEIRA] mostrarFeedback: titulo='{}', conteudo length={}", titulo,
                conteudo != null ? conteudo.length() : 0);
        Platform.runLater(() -> {
            this.onFeedbackClose = callback;

            if (isConteudoHtml(conteudo)) {
                exibirFeedbackRico(icone, titulo, conteudo);
            } else {
                exibirFeedbackSimples(icone, titulo, conteudo);
            }
        });
    }

    private boolean isConteudoHtml(String conteudo) {
        boolean html = conteudo != null && (conteudo.contains("<strong") || conteudo.contains("<span") ||
                conteudo.contains("<p>") || conteudo.contains("<br>") ||
                conteudo.contains("<table") || conteudo.contains("<ul"));
        log.trace("[ESTEIRA] isConteudoHtml: {}", html);
        return html;
    }

    private void exibirFeedbackRico(String icone, String titulo, String htmlBody) {
        log.debug("[ESTEIRA] exibirFeedbackRico: Exibindo feedback HTML");
        lblFeedbackIcon.setText(icone);
        lblFeedbackTitle.setText(titulo);

        WebEngine engine = webFeedback.getEngine();
        engine.loadContent(String.format(HTML_TEMPLATE_IA, htmlBody));

        estilizarBotaoBaseadoNoIcone(btnFeedbackAction, icone);
        overlayFeedback.setVisible(true);
    }

    private void exibirFeedbackSimples(String icone, String titulo, String conteudo) {
        log.debug("[ESTEIRA] exibirFeedbackSimples: Exibindo alerta simples");
        lblAlertaIcone.setText(icone);
        lblAlertaTitulo.setText(titulo);
        lblAlertaMensagem.setText(conteudo);

        estilizarBotaoBaseadoNoIcone(btnAlertaAcao, icone);
        overlayAlertaSimples.setVisible(true);
    }

    private void estilizarBotaoBaseadoNoIcone(Button botao, String icone) {
        if (icone.contains("✅")) {
            botao.setStyle(STYLE_BTN_SUCCESS);
            log.trace("[ESTEIRA] Estilo aplicado: sucesso");
        } else if (icone.contains("❌") || icone.contains("🗑️")) {
            botao.setStyle(STYLE_BTN_DANGER);
            log.trace("[ESTEIRA] Estilo aplicado: perigo");
        } else {
            botao.setStyle(STYLE_BTN_WARNING);
            log.trace("[ESTEIRA] Estilo aplicado: aviso");
        }
    }

    @FXML
    public void fecharFeedback() {
        log.debug("[ESTEIRA] fecharFeedback: Fechando overlay de feedback");
        overlayFeedback.setVisible(false);
        dispararCallbackFechamento();
    }

    @FXML
    public void fecharAlertaSimples() {
        log.debug("[ESTEIRA] fecharAlertaSimples: Fechando overlay de alerta simples");
        overlayAlertaSimples.setVisible(false);
        dispararCallbackFechamento();
    }

    private void dispararCallbackFechamento() {
        if (onFeedbackClose != null) {
            log.trace("[ESTEIRA] Disparando callback de fechamento");
            onFeedbackClose.run();
            onFeedbackClose = null;
        }
    }

    private void mostrarErro(String msg) {
        log.error("[ESTEIRA] mostrarErro: {}", msg);
        mostrarFeedback("❌", "Erro Interno", msg, null);
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS
    // =========================================================================
    private SimpleStringProperty formatarData(LocalDate data) {
        String formatted = data != null ? data.format(FMT_DATA) : "-";
        log.trace("[ESTEIRA] formatarData: {} -> {}", data, formatted);
        return new SimpleStringProperty(formatted);
    }

    private SimpleStringProperty formatarMoeda(BigDecimal valor) {
        String formatted = (valor != null && valor.compareTo(BigDecimal.ZERO) > 0)
                ? FinanceiroUtils.formatarParaExibicao(valor)
                : "-";
        log.trace("[ESTEIRA] formatarMoeda: {} -> {}", valor, formatted);
        return new SimpleStringProperty(formatted);
    }

    private SimpleStringProperty textoOuHifen(String texto) {
        String result = (texto != null && !texto.isBlank()) ? texto : "-";
        log.trace("[ESTEIRA] textoOuHifen: '{}' -> '{}'", texto, result);
        return new SimpleStringProperty(result);
    }

    private Color corDoStatus(StatusPropostaModel status) {
        Color cor = switch (status) {
            case PAGO -> Color.GREEN;
            case REPROVADA, CANCELADO -> Color.RED;
            case PENDENTE, AGUARDANDO_DOC -> Color.DARKORANGE;
            default -> Color.BLACK;
        };
        log.trace("[ESTEIRA] corDoStatus: {} -> {}", status, cor);
        return cor;
    }

    @Override
    public void dispose() {
        log.info("[ESTEIRA] dispose: Desinscrevendo do event hub e liberando recursos");
        eventHub.desinscrever(this::recarregarDados);
        proponenteEventHub.desinscrever(this::recarregarDados);
    }
}