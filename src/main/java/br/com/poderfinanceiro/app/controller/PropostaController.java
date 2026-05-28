package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.facade.IPropostaFacade;
import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;
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
 * @version 2.1 (Padronização de Overlays via Navigator)
 */
@Component
@Scope("prototype")
public class PropostaController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(PropostaController.class);
    private static final String LOG_PREFIX = "[PropostaController]";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MODELO_IA_FALLBACK = "gemini-3.5-flash";
    private static final ObservableList<String> TIPOS_DOCUMENTO_PADRAO = FXCollections.observableArrayList("RG Frente",
            "RG Verso", "CNH Frente", "CNH Verso", "CPF", "Comprovante Residencia", "Contracheque", "Holerite",
            "Extrato Hiscon", "Extrato Bancario", "Outros");

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
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

    public PropostaController(PropostaViewModel viewModel, IPropostaFacade propostaFacade, Navigator navigator,
            HostServices hostServices) {
        this.viewModel = viewModel;
        this.propostaFacade = propostaFacade;
        this.navigator = navigator;
        this.hostServices = hostServices;
        log.info("{} [SISTEMA] Controlador instanciado com suporte a Navigator Global.", LOG_PREFIX);
    }

    public void setEsteiraController(EsteiraPropostasController esteiraController) {
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
        log.debug("{} [LIFECYCLE] Interface inicializada.", LOG_PREFIX);
    }

    public void carregarProposta(PropostaModel completa) {
        log.info("{} [AUDITORIA] Carregando proposta ID: {}", LOG_PREFIX, completa != null ? completa.getId() : "NOVA");
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
            navigator.notificarAviso("Preencha o Cliente, Banco, Tabela e Convênio para salvar.");
            return;
        }

        PropostaModel snapshotDaTela = viewModel.atualizarModel(new PropostaModel());
        navigator.mostrarLoading("Salvando proposta...");

        AsyncUtils.executarTaskAsync(() -> propostaFacade.salvarProposta(snapshotDaTela), (PropostaModel salva) -> {
            navigator.ocultarLoading();
            log.info("{} [AUDITORIA] Proposta salva com sucesso. ID: {}", LOG_PREFIX, salva.getId());
            carregarProposta(salva);
            navigator.notificarSucesso("Proposta salva com sucesso!");
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [AUDITORIA] Falha ao salvar: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso("Erro ao salvar: " + erro.getMessage());
        });
    }

    @FXML public void removerProposta() {
        Long id = viewModel.idProperty().get();
        if (id == null) {
            confirmarFechar();
            return;
        }

        log.warn("{} [TELEMETRIA] Solicitando exclusão global da proposta ID: {}", LOG_PREFIX, id);
        navigator.solicitarConfirmacao("⚠️ Confirmar Exclusão", "Deseja excluir permanentemente esta proposta?",
                "Sim, Excluir", "#c62828", () -> {
                    navigator.mostrarLoading("Excluindo proposta...");
                    AsyncUtils.executarTaskAsync(() -> {
                        propostaFacade.excluirProposta(id);
                        return null;
                    }, sucesso -> {
                        navigator.ocultarLoading();
                        log.info("{} [AUDITORIA] Proposta ID {} removida.", LOG_PREFIX, id);
                        confirmarFechar();
                        navigator.notificarSucesso("Proposta removida com sucesso.");
                    }, erro -> {
                        navigator.ocultarLoading();
                        navigator.notificarAviso("Erro ao excluir: " + erro.getMessage());
                    });
                });
    }

    @FXML public void fecharProposta() {
        if (viewModel.isDirty()) {
            navigator.solicitarConfirmacao("⚠️ Descartar alterações?",
                    "Existem alterações não salvas. Deseja fechar mesmo assim?", "Descartar", "#c62828",
                    this::confirmarFechar);
        } else {
            confirmarFechar();
        }
    }

    private void confirmarFechar() {
        if (onPropostaFechada != null)
            onPropostaFechada.run();
    }

    // ==========================================================================================
    // MÓDULO 7: GESTÃO DE DOCUMENTOS
    // ==========================================================================================

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
        AsyncUtils.executarTaskAsync(() -> propostaFacade.buscarDocumentosDaProposta(propostaId), docs -> {
            listaDocumentos.setAll(docs);
            tableDocumentos.refresh();
        }, erro -> log.error("{} [AUDITORIA] Erro ao carregar documentos: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML private void anexarDocumento() {
        if (viewModel.idProperty().get() == null) {
            navigator.notificarAviso("Salve a proposta primeiro para gerar um código antes de anexar documentos.");
            return;
        }
        prepararOverlayDocumento(null, "➕ Anexar Novo Documento");
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

    @FXML private void procurarArquivo() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(overlayDocumento.getScene().getWindow());
        if (arquivo != null && arquivo.exists()) {
            arquivoSelecionadoParaUpload = arquivo;
            lblArquivoSelecionado.setText(arquivo.getName());
            validarFormularioDocumento();
        }
    }

    @FXML public void confirmarSalvarDocumento() {
        String nomePersonalizado = cbTipoDocumentoOverlay.getEditor().getText().trim();
        btnSalvarDocumento.setDisable(true);
        navigator.mostrarLoading("Enviando documento...");

        AsyncUtils.executarTaskAsync(() -> {
            if (documentoSendoEditado != null) {
                return propostaFacade.atualizarTipoDocumento(documentoSendoEditado.getId(), nomePersonalizado);
            } else {
                PropostaModel p = viewModel.atualizarModel(new PropostaModel());
                return propostaFacade.salvarDocumento(arquivoSelecionadoParaUpload, nomePersonalizado,
                        p.getProponente(), p);
            }
        }, sucesso -> {
            navigator.ocultarLoading();
            fecharOverlayDocumento();
            carregarDocumentosDaProposta(viewModel.idProperty().get());
            navigator.notificarSucesso("Documento processado com sucesso!");
        }, erro -> {
            navigator.ocultarLoading();
            btnSalvarDocumento.setDisable(false);
            navigator.notificarAviso("Erro de Documento: " + erro.getMessage());
        });
    }

    private void abrirDocumentoFisico(DocumentoProponenteModel doc) {
        if (doc == null || doc.getArquivoPath() == null)
            return;
        File f = new File(doc.getArquivoPath());
        if (f.exists()) {
            Platform.runLater(() -> hostServices.showDocument(f.getAbsoluteFile().toURI().toString()));
        } else {
            navigator.notificarAviso("Arquivo físico não encontrado no servidor.");
        }
    }

    private void excluirDocumento(DocumentoProponenteModel doc) {
        navigator.solicitarConfirmacao("🗑️ Excluir Documento", "Deseja remover '" + doc.getTipoDocumento() + "'?",
                "Sim, Excluir", "#c62828", () -> {
                    navigator.mostrarLoading("Removendo documento...");
                    AsyncUtils.executarTaskAsync(() -> {
                        propostaFacade.excluirDocumento(doc.getId());
                        return null;
                    }, sucesso -> {
                        navigator.ocultarLoading();
                        listaDocumentos.remove(doc);
                        navigator.notificarSucesso("Documento removido.");
                    }, erro -> {
                        navigator.ocultarLoading();
                        navigator.notificarAviso("Erro ao excluir: " + erro.getMessage());
                    });
                });
    }

    @FXML private void fecharOverlayDocumento() {
        overlayDocumento.setVisible(false);
        documentoSendoEditado = null;
        arquivoSelecionadoParaUpload = null;
    }

    private void validarFormularioDocumento() {
        String texto = cbTipoDocumentoOverlay.getEditor().getText();
        boolean temTexto = texto != null && !texto.trim().isEmpty();
        btnSalvarDocumento.setDisable(
                documentoSendoEditado != null ? !temTexto : (!temTexto || arquivoSelecionadoParaUpload == null));
    }

    // ==========================================================================================
    // MÓDULO 8: INTELIGÊNCIA ARTIFICIAL (GEMINI)
    // ==========================================================================================

    private void inicializarComboBoxIA() {
        if (cmbModeloIA == null)
            return;
        cmbModeloIA.getItems().add(MODELO_IA_FALLBACK);
        cmbModeloIA.getSelectionModel().selectFirst();

        if (!modelosCarregados) {
            AsyncUtils.executarTaskAsync(propostaFacade::listarModelosIADisponiveis, modelos -> {
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
        navigator.mostrarLoading("O Gemini está atuando como especialista em " + config.titulo() + "...");

        String modelo = cmbModeloIA != null && cmbModeloIA.getValue() != null ? cmbModeloIA.getValue()
                : MODELO_IA_FALLBACK;

        AsyncUtils.executarTaskAsync(
                () -> propostaFacade.analisarDocumentoComIA(doc, viewModel.proponenteProperty().get(), modelo),
                resultado -> {
                    navigator.ocultarLoading();
                    if (esteiraController != null) {
                        esteiraController.mostrarFeedback(config.icone(), config.titulo(), resultado, null);
                    }
                }, erro -> {
                    navigator.ocultarLoading();
                    navigator.notificarAviso("Falha na Análise: " + erro.getMessage());
                });
    }

    // ==========================================================================================
    // MÓDULO 9: REGRAS DE INTERFACE E BINDINGS
    // ==========================================================================================

    public void aplicarBloqueio() {
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
        PropostaModel snapshot = viewModel.atualizarModel(new PropostaModel());
        BigDecimal comissao = propostaFacade.calcularComissao(snapshot);
        viewModel.comissaoEstimadaProperty().set(comissao);

        TabelaJurosModel tabela = cbTabela.getValue();
        if (comissao.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            lblComissaoEstimada.setText(String.format("%s (%s%%)", FinanceiroUtils.formatarParaExibicao(comissao),
                    tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: #1b5e20; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
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

    private void atualizarTabelasDoBanco(BancoModel banco) {
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
                    tabelas.stream().filter(t -> t.getId().equals(atual.getId())).findFirst()
                            .ifPresentOrElse(cbTabela::setValue, () -> cbTabela.setValue(null));
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
            if (b != null)
                atualizarTabelasDoBanco(b);
            sincronizarComboModel(cbTabela, tab, TabelaJurosModel::getId);
        } else {
            cbTabela.getSelectionModel().clearSelection();
        }
        calcularComissao();
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
            BooleanBinding isTerminal = Bindings.createBooleanBinding(() -> propostaFacade
                    .isStatusTerminal(viewModel.statusProperty().get(), viewModel.idProperty().get()),
                    viewModel.statusProperty());
            btnAnexarDocumento.disableProperty().bind(viewModel.idProperty().isNull().or(isTerminal));
        }
        addBorderStyleListener(cbBanco);
        addBorderStyleListener(cbTabela);
    }

    private void configurarOverlayDocumentos() {
        if (cbTipoDocumentoOverlay == null)
            return;
        cbTipoDocumentoOverlay.setItems(TIPOS_DOCUMENTO_PADRAO);
        cbTipoDocumentoOverlay.valueProperty().addListener((obs, old, val) -> validarFormularioDocumento());
        cbTipoDocumentoOverlay.getEditor().textProperty().addListener((obs, old, val) -> validarFormularioDocumento());
    }

    private void configurarCalculosAutomaticos() {
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

    private void addBorderStyleListener(ComboBox<?> combo) {
        combo.valueProperty().addListener((obs, old, val) -> combo.setStyle(
                val == null ? "-fx-border-color: #ffb74d; -fx-border-width: 1px; -fx-border-radius: 4px;" : ""));
    }

    private <T> void sincronizarComboModel(ComboBox<T> combo, T model, Function<T, Long> idExtractor) {
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
            @Override public String toString(T obj) {
                return formatter.apply(obj);
            }

            @Override public T fromString(String s) {
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

    public void setOnPropostaFechada(Runnable callback) {
        this.onPropostaFechada = callback;
    }

    /**
     * Classe interna para botões de ação na tabela de documentos.
     */
    private class BotoesAcaoDocumentoCell extends TableCell<DocumentoProponenteModel, Void> {
        private final Button btnAbrir = criarBotaoAcao("👁️", "Visualizar Documento", "flat");
        private final Button btnEditar = criarBotaoAcao("✏️", "Renomear / Mudar Tipo", "flat");
        private final Button btnAnalisarIA = criarBotaoAcao("🤖", "Análise Cognitiva com Gemini IA", "flat", "accent");
        private final Button btnExcluir = criarBotaoAcao("🗑️", "Excluir Documento", "flat", "danger");
        private final HBox container = new HBox(6, btnAbrir, btnEditar, btnAnalisarIA, btnExcluir);

        public BotoesAcaoDocumentoCell() {
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
