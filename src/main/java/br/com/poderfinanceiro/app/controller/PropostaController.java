package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.facade.IPropostaFacade;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.application.HostServices;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * <h1>PropostaController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar a tela de Propostas.
 * Implementa o padrão <b>Humble Object</b>: não contém regras de negócio,
 * delegando toda a inteligência, cálculos e persistência para a
 * {@link IPropostaFacade}.
 * </p>
 * 
 * @author Arquiteto de Software
 * @version 2.0 (Refatoração SOLID e Telemetria Avançada)
 */
@Component @Scope("prototype")
public class PropostaController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(PropostaController.class);
    private static final String LOG_PREFIX = "[PropostaController]";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MODELO_IA_FALLBACK = "gemini-3.5-flash";
    private static final ObservableList<String> TIPOS_DOCUMENTO_PADRAO = FXCollections.observableArrayList("RG Frente", "RG Verso",
            "CNH Frente", "CNH Verso", "CPF", "Comprovante Residencia", "Contracheque", "Holerite", "Extrato Hiscon", "Extrato Bancario",
            "Outros");

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP - Dependency Inversion Principle)
    // ==========================================================================================
    private final PropostaViewModel viewModel;
    private final IPropostaFacade propostaFacade;
    private final Navigator navigator;
    private final HostServices hostServices;
    private EsteiraPropostasController esteiraController;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private ComboBox<TipoConvenioModel> cbConvenio;
    @FXML private ComboBox<BancoModel> cbBanco;
    @FXML private ComboBox<TabelaJurosModel> cbTabela;
    @FXML private ComboBox<StatusPropostaModel> cbStatus;
    @FXML private ComboBox<ProponenteModel> cbCliente;
    @FXML private TextField txtValorSolicitado, txtValorAprovado, txtParcela;
    @FXML private Spinner<Integer> spinPrazo, spinPrazoDesejado;
    @FXML private TextArea txtObservacoes;
    @FXML private Label lblComissaoEstimada, lblTituloComissao, lblTotalPago;
    @FXML private Button btnAnexarDocumento, btnSalvar, btnRemover;
    @FXML private ComboBox<String> cmbModeloIA;
    @FXML private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML private TableColumn<DocumentoProponenteModel, String> colTipoDocumento, colDataUpload;
    @FXML private TableColumn<DocumentoProponenteModel, Void> colAcoes;
    @FXML private VBox overlayDocumento, boxSelecaoArquivo;
    @FXML private Label lblOverlayDocTitulo, lblArquivoSelecionado;
    @FXML private ComboBox<String> cbTipoDocumentoOverlay;
    @FXML private Button btnSalvarDocumento;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private File arquivoSelecionadoParaUpload;
    private DocumentoProponenteModel documentoSendoEditado;
    private Runnable onPropostaFechada;
    private List<TabelaJurosModel> todasTabelasAtivas;
    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();
    private boolean isUpdatingInterface = false;
    private boolean modelosCarregados = false;

    /**
     * Injeção de dependência via construtor (Recomendado pelo Spring). Garante
     * imutabilidade e facilita testes unitários (Mocking).
     */
    public PropostaController(PropostaViewModel viewModel, IPropostaFacade propostaFacade, Navigator navigator, HostServices hostServices) {
        this.viewModel = viewModel;
        this.propostaFacade = propostaFacade;
        this.navigator = navigator;
        this.hostServices = hostServices;
        log.debug("{} [SISTEMA] Instanciando controlador e injetando dependências.", LOG_PREFIX);
    }

    public void setEsteiraController(EsteiraPropostasController esteiraController) {
        log.trace("{} [SISTEMA] Vinculando EsteiraPropostasController ao PropostaController.", LOG_PREFIX);
        this.esteiraController = esteiraController;
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================

    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface da Proposta...", LOG_PREFIX);
        carregarListasBase();
        configurarConversoresVisuais();
        configurarBindings();
        configurarCalculosAutomaticos();
        configurarTabelaDocumentos();
        configurarAutoSelecaoTextos();
        configurarIndicadoresDinamicos();
        configurarTravasEAlertas();
        inicializarComboBoxIA();
        configurarOverlayDocumentos();
        log.debug("{} [LIFECYCLE] Interface inicializada com sucesso.", LOG_PREFIX);
    }

    public void carregarProposta(PropostaModel completa) {
        log.info("{} [AUDITORIA] Carregando proposta na tela. ID: {}", LOG_PREFIX, completa != null ? completa.getId() : "NOVA");
        executarSemGatilhos(() -> {
            viewModel.loadFromModel(completa != null ? completa : new PropostaModel());
            sincronizarComboBoxesComViewModel();
            cbCliente.setDisable(completa != null && completa.getId() != null);

            if (completa != null && completa.getId() != null) {
                carregarDocumentosDaProposta(completa.getId());
            }
            aplicarBloqueio();
        });
        propostaFacade.atualizarContextoAtendimento(completa);
    }

    private void carregarListasBase() {
        log.trace("{} [TELEMETRIA] Carregando listas base (Tabelas, Bancos, Status, Convênios, Clientes).", LOG_PREFIX);
        this.todasTabelasAtivas = propostaFacade.listarTabelasAtivas();
        List<BancoModel> bancosAtivos = propostaFacade.listarBancosDasTabelasAtivas();

        executarSemGatilhos(() -> cbBanco.setItems(FXCollections.observableArrayList(bancosAtivos)));
        cbStatus.setItems(FXCollections.observableArrayList(StatusPropostaModel.values()));

        if (cbConvenio != null)
            cbConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        if (cbCliente != null)
            cbCliente.setItems(FXCollections.observableArrayList(propostaFacade.listarClientesCarteira()));
    }

    // ==========================================================================================
    // MÓDULO 6: AÇÕES PRINCIPAIS (CRUD)
    // ==========================================================================================

    @FXML public void salvarProposta() {
        log.info("{} [TELEMETRIA] Ação acionada: Salvar Proposta.", LOG_PREFIX);

        if (!viewModel.isValido()) {
            log.warn("{} [NEGOCIO] Tentativa de salvamento bloqueada: Formulário inválido.", LOG_PREFIX);
            mostrarFeedback("⚠️", "Atenção", "Preencha o Cliente, Banco, Tabela e Convênio para salvar.", null);
            return;
        }

        // Snapshot seguro na UI Thread
        PropostaModel snapshotDaTela = viewModel.atualizarModel(new PropostaModel());

        AsyncUtils.executarTaskAsync(() -> propostaFacade.salvarProposta(snapshotDaTela), (PropostaModel salva) -> {
            log.info("{} [AUDITORIA] Proposta salva com sucesso. ID: {}", LOG_PREFIX, salva.getId());
            carregarProposta(salva);
            mostrarFeedback("✅", "Sucesso!", "Proposta salva com sucesso.", null);
        }, erro -> {
            log.error("{} [AUDITORIA] Falha ao salvar proposta: {}", LOG_PREFIX, erro.getMessage());
            mostrarFeedback("❌", "Erro", erro.getMessage(), null);
        });
    }

    @FXML public void removerProposta() {
        Long id = viewModel.idProperty().get();
        if (id == null) {
            log.trace("{} [TELEMETRIA] Tentativa de remover proposta não salva. Fechando formulário.", LOG_PREFIX);
            confirmarFechar();
            return;
        }

        log.warn("{} [TELEMETRIA] Ação acionada: Solicitação de Exclusão. ID: {}", LOG_PREFIX, id);
        if (esteiraController != null) {
            esteiraController.solicitarConfirmacao("⚠️ Confirmar Exclusão", "Deseja excluir permanentemente esta proposta?", "Sim, Excluir",
                    "#c62828", () -> AsyncUtils.executarTaskAsync(() -> {
                        propostaFacade.excluirProposta(id);
                        return null;
                    }, sucesso -> {
                        log.info("{} [AUDITORIA] Proposta excluída com sucesso. ID: {}", LOG_PREFIX, id);
                        confirmarFechar();
                    }, erro -> {
                        log.error("{} [AUDITORIA] Erro ao excluir proposta ID {}: {}", LOG_PREFIX, id, erro.getMessage());
                        mostrarFeedback("❌", "Erro", erro.getMessage(), null);
                    }), null);
        }
    }

    @FXML public void fecharProposta() {
        log.debug("{} [TELEMETRIA] Ação acionada: Fechar formulário.", LOG_PREFIX);
        if (viewModel.isDirty() && esteiraController != null) {
            log.debug("{} [TELEMETRIA] Tentativa de fechar com alterações pendentes.", LOG_PREFIX);
            esteiraController.solicitarConfirmacao("⚠️ Descartar alterações?", "Existem alterações não salvas. Deseja fechar?", "Descartar",
                    "#c62828", this::confirmarFechar, null);
        } else {
            confirmarFechar();
        }
    }

    private void confirmarFechar() {
        log.debug("{} [LIFECYCLE] Fechando formulário de proposta.", LOG_PREFIX);
        if (onPropostaFechada != null)
            onPropostaFechada.run();
    }

    // ==========================================================================================
    // MÓDULO 7: GESTÃO DE DOCUMENTOS
    // ==========================================================================================

    private void configurarTabelaDocumentos() {
        log.trace("{} [UI] Configurando colunas da tabela de documentos.", LOG_PREFIX);
        tableDocumentos.setItems(listaDocumentos);
        colAcoes.setPrefWidth(240);
        colTipoDocumento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoDocumento()));
        colDataUpload.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDataUpload() != null ? d.getValue().getDataUpload().format(DATE_FORMATTER) : "-"));
        colAcoes.setCellFactory(param -> new BotoesAcaoDocumentoCell());
    }

    private void carregarDocumentosDaProposta(Long propostaId) {
        if (propostaId == null) {
            log.trace("{} [UI] Proposta ID nulo, limpando lista de documentos.", LOG_PREFIX);
            listaDocumentos.clear();
            return;
        }
        log.debug("{} [TELEMETRIA] Carregando documentos para a proposta ID: {}", LOG_PREFIX, propostaId);

        AsyncUtils.executarTaskAsync(() -> propostaFacade.buscarDocumentosDaProposta(propostaId), docs -> {
            listaDocumentos.setAll(docs);
            tableDocumentos.refresh();
            log.info("{} [TELEMETRIA] {} documentos carregados.", LOG_PREFIX, docs.size());
        }, erro -> log.error("{} [AUDITORIA] Erro ao carregar documentos: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML private void anexarDocumento() {
        log.debug("{} [TELEMETRIA] Ação acionada: Anexar Documento.", LOG_PREFIX);
        if (viewModel.idProperty().get() == null) {
            log.warn("{} [NEGOCIO] Tentativa de anexar documento bloqueada: Proposta não salva.", LOG_PREFIX);
            mostrarFeedback("⚠️", "Atenção", "Salve a proposta primeiro para gerar um código antes de anexar documentos.", null);
            return;
        }
        prepararOverlayDocumento(null, "➕ Anexar Novo Documento");
    }

    private void prepararOverlayDocumento(DocumentoProponenteModel doc, String titulo) {
        log.trace("{} [UI] Preparando overlay de documento. Título: {}", LOG_PREFIX, titulo);
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

    @FXML private void procurarArquivo() {
        log.debug("{} [TELEMETRIA] Ação acionada: Procurar arquivo no disco.", LOG_PREFIX);
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(overlayDocumento.getScene().getWindow());
        if (arquivo != null && arquivo.exists()) {
            log.trace("{} [UI] Arquivo selecionado: {}", LOG_PREFIX, arquivo.getName());
            arquivoSelecionadoParaUpload = arquivo;
            lblArquivoSelecionado.setText(arquivo.getName());
            validarFormularioDocumento();
        } else {
            log.trace("{} [UI] Seleção de arquivo cancelada ou arquivo inválido.", LOG_PREFIX);
        }
    }

    @FXML public void confirmarSalvarDocumento() {
        String nomePersonalizado = cbTipoDocumentoOverlay.getEditor().getText().trim();
        btnSalvarDocumento.setDisable(true);
        log.info("{} [TELEMETRIA] Iniciando upload/atualização de documento: {}", LOG_PREFIX, nomePersonalizado);

        AsyncUtils.executarTaskAsync(() -> {
            if (documentoSendoEditado != null) {
                return propostaFacade.atualizarTipoDocumento(documentoSendoEditado.getId(), nomePersonalizado);
            } else {
                PropostaModel p = viewModel.atualizarModel(new PropostaModel());
                return propostaFacade.salvarDocumento(arquivoSelecionadoParaUpload, nomePersonalizado, p.getProponente(), p);
            }
        }, sucesso -> {
            log.info("{} [AUDITORIA] Documento salvo/atualizado com sucesso.", LOG_PREFIX);
            fecharOverlayDocumento();
            carregarDocumentosDaProposta(viewModel.idProperty().get());
        }, erro -> {
            log.error("{} [AUDITORIA] Erro ao processar documento: {}", LOG_PREFIX, erro.getMessage());
            btnSalvarDocumento.setDisable(false);
            mostrarFeedback("❌", "Erro de Documento", erro.getMessage(), null);
        });
    }

    private void abrirDocumentoFisico(DocumentoProponenteModel doc) {
        if (doc == null || doc.getArquivoPath() == null) {
            log.warn("{} [NEGOCIO] Tentativa de abrir documento nulo ou sem caminho.", LOG_PREFIX);
            return;
        }

        File f = new File(doc.getArquivoPath());
        if (f.exists()) {
            log.info("{} [TELEMETRIA] Abrindo visualizador nativo para o documento ID: {}", LOG_PREFIX, doc.getId());
            Platform.runLater(() -> hostServices.showDocument(f.getAbsoluteFile().toURI().toString()));
        } else {
            log.warn("{} [AUDITORIA] Tentativa de abrir arquivo inexistente no disco: {}", LOG_PREFIX, doc.getArquivoPath());
            mostrarFeedback("⚠️", "Aviso", "Arquivo físico não encontrado no servidor.", null);
        }
    }

    private void excluirDocumento(DocumentoProponenteModel doc) {
        log.warn("{} [TELEMETRIA] Solicitação de exclusão de documento ID: {}", LOG_PREFIX, doc.getId());
        if (esteiraController != null) {
            esteiraController.solicitarConfirmacao("🗑️ Excluir Documento", "Deseja remover '" + doc.getTipoDocumento() + "'?",
                    "Sim, Excluir", "#c62828", () -> AsyncUtils.executarTaskAsync(() -> {
                        propostaFacade.excluirDocumento(doc.getId());
                        return null;
                    }, sucesso -> {
                        log.info("{} [AUDITORIA] Documento ID {} excluído com sucesso.", LOG_PREFIX, doc.getId());
                        listaDocumentos.remove(doc);
                    }, erro -> {
                        log.error("{} [AUDITORIA] Erro ao excluir documento: {}", LOG_PREFIX, erro.getMessage());
                        mostrarFeedback("❌", "Erro", erro.getMessage(), null);
                    }), null);
        }
    }

    @FXML private void fecharOverlayDocumento() {
        log.trace("{} [UI] Fechando overlay de documento.", LOG_PREFIX);
        overlayDocumento.setVisible(false);
        documentoSendoEditado = null;
        arquivoSelecionadoParaUpload = null;
    }

    private void validarFormularioDocumento() {
        log.trace("{} [UI] Validando formulário de documento.", LOG_PREFIX);
        String texto = cbTipoDocumentoOverlay.getEditor().getText();
        boolean temTexto = texto != null && !texto.trim().isEmpty();
        btnSalvarDocumento.setDisable(documentoSendoEditado != null ? !temTexto : (!temTexto || arquivoSelecionadoParaUpload == null));
    }

    // ==========================================================================================
    // MÓDULO 8: INTELIGÊNCIA ARTIFICIAL (GEMINI)
    // ==========================================================================================

    private void inicializarComboBoxIA() {
        log.trace("{} [UI] Inicializando ComboBox de modelos de IA.", LOG_PREFIX);
        if (cmbModeloIA == null)
            return;
        cmbModeloIA.getItems().add(MODELO_IA_FALLBACK);
        cmbModeloIA.getSelectionModel().selectFirst();

        if (!modelosCarregados) {
            AsyncUtils.executarTaskAsync(propostaFacade::listarModelosIADisponiveis, modelos -> {
                log.trace("{} [UI] Modelos de IA carregados com sucesso.", LOG_PREFIX);
                String atual = cmbModeloIA.getValue();
                cmbModeloIA.getItems().setAll(modelos);
                if (modelos.contains(atual))
                    cmbModeloIA.getSelectionModel().select(atual);
                else
                    cmbModeloIA.getSelectionModel().selectFirst();
                modelosCarregados = true;
            }, erro -> log.error("{} [TELEMETRIA] Erro ao carregar modelos IA: {}", LOG_PREFIX, erro.getMessage()));
        }
    }

    private void analisarDocumentoComIA(DocumentoProponenteModel doc) {
        var config = propostaFacade.obterConfiguracaoIADocumento(doc.getTipoDocumento());
        log.info("{} [TELEMETRIA] Iniciando análise de IA. Tipo: {}, Modelo: {}", LOG_PREFIX, config.titulo(), cmbModeloIA.getValue());

        if (navigator != null) {
            navigator.mostrarLoading("O Gemini está atuando como especialista em " + config.titulo() + "...");
        }

        String modelo = cmbModeloIA != null && cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : MODELO_IA_FALLBACK;

        AsyncUtils.executarTaskAsync(() -> propostaFacade.analisarDocumentoComIA(doc, viewModel.proponenteProperty().get(), modelo),
                resultado -> {
                    log.info("{} [AUDITORIA] Análise de IA concluída com sucesso para documento ID: {}", LOG_PREFIX, doc.getId());
                    if (navigator != null)
                        navigator.ocultarLoading();

                    // 🚀 CORREÇÃO: Chama o WebView diretamente para renderizar
                    // o HTML da IA
                    if (esteiraController != null) {
                        esteiraController.mostrarFeedback(config.icone(), config.titulo(), resultado, null);
                    }
                }, erro -> {
                    log.error("{} [AUDITORIA] Falha na análise de IA: {}", LOG_PREFIX, erro.getMessage());
                    if (navigator != null)
                        navigator.ocultarLoading();
                    mostrarFeedback("❌", "Falha na Análise", erro.getMessage(), null);
                });
    }

    // ==========================================================================================
    // MÓDULO 9: REGRAS DE INTERFACE E BINDINGS (Humble Object)
    // ==========================================================================================

    public void aplicarBloqueio() {
        log.trace("{} [UI] Aplicando bloqueios de interface baseados no status da proposta.", LOG_PREFIX);
        PropostaModel snapshot = viewModel.atualizarModel(new PropostaModel());
        boolean terminal = propostaFacade.isStatusTerminal(snapshot.getStatus(), snapshot.getId());
        boolean bloqueadaPeloCopiloto = propostaFacade.isBloqueadaPeloCopiloto(snapshot);

        cbStatus.setDisable(terminal);
        cbTabela.setDisable(terminal || bloqueadaPeloCopiloto);
        cbBanco.setDisable(terminal || bloqueadaPeloCopiloto);
        cbConvenio.setDisable(terminal || bloqueadaPeloCopiloto);
        txtValorSolicitado.setDisable(terminal || bloqueadaPeloCopiloto);
        if (spinPrazoDesejado != null)
            spinPrazoDesejado.setDisable(terminal || bloqueadaPeloCopiloto);

        txtValorAprovado.setDisable(terminal);
        txtParcela.setDisable(terminal);
        spinPrazo.setDisable(terminal);

        if (terminal)
            txtObservacoes.setPromptText("Proposta liquidada. Alterações não permitidas.");
        if (btnSalvar != null)
            btnSalvar.setDisable(terminal);
        if (btnRemover != null)
            btnRemover.setDisable(terminal);
    }

    private void calcularComissao() {
        log.trace("{} [UI] Atualizando exibição da comissão estimada.", LOG_PREFIX);
        PropostaModel snapshot = viewModel.atualizarModel(new PropostaModel());
        BigDecimal comissao = propostaFacade.calcularComissao(snapshot);

        viewModel.comissaoEstimadaProperty().set(comissao);

        TabelaJurosModel tabela = cbTabela.getValue();
        if (comissao.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            lblComissaoEstimada
                    .setText(String.format("%s (%s%%)", FinanceiroUtils.formatarParaExibicao(comissao), tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: #1b5e20; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    private void configurarBindings() {
        log.trace("{} [UI] Configurando bindings bidirecionais.", LOG_PREFIX);
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
        log.trace("{} [UI] Configurando bindings do cliente.", LOG_PREFIX);
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

    private void atualizarTabelasDoBanco(BancoModel banco) {
        log.trace("{} [UI] Atualizando tabelas disponíveis para o banco selecionado.", LOG_PREFIX);
        if (isUpdatingInterface)
            return;
        if (banco != null) {
            List<TabelaJurosModel> tabelas = todasTabelasAtivas.stream()
                    .filter(t -> t.getBanco() != null && t.getBanco().getId().equals(banco.getId()))
                    .sorted(Comparator.comparing(TabelaJurosModel::getComissaoPercentual).reversed()).toList();
            TabelaJurosModel atual = cbTabela.getValue();
            executarSemGatilhos(() -> {
                cbTabela.setItems(FXCollections.observableArrayList(tabelas));
                if (atual != null)
                    tabelas.stream().filter(t -> t.getId().equals(atual.getId())).findFirst().ifPresentOrElse(cbTabela::setValue,
                            () -> cbTabela.setValue(null));
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
        log.trace("{} [UI] Carregando dados vinculados à tabela selecionada.", LOG_PREFIX);
        if (isUpdatingInterface)
            return;
        executarSemGatilhos(() -> {
            if (idTabela != null) {
                TabelaJurosModel tab = todasTabelasAtivas.stream().filter(t -> t.getId().equals(idTabela)).findFirst().orElse(null);
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
        log.trace("{} [UI] Sincronizando ComboBoxes com o ViewModel.", LOG_PREFIX);
        sincronizarComboModel(cbCliente, viewModel.proponenteProperty().get(), ProponenteModel::getId);
        cbStatus.setValue(viewModel.statusProperty().get());
        cbConvenio.setValue(viewModel.convenioProperty().get());
        BancoModel b = viewModel.bancoProperty().get();
        sincronizarComboModel(cbBanco, b, BancoModel::getId);
        Long idTabela = viewModel.tabelaIdProperty().get();
        if (idTabela != null) {
            TabelaJurosModel tab = todasTabelasAtivas.stream().filter(t -> t.getId().equals(idTabela)).findFirst().orElse(null);
            if (b != null)
                atualizarTabelasDoBanco(b);
            sincronizarComboModel(cbTabela, tab, TabelaJurosModel::getId);
        } else {
            cbTabela.getSelectionModel().clearSelection();
        }
        calcularComissao();
    }

    private void configurarIndicadoresDinamicos() {
        log.trace("{} [UI] Configurando indicadores dinâmicos (Total Pago, etc).", LOG_PREFIX);
        lblTituloComissao.textProperty().bind(Bindings.createStringBinding(() -> {
            BigDecimal aprovado = viewModel.valorAprovadoProperty().get();
            return (aprovado != null && aprovado.compareTo(BigDecimal.ZERO) > 0) ? "Valor da Comissão" : "Comissão Estimada";
        }, viewModel.valorAprovadoProperty()));

        lblTotalPago.textProperty().bind(Bindings.createStringBinding(() -> {
            Integer prazo = viewModel.quantidadeParcelasProperty().get();
            BigDecimal parcela = viewModel.valorParcelaProperty().get();
            if (prazo != null && prazo > 0 && parcela != null && parcela.compareTo(BigDecimal.ZERO) > 0) {
                return "Total a Pagar: " + FinanceiroUtils.formatarParaExibicao(parcela.multiply(BigDecimal.valueOf(prazo)));
            }
            return "Total a Pagar: R$ 0,00";
        }, viewModel.quantidadeParcelasProperty(), viewModel.valorParcelaProperty()));
    }

    private void configurarTravasEAlertas() {
        log.trace("{} [UI] Configurando travas e alertas visuais.", LOG_PREFIX);
        if (btnAnexarDocumento != null) {
            BooleanBinding isTerminal = Bindings.createBooleanBinding(
                    () -> propostaFacade.isStatusTerminal(viewModel.statusProperty().get(), viewModel.idProperty().get()),
                    viewModel.statusProperty());
            btnAnexarDocumento.disableProperty().bind(viewModel.idProperty().isNull().or(isTerminal));
        }
        addBorderStyleListener(cbBanco);
        addBorderStyleListener(cbTabela);
    }

    private void configurarOverlayDocumentos() {
        log.trace("{} [UI] Configurando overlay de documentos.", LOG_PREFIX);
        if (cbTipoDocumentoOverlay == null)
            return;
        cbTipoDocumentoOverlay.setItems(TIPOS_DOCUMENTO_PADRAO);
        cbTipoDocumentoOverlay.valueProperty().addListener((obs, old, val) -> validarFormularioDocumento());
        cbTipoDocumentoOverlay.getEditor().textProperty().addListener((obs, old, val) -> validarFormularioDocumento());
    }

    private void configurarCalculosAutomaticos() {
        log.trace("{} [UI] Configurando listeners para cálculos automáticos.", LOG_PREFIX);
        viewModel.valorAprovadoProperty().addListener((obs, old, val) -> calcularComissao());
        viewModel.valorSolicitadoProperty().addListener((obs, old, val) -> calcularComissao());
        viewModel.bancoProperty().addListener((obs, old, banco) -> atualizarTabelasDoBanco((BancoModel) banco));
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

    // ==========================================================================================
    // MÓDULO 10: UTILITÁRIOS E CLASSES INTERNAS
    // ==========================================================================================

    private void configurarConversoresVisuais() {
        log.trace("{} [UI] Configurando conversores visuais para ComboBoxes.", LOG_PREFIX);
        if (cbConvenio != null)
            cbConvenio.setConverter(criarConversor(TipoConvenioModel::getLabel));
        cbBanco.setConverter(criarConversor(b -> b != null ? b.getNome() : "Selecione o Banco..."));
        cbTabela.setConverter(
                criarConversor(t -> t != null ? t.getNomeTabela() + " (" + t.getComissaoPercentual() + "%)" : "Selecione a Tabela..."));
        if (cbCliente != null)
            cbCliente.setConverter(
                    criarConversor(c -> c != null ? c.getNomeCompleto() + " (" + c.getCpf() + ")" : "Selecione o Cliente..."));
    }

    private void addBorderStyleListener(ComboBox<?> combo) {
        log.trace("{} [UI] Adicionando listener de estilo de borda.", LOG_PREFIX);
        combo.valueProperty().addListener((obs, old, val) -> combo
                .setStyle(val == null ? "-fx-border-color: #ffb74d; -fx-border-width: 1px; -fx-border-radius: 4px;" : ""));
    }

    private <T> void sincronizarComboModel(ComboBox<T> combo, T model, Function<T, Long> idExtractor) {
        log.trace("{} [UI] Sincronizando modelo do ComboBox.", LOG_PREFIX);
        if (model != null) {
            combo.getItems().stream().filter(c -> idExtractor.apply(c).equals(idExtractor.apply(model))).findFirst()
                    .ifPresentOrElse(combo::setValue, () -> {
                        combo.getItems().add(model);
                        combo.setValue(model);
                    });
        } else
            combo.getSelectionModel().clearSelection();
    }

    private void executarSemGatilhos(Runnable acao) {
        log.trace("{} [SISTEMA] Executando ação com gatilhos de interface desativados.", LOG_PREFIX);
        isUpdatingInterface = true;
        try {
            acao.run();
        } finally {
            isUpdatingInterface = false;
        }
    }

    private <T> void bindComboSafely(ComboBox<T> combo, javafx.beans.property.Property<T> property) {
        log.trace("{} [UI] Realizando bind seguro para ComboBox.", LOG_PREFIX);
        combo.valueProperty().addListener((obs, old, val) -> {
            if (!isUpdatingInterface)
                property.setValue(val);
        });
    }

    private <T> void safeSet(ComboBox<T> combo, T val) {
        log.trace("{} [UI] Definindo valor seguro para ComboBox.", LOG_PREFIX);
        if (!isUpdatingInterface)
            combo.setValue(val);
    }

    private <T> StringConverter<T> criarConversor(Function<T, String> formatter) {
        log.trace("{} [UI] Criando StringConverter.", LOG_PREFIX);
        return new StringConverter<>() {
            @Override public String toString(T obj) {
                return formatter.apply(obj);
            }

            @Override public T fromString(String s) {
                return null;
            }
        };
    }

    private void vincularMascaraMoeda(TextField campo, javafx.beans.property.Property<BigDecimal> prop) {
        log.trace("{} [UI] Vinculando máscara de moeda ao TextField.", LOG_PREFIX);
        TextFormatter<BigDecimal> fmt = FinanceiroUtils.criarFormatadorMoeda();
        campo.setTextFormatter(fmt);
        fmt.valueProperty().bindBidirectional(prop);
    }

    private void configurarSpinner(Spinner<Integer> spinner, javafx.beans.property.Property<Integer> prop) {
        log.trace("{} [UI] Configurando Spinner numérico.", LOG_PREFIX);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 1));
        spinner.setEditable(true);
        spinner.getEditor().focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                try {
                    spinner.getValueFactory().setValue(spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText()));
                } catch (Exception e) {
                    spinner.getEditor().setText(spinner.getValueFactory().getConverter().toString(spinner.getValue()));
                }
            }
        });
        spinner.getValueFactory().valueProperty().bindBidirectional(prop);
    }

    private void configurarAutoSelecaoTextos() {
        log.trace("{} [UI] Configurando auto-seleção de textos nos TextFields.", LOG_PREFIX);
        for (TextField campo : new TextField[] { txtValorSolicitado, txtValorAprovado, txtParcela })
            campo.focusedProperty().addListener((obs, old, focused) -> {
                if (focused)
                    Platform.runLater(campo::selectAll);
            });
    }

    public PropostaViewModel getViewModel() {
        log.trace("{} [SISTEMA] Acessando ViewModel.", LOG_PREFIX);
        return viewModel;
    }

    public void setOnPropostaFechada(Runnable callback) {
        log.trace("{} [SISTEMA] Configurando callback de fechamento da proposta.", LOG_PREFIX);
        this.onPropostaFechada = callback;
    }

    private void mostrarFeedback(String icone, String titulo, String msg, Runnable callback) {
        log.trace("{} [UI] Exibindo feedback visual: {} - {}", LOG_PREFIX, titulo, msg);
        if (esteiraController != null) {
            // Usa o alerta simples (pequeno) para mensagens comuns
            String tipoBotao = titulo.contains("Erro") || titulo.contains("Falha") ? "danger" : "success";
            esteiraController.mostrarAlertaSimples(icone, titulo, msg, tipoBotao, callback);
        }
    }

    /**
     * Classe interna responsável exclusivamente por desenhar os botões de ação
     * na tabela de documentos. Delega todas as ações complexas para o
     * Controller pai.
     */
    private class BotoesAcaoDocumentoCell extends TableCell<DocumentoProponenteModel, Void> {
        private final Button btnAbrir = criarBotaoAcao("👁️", "Visualizar Documento", "flat");
        private final Button btnEditar = criarBotaoAcao("✏️", "Renomear / Mudar Tipo", "flat");
        private final Button btnAnalisarIA = criarBotaoAcao("🤖", "Análise Cognitiva com Gemini IA", "flat", "accent");
        private final Button btnExcluir = criarBotaoAcao("🗑️", "Excluir Documento", "flat", "danger");
        private final HBox container = new HBox(6, btnAbrir, btnEditar, btnAnalisarIA, btnExcluir);

        public BotoesAcaoDocumentoCell() {
            log.trace("{} [UI] Instanciando célula de botões de ação para documentos.", LOG_PREFIX);
            container.setAlignment(Pos.CENTER);
            btnAbrir.setOnAction(e -> abrirDocumentoFisico(getDoc()));
            btnEditar.setOnAction(e -> prepararOverlayDocumento(getDoc(), "✏️ Renomear Documento"));
            btnAnalisarIA.setOnAction(e -> analisarDocumentoComIA(getDoc()));
            btnExcluir.setOnAction(e -> excluirDocumento(getDoc()));
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

        @Override protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getDoc() == null) {
                setGraphic(null);
            } else {
                setGraphic(container);
                PropostaModel snapshot = viewModel.atualizarModel(new PropostaModel());
                boolean terminal = propostaFacade.isStatusTerminal(snapshot.getStatus(), snapshot.getId());

                btnEditar.setVisible(!terminal);
                btnEditar.setManaged(!terminal);
                btnExcluir.setVisible(!terminal);
                btnExcluir.setManaged(!terminal);
            }
        }
    }
}
