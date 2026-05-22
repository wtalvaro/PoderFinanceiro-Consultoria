package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
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

@Component
@Scope("prototype")
public class EsteiraPropostasController {

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

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final PropostaRepository repository;
    private final PropostaService propostaService;
    private final ApplicationContext context;

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
            ApplicationContext context) {
        this.repository = repository;
        this.propostaService = propostaService;
        this.context = context;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltroReativo();
        recarregarDados();
    }

    // =========================================================================
    // CONFIGURAÇÃO DA TABELA (SRP)
    // =========================================================================
    private void configurarTabela() {
        configurarColunasDados();
        configurarColunaStatus();
        configurarEventosSelecao();
    }

    private void configurarColunasDados() {
        colData.setCellValueFactory(d -> formatarData(d.getValue().getDataSolicitacao()));
        colCliente.setCellValueFactory(d -> textoOuHifen(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(
                d -> textoOuHifen(d.getValue().getBanco() != null ? d.getValue().getBanco().getNome() : null));
        colValorSol.setCellValueFactory(d -> formatarMoeda(d.getValue().getValorSolicitado()));
    }

    private void configurarColunaStatus() {
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
            }
        });
    }

    private void configurarEventosSelecao() {
        tablePropostas.getSelectionModel().selectedItemProperty().addListener((obs, old, novaProposta) -> {
            if (isRevertendoSelecao) {
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
        FilteredList<PropostaModel> filteredData = new FilteredList<>(masterData, p -> true);
        txtBusca.textProperty()
                .addListener((obs, old, newValue) -> filteredData.setPredicate(criarPredicadoBusca(newValue)));

        SortedList<PropostaModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePropostas.comparatorProperty());
        tablePropostas.setItems(sortedData);
    }

    private Predicate<PropostaModel> criarPredicadoBusca(String filtro) {
        if (filtro == null || filtro.isBlank())
            return p -> true;

        String termo = filtro.toLowerCase();
        return p -> {
            String nome = p.getProponente().getNomeCompleto().toLowerCase();
            String cpf = p.getProponente().getCpf().replaceAll("[^0-9]", "");
            String banco = p.getBanco() != null ? p.getBanco().getNome().toLowerCase() : "";

            return nome.contains(termo) || cpf.contains(termo) || banco.contains(termo)
                    || p.getStatus().name().toLowerCase().contains(termo);
        };
    }

    @FXML
    public void recarregarDados() {
        masterData.setAll(repository.findAllComDetalhes());
    }

    // =========================================================================
    // INTEGRAÇÃO COM FORMULÁRIO (PROPOSTA CONTROLLER)
    // =========================================================================
    @FXML
    public void criarNovaProposta() {
        abrirFormularioComProposta(new PropostaModel(), null);
        tablePropostas.getSelectionModel().clearSelection();
    }

    private void abrirFormularioComProposta(PropostaModel proposta, PropostaModel oldSelection) {
        if (formController != null && formController.getViewModel().isDirty()) {
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
        try {
            garantirFormularioCarregado();

            if (proposta.getId() != null) {
                carregarPropostaExistenteAssincrono(proposta.getId());
            } else {
                formController.getViewModel().loadFromModel(proposta);
                exibirPainelFormulario();
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao carregar formulário de proposta", e);
        }
    }

    private void carregarPropostaExistenteAssincrono(Long id) {
        Task<PropostaModel> task = new Task<>() {
            @Override
            protected PropostaModel call() {
                return propostaService.carregarPropostaDetalhada(id);
            }
        };

        task.setOnSucceeded(e -> processarSucessoCarregamentoProposta(task.getValue()));
        task.setOnFailed(e -> task.getException().printStackTrace());

        new Thread(task).start();
    }

    private void processarSucessoCarregamentoProposta(PropostaModel completa) {
        if (completa != null) {
            formController.carregarProposta(completa);
            exibirPainelFormulario();
        } else {
            mostrarErro("Proposta não encontrada no banco de dados.");
        }
    }

    private void garantirFormularioCarregado() throws IOException {
        if (formController != null)
            return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proposta.fxml"));
        loader.setControllerFactory(context::getBean);
        Node view = loader.load();

        formController = loader.getController();
        formController.setEsteiraController(this);

        formController.setOnPropostaSalva(this::recarregarDados);
        formController.setOnPropostaFechada(this::limparPainelDetail);
        formController.setOnPropostaRemovida(() -> {
            limparPainelDetail();
            recarregarDados();
        });

        containerFormulario.getChildren().add(view);
    }

    private void exibirPainelFormulario() {
        paneVazio.setVisible(false);
        paneVazio.setManaged(false);
    }

    private void reverterSelecao(PropostaModel oldSelection) {
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
        containerFormulario.getChildren().removeIf(node -> node != paneVazio);
        paneVazio.setVisible(true);
        paneVazio.setManaged(true);
        tablePropostas.getSelectionModel().clearSelection();
        formController = null;
    }

    public void selecionarPropostaPorId(Long idTarget) {
        if (idTarget == null || tablePropostas == null)
            return;

        if (txtBusca != null)
            txtBusca.clear();

        masterData.stream()
                .filter(p -> idTarget.equals(p.getId()))
                .findFirst()
                .ifPresent(proposta -> {
                    tablePropostas.getSelectionModel().select(proposta);
                    tablePropostas.scrollTo(proposta);
                });
    }

    // =========================================================================
    // API PUBLICA DE OVERLAYS (SISTEMA DE AVISOS E IA)
    // =========================================================================
    public void solicitarConfirmacao(String titulo, String mensagem, String txtBotao, String corHex, Runnable onConfirm,
            Runnable onCancel) {
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
        overlayConfirmacao.setVisible(false);
        if (acaoPendente != null) {
            acaoPendente.run();
            acaoPendente = null;
        }
    }

    @FXML
    public void cancelarAcaoBase() {
        overlayConfirmacao.setVisible(false);
        if (cancelPendente != null) {
            cancelPendente.run();
            cancelPendente = null;
        }
        acaoPendente = null;
    }

    public void mostrarFeedback(String icone, String titulo, String conteudo, Runnable callback) {
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
        return conteudo != null && (conteudo.contains("<strong") || conteudo.contains("<span") ||
                conteudo.contains("<p>") || conteudo.contains("<br>") ||
                conteudo.contains("<table") || conteudo.contains("<ul"));
    }

    private void exibirFeedbackRico(String icone, String titulo, String htmlBody) {
        lblFeedbackIcon.setText(icone);
        lblFeedbackTitle.setText(titulo);

        WebEngine engine = webFeedback.getEngine();
        engine.loadContent(String.format(HTML_TEMPLATE_IA, htmlBody));

        estilizarBotaoBaseadoNoIcone(btnFeedbackAction, icone);
        overlayFeedback.setVisible(true);
    }

    private void exibirFeedbackSimples(String icone, String titulo, String conteudo) {
        lblAlertaIcone.setText(icone);
        lblAlertaTitulo.setText(titulo);
        lblAlertaMensagem.setText(conteudo);

        estilizarBotaoBaseadoNoIcone(btnAlertaAcao, icone);
        overlayAlertaSimples.setVisible(true);
    }

    private void estilizarBotaoBaseadoNoIcone(Button botao, String icone) {
        if (icone.contains("✅")) {
            botao.setStyle(STYLE_BTN_SUCCESS);
        } else if (icone.contains("❌") || icone.contains("🗑️")) {
            botao.setStyle(STYLE_BTN_DANGER);
        } else {
            botao.setStyle(STYLE_BTN_WARNING);
        }
    }

    @FXML
    public void fecharFeedback() {
        overlayFeedback.setVisible(false);
        dispararCallbackFechamento();
    }

    @FXML
    public void fecharAlertaSimples() {
        overlayAlertaSimples.setVisible(false);
        dispararCallbackFechamento();
    }

    private void dispararCallbackFechamento() {
        if (onFeedbackClose != null) {
            onFeedbackClose.run();
            onFeedbackClose = null;
        }
    }

    private void mostrarErro(String msg) {
        mostrarFeedback("❌", "Erro Interno", msg, null);
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS
    // =========================================================================
    private SimpleStringProperty formatarData(LocalDate data) {
        return new SimpleStringProperty(data != null ? data.format(FMT_DATA) : "-");
    }

    private SimpleStringProperty formatarMoeda(BigDecimal valor) {
        return new SimpleStringProperty(
                valor != null && valor.compareTo(BigDecimal.ZERO) > 0 ? FinanceiroUtils.formatarParaExibicao(valor)
                        : "-");
    }

    private SimpleStringProperty textoOuHifen(String texto) {
        return new SimpleStringProperty(texto != null && !texto.isBlank() ? texto : "-");
    }

    private Color corDoStatus(StatusPropostaModel status) {
        return switch (status) {
            case PAGO -> Color.GREEN;
            case REPROVADA, CANCELADO -> Color.RED;
            case PENDENTE, AGUARDANDO_DOC -> Color.DARKORANGE;
            default -> Color.BLACK;
        };
    }
}