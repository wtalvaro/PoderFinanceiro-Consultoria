package br.com.poderfinanceiro.app.presentation.controller.atendimento;

import br.com.poderfinanceiro.app.application.facade.IDocumentoFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * <h1>DocumentoController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela gestão de documentos do
 * cliente (CRM). Implementa o padrão <b>Humble Object</b>, delegando a
 * persistência e I/O para a {@link IDocumentoFacade} e interações de overlay
 * para o {@link Navigator}.
 * </p>
 */
@Component
public class DocumentoController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);
    private static final String LOG_PREFIX = "[DocumentoController]";

    private static final List<String> TIPOS_DOCUMENTO = List.of("RG", "CNH", "CPF", "Comprovante de Residência",
            "Contracheque", "Outros");

    private static final String STYLE_DROP_ACTIVE = "-fx-border-color: #2196F3; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #e3f2fd;";
    private static final String STYLE_DROP_INACTIVE = "-fx-border-color: #aaaaaa; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #fafafa;";
    private static final String STYLE_STATUS_OK = "-fx-text-fill: #2e7d32; -fx-underline: false; -fx-font-weight: bold;";
    private static final String STYLE_STATUS_PENDENTE = "-fx-text-fill: #ffa000; -fx-underline: false; -fx-font-weight: bold;";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IDocumentoFacade documentoFacade;
    private final HostServices hostServices;
    private final Navigator navigator;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private VBox dropZone;
    @FXML private VBox panelClassificacao;
    @FXML private Label lblNomeArquivo, lblTituloPanel;
    @FXML private ComboBox<String> comboTipoDocumento;
    @FXML private ScrollPane scrollPrincipal;

    @FXML private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML private TableColumn<DocumentoProponenteModel, String> colTipo, colStatus;
    @FXML private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private ProponenteModel proponenteAtual;
    private File arquivoPendenteUpload;
    private DocumentoProponenteModel documentoEmEdicao;

    public DocumentoController(IDocumentoFacade documentoFacade, HostServices hostServices, Navigator navigator) {
        this.documentoFacade = documentoFacade;
        this.hostServices = hostServices;
        this.navigator = navigator;
        log.info("{} [SISTEMA] Controlador de Documentos instanciado com suporte a Navigator.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Documentos...", LOG_PREFIX);
        configurarTabela();
        comboTipoDocumento.setItems(FXCollections.observableArrayList(TIPOS_DOCUMENTO));
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    public void carregarDocumentos(ProponenteModel proponente) {
        log.info("{} [TELEMETRIA] Carregando documentos para o proponente ID: {}", LOG_PREFIX,
                proponente != null ? proponente.getId() : "NOVO");
        this.proponenteAtual = proponente;
        cancelarUploadInline();

        if (isProponenteValido()) {
            atualizarTabela();
            dropZone.setDisable(false);
        } else {
            tableDocumentos.getItems().clear();
            dropZone.setDisable(true);
            log.debug("{} [NEGOCIO] Bloqueando upload: Proponente não persistido.", LOG_PREFIX);
        }
    }

    private void atualizarTabela() {
        if (!isProponenteValido())
            return;

        AsyncUtils.executarTaskAsync(() -> documentoFacade.listarDocumentosDoProponente(proponenteAtual.getId()),
                docs -> {
                    tableDocumentos.setItems(FXCollections.observableArrayList(docs));
                    log.debug("{} [TELEMETRIA] {} documentos carregados na tabela.", LOG_PREFIX, docs.size());
                }, erro -> log.error("{} [SISTEMA] Erro ao carregar documentos: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 6: DRAG & DROP E SELEÇÃO DE ARQUIVOS
    // ==========================================================================================
    @FXML private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles() && isProponenteValido()) {
            event.acceptTransferModes(TransferMode.COPY);
            dropZone.setStyle(STYLE_DROP_ACTIVE);
        }
        event.consume();
    }

    @FXML private void handleDragDropped(DragEvent event) {
        dropZone.setStyle(STYLE_DROP_INACTIVE);
        boolean success = false;

        if (event.getDragboard().hasFiles()) {
            success = true;
            File file = event.getDragboard().getFiles().get(0);
            log.info("{} [TELEMETRIA] Arquivo recebido via Drag & Drop: {}", LOG_PREFIX, file.getName());
            prepararUploadEmbutido(file);
        }

        event.setDropCompleted(success);
        event.consume();
    }

    @FXML private void handleFileSelection() {
        if (!isProponenteValido()) {
            navigator.notificarAviso("Salve o atendimento primeiro para poder anexar documentos.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Documento");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

        File file = fileChooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            log.info("{} [TELEMETRIA] Arquivo selecionado via FileChooser: {}", LOG_PREFIX, file.getName());
            prepararUploadEmbutido(file);
        }
    }

    private boolean isProponenteValido() {
        return proponenteAtual != null && proponenteAtual.getId() != null;
    }

    // ==========================================================================================
    // MÓDULO 7: FLUXO DE UPLOAD E EDIÇÃO (INLINE)
    // ==========================================================================================
    private void prepararUploadEmbutido(File file) {
        log.trace("{} [UI] Preparando painel para novo upload.", LOG_PREFIX);
        this.arquivoPendenteUpload = file;
        this.documentoEmEdicao = null;

        this.lblTituloPanel.setText("Classifique o novo documento:");
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.getSelectionModel().clearSelection();

        abrirPainelAzul();
    }

    private void prepararEdicaoEmbutida(DocumentoProponenteModel doc) {
        log.trace("{} [UI] Preparando painel para edição do documento ID: {}", LOG_PREFIX, doc.getId());
        this.documentoEmEdicao = doc;
        this.arquivoPendenteUpload = null;

        this.lblTituloPanel.setText("✏️ Editando classificação do documento:");
        this.lblNomeArquivo.setText(new File(doc.getArquivoPath()).getName());
        this.comboTipoDocumento.setValue(doc.getTipoDocumento());

        abrirPainelAzul();
    }

    private void abrirPainelAzul() {
        alternarVisibilidadePainel(false);
        if (scrollPrincipal != null)
            scrollPrincipal.setVvalue(0.0);
    }

    @FXML private void cancelarUploadInline() {
        log.trace("{} [UI] Cancelando operação inline.", LOG_PREFIX);
        double posicaoScroll = scrollPrincipal != null ? scrollPrincipal.getVvalue() : 0.0;

        this.arquivoPendenteUpload = null;
        this.documentoEmEdicao = null;

        alternarVisibilidadePainel(true);
        dropZone.requestFocus();

        if (scrollPrincipal != null)
            Platform.runLater(() -> scrollPrincipal.setVvalue(posicaoScroll));
    }

    @FXML private void confirmarUploadInline() {
        String tipoSelecionado = comboTipoDocumento.getValue();
        log.info("{} [TELEMETRIA] Confirmando operação inline. Tipo: {}", LOG_PREFIX, tipoSelecionado);

        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            navigator.notificarAviso("Por favor, selecione um tipo de documento.");
            return;
        }

        navigator.mostrarLoading("Processando documento...");
        AsyncUtils.executarTaskAsync(() -> {
            if (documentoEmEdicao != null) {
                return documentoFacade.atualizarTipoDocumento(documentoEmEdicao.getId(), tipoSelecionado);
            } else {
                return documentoFacade.salvarNovoDocumento(arquivoPendenteUpload, tipoSelecionado, proponenteAtual);
            }
        }, sucesso -> {
            navigator.ocultarLoading();
            log.info("{} [AUDITORIA] Operação de documento concluída com sucesso.", LOG_PREFIX);
            navigator.notificarSucesso(
                    documentoEmEdicao != null ? "Classificação atualizada!" : "Documento anexado com sucesso!");
            atualizarTabela();
            cancelarUploadInline();
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [AUDITORIA] Erro na operação de documento: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso(erro.getMessage());
        });
    }

    private void alternarVisibilidadePainel(boolean mostrarDropZone) {
        dropZone.setVisible(mostrarDropZone);
        dropZone.setManaged(mostrarDropZone);
        panelClassificacao.setVisible(!mostrarDropZone);
        panelClassificacao.setManaged(!mostrarDropZone);
    }

    // ==========================================================================================
    // MÓDULO 8: CONFIGURAÇÃO DA TABELA E AÇÕES
    // ==========================================================================================
    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da tabela.", LOG_PREFIX);
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoDocumento()));
        configurarColunaStatus();
        configurarColunaAcoes();
    }

    private void configurarColunaStatus() {
        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(criarLinkStatus(getTableRow().getItem()));
                }
            }
        });
    }

    private Hyperlink criarLinkStatus(DocumentoProponenteModel doc) {
        boolean verificado = doc.getVerificado() != null && doc.getVerificado();
        Hyperlink linkStatus = new Hyperlink(verificado ? "✅ Verificado" : "⏳ Pendente");
        linkStatus.setStyle(verificado ? STYLE_STATUS_OK : STYLE_STATUS_PENDENTE);

        linkStatus.setOnAction(event -> {
            event.consume();
            log.info("{} [TELEMETRIA] Solicitando alteração de status de verificação. ID: {}", LOG_PREFIX, doc.getId());

            AsyncUtils.executarTaskAsync(() -> documentoFacade.alternarStatusVerificacao(doc.getId()), atualizado -> {
                doc.setVerificado(atualizado.getVerificado());
                tableDocumentos.refresh();
                log.debug("{} [AUDITORIA] Status de verificação alterado para ID: {}", LOG_PREFIX, doc.getId());
            }, erro -> {
                log.error("{} [AUDITORIA] Erro ao alterar status: {}", LOG_PREFIX, erro.getMessage());
                navigator.notificarAviso("Erro ao atualizar status: " + erro.getMessage());
            });
        });

        return linkStatus;
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotaoAcao("👁️", "flat");
            private final Button btnEditar = criarBotaoAcao("✏️", "flat");
            private final Button btnExcluir = criarBotaoAcao("🗑️", "flat", "danger");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> {
                    e.consume();
                    handleVisualizarArquivo(getDocAtual());
                });
                btnEditar.setOnAction(e -> {
                    e.consume();
                    prepararEdicaoEmbutida(getDocAtual());
                });
                btnExcluir.setOnAction(e -> {
                    e.consume();
                    solicitarExclusaoDeDocumento(getDocAtual());
                });
            }

            private DocumentoProponenteModel getDocAtual() {
                return getTableView().getItems().get(getIndex());
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : container);
            }
        });
    }

    private Button criarBotaoAcao(String icone, String... classesCss) {
        Button btn = new Button(icone);
        btn.getStyleClass().addAll(classesCss);
        return btn;
    }

    private void handleVisualizarArquivo(DocumentoProponenteModel doc) {
        if (doc == null)
            return;
        log.info("{} [TELEMETRIA] Solicitando visualização do arquivo ID: {}", LOG_PREFIX, doc.getId());

        if (documentoFacade.validarExistenciaArquivoFisico(doc)) {
            hostServices.showDocument(new File(doc.getArquivoPath()).toURI().toString());
        } else {
            log.warn("{} [AUDITORIA] Arquivo físico não localizado para o documento ID: {}", LOG_PREFIX, doc.getId());
            navigator.notificarAviso("O arquivo físico não foi encontrado no disco.");
        }
    }

    // ==========================================================================================
    // MÓDULO 9: EXCLUSÃO GLOBAL (NAVIGATOR)
    // ==========================================================================================

    /**
     * Solicita confirmação global via Navigator para exclusão de um documento.
     */
    private void solicitarExclusaoDeDocumento(DocumentoProponenteModel doc) {
        log.info("{} [TELEMETRIA] Solicitando confirmação global para exclusão. ID: {}", LOG_PREFIX, doc.getId());

        navigator.solicitarConfirmacao("🗑️ Excluir Documento",
                "Tem certeza que deseja apagar permanentemente este documento?\nO arquivo físico será removido do sistema.",
                "Sim, Excluir", "#c62828", () -> executarExclusaoReal(doc));
    }

    private void executarExclusaoReal(DocumentoProponenteModel doc) {
        log.info("{} [TELEMETRIA] Executando exclusão assíncrona do documento ID: {}", LOG_PREFIX, doc.getId());
        navigator.mostrarLoading("Removendo arquivo...");

        AsyncUtils.executarTaskAsync(() -> {
            documentoFacade.excluirDocumento(doc.getId());
            return null;
        }, sucesso -> {
            navigator.ocultarLoading();
            log.info("{} [AUDITORIA] Documento ID {} removido com sucesso.", LOG_PREFIX, doc.getId());
            tableDocumentos.getItems().remove(doc);
            navigator.notificarSucesso("Documento excluído permanentemente.");
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [AUDITORIA] Falha ao excluir documento: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso("Erro ao apagar: " + erro.getMessage());
        });
    }
}
