package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.facade.IDocumentoFacade;
import br.com.poderfinanceiro.app.util.AsyncUtils;
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
 * persistência, I/O de arquivos e regras de negócio para a
 * {@link IDocumentoFacade}.
 * </p>
 */
@Component
public class DocumentoController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);
    private static final String LOG_PREFIX = "[DocumentoController]";

    private static final List<String> TIPOS_DOCUMENTO = List.of("RG", "CNH", "CPF", "Comprovante de Residência", "Contracheque", "Outros");

    private static final String STYLE_DROP_ACTIVE = "-fx-border-color: #2196F3; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #e3f2fd;";
    private static final String STYLE_DROP_INACTIVE = "-fx-border-color: #aaaaaa; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #fafafa;";
    private static final String STYLE_STATUS_OK = "-fx-text-fill: #2e7d32; -fx-underline: false; -fx-font-weight: bold;";
    private static final String STYLE_STATUS_PENDENTE = "-fx-text-fill: #ffa000; -fx-underline: false; -fx-font-weight: bold;";
    private static final String STYLE_MSG_SUCESSO = "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
    private static final String STYLE_MSG_ERRO = "-fx-text-fill: #c62828; -fx-font-weight: bold;";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IDocumentoFacade documentoFacade;
    private final HostServices hostServices;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private VBox dropZone;
    @FXML private VBox panelClassificacao;
    @FXML private Label lblNomeArquivo, lblTituloPanel, lblAviso;
    @FXML private ComboBox<String> comboTipoDocumento;
    @FXML private ScrollPane scrollPrincipal;
    @FXML private VBox overlayExclusao;

    @FXML private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML private TableColumn<DocumentoProponenteModel, String> colTipo, colStatus;
    @FXML private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private ProponenteModel proponenteAtual;
    private File arquivoPendenteUpload;
    private DocumentoProponenteModel documentoEmEdicao;
    private DocumentoProponenteModel documentoParaExcluir;

    public DocumentoController(IDocumentoFacade documentoFacade, HostServices hostServices) {
        this.documentoFacade = documentoFacade;
        this.hostServices = hostServices;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
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
            mostrarAviso("Salve o atendimento primeiro para poder anexar documentos.", false);
        }
    }

    private void atualizarTabela() {
        if (!isProponenteValido())
            return;

        AsyncUtils.executarTaskAsync(() -> documentoFacade.listarDocumentosDoProponente(proponenteAtual.getId()), docs -> {
            tableDocumentos.setItems(FXCollections.observableArrayList(docs));
            log.info("{} [TELEMETRIA] {} documentos carregados na tabela.", LOG_PREFIX, docs.size());
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
        if (!isProponenteValido())
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Documento");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

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
        mostrarAviso("", true);
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
            mostrarAviso("Por favor, selecione um tipo de documento.", false);
            return;
        }

        AsyncUtils.executarTaskAsync(() -> {
            if (documentoEmEdicao != null) {
                return documentoFacade.atualizarTipoDocumento(documentoEmEdicao.getId(), tipoSelecionado);
            } else {
                return documentoFacade.salvarNovoDocumento(arquivoPendenteUpload, tipoSelecionado, proponenteAtual);
            }
        }, sucesso -> {
            log.info("{} [AUDITORIA] Operação de documento concluída com sucesso.", LOG_PREFIX);
            mostrarAviso(documentoEmEdicao != null ? "Documento atualizado com sucesso!" : "Documento salvo com sucesso!", true);
            atualizarTabela();
            cancelarUploadInline();
        }, erro -> {
            log.error("{} [AUDITORIA] Erro na operação de documento: {}", LOG_PREFIX, erro.getMessage());
            mostrarAviso(erro.getMessage(), false);
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
                log.info("{} [AUDITORIA] Status alterado com sucesso.", LOG_PREFIX);
            }, erro -> {
                log.error("{} [AUDITORIA] Erro ao alterar status: {}", LOG_PREFIX, erro.getMessage());
                mostrarAviso("Erro ao atualizar status: " + erro.getMessage(), false);
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
        if (doc == null) {
            log.warn("{} [NEGOCIO] Tentativa de visualizar documento nulo ignorada.", LOG_PREFIX);
            return;
        }

        log.info("{} [TELEMETRIA] Solicitando visualização do arquivo ID: {}", LOG_PREFIX, doc.getId());

        if (documentoFacade.validarExistenciaArquivoFisico(doc)) {
            hostServices.showDocument(new File(doc.getArquivoPath()).toURI().toString());
        } else {
            log.warn("{} [AUDITORIA] Arquivo físico não encontrado no disco para o documento ID: {}", LOG_PREFIX, doc.getId());
            mostrarAviso("Erro: O arquivo físico não foi encontrado no disco.", false);
        }
    }

    // ==========================================================================================
    // MÓDULO 9: EXCLUSÃO E AVISOS
    // ==========================================================================================
    private void solicitarExclusaoDeDocumento(DocumentoProponenteModel doc) {
        log.info("{} [TELEMETRIA] Solicitando confirmação de exclusão. ID: {}", LOG_PREFIX, doc.getId());
        this.documentoParaExcluir = doc;
        overlayExclusao.setVisible(true);
    }

    @FXML private void cancelarExclusao() {
        log.trace("{} [UI] Exclusão cancelada pelo usuário.", LOG_PREFIX);
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML private void confirmarExclusao() {
        if (this.documentoParaExcluir == null)
            return;

        Long idDoc = documentoParaExcluir.getId();
        log.info("{} [TELEMETRIA] Confirmando exclusão do documento ID: {}", LOG_PREFIX, idDoc);

        if (documentoEmEdicao != null && documentoEmEdicao.getId().equals(idDoc)) {
            cancelarUploadInline();
        }

        AsyncUtils.executarTaskAsync(() -> {
            documentoFacade.excluirDocumento(idDoc);
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] Documento excluído com sucesso.", LOG_PREFIX);
            tableDocumentos.getItems().remove(documentoParaExcluir);
            mostrarAviso("Documento apagado.", true);
            cancelarExclusao();
        }, erro -> {
            log.error("{} [AUDITORIA] Erro ao excluir documento: {}", LOG_PREFIX, erro.getMessage());
            mostrarAviso("Erro ao apagar: " + erro.getMessage(), false);
            cancelarExclusao();
        });
    }

    private void mostrarAviso(String msg, boolean sucesso) {
        if (msg == null || msg.isBlank()) {
            lblAviso.setVisible(false);
            lblAviso.setManaged(false);
            return;
        }
        log.trace("{} [UI] Exibindo aviso: {}", LOG_PREFIX, msg);
        lblAviso.setText(msg);
        lblAviso.setStyle(sucesso ? STYLE_MSG_SUCESSO : STYLE_MSG_ERRO);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }
}
