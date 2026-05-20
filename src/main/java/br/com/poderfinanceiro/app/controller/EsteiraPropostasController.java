package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
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
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

@Component
@Scope("prototype")
public class EsteiraPropostasController {

    private final PropostaRepository repository;
    private final PropostaService propostaService;
    private final ApplicationContext context;

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

    // Overlays Universais
    @FXML
    private VBox overlayConfirmacao, overlayFeedback, overlayAlertaSimples;

    // Componentes Overlay Confirmação
    @FXML
    private Label lblConfirmacaoTitulo, lblConfirmacaoTexto;
    @FXML
    private Button btnConfirmarAcao;

    // Componentes Overlay Feedback IA (WebView)
    @FXML
    private Label lblFeedbackIcon, lblFeedbackTitle;
    @FXML
    private Button btnFeedbackAction;
    @FXML
    private WebView webFeedback;

    // Componentes Overlay Alerta Simples (Novo)
    @FXML
    private Label lblAlertaIcone, lblAlertaTitulo, lblAlertaMensagem;
    @FXML
    private Button btnAlertaAcao;

    private final ObservableList<PropostaModel> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PropostaController formController;
    private boolean isRevertendoSelecao = false;

    // Controle de Callbacks dos Overlays
    private Runnable acaoPendente;
    private Runnable cancelPendente;
    private Runnable onFeedbackClose;

    public EsteiraPropostasController(PropostaRepository repository, PropostaService propostaService,
            ApplicationContext context) {
        this.repository = repository;
        this.propostaService = propostaService;
        this.context = context;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltroReativo();
        recarregarDados();
    }

    private void configurarTabela() {
        colData.setCellValueFactory(d -> formatarData(d.getValue().getDataSolicitacao()));
        colCliente.setCellValueFactory(d -> textoOuHifen(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(
                d -> textoOuHifen(d.getValue().getBanco() != null ? d.getValue().getBanco().getNome() : null));
        colValorSol.setCellValueFactory(d -> formatarMoeda(d.getValue().getValorSolicitado()));

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

        tablePropostas.getSelectionModel().selectedItemProperty().addListener((obs, old, novaProposta) -> {
            if (isRevertendoSelecao) {
                isRevertendoSelecao = false;
                return;
            }
            if (novaProposta != null)
                abrirFormularioComProposta(novaProposta, old);
        });
    }

    private void configurarFiltroReativo() {
        FilteredList<PropostaModel> filteredData = new FilteredList<>(masterData, p -> true);
        txtBusca.textProperty()
                .addListener((obs, old, newValue) -> filteredData.setPredicate(criarPredicadoBusca(newValue)));
        SortedList<PropostaModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePropostas.comparatorProperty());
        tablePropostas.setItems(sortedData);
    }

    private Predicate<PropostaModel> criarPredicadoBusca(String filtro) {
        if (filtro == null || filtro.isEmpty())
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

    @FXML
    public void criarNovaProposta() {
        abrirFormularioComProposta(new PropostaModel(), null);
        tablePropostas.getSelectionModel().clearSelection();
    }

    private void abrirFormularioComProposta(PropostaModel proposta, PropostaModel oldSelection) {
        if (formController != null && formController.getViewModel().isDirty()) {
            solicitarConfirmacao("⚠️ Alterações Não Salvas",
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
                Task<PropostaModel> task = new Task<>() {
                    @Override
                    protected PropostaModel call() {
                        return propostaService.carregarPropostaDetalhada(proposta.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    PropostaModel completa = task.getValue();
                    if (completa != null) {
                        formController.carregarProposta(completa);
                        paneVazio.setVisible(false);
                        paneVazio.setManaged(false);
                    } else
                        mostrarErro("Proposta não encontrada no banco de dados.");
                });
                task.setOnFailed(e -> {
                    task.getException().printStackTrace();
                });
                new Thread(task).start();
            } else {
                formController.getViewModel().loadFromModel(proposta);
                paneVazio.setVisible(false);
                paneVazio.setManaged(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao carregar formulário de proposta", e);
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

    private void reverterSelecao(PropostaModel oldSelection) {
        isRevertendoSelecao = true;
        Platform.runLater(() -> {
            if (oldSelection != null)
                tablePropostas.getSelectionModel().select(oldSelection);
            else
                tablePropostas.getSelectionModel().clearSelection();
        });
    }

    private void limparPainelDetail() {
        containerFormulario.getChildren().removeIf(node -> node != paneVazio);
        paneVazio.setVisible(true);
        paneVazio.setManaged(true);
        tablePropostas.getSelectionModel().clearSelection();
        formController = null;
    }

    // =========================================================================
    // API PUBLICA DE OVERLAYS (Chamada pelo PropostaController)
    // =========================================================================

    public void solicitarConfirmacao(String titulo, String mensagem, String txtBotao, String corHex, Runnable onConfirm,
            Runnable onCancel) {
        Platform.runLater(() -> {
            lblConfirmacaoTitulo.setText(titulo);
            lblConfirmacaoTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + corHex + ";");
            lblConfirmacaoTexto.setText(mensagem);

            btnConfirmarAcao.setText(txtBotao);
            btnConfirmarAcao.setStyle("-fx-background-color: " + corHex
                    + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");

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

    // 🚀 MÁGICA: O mesmo método agora roteia para o layout adequado dependendo do
    // conteúdo
    public void mostrarFeedback(String icone, String titulo, String conteudo, Runnable callback) {
        Platform.runLater(() -> {
            // Verifica de forma segura se o conteúdo tem marcadores HTML usados pela IA
            boolean isHtml = conteudo != null && (conteudo.contains("<strong") || conteudo.contains("<span")
                    || conteudo.contains("<p>") || conteudo.contains("<br>") || conteudo.contains("<table")
                    || conteudo.contains("<ul"));

            this.onFeedbackClose = callback;

            if (isHtml) {
                // EXIBIÇÃO RICA (Gemini IA em WebView)
                lblFeedbackIcon.setText(icone);
                lblFeedbackTitle.setText(titulo);

                String htmlTemplate = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
                            <style>
                                body {
                                    font-family: sans-serif;
                                    padding: 25px;
                                    box-sizing: border-box;
                                    color: #333;
                                }
                            </style>
                        </head>
                        <body>
                            %s
                        </body>
                        </html>
                        """
                        .formatted(conteudo);

                WebEngine engine = webFeedback.getEngine();
                engine.loadContent(htmlTemplate);
                estilizarBotaoBaseadoNoIcone(btnFeedbackAction, icone);
                overlayFeedback.setVisible(true);

            } else {
                // EXIBIÇÃO COMPACTA (Notificações do Sistema: Salvar, Excluir, etc)
                lblAlertaIcone.setText(icone);
                lblAlertaTitulo.setText(titulo);
                lblAlertaMensagem.setText(conteudo);
                estilizarBotaoBaseadoNoIcone(btnAlertaAcao, icone);
                overlayAlertaSimples.setVisible(true);
            }
        });
    }

    private void estilizarBotaoBaseadoNoIcone(Button botao, String icone) {
        String baseStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 6;";
        if (icone.contains("✅"))
            botao.setStyle("-fx-background-color: #2e7d32; " + baseStyle);
        else if (icone.contains("❌") || icone.contains("🗑️"))
            botao.setStyle("-fx-background-color: #c62828; " + baseStyle);
        else
            botao.setStyle("-fx-background-color: #f57c00; " + baseStyle);
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

    // 🚀 Método invocado pelo WorkspaceController para selecionar uma proposta
    // vinda do Dashboard
    public void selecionarPropostaPorId(Long idTarget) {
        if (idTarget == null || tablePropostas == null)
            return;

        if (txtBusca != null) {
            txtBusca.clear();
        }

        for (PropostaModel proposta : masterData) {
            if (idTarget.equals(proposta.getId())) {
                tablePropostas.getSelectionModel().select(proposta);
                tablePropostas.scrollTo(proposta);
                break;
            }
        }
    }

    private void mostrarErro(String msg) {
        mostrarFeedback("❌", "Erro Interno", msg, null);
    }

    private SimpleStringProperty formatarData(java.time.LocalDate data) {
        return new SimpleStringProperty(data != null ? data.format(dateFormatter) : "-");
    }

    private SimpleStringProperty formatarMoeda(BigDecimal valor) {
        return new SimpleStringProperty(
                valor != null && valor.compareTo(BigDecimal.ZERO) > 0 ? FinanceiroUtils.formatarParaExibicao(valor)
                        : "-");
    }

    private SimpleStringProperty textoOuHifen(String texto) {
        return new SimpleStringProperty(texto != null ? texto : "-");
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