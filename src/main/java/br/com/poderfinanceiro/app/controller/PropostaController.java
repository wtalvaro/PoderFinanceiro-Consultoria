package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.*;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.service.*;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Component
@Scope("prototype")
public class PropostaController {

    private final PropostaViewModel viewModel;
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;
    private final DocumentoService documentoService;
    private final MainController mainController;
    private final ProponenteService proponenteService;
    private final AtendimentoContextService contextoService;
    private final AuthService authService; // 🚀 NOVO: Necessário para resgatar credenciais da IA
    private final GeminiService geminiService; // 🚀 NOVO: Motor de inteligência artificial multimodal

    private EsteiraPropostasController esteiraController;

    @FXML
    private ComboBox<TipoConvenioModel> cbConvenio;
    @FXML
    private ComboBox<BancoModel> cbBanco;
    @FXML
    private ComboBox<TabelaJurosModel> cbTabela;
    @FXML
    private ComboBox<StatusPropostaModel> cbStatus;
    @FXML
    private ComboBox<ProponenteModel> cbCliente;
    @FXML
    private TextField txtValorSolicitado, txtValorAprovado, txtParcela;
    @FXML
    private Spinner<Integer> spinPrazo, spinPrazoDesejado;
    @FXML
    private TextArea txtObservacoes;
    @FXML
    private Label lblComissaoEstimada, lblTituloComissao, lblTotalPago;

    // Botões de Ação
    @FXML
    private Button btnAnexarDocumento;
    @FXML
    private Button btnSalvar;
    @FXML
    private Button btnRemover;

    @FXML
    private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colTipoDocumento, colDataUpload;
    @FXML
    private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    private Runnable onPropostaSalva;
    private Runnable onPropostaFechada;
    private Runnable onPropostaRemovida;

    private List<TabelaJurosModel> todasTabelasAtivas;
    private List<TabelaJurosModel> tabelasElegiveisDaTriagem;
    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();
    private boolean isUpdatingInterface = false;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PropostaController(PropostaViewModel viewModel, DocumentoService documentoService,
            PropostaService propostaService, TabelaJurosService tabelaJurosService,
            MainController mainController, ProponenteService proponenteService,
            AtendimentoContextService contextoService, AuthService authService, GeminiService geminiService) {
        this.viewModel = viewModel;
        this.documentoService = documentoService;
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
        this.mainController = mainController;
        this.proponenteService = proponenteService;
        this.contextoService = contextoService;
        this.authService = authService;
        this.geminiService = geminiService;
    }

    public void setEsteiraController(EsteiraPropostasController esteiraController) {
        this.esteiraController = esteiraController;
    }

    @FXML
    public void initialize() {
        carregarListasBase();
        configurarConversoresVisuais();
        configurarBindings();
        configurarFiltrosInteligentes();
        configurarTabelaDocumentos();
        configurarAutoSelecaoTextos();
        configurarIndicadoresDinamicos();
        configurarTravasEAlertas();
    }

    private void carregarListasBase() {
        todasTabelasAtivas = tabelaJurosService.listarAtivas();
        cbStatus.setItems(FXCollections.observableArrayList(StatusPropostaModel.values()));
        if (cbConvenio != null)
            cbConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        if (cbCliente != null)
            cbCliente.setItems(FXCollections.observableArrayList(proponenteService.listarMinhaCarteira()));
    }

    private void configurarConversoresVisuais() {
        if (cbConvenio != null)
            cbConvenio.setConverter(criarConversor(TipoConvenioModel::getLabel));
        cbBanco.setConverter(criarConversor(b -> b != null ? b.getNome() : "Aguardando Triagem..."));
        cbTabela.setConverter(
                criarConversor(t -> t != null ? t.getNomeTabela() + " (" + t.getComissaoPercentual() + "%)"
                        : "Selecione a Tabela..."));
        if (cbCliente != null)
            cbCliente.setConverter(criarConversor(
                    c -> c != null ? c.getNomeCompleto() + " (" + c.getCpf() + ")" : "Selecione o Cliente..."));
    }

    private void configurarBindings() {
        cbStatus.valueProperty().bindBidirectional(viewModel.statusProperty());
        txtObservacoes.textProperty().bindBidirectional(viewModel.observacoesProperty());
        bindComboSafely(cbConvenio, viewModel.convenioProperty());
        bindComboSafely(cbBanco, viewModel.bancoProperty());
        bindCliente();
        viewModel.convenioProperty().addListener((obs, old, val) -> safeSet(cbConvenio, val));
        viewModel.bancoProperty().addListener((obs, old, val) -> safeSet(cbBanco, val));
        configurarSpinner(spinPrazo, viewModel.quantidadeParcelasProperty());
        if (spinPrazoDesejado != null)
            configurarSpinner(spinPrazoDesejado, viewModel.prazoDesejadoProperty());
        vincularMascaraMoeda(txtValorSolicitado, viewModel.valorSolicitadoProperty());
        vincularMascaraMoeda(txtValorAprovado, viewModel.valorAprovadoProperty());
        vincularMascaraMoeda(txtParcela, viewModel.valorParcelaProperty());
    }

    private <T> void bindComboSafely(ComboBox<T> combo, javafx.beans.property.Property<T> property) {
        combo.valueProperty().addListener((obs, old, val) -> {
            if (!isUpdatingInterface)
                property.setValue(val);
        });
    }

    private <T> void safeSet(ComboBox<T> combo, T val) {
        if (!isUpdatingInterface)
            combo.setValue(val);
    }

    private void bindCliente() {
        cbCliente.valueProperty().addListener((obs, old, val) -> {
            if (!isUpdatingInterface)
                viewModel.proponenteProperty().set(val);
        });
        viewModel.proponenteProperty().addListener((obs, old, val) -> {
            if (!isUpdatingInterface)
                cbCliente.setValue(val);
            cbCliente.setDisable(val != null && viewModel.idProperty().get() != null);
        });
    }

    public void carregarProposta(PropostaModel completa) {
        isUpdatingInterface = true;
        try {
            viewModel.loadFromModel(completa);
            sincronizarComboBoxesComViewModel();
            cbCliente.setDisable(completa.getId() != null);

            if (completa.getId() != null) {
                carregarDocumentosDaProposta(completa.getId());
                aplicarBloqueio();
            }
        } finally {
            isUpdatingInterface = false;
        }

        if (contextoService != null) {
            contextoService.setPropostaAtiva(completa);
            contextoService.setTelaAtualFocada(AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS);
        }
    }

    private void sincronizarComboBoxesComViewModel() {
        ProponenteModel p = viewModel.proponenteProperty().get();
        if (p != null) {
            cbCliente.getItems().stream().filter(c -> c.getId().equals(p.getId())).findFirst()
                    .ifPresentOrElse(cbCliente::setValue, () -> {
                        cbCliente.getItems().add(p);
                        cbCliente.setValue(p);
                    });
        } else
            cbCliente.getSelectionModel().clearSelection();

        cbStatus.setValue(viewModel.statusProperty().get());
        cbConvenio.setValue(viewModel.convenioProperty().get());

        BancoModel b = viewModel.bancoProperty().get();
        if (b != null) {
            cbBanco.getItems().stream().filter(item -> item.getId().equals(b.getId())).findFirst()
                    .ifPresentOrElse(cbBanco::setValue, () -> {
                        cbBanco.getItems().add(b);
                        cbBanco.setValue(b);
                    });
        } else
            cbBanco.getSelectionModel().clearSelection();

        Long idTabela = viewModel.tabelaIdProperty().get();
        if (idTabela != null) {
            TabelaJurosModel tab = todasTabelasAtivas.stream().filter(t -> t.getId().equals(idTabela)).findFirst()
                    .orElse(null);
            if (tab != null) {
                cbTabela.getItems().stream().filter(item -> item.getId().equals(idTabela)).findFirst()
                        .ifPresentOrElse(cbTabela::setValue, () -> {
                            cbTabela.getItems().add(tab);
                            cbTabela.setValue(tab);
                        });
            }
        } else
            cbTabela.getSelectionModel().clearSelection();

        calcularComissao();
    }

    private void configurarFiltrosInteligentes() {
        viewModel.valorSolicitadoProperty().addListener((obs, old, val) -> executarTriagem());
        viewModel.prazoDesejadoProperty().addListener((obs, old, val) -> executarTriagem());
        viewModel.convenioProperty().addListener((obs, old, val) -> executarTriagem());
        viewModel.bancoProperty().addListener((obs, old, banco) -> atualizarTabelasDoBanco((BancoModel) banco));
        viewModel.valorAprovadoProperty().addListener((obs, old, val) -> calcularComissao());
        cbTabela.valueProperty().addListener((obs, old, nova) -> {
            if (!isUpdatingInterface) {
                viewModel.tabelaIdProperty().set(nova != null ? nova.getId() : null);
                calcularComissao();
            }
        });
        viewModel.tabelaIdProperty().addListener((obs, old, id) -> {
            if (!isUpdatingInterface)
                carregarDadosDaTabela(id);
        });
    }

    private void executarTriagem() {
        if (isUpdatingInterface)
            return;
        TipoConvenioModel convenio = cbConvenio != null ? cbConvenio.getValue() : null;
        BigDecimal valor = viewModel.valorSolicitadoProperty().get();
        if (convenio == null || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            limparFiltrosDeTriagem();
            return;
        }

        ProponenteModel proponente = viewModel.proponenteProperty().get();
        Integer prazo = viewModel.prazoDesejadoProperty().get();

        tabelasElegiveisDaTriagem = todasTabelasAtivas.stream()
                .filter(t -> t.getTipoConvenio() == convenio)
                .filter(t -> atendeValor(t, valor))
                .filter(t -> atendePrazo(t, prazo))
                .filter(t -> atendeRenda(t, proponente))
                .filter(t -> atendeIdade(t, proponente))
                .toList();

        List<BancoModel> bancos = tabelasElegiveisDaTriagem.stream().map(TabelaJurosModel::getBanco)
                .filter(Objects::nonNull).distinct().toList();
        atualizarComboBancos(bancos);
    }

    private boolean atendeValor(TabelaJurosModel t, BigDecimal valor) {
        BigDecimal min = (t.getValorMinimoEmprestimo() != null
                && t.getValorMinimoEmprestimo().compareTo(BigDecimal.ZERO) > 0) ? t.getValorMinimoEmprestimo()
                        : BigDecimal.ZERO;
        if (valor.compareTo(min) < 0)
            return false;
        boolean hasMax = t.getValorMaximoEmprestimo() != null
                && t.getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) > 0;
        return !hasMax || valor.compareTo(t.getValorMaximoEmprestimo()) <= 0;
    }

    private boolean atendePrazo(TabelaJurosModel t, Integer prazo) {
        if (prazo == null || prazo <= 0)
            return true;
        int min = (t.getPrazoMinimo() != null && t.getPrazoMinimo() > 0) ? t.getPrazoMinimo() : 1;
        int max = (t.getPrazoMaximo() != null && t.getPrazoMaximo() > 0) ? t.getPrazoMaximo() : 999;
        return prazo >= min && prazo <= max;
    }

    private boolean atendeRenda(TabelaJurosModel t, ProponenteModel p) {
        BigDecimal minRenda = t.getRendaMinima() != null ? t.getRendaMinima() : BigDecimal.ZERO;
        if (minRenda.compareTo(BigDecimal.ZERO) <= 0)
            return true;
        BigDecimal renda = (p != null && p.getRendaMensal() != null) ? p.getRendaMensal() : BigDecimal.ZERO;
        return renda.compareTo(minRenda) >= 0;
    }

    private boolean atendeIdade(TabelaJurosModel t, ProponenteModel p) {
        int idade = calcularIdade(p);
        if (idade <= 0)
            return true;
        int min = (t.getIdadeMinima() != null && t.getIdadeMinima() > 0) ? t.getIdadeMinima() : 0;
        int max = (t.getIdadeMaxima() != null && t.getIdadeMaxima() > 0) ? t.getIdadeMaxima() : 999;
        return idade >= min && idade <= max;
    }

    private void limparFiltrosDeTriagem() {
        cbBanco.getItems().clear();
        cbTabela.getItems().clear();
        tabelasElegiveisDaTriagem = null;
    }

    private void atualizarComboBancos(List<BancoModel> bancos) {
        BancoModel atual = cbBanco.getValue();
        executarSemGatilhos(() -> {
            cbBanco.setItems(FXCollections.observableArrayList(bancos));
            if (atual != null && bancos.stream().anyMatch(b -> b.getId().equals(atual.getId())))
                cbBanco.setValue(atual);
        });
        viewModel.bancoProperty().set(cbBanco.getValue());
        atualizarTabelasDoBanco(cbBanco.getValue());
    }

    private void atualizarTabelasDoBanco(BancoModel banco) {
        if (isUpdatingInterface)
            return;
        if (banco != null && tabelasElegiveisDaTriagem != null) {
            List<TabelaJurosModel> tabelas = tabelasElegiveisDaTriagem.stream()
                    .filter(t -> t.getBanco().getId().equals(banco.getId()))
                    .sorted(Comparator.comparing(TabelaJurosModel::getComissaoPercentual).reversed()).toList();
            TabelaJurosModel atual = cbTabela.getValue();
            executarSemGatilhos(() -> {
                cbTabela.setItems(FXCollections.observableArrayList(tabelas));
                if (atual != null && tabelas.stream().anyMatch(t -> t.getId().equals(atual.getId())))
                    cbTabela.setValue(atual);
                else
                    cbTabela.setValue(null);
            });
        } else {
            executarSemGatilhos(() -> {
                cbTabela.getItems().clear();
                cbTabela.setValue(null);
            });
        }
        viewModel.tabelaIdProperty().set(cbTabela.getValue() != null ? cbTabela.getValue().getId() : null);
        calcularComissao();
    }

    private void carregarDadosDaTabela(Long idTabela) {
        if (isUpdatingInterface)
            return;
        executarSemGatilhos(() -> {
            if (idTabela != null) {
                TabelaJurosModel tab = todasTabelasAtivas.stream().filter(t -> t.getId().equals(idTabela)).findFirst()
                        .orElse(null);
                if (tab != null) {
                    cbConvenio.setValue(tab.getTipoConvenio());
                    viewModel.convenioProperty().set(tab.getTipoConvenio());
                    executarTriagem();
                    cbBanco.setValue(tab.getBanco());
                    viewModel.bancoProperty().set(tab.getBanco());
                    atualizarTabelasDoBanco(tab.getBanco());
                    cbTabela.setValue(tab);
                    viewModel.tabelaIdProperty().set(tab.getId());
                    calcularComissao();
                }
            } else {
                limparFiltrosDeTriagem();
                calcularComissao();
            }
        });
    }

    private void calcularComissao() {
        BigDecimal base = viewModel.valorAprovadoProperty().get();
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0)
            base = viewModel.valorSolicitadoProperty().get();
        TabelaJurosModel tabela = cbTabela.getValue();

        if (base != null && base.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            BigDecimal comissao = propostaService.calcularComissaoEstimada(base, tabela.getId());
            viewModel.comissaoEstimadaProperty().set(comissao);
            lblComissaoEstimada.setText(String.format("%s (%s%%)", FinanceiroUtils.formatarParaExibicao(comissao),
                    tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            viewModel.comissaoEstimadaProperty().set(BigDecimal.ZERO);
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-default;");
        }
    }

    public void setOnPropostaSalva(Runnable callback) {
        this.onPropostaSalva = callback;
    }

    public void setOnPropostaFechada(Runnable callback) {
        this.onPropostaFechada = callback;
    }

    public void setOnPropostaRemovida(Runnable callback) {
        this.onPropostaRemovida = callback;
    }

    private void mostrarFeedback(String icone, String titulo, String msg, Runnable callback) {
        if (esteiraController != null)
            esteiraController.mostrarFeedback(icone, titulo, msg, callback);
    }

    @FXML
    public void handleSalvar() {
        if (!viewModel.isValido()) {
            mostrarFeedback("⚠️", "Atenção", "Preencha o Cliente, Banco, Tabela e Convênio para salvar.", null);
            return;
        }

        Task<PropostaModel> task = new Task<>() {
            @Override
            protected PropostaModel call() {
                PropostaModel base = new PropostaModel();
                if (viewModel.idProperty().get() != null) {
                    base = propostaService.carregarPropostaDetalhada(viewModel.idProperty().get());
                    if (base == null)
                        base = new PropostaModel();
                }
                PropostaModel salva = propostaService.salvarProposta(viewModel.atualizarModel(base));
                return propostaService.carregarPropostaDetalhada(salva.getId());
            }
        };

        task.setOnSucceeded(e -> {
            carregarProposta(task.getValue());
            mostrarFeedback("✅", "Sucesso!", "Proposta salva com sucesso.", () -> {
                if (onPropostaSalva != null)
                    onPropostaSalva.run();
            });
        });

        task.setOnFailed(e -> mostrarFeedback("❌", "Erro",
                "Não foi possível salvar: " + task.getException().getMessage(), null));
        new Thread(task).start();
    }

    @FXML
    public void handleFechar() {
        if (viewModel.isDirty() && esteiraController != null) {
            esteiraController.solicitarConfirmacao("⚠️ Descartar alterações?",
                    "Existem alterações não salvas. Deseja realmente fechar a proposta?", "Descartar", "#c62828",
                    this::confirmarFechar, null);
        } else
            confirmarFechar();
    }

    private void confirmarFechar() {
        if (onPropostaFechada != null)
            onPropostaFechada.run();
    }

    @FXML
    public void handleRemover() {
        if (viewModel.idProperty().get() == null) {
            confirmarFechar();
            return;
        }
        if (esteiraController != null) {
            esteiraController.solicitarConfirmacao("⚠️ Confirmar Exclusão",
                    "Tem certeza que deseja excluir permanentemente esta proposta?", "Sim, Excluir", "#c62828",
                    this::confirmarRemover, null);
        }
    }

    private void confirmarRemover() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                propostaService.excluirProposta(viewModel.idProperty().get());
                return null;
            }
        };
        task.setOnSucceeded(e -> mostrarFeedback("🗑️", "Removida", "Proposta excluída permanentemente.", () -> {
            if (onPropostaRemovida != null)
                onPropostaRemovida.run();
        }));
        task.setOnFailed(e -> mostrarFeedback("❌", "Erro", "Não foi possível remover.", null));
        new Thread(task).start();
    }

    private void carregarDocumentosDaProposta(Long propostaId) {
        if (propostaId == null) {
            listaDocumentos.clear();
            return;
        }
        Task<List<DocumentoProponenteModel>> task = new Task<>() {
            @Override
            protected List<DocumentoProponenteModel> call() {
                return documentoService.buscarPorProposta(propostaId);
            }
        };
        task.setOnSucceeded(e -> {
            listaDocumentos.setAll(task.getValue());
            tableDocumentos.refresh();
        });
        task.setOnFailed(e -> mostrarFeedback("❌", "Erro", "Erro ao carregar documentos da proposta.", null));
        new Thread(task).start();
    }

    @FXML
    private void handleAnexarDocumento() {
        if (viewModel.idProperty().get() == null) {
            mostrarFeedback("⚠️", "Atenção",
                    "Salve a proposta primeiro para gerar um código antes de anexar documentos.", null);
            return;
        }
        Optional<String> tipo = new ChoiceDialog<>("RG",
                List.of("RG", "CPF", "CNH", "Contracheque", "Comprovante de Residência", "Extrato Bancário", "Holerite", "Hiscon", "Outros"))
                .showAndWait();
        tipo.ifPresent(t -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Selecionar Arquivo: " + t);
            fc.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
            File arquivo = fc.showOpenDialog(tableDocumentos.getScene().getWindow());
            if (arquivo != null && arquivo.exists()) {
                Task<DocumentoProponenteModel> upload = new Task<>() {
                    @Override
                    protected DocumentoProponenteModel call() throws Exception {
                        PropostaModel p = viewModel.atualizarModel(new PropostaModel());
                        return documentoService.processarUpload(arquivo, t, p.getProponente(), p);
                    }
                };
                upload.setOnSucceeded(e -> carregarDocumentosDaProposta(viewModel.idProperty().get()));
                upload.setOnFailed(e -> mostrarFeedback("❌", "Erro",
                        "Falha ao anexar documento: " + upload.getException().getMessage(), null));
                new Thread(upload).start();
            }
        });
    }

    private void configurarTabelaDocumentos() {
        tableDocumentos.setItems(listaDocumentos);

        // 📐 Largura ideal para acomodar confortavelmente os 3 botões agora
        colAcoes.setPrefWidth(200);

        colTipoDocumento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoDocumento()));
        colDataUpload.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDataUpload() != null ? d.getValue().getDataUpload().format(DATE_FORMATTER) : "-"));

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️");
            private final Button btnAnalisarIA = new Button("🤖"); // 🚀 O NOVO PROTAGONISTA
            private final Button btnExcluir = new Button("🗑️");
            private final HBox container = new HBox(6, btnAbrir, btnAnalisarIA, btnExcluir);
            {
                btnAbrir.getStyleClass().add("flat");
                btnAnalisarIA.getStyleClass().addAll("flat", "accent"); // Destaca com a cor de sotaque
                btnExcluir.getStyleClass().addAll("flat", "danger");
                container.setAlignment(Pos.CENTER);

                // Tooltips para dar um toque profissional ao passar o mouse
                btnAbrir.setTooltip(new Tooltip("Visualizar Documento"));
                btnAnalisarIA.setTooltip(new Tooltip("Análise Cognitiva com Gemini IA"));
                btnExcluir.setTooltip(new Tooltip("Excluir Documento"));

                btnAbrir.setOnAction(e -> abrirDocumentoFisico(getTableView().getItems().get(getIndex())));
                btnAnalisarIA.setOnAction(e -> analisarDocumentoComIA(getTableView().getItems().get(getIndex())));
                btnExcluir.setOnAction(e -> exibirOverlayExclusao(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : container);

                // 🔒 Trava de segurança: oculta botões de mutação se a proposta estiver
                // finalizada
                if (!empty) {
                    boolean terminal = viewModel.idProperty().get() != null &&
                            (viewModel.statusProperty().get() == StatusPropostaModel.PAGO ||
                                    viewModel.statusProperty().get() == StatusPropostaModel.REPROVADA ||
                                    viewModel.statusProperty().get() == StatusPropostaModel.CANCELADO);
                    btnExcluir.setVisible(!terminal);
                    btnExcluir.setManaged(!terminal);
                    // Mantemos o btnAnalisarIA visível mesmo se terminal, pois ler dados antigos é
                    // útil!
                }
            }
        });
    }

    // =========================================================================
    // 🧠 MOTOR DE INTELIGÊNCIA COGNITIVA: TRIAGEM E AUDITORIA VIA GEMINI
    // =========================================================================
    private void analisarDocumentoComIA(DocumentoProponenteModel doc) {
        if (doc == null || doc.getArquivoPath() == null) {
            mostrarFeedback("⚠️", "Erro", "Dados do documento inválidos.", null);
            return;
        }

        File arquivoFisico = new File(doc.getArquivoPath());
        if (!arquivoFisico.exists()) {
            mostrarFeedback("❌", "Arquivo Físico Ausente", "O arquivo físico não foi encontrado no servidor local.",
                    null);
            return;
        }

        String tipoDoc = doc.getTipoDocumento() != null ? doc.getTipoDocumento().toUpperCase() : "OUTROS";

        // Context-Aware Prompting: Engenharia de prompt dinâmica conforme o tipo do
        // documento
        String promptEspecialista;
        String tituloAnalise;
        String iconeAnalise;

        if (tipoDoc.contains("RG") || tipoDoc.contains("CPF") || tipoDoc.contains("CNH")) {
            iconeAnalise = "🔍";
            tituloAnalise = "Triagem Visual de Identificação";
            promptEspecialista = """
                    Você é um Inspetor de Compliance e Segurança Documental Bancária do Poder Financeiro.
                    Analise estritamente os aspectos visuais, de nitidez e enquadramento deste documento de identificação em anexo.

                    Critérios obrigatórios para aceitação na esteira de crédito do banco:
                    1. Enquadramento e Corte: O documento está inteiro na foto ou faltam bordas, textos ou assinaturas?
                    2. Nitidez e Legibilidade: Há algum desfoque, trepidação ou pixelização que impeça ler os dados?
                    3. Reflexos: Há clarões de flash ou luz artificial em cima de dados críticos (CPF, nome, foto ou data de emissão)?
                    4. Tempo de Emissão: Se for RG, avalie visualmente se aparenta ter mais de 10 anos de emissão, alertando sobre risco de recusa.

                    Retorne um relatório scannável em Bootstrap 5/HTML (envie direto as tags HTML como <p>, <br>, <strong> e tabelas, sem envolver o bloco em blocos de código com crases). Comece com uma badge elegante de status: <span class='badge bg-success'>RECOMENDADO</span> ou <span class='badge bg-warning'>RASCUNHO COM RESTRIÇÕES</span> ou <span class='badge bg-danger'>REPROVADO NA CONFERÊNCIA</span>. Seja direto, prático e profissional.
                    """;
        } else if (tipoDoc.contains("CONTRACHEQUE") || tipoDoc.contains("EXTRATO BANCARIO") || tipoDoc.contains("HOLERITE")
                || tipoDoc.contains("HISCON")) {
            iconeAnalise = "📊";
            tituloAnalise = "Auditoria de Margem Consignável";
            promptEspecialista = """
                    Você é um Analista de Crédito Consignado Sênior do Poder Financeiro.
                    Sua missão é extrair a verdade financeira deste holerite / contracheque em anexo.

                    Execute os seguintes passos e monte o relatório matemático:
                    1. Identifique a Renda Bruta e os Descontos Obrigatórios (Previdência, Imposto de Renda).
                    2. Localize empréstimos ativos já descontados diretamente em folha de pagamento.
                    3. Calcule ou localize a MARGEM CONSIGNÁVEL disponível para novos empréstimos (normalmente 30% a 35% da base regulamentar líquida).
                    4. Alerte sobre rasuras, competência antiga (meses atrás) ou anotações suspeitas.

                    Retorne um resumo executivo formatado com tabelas do Bootstrap 5 (sem incluir blocos de código com crases) detalhando: Renda Bruta, Descontos, Margem Utilizada e Margem Livre Estimada para novos contratos. Use <strong> para destacar todos os valores monetários.
                    """;
        } else {
            iconeAnalise = "🤖";
            tituloAnalise = "Análise Documental Geral";
            promptEspecialista = """
                    Você é um Assistente Analítico do Poder Financeiro.
                    Analise o documento em anexo, identifique o seu propósito principal (ex: se for comprovante de residência, verifique a data de emissão recente e se está no nome do cliente ativo) e valide se a foto está nítida e elegível para ser submetida a uma esteira de crédito bancário tradicional.
                    """;
        }

        // Exibe o carregamento global animado na tela do operador
        if (mainController != null) {
            mainController.mostrarLoading("O Gemini está atuando como especialista em " + tituloAnalise + "...");
        }

        // Executa a requisição assíncrona para não congelar a interface (UI Thread) do
        // JavaFX
        Task<String> taskAnaliseDoc = new Task<>() {
            @Override
            protected String call() throws Exception {
                String token = (authService.getUsuarioLogado() != null)
                        ? authService.getUsuarioLogado().getGeminiApiKey()
                        : null;

                // Fornece dados adicionais do proponente para o Gemini validar se a foto é
                // mesmo deste cliente
                var proponente = viewModel.proponenteProperty().get();
                String jsonCliente = br.com.poderfinanceiro.app.utils.SummaryGeneratorUtils
                        .gerarJsonContextualParaIA(proponente, true);

                return geminiService.perguntarAoAssistente(promptEspecialista, token, arquivoFisico, jsonCliente, "[]",
                        "[]", "[]");
            }
        };

        taskAnaliseDoc.setOnSucceeded(e -> {
            if (mainController != null)
                mainController.ocultarLoading();
            // Entrega o diagnóstico polido com Bootstrap 5 diretamente no overlay universal
            // do pai!
            mostrarFeedback(iconeAnalise, tituloAnalise, taskAnaliseDoc.getValue(), null);
        });

        taskAnaliseDoc.setOnFailed(e -> {
            if (mainController != null)
                mainController.ocultarLoading();
            taskAnaliseDoc.getException().printStackTrace();
            mostrarFeedback("❌", "Falha na Análise",
                    "O motor de IA falhou ao analisar este arquivo: " + taskAnaliseDoc.getException().getMessage(),
                    null);
        });

        new Thread(taskAnaliseDoc).start();
    }

    private void abrirDocumentoFisico(DocumentoProponenteModel doc) {
        if (doc != null && doc.getArquivoPath() != null) {
            File f = new File(doc.getArquivoPath());
            if (f.exists())
                mainController.getHostServices().showDocument(f.toURI().toString());
            else
                mostrarFeedback("⚠️", "Aviso", "Arquivo físico não encontrado no servidor.", null);
        }
    }

    private void exibirOverlayExclusao(DocumentoProponenteModel doc) {
        if (esteiraController != null) {
            esteiraController.solicitarConfirmacao("🗑️ Excluir Documento",
                    "Deseja remover '" + doc.getTipoDocumento() + "'?", "Sim, Excluir", "#c62828",
                    () -> confirmarExclusao(doc), null);
        }
    }

    private void confirmarExclusao(DocumentoProponenteModel doc) {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                documentoService.excluirDocumento(doc.getId());
                return null;
            }
        }).start();
        listaDocumentos.remove(doc);
    }

    // =========================================================================
    // 8. UTILITÁRIOS E UI TRAVAS
    // =========================================================================

    public void aplicarBloqueio() {
        boolean terminal = viewModel.idProperty().get() != null &&
                (viewModel.statusProperty().get() == StatusPropostaModel.PAGO ||
                        viewModel.statusProperty().get() == StatusPropostaModel.REPROVADA ||
                        viewModel.statusProperty().get() == StatusPropostaModel.CANCELADO);

        cbStatus.setDisable(terminal);
        cbTabela.setDisable(terminal);
        cbBanco.setDisable(terminal);
        cbConvenio.setDisable(terminal);
        txtValorSolicitado.setDisable(terminal);
        txtValorAprovado.setDisable(terminal);
        txtParcela.setDisable(terminal);
        spinPrazoDesejado.setDisable(terminal);
        spinPrazo.setDisable(terminal);
        txtObservacoes.setPromptText(terminal ? "Proposta liquidada. Alterações não permitidas." : "");

        // 🚀 Bloqueio dos botões principais
        if (btnSalvar != null)
            btnSalvar.setDisable(terminal);
        if (btnRemover != null)
            btnRemover.setDisable(terminal);
    }

    private void configurarIndicadoresDinamicos() {
        lblTituloComissao.textProperty().bind(Bindings.createStringBinding(() -> {
            BigDecimal aprovado = viewModel.valorAprovadoProperty().get();
            return (aprovado != null && aprovado.compareTo(BigDecimal.ZERO) > 0) ? "Valor da Comissão"
                    : "Comissão Estimada";
        }, viewModel.valorAprovadoProperty()));

        lblTotalPago.textProperty().bind(Bindings.createStringBinding(() -> {
            Integer prazo = viewModel.quantidadeParcelasProperty().get();
            BigDecimal parcela = viewModel.valorParcelaProperty().get();
            if (prazo != null && prazo > 0 && parcela != null && parcela.compareTo(BigDecimal.ZERO) > 0) {
                return "Total a Pagar: "
                        + FinanceiroUtils.formatarParaExibicao(parcela.multiply(BigDecimal.valueOf(prazo)));
            }
            return "Total a Pagar: R$ 0,00";
        }, viewModel.quantidadeParcelasProperty(), viewModel.valorParcelaProperty()));
    }

    private void configurarTravasEAlertas() {
        if (btnAnexarDocumento != null) {

            // 🚀 Criação de uma inteligência reativa para verificar se o status é terminal
            BooleanBinding isTerminal = Bindings.createBooleanBinding(() -> {
                StatusPropostaModel status = viewModel.statusProperty().get();
                return status == StatusPropostaModel.PAGO ||
                        status == StatusPropostaModel.REPROVADA ||
                        status == StatusPropostaModel.CANCELADO;
            }, viewModel.statusProperty());

            // O botão de anexar só ficará desativado se não tiver ID gerado OU se o status
            // for terminal
            btnAnexarDocumento.disableProperty().bind(viewModel.idProperty().isNull().or(isTerminal));

            Tooltip tooltip = new Tooltip();
            tooltip.textProperty().bind(Bindings.when(viewModel.idProperty().isNull())
                    .then("⚠️ Salve a proposta primeiro para gerar um código e habilitar o envio de anexos.")
                    .otherwise(Bindings.when(isTerminal)
                            .then("🔒 Proposta liquidada. Não é possível adicionar novos documentos.")
                            .otherwise("Clique para anexar um novo documento a esta proposta.")));

            btnAnexarDocumento.setTooltip(tooltip);
        }

        addBorderStyleListener(cbBanco);
        addBorderStyleListener(cbTabela);
    }

    private void addBorderStyleListener(ComboBox<?> combo) {
        combo.valueProperty().addListener((obs, old, val) -> combo.setStyle(
                val == null ? "-fx-border-color: #ffb74d; -fx-border-width: 1px; -fx-border-radius: 4px;" : ""));
    }

    private void executarSemGatilhos(Runnable acao) {
        isUpdatingInterface = true;
        try {
            acao.run();
        } finally {
            isUpdatingInterface = false;
        }
    }

    private <T> StringConverter<T> criarConversor(Function<T, String> formatter) {
        return new StringConverter<>() {
            @Override
            public String toString(T obj) {
                return formatter.apply(obj);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    private void vincularMascaraMoeda(TextField campo, javafx.beans.property.Property<BigDecimal> prop) {
        TextFormatter<BigDecimal> fmt = FinanceiroUtils.criarFormatadorMoeda();
        campo.setTextFormatter(fmt);
        fmt.valueProperty().bindBidirectional(prop);
    }

    private void configurarSpinner(Spinner<Integer> spinner, javafx.beans.property.Property<Integer> prop) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 1));
        spinner.setEditable(true);
        spinner.getEditor().focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                try {
                    spinner.getValueFactory().setValue(
                            spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText()));
                } catch (Exception e) {
                    spinner.getEditor().setText(spinner.getValueFactory().getConverter().toString(spinner.getValue()));
                }
            }
        });
        spinner.getValueFactory().valueProperty().bindBidirectional(prop);
    }

    private void configurarAutoSelecaoTextos() {
        for (TextField campo : new TextField[] { txtValorSolicitado, txtValorAprovado, txtParcela })
            campo.focusedProperty().addListener((obs, old, focused) -> {
                if (focused)
                    Platform.runLater(campo::selectAll);
            });
    }

    private int calcularIdade(ProponenteModel p) {
        return (p != null && p.getDataNascimento() != null)
                ? Period.between(p.getDataNascimento(), LocalDate.now()).getYears()
                : 0;
    }

    public PropostaViewModel getViewModel() {
        return viewModel;
    }
}