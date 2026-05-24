package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.service.*;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
@Scope("prototype")
public class PropostaController {

    // =========================================================================
    // CONSTANTES
    // =========================================================================
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MODELO_IA_FALLBACK = "gemini-3.5-flash";

    private static final ObservableList<String> TIPOS_DOCUMENTO_PADRAO = FXCollections.observableArrayList(
            "RG Frente", "RG Verso", "CNH Frente", "CNH Verso", "CPF",
            "Comprovante Residencia", "Contracheque", "Holerite",
            "Extrato Hiscon", "Extrato Bancario", "Outros");

    private static final String PROMPT_IDENTIFICACAO = """
            Você é um Inspetor de Compliance e Segurança Documental Bancária do Poder Financeiro.
            Analise estritamente os aspectos visuais, de nitidez e enquadramento deste documento de identificação em anexo.

            Critérios obrigatórios para aceitação na esteira de crédito do banco:
            1. Enquadramento e Corte: O documento está inteiro na foto ou faltam bordas, textos ou assinaturas?
            2. Nitidez e Legibilidade: Há algum desfoque, trepidação ou pixelização que impeça ler os dados?
            3. Reflexos: Há clarões de flash ou luz artificial em cima de dados críticos (CPF, nome, foto ou data de emissão)?
            4. Tempo de Emissão: Se for RG, avalie visualmente se aparenta ter mais de 10 anos de emissão, alertando sobre risco de recusa.

            Retorne um relatório scannável em Bootstrap 5/HTML (envie direto as tags HTML como <p>, <br>, <strong> e tabelas, sem envolver o bloco em blocos de código com crases). Comece com uma badge elegante de status: <span class='badge bg-success'>RECOMENDADO</span> ou <span class='badge bg-warning'>RASCUNHO COM RESTRIÇÕES</span> ou <span class='badge bg-danger'>REPROVADO NA CONFERÊNCIA</span>. Seja direto, prático e profissional.
            """;

    private static final String PROMPT_FINANCEIRO = """
            Você é um Analista de Crédito Consignado Sênior do Poder Financeiro.
            Sua missão é extrair a verdade financeira deste holerite / contracheque em anexo.

            Execute os seguintes passos e monte o relatório matemático:
            1. Identifique a Renda Bruta e os Descontos Obrigatórios (Previdência, Imposto de Renda).
            2. Localize empréstimos ativos já descontados diretamente em folha de pagamento.
            3. Calcule ou localize a MARGEM CONSIGNÁVEL disponível para novos empréstimos (normalmente 30% a 35% da base regulamentar líquida).
            4. Alerte sobre rasuras, competência antiga (meses atrás) ou anotações suspeitas.

            Retorne um resumo executivo formatado com tabelas do Bootstrap 5 (sem incluir blocos de código com crases) detalhando: Renda Bruta, Descontos, Margem Utilizada e Margem Livre Estimada para novos contratos. Use <strong> para destacar todos os valores monetários.
            """;

    private static final String PROMPT_GERAL = """
            Você é um Assistente Analítico do Poder Financeiro.
            Analise o documento em anexo, identifique o seu propósito principal (ex: se for comprovante de residência, verifique a data de emissão recente e se está no nome do cliente ativo) e valide se a foto está nítida e elegível para ser submetida a uma esteira de crédito bancário tradicional.
            """;

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final PropostaViewModel viewModel;
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;
    private final DocumentoService documentoService;
    private final MainController mainController;
    private final ProponenteService proponenteService;
    private final AtendimentoContextService contextoService;
    private final AuthService authService;
    private final GeminiService geminiService;

    // =========================================================================
    // COMPONENTES DE UI (FXML)
    // =========================================================================
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

    @FXML
    private Button btnAnexarDocumento;
    @FXML
    private Button btnSalvar;
    @FXML
    private Button btnRemover;
    @FXML
    private ComboBox<String> cmbModeloIA;

    @FXML
    private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colTipoDocumento, colDataUpload;
    @FXML
    private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    @FXML
    private VBox overlayDocumento, boxSelecaoArquivo;
    @FXML
    private Label lblOverlayDocTitulo, lblArquivoSelecionado;
    @FXML
    private ComboBox<String> cbTipoDocumentoOverlay;
    @FXML
    private Button btnSalvarDocumento;

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private File arquivoSelecionadoParaUpload;
    private DocumentoProponenteModel documentoSendoEditado;
    private Runnable onPropostaSalva;
    private Runnable onPropostaFechada;
    private Runnable onPropostaRemovida;

    private List<TabelaJurosModel> todasTabelasAtivas;
    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();
    private boolean isUpdatingInterface = false;
    private boolean modelosCarregados = false;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
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

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        carregarListasBase();
        configurarConversoresVisuais();
        configurarBindings();
        configurarCalculosAutomaticos(); // Renomeado, pois não faz mais triagem
        configurarTabelaDocumentos();
        configurarAutoSelecaoTextos();
        configurarIndicadoresDinamicos();
        configurarTravasEAlertas();
        inicializarComboBoxIA();
        configurarOverlayDocumentos();
    }

    private void carregarListasBase() {
        todasTabelasAtivas = tabelaJurosService.listarAtivas();

        // Pega apenas os Bancos únicos que possuem tabelas ativas
        List<BancoModel> bancosAtivos = todasTabelasAtivas.stream()
                .map(TabelaJurosModel::getBanco)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        executarSemGatilhos(() -> {
            cbBanco.setItems(FXCollections.observableArrayList(bancosAtivos));
        });

        cbStatus.setItems(FXCollections.observableArrayList(StatusPropostaModel.values()));
        if (cbConvenio != null)
            cbConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        if (cbCliente != null)
            cbCliente.setItems(FXCollections.observableArrayList(proponenteService.listarMinhaCarteira()));
    }

    private void configurarConversoresVisuais() {
        if (cbConvenio != null)
            cbConvenio.setConverter(criarConversor(TipoConvenioModel::getLabel));
        cbBanco.setConverter(criarConversor(b -> b != null ? b.getNome() : "Selecione o Banco..."));
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

    private void configurarOverlayDocumentos() {
        if (cbTipoDocumentoOverlay == null)
            return;
        cbTipoDocumentoOverlay.setItems(TIPOS_DOCUMENTO_PADRAO);
        cbTipoDocumentoOverlay.valueProperty().addListener((obs, old, val) -> validarFormularioDocumento());
        cbTipoDocumentoOverlay.getEditor().textProperty().addListener((obs, old, val) -> validarFormularioDocumento());
    }

    // =========================================================================
    // CORE FLUXO DA PROPOSTA (CARREGAR, SALVAR, EXCLUIR)
    // =========================================================================
    // =========================================================================
    // CORE FLUXO DA PROPOSTA (CARREGAR, SALVAR, EXCLUIR)
    // =========================================================================
    public void carregarProposta(PropostaModel completa) {
        executarSemGatilhos(() -> {
            viewModel.loadFromModel(completa);
            sincronizarComboBoxesComViewModel();
            cbCliente.setDisable(completa.getId() != null);

            if (completa.getId() != null) {
                carregarDocumentosDaProposta(completa.getId());
            }

            // 🚀 CORREÇÃO: Fora do 'if' para limpar o estado de desabilitação em novas
            // propostas
            aplicarBloqueio();
        });

        if (contextoService != null) {
            contextoService.setPropostaAtiva(completa);
            contextoService.setTelaAtualFocada(AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS);
        }
    }

    @FXML
    public void handleSalvar() {
        if (!viewModel.isValido()) {
            mostrarFeedback("⚠️", "Atenção", "Preencha o Cliente, Banco, Tabela e Convênio para salvar.", null);
            return;
        }

        AsyncUtils.executarTaskAsync(
                this::executarSalvamentoBackground,
                salva -> {
                    carregarProposta(salva);
                    mostrarFeedback("✅", "Sucesso!", "Proposta salva com sucesso.", () -> {
                        if (onPropostaSalva != null)
                            onPropostaSalva.run();
                    });
                },
                erro -> mostrarFeedback("❌", "Erro", "Não foi possível salvar: " + erro.getMessage(), null));
    }

    private PropostaModel executarSalvamentoBackground() {
        PropostaModel base = new PropostaModel();
        if (viewModel.idProperty().get() != null) {
            base = propostaService.carregarPropostaDetalhada(viewModel.idProperty().get());
            if (base == null)
                base = new PropostaModel();
        }
        PropostaModel salva = propostaService.salvarProposta(viewModel.atualizarModel(base));
        return propostaService.carregarPropostaDetalhada(salva.getId());
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
        AsyncUtils.executarTaskAsync(
                () -> {
                    propostaService.excluirProposta(viewModel.idProperty().get());
                    return null;
                },
                sucesso -> {
                    if (onPropostaRemovida != null)
                        onPropostaRemovida.run();
                },
                erro -> mostrarFeedback("❌", "Erro", "Não foi possível remover: " + erro.getMessage(), null));
    }

    @FXML
    public void handleFechar() {
        if (viewModel.isDirty() && esteiraController != null) {
            esteiraController.solicitarConfirmacao("⚠️ Descartar alterações?",
                    "Existem alterações não salvas. Deseja realmente fechar a proposta?", "Descartar", "#c62828",
                    this::confirmarFechar, null);
        } else {
            confirmarFechar();
        }
    }

    private void confirmarFechar() {
        if (onPropostaFechada != null)
            onPropostaFechada.run();
    }

    // =========================================================================
    // LÓGICA DE NEGÓCIO E CÁLCULOS
    // =========================================================================
    private void configurarCalculosAutomaticos() {
        // Ao alterar valor aprovado ou solicitado, refaz a conta de comissão
        viewModel.valorAprovadoProperty().addListener((obs, old, val) -> calcularComissao());
        viewModel.valorSolicitadoProperty().addListener((obs, old, val) -> calcularComissao());

        // Quando o usuário escolhe um Banco, filtra as tabelas
        viewModel.bancoProperty().addListener((obs, old, banco) -> atualizarTabelasDoBanco((BancoModel) banco));

        // Quando o usuário escolhe a Tabela, atualiza a propriedade ID e calcula
        cbTabela.valueProperty().addListener((obs, old, nova) -> {
            if (!isUpdatingInterface) {
                viewModel.tabelaIdProperty().set(nova != null ? nova.getId() : null);
                calcularComissao();
            }
        });

        // Quando o ID muda via ViewModel, reflete visualmente
        viewModel.tabelaIdProperty().addListener((obs, old, id) -> {
            if (!isUpdatingInterface)
                carregarDadosDaTabela(id);
        });
    }

    private void calcularComissao() {
        BigDecimal base = viewModel.valorAprovadoProperty().get();
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            base = viewModel.valorSolicitadoProperty().get();
        }

        TabelaJurosModel tabela = cbTabela.getValue();

        if (base != null && base.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            BigDecimal comissao = propostaService.calcularComissaoEstimada(base, tabela.getId());
            viewModel.comissaoEstimadaProperty().set(comissao);
            lblComissaoEstimada.setText(String.format("%s (%s%%)", FinanceiroUtils.formatarParaExibicao(comissao),
                    tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: #1b5e20; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            viewModel.comissaoEstimadaProperty().set(BigDecimal.ZERO);
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    private void atualizarTabelasDoBanco(BancoModel banco) {
        if (isUpdatingInterface)
            return;

        if (banco != null) {
            // Filtra da lista mestra apenas as tabelas do banco selecionado
            List<TabelaJurosModel> tabelas = todasTabelasAtivas.stream()
                    .filter(t -> t.getBanco() != null && t.getBanco().getId().equals(banco.getId()))
                    .sorted(Comparator.comparing(TabelaJurosModel::getComissaoPercentual).reversed())
                    .toList();

            TabelaJurosModel atual = cbTabela.getValue();
            executarSemGatilhos(() -> {
                cbTabela.setItems(FXCollections.observableArrayList(tabelas));
                if (atual != null) {
                    tabelas.stream()
                            .filter(t -> t.getId().equals(atual.getId()))
                            .findFirst()
                            .ifPresentOrElse(cbTabela::setValue, () -> cbTabela.setValue(null));
                } else {
                    cbTabela.setValue(null);
                }
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
                TabelaJurosModel tab = todasTabelasAtivas.stream()
                        .filter(t -> t.getId().equals(idTabela))
                        .findFirst()
                        .orElse(null);

                if (tab != null) {
                    cbConvenio.setValue(tab.getTipoConvenio());
                    viewModel.convenioProperty().set(tab.getTipoConvenio());

                    cbBanco.setValue(tab.getBanco());
                    viewModel.bancoProperty().set(tab.getBanco());

                    atualizarTabelasDoBanco(tab.getBanco());

                    cbTabela.setValue(tab);
                    viewModel.tabelaIdProperty().set(tab.getId());
                }
            } else {
                cbTabela.setValue(null);
            }
            calcularComissao();
        });
    }

    private void sincronizarComboBoxesComViewModel() {
        sincronizarComboModel(cbCliente, viewModel.proponenteProperty().get(), ProponenteModel::getId);
        cbStatus.setValue(viewModel.statusProperty().get());
        cbConvenio.setValue(viewModel.convenioProperty().get());

        BancoModel b = viewModel.bancoProperty().get();
        sincronizarComboModel(cbBanco, b, BancoModel::getId);

        Long idTabela = viewModel.tabelaIdProperty().get();
        if (idTabela != null) {
            TabelaJurosModel tab = todasTabelasAtivas.stream().filter(t -> t.getId().equals(idTabela)).findFirst()
                    .orElse(null);

            // Garante que a lista de tabelas está populada pro banco correto
            if (b != null)
                atualizarTabelasDoBanco(b);

            sincronizarComboModel(cbTabela, tab, TabelaJurosModel::getId);
        } else {
            cbTabela.getSelectionModel().clearSelection();
        }
        calcularComissao();
    }

    // =========================================================================
    // GESTÃO DE DOCUMENTOS (OVERLAY E TABELA)
    // =========================================================================
    private void configurarTabelaDocumentos() {
        tableDocumentos.setItems(listaDocumentos);
        colAcoes.setPrefWidth(240);
        colTipoDocumento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoDocumento()));
        colDataUpload.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDataUpload() != null ? d.getValue().getDataUpload().format(DATE_FORMATTER) : "-"));
        colAcoes.setCellFactory(param -> new BotoesAcaoDocumentoCell());
    }

    private void carregarDocumentosDaProposta(Long propostaId) {
        if (propostaId == null) {
            listaDocumentos.clear();
            return;
        }
        AsyncUtils.executarTaskAsync(
                () -> documentoService.buscarPorProposta(propostaId),
                docs -> {
                    listaDocumentos.setAll(docs);
                    tableDocumentos.refresh();
                },
                erro -> mostrarFeedback("❌", "Erro", "Erro ao carregar documentos.", null));
    }

    @FXML
    private void handleAnexarDocumento() {
        if (viewModel.idProperty().get() == null) {
            mostrarFeedback("⚠️", "Atenção",
                    "Salve a proposta primeiro para gerar um código antes de anexar documentos.", null);
            return;
        }
        prepararOverlayDocumento(null, "➕ Anexar Novo Documento");
    }

    private void abrirEdicaoDocumento(DocumentoProponenteModel doc) {
        prepararOverlayDocumento(doc, "✏️ Renomear Documento");
    }

    private void prepararOverlayDocumento(DocumentoProponenteModel doc, String titulo) {
        documentoSendoEditado = doc;
        arquivoSelecionadoParaUpload = null;
        lblOverlayDocTitulo.setText(titulo);
        cbTipoDocumentoOverlay.setValue(doc != null ? doc.getTipoDocumento() : null);
        cbTipoDocumentoOverlay.getEditor().setText(doc != null ? doc.getTipoDocumento() : "");
        boxSelecaoArquivo.setVisible(doc == null);
        boxSelecaoArquivo.setManaged(doc == null);
        lblArquivoSelecionado.setText("Nenhum arquivo selecionado");
        validarFormularioDocumento();
        overlayDocumento.setVisible(true);
    }

    @FXML
    private void handleProcurarArquivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Arquivo Físico");
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(overlayDocumento.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            arquivoSelecionadoParaUpload = arquivo;
            lblArquivoSelecionado.setText(arquivo.getName());
            validarFormularioDocumento();
        }
    }

    private void validarFormularioDocumento() {
        String texto = cbTipoDocumentoOverlay.getEditor().getText();
        boolean temTexto = texto != null && !texto.trim().isEmpty();
        btnSalvarDocumento.setDisable(
                documentoSendoEditado != null ? !temTexto : (!temTexto || arquivoSelecionadoParaUpload == null));
    }

    @FXML
    private void fecharOverlayDocumento() {
        overlayDocumento.setVisible(false);
        documentoSendoEditado = null;
        arquivoSelecionadoParaUpload = null;
    }

    @FXML
    public void confirmarSalvarDocumento() {
        String nomePersonalizado = cbTipoDocumentoOverlay.getEditor().getText().trim();
        btnSalvarDocumento.setDisable(true);

        AsyncUtils.executarTaskAsync(
                () -> {
                    if (documentoSendoEditado != null) {
                        return documentoService.atualizarTipoDocumento(documentoSendoEditado.getId(),
                                nomePersonalizado);
                    } else {
                        PropostaModel p = viewModel.atualizarModel(new PropostaModel());
                        return documentoService.processarUpload(arquivoSelecionadoParaUpload, nomePersonalizado,
                                p.getProponente(), p);
                    }
                },
                sucesso -> {
                    fecharOverlayDocumento();
                    carregarDocumentosDaProposta(viewModel.idProperty().get());
                },
                erro -> {
                    btnSalvarDocumento.setDisable(false);
                    mostrarFeedback("❌", "Erro de Documento", erro.getMessage(), null);
                });
    }

    // 🚀 PATCH: Refatoração para usar o AsyncUtils centralizado
    private void exibirOverlayExclusao(DocumentoProponenteModel doc) {
        if (esteiraController != null) {
            esteiraController.solicitarConfirmacao("🗑️ Excluir Documento",
                    "Deseja remover '" + doc.getTipoDocumento() + "'?", "Sim, Excluir", "#c62828",
                    () -> {
                        AsyncUtils.executarTaskAsync(
                                () -> {
                                    documentoService.excluirDocumento(doc.getId());
                                    return null;
                                },
                                sucesso -> listaDocumentos.remove(doc),
                                null);
                    }, null);
        }
    }

    private void abrirDocumentoFisico(DocumentoProponenteModel doc) {
        if (doc == null || doc.getArquivoPath() == null)
            return;
        File f = new File(doc.getArquivoPath());

        if (f.exists()) {
            Platform.runLater(
                    () -> mainController.getHostServices().showDocument(f.getAbsoluteFile().toURI().toString()));
        } else {
            mostrarFeedback("⚠️", "Aviso", "Arquivo físico não encontrado no servidor.", null);
        }
    }

    // =========================================================================
    // MOTOR IA GEMINI (ANÁLISE COGNITIVA)
    // =========================================================================
    private void inicializarComboBoxIA() {
        if (cmbModeloIA == null)
            return;
        cmbModeloIA.getItems().add(MODELO_IA_FALLBACK);
        cmbModeloIA.getSelectionModel().selectFirst();

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (!modelosCarregados && token != null && !token.isBlank()) {
            AsyncUtils.executarTaskAsync(
                    () -> geminiService.listarModelosMultimodais(token),
                    modelos -> {
                        String atual = cmbModeloIA.getValue();
                        cmbModeloIA.getItems().setAll(modelos);
                        if (modelos.contains(atual))
                            cmbModeloIA.getSelectionModel().select(atual);
                        else if (modelos.contains(MODELO_IA_FALLBACK))
                            cmbModeloIA.getSelectionModel().select(MODELO_IA_FALLBACK);
                        else
                            cmbModeloIA.getSelectionModel().selectFirst();
                        modelosCarregados = true;
                    },
                    null);
        }
    }

    private void analisarDocumentoComIA(DocumentoProponenteModel doc) {
        if (doc == null || doc.getArquivoPath() == null) {
            mostrarFeedback("⚠️", "Erro", "Dados do documento inválidos.", null);
            return;
        }

        File arquivoFisico = new File(doc.getArquivoPath());
        if (!arquivoFisico.exists()) {
            mostrarFeedback("❌", "Arquivo Ausente", "Arquivo físico não encontrado.", null);
            return;
        }

        ConfigIA config = determinarConfiguracaoIA(doc.getTipoDocumento());
        if (mainController != null)
            mainController.mostrarLoading("O Gemini está atuando como especialista em " + config.titulo() + "...");

        String modeloSelecionado = cmbModeloIA != null && cmbModeloIA.getValue() != null ? cmbModeloIA.getValue()
                : MODELO_IA_FALLBACK;
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        String jsonCliente = br.com.poderfinanceiro.app.util.SummaryGeneratorUtils
                .gerarJsonContextualParaIA(viewModel.proponenteProperty().get(), true);

        AsyncUtils.executarTaskAsync(
                () -> geminiService.perguntarAoAssistente(config.prompt(), token, modeloSelecionado, arquivoFisico,
                        jsonCliente, "[]", "[]", "[]"),
                resultado -> {
                    if (mainController != null)
                        mainController.ocultarLoading();
                    mostrarFeedback(config.icone(), config.titulo(), resultado, null);
                },
                erro -> {
                    if (mainController != null)
                        mainController.ocultarLoading();
                    erro.printStackTrace();
                    mostrarFeedback("❌", "Falha na Análise", "O motor de IA falhou: " + erro.getMessage(), null);
                });
    }

    private ConfigIA determinarConfiguracaoIA(String tipo) {
        String upper = tipo != null ? tipo.toUpperCase() : "OUTROS";
        if (upper.contains("RG") || upper.contains("CPF") || upper.contains("CNH")) {
            return new ConfigIA("🔍", "Triagem Visual de Identificação", PROMPT_IDENTIFICACAO);
        } else if (upper.contains("CONTRACHEQUE") || upper.contains("EXTRATO BANCARIO") || upper.contains("HOLERITE")
                || upper.contains("HISCON")) {
            return new ConfigIA("📊", "Auditoria de Margem Consignável", PROMPT_FINANCEIRO);
        }
        return new ConfigIA("🤖", "Análise Documental Geral", PROMPT_GERAL);
    }

    // =========================================================================
    // UTILITÁRIOS E UI
    // =========================================================================
    // 🚀 PATCH: PropostaController.java
    public void aplicarBloqueio() {
        boolean terminal = isPropostaTerminal();

        // Identifica se a proposta nasceu da IA lendo a assinatura
        String obs = viewModel.observacoesProperty().get();
        boolean bloqueadaPeloCopiloto = obs != null && obs.contains("Copiloto de Vendas");

        cbStatus.setDisable(terminal);

        // Se veio do Copiloto, trava a inteligência de negócio e a intenção original do
        // cliente
        cbTabela.setDisable(terminal || bloqueadaPeloCopiloto);
        cbBanco.setDisable(terminal || bloqueadaPeloCopiloto);
        cbConvenio.setDisable(terminal || bloqueadaPeloCopiloto);

        // 🚀 NOVO: Trava o Valor Solicitado e o Prazo Desejado para propostas da IA
        txtValorSolicitado.setDisable(terminal || bloqueadaPeloCopiloto);
        spinPrazoDesejado.setDisable(terminal || bloqueadaPeloCopiloto);

        // Campos que o consultor sempre pode editar (até a proposta ser
        // liquidada/terminal)
        txtValorAprovado.setDisable(terminal);
        txtParcela.setDisable(terminal);
        spinPrazo.setDisable(terminal);

        if (terminal) {
            txtObservacoes.setPromptText("Proposta liquidada. Alterações não permitidas.");
        }

        if (btnSalvar != null)
            btnSalvar.setDisable(terminal);
        if (btnRemover != null)
            btnRemover.setDisable(terminal);
    }

    private boolean isPropostaTerminal() {
        StatusPropostaModel status = viewModel.statusProperty().get();
        return viewModel.idProperty().get() != null &&
                (status == StatusPropostaModel.PAGO || status == StatusPropostaModel.REPROVADA
                        || status == StatusPropostaModel.CANCELADO);
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
            BooleanBinding isTerminal = Bindings.createBooleanBinding(this::isPropostaTerminal,
                    viewModel.statusProperty());
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

    private <T> void sincronizarComboModel(ComboBox<T> combo, T model, Function<T, Long> idExtractor) {
        if (model != null) {
            combo.getItems().stream()
                    .filter(c -> idExtractor.apply(c).equals(idExtractor.apply(model)))
                    .findFirst()
                    .ifPresentOrElse(combo::setValue, () -> {
                        combo.getItems().add(model);
                        combo.setValue(model);
                    });
        } else {
            combo.getSelectionModel().clearSelection();
        }
    }

    private void executarSemGatilhos(Runnable acao) {
        isUpdatingInterface = true;
        try {
            acao.run();
        } finally {
            isUpdatingInterface = false;
        }
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

    public PropostaViewModel getViewModel() {
        return viewModel;
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

    // =========================================================================
    // CLASSES E RECORDS AUXILIARES
    // =========================================================================
    private record ConfigIA(String icone, String titulo, String prompt) {
    }

    private class BotoesAcaoDocumentoCell extends TableCell<DocumentoProponenteModel, Void> {
        private final Button btnAbrir = criarBotaoAcao("👁️", "Visualizar Documento", "flat");
        private final Button btnEditar = criarBotaoAcao("✏️", "Renomear / Mudar Tipo", "flat");
        private final Button btnAnalisarIA = criarBotaoAcao("🤖", "Análise Cognitiva com Gemini IA", "flat", "accent");
        private final Button btnExcluir = criarBotaoAcao("🗑️", "Excluir Documento", "flat", "danger");
        private final HBox container = new HBox(6, btnAbrir, btnEditar, btnAnalisarIA, btnExcluir);

        public BotoesAcaoDocumentoCell() {
            container.setAlignment(Pos.CENTER);
            btnAbrir.setOnAction(e -> abrirDocumentoFisico(getDoc()));
            btnEditar.setOnAction(e -> abrirEdicaoDocumento(getDoc()));
            btnAnalisarIA.setOnAction(e -> analisarDocumentoComIA(getDoc()));
            btnExcluir.setOnAction(e -> exibirOverlayExclusao(getDoc()));
        }

        private DocumentoProponenteModel getDoc() {
            return getTableView().getItems().get(getIndex());
        }

        private Button criarBotaoAcao(String icone, String tooltipText, String... styleClasses) {
            Button btn = new Button(icone);
            btn.getStyleClass().addAll(styleClasses);
            btn.setTooltip(new Tooltip(tooltipText));
            return btn;
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getDoc() == null) {
                setGraphic(null);
            } else {
                setGraphic(container);
                boolean terminal = isPropostaTerminal();
                btnEditar.setVisible(!terminal);
                btnEditar.setManaged(!terminal);
                btnExcluir.setVisible(!terminal);
                btnExcluir.setManaged(!terminal);
            }
        }
    }
}