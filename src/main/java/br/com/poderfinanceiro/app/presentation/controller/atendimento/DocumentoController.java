package br.com.poderfinanceiro.app.presentation.controller.atendimento;

import br.com.poderfinanceiro.app.application.facade.IDocumentoFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import javafx.application.HostServices;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
 * cliente.
 * Implementa o padrão <b>Humble Object</b>, utilizando utilitários Gold
 * Standard
 * para garantir integridade de I/O e fluidez via Virtual Threads.
 * </p>
 */
@Component
public class DocumentoController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);
    private static final String LOG_PREFIX = "[DocumentoController]";

    private static final List<String> TIPOS_DOCUMENTO = List.of(
            "RG", "CNH", "CPF", "Comprovante de Residência", "Contracheque", "Outros");

    private static final String STYLE_DROP_ACTIVE = "-fx-border-color: #2196F3; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #e3f2fd;";
    private static final String STYLE_DROP_INACTIVE = "-fx-border-color: #aaaaaa; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #fafafa;";
    private static final String STYLE_STATUS_OK = "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
    private static final String STYLE_STATUS_PENDENTE = "-fx-text-fill: #ffa000; -fx-font-weight: bold;";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IDocumentoFacade documentoFacade;
    private final HostServices hostServices;
    private final Navigator navigator;
    private final SummaryGeneratorUtils summaryUtils;

    public DocumentoController(IDocumentoFacade documentoFacade,
            HostServices hostServices,
            Navigator navigator,
            SummaryGeneratorUtils summaryUtils) {
        this.documentoFacade = documentoFacade;
        this.hostServices = hostServices;
        this.navigator = navigator;
        this.summaryUtils = summaryUtils;
        log.debug("{} [SISTEMA] Controlador de Documentos instanciado com suporte a SummaryUtils.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private VBox dropZone;
    @FXML
    private VBox panelClassificacao;
    @FXML
    private Label lblNomeArquivo, lblTituloPanel;
    @FXML
    private ComboBox<String> comboTipoDocumento;
    @FXML
    private ScrollPane scrollPrincipal;
    @FXML
    private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colTipo, colStatus;
    @FXML
    private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private ProponenteModel proponenteAtual;
    private File arquivoPendenteUpload;
    private DocumentoProponenteModel documentoEmEdicao;

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de gestão documental...", LOG_PREFIX);
        configurarTabela();
        comboTipoDocumento.setItems(FXCollections.observableArrayList(TIPOS_DOCUMENTO));
        log.debug("{} [SISTEMA] Inicialização da UI concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos do controlador de documentos.", LOG_PREFIX);
        this.proponenteAtual = null;
        this.arquivoPendenteUpload = null;
        this.documentoEmEdicao = null;
        tableDocumentos.getItems().clear();
    }

    public void carregarDocumentos(ProponenteModel proponente) {
        log.info("{} [TELEMETRIA] Solicitando carga de documentos. Proponente ID: {}", LOG_PREFIX,
                proponente != null ? proponente.getId() : "NOVO");
        this.proponenteAtual = proponente;
        cancelarUploadInline();

        if (isProponenteValido()) {
            atualizarTabela();
            dropZone.setDisable(false);
        } else {
            tableDocumentos.getItems().clear();
            dropZone.setDisable(true);
            log.debug("{} [NEGOCIO] Upload desabilitado: Proponente ainda não persistido.", LOG_PREFIX);
        }
    }

    private void atualizarTabela() {
        if (!isProponenteValido())
            return;

        AsyncUtils.executarTaskAsync(() -> documentoFacade.listarDocumentosDoProponente(proponenteAtual.getId()),
                docs -> {
                    tableDocumentos.setItems(FXCollections.observableArrayList(docs));
                    log.info("{} [TELEMETRIA] Tabela atualizada com {} documento(s).", LOG_PREFIX, docs.size());
                }, erro -> log.error("{} [SISTEMA] Falha ao listar documentos: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 6: DRAG & DROP E SELEÇÃO
    // ==========================================================================================
    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles() && isProponenteValido()) {
            event.acceptTransferModes(TransferMode.COPY);
            dropZone.setStyle(STYLE_DROP_ACTIVE);
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        dropZone.setStyle(STYLE_DROP_INACTIVE);
        if (event.getDragboard().hasFiles()) {
            File file = event.getDragboard().getFiles().get(0);
            log.info("{} [TELEMETRIA] Arquivo recebido via Drop: {}", LOG_PREFIX, file.getName());
            prepararUploadEmbutido(file);
            event.setDropCompleted(true);
        }
        event.consume();
    }

    @FXML
    private void handleFileSelection() {
        if (!isProponenteValido()) {
            log.warn("{} [NEGOCIO] Tentativa de seleção de arquivo sem proponente ativo.", LOG_PREFIX);
            navigator.notificarAviso("Salve o atendimento primeiro para poder anexar documentos.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Documento");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos Suportados", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

        File file = fileChooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            log.info("{} [TELEMETRIA] Arquivo selecionado via Explorer: {}", LOG_PREFIX, file.getName());
            prepararUploadEmbutido(file);
        }
    }

    private boolean isProponenteValido() {
        return proponenteAtual != null && proponenteAtual.getId() != null;
    }

    // ==========================================================================================
    // MÓDULO 7: FLUXO DE UPLOAD E EDIÇÃO
    // ==========================================================================================
    private void prepararUploadEmbutido(File file) {
        log.trace("{} [UI] Exibindo painel de classificação para novo arquivo.", LOG_PREFIX);
        this.arquivoPendenteUpload = file;
        this.documentoEmEdicao = null;
        this.lblTituloPanel.setText("Classifique o novo documento:");
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.getSelectionModel().clearSelection();
        alternarVisibilidadePainel(false);
    }

    private void prepararEdicaoEmbutida(DocumentoProponenteModel doc) {
        log.trace("{} [UI] Exibindo painel de reclassificação para ID: {}", LOG_PREFIX, doc.getId());
        this.documentoEmEdicao = doc;
        this.arquivoPendenteUpload = null;
        this.lblTituloPanel.setText("✏️ Editando classificação:");
        this.lblNomeArquivo.setText(new File(doc.getArquivoPath()).getName());
        this.comboTipoDocumento.setValue(doc.getTipoDocumento());
        alternarVisibilidadePainel(false);
    }

    @FXML
    private void confirmarUploadInline() {
        String tipo = comboTipoDocumento.getValue();
        log.info("{} [TELEMETRIA] Confirmando operação de documento. Tipo: {}", LOG_PREFIX, tipo);

        if (tipo == null || tipo.isBlank()) {
            log.warn("{} [NEGOCIO] Validação falhou: Tipo de documento não selecionado.", LOG_PREFIX);
            navigator.notificarAviso("Por favor, selecione um tipo de documento.");
            return;
        }

        navigator.mostrarLoading("Processando arquivo...");
        AsyncUtils.executarTaskAsync(() -> {
            if (documentoEmEdicao != null) {
                return documentoFacade.atualizarTipoDocumento(documentoEmEdicao.getId(), tipo);
            } else {
                return documentoFacade.salvarNovoDocumento(arquivoPendenteUpload, tipo, proponenteAtual);
            }
        }, sucesso -> {
            navigator.ocultarLoading();
            log.info("{} [AUDITORIA] Documento processado com sucesso. ID: {}", LOG_PREFIX, sucesso.getId());
            navigator.notificarSucesso(documentoEmEdicao != null ? "Classificação atualizada!" : "Documento anexado!");
            atualizarTabela();
            cancelarUploadInline();
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [AUDITORIA] Falha na operação de documento: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso(erro.getMessage());
        });
    }

    @FXML
    private void cancelarUploadInline() {
        log.trace("{} [UI] Cancelando operação e resetando painel.", LOG_PREFIX);
        this.arquivoPendenteUpload = null;
        this.documentoEmEdicao = null;
        alternarVisibilidadePainel(true);
    }

    private void alternarVisibilidadePainel(boolean mostrarDropZone) {
        dropZone.setVisible(mostrarDropZone);
        dropZone.setManaged(mostrarDropZone);
        panelClassificacao.setVisible(!mostrarDropZone);
        panelClassificacao.setManaged(!mostrarDropZone);
        if (!mostrarDropZone && scrollPrincipal != null)
            scrollPrincipal.setVvalue(0.0);
    }

    // ==========================================================================================
    // MÓDULO 8: CONFIGURAÇÃO DA TABELA E AÇÕES
    // ==========================================================================================
    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da TableView.", LOG_PREFIX);
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoDocumento()));
        configurarColunaStatus();
        configurarColunaAcoes();
    }

    private void configurarColunaStatus() {
        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(criarLinkStatus(getTableRow().getItem()));
                }
            }
        });
    }

    private Hyperlink criarLinkStatus(DocumentoProponenteModel doc) {
        boolean verificado = Boolean.TRUE.equals(doc.getVerificado());
        Hyperlink link = new Hyperlink(verificado ? "✅ Verificado" : "⏳ Pendente");
        link.setStyle(verificado ? STYLE_STATUS_OK : STYLE_STATUS_PENDENTE);

        link.setOnAction(event -> {
            log.info("{} [TELEMETRIA] Solicitando alteração de status. ID: {}", LOG_PREFIX, doc.getId());
            AsyncUtils.executarTaskAsync(() -> documentoFacade.alternarStatusVerificacao(doc.getId()), atualizado -> {
                doc.setVerificado(atualizado.getVerificado());
                tableDocumentos.refresh();
                log.info("{} [AUDITORIA] Status de verificação atualizado para ID: {}", LOG_PREFIX, doc.getId());
            }, erro -> log.error("{} [AUDITORIA] Erro ao alterar status: {}", LOG_PREFIX, erro.getMessage()));
        });
        return link;
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotao("👁️", "flat");
            private final Button btnEditar = criarBotao("✏️", "flat");
            private final Button btnCopiar = criarBotao("📋", "flat"); // Novo: Copiar para IA
            private final Button btnExcluir = criarBotao("🗑️", "flat", "danger");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnCopiar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> handleVisualizarArquivo(getDocAtual()));
                btnEditar.setOnAction(e -> prepararEdicaoEmbutida(getDocAtual()));
                btnCopiar.setOnAction(e -> handleCopiarContextoIA(getDocAtual()));
                btnExcluir.setOnAction(e -> solicitarExclusao(getDocAtual()));
            }

            private DocumentoProponenteModel getDocAtual() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : container);
            }
        });
    }

    private Button criarBotao(String icone, String... classes) {
        Button btn = new Button(icone);
        btn.getStyleClass().addAll(classes);
        return btn;
    }

    private void handleVisualizarArquivo(DocumentoProponenteModel doc) {
        if (doc == null)
            return;
        log.info("{} [TELEMETRIA] Solicitando abertura de arquivo físico. ID: {}", LOG_PREFIX, doc.getId());

        if (documentoFacade.validarExistenciaArquivoFisico(doc)) {
            hostServices.showDocument(new File(doc.getArquivoPath()).toURI().toString());
            log.info("{} [AUDITORIA] Arquivo aberto no visualizador do SO.", LOG_PREFIX);
        } else {
            log.warn("{} [AUDITORIA] Falha: Arquivo físico não localizado no storage.", LOG_PREFIX);
            navigator.notificarAviso("O arquivo físico não foi encontrado no disco.");
        }
    }

    /**
     * Gera um contexto JSON híbrido (Documento + Proponente) para a IA.
     * Resolve o warning de 'summaryUtils is not used'.
     */
    private void handleCopiarContextoIA(DocumentoProponenteModel doc) {
        if (doc == null)
            return;
        log.info("{} [TELEMETRIA] Gerando contexto expandido para IA. Doc ID: {}", LOG_PREFIX, doc.getId());

        // USO EFETIVO DA DEPENDÊNCIA: Obtém o contexto do cliente via SummaryUtils
        String contextoClienteJson = summaryUtils.gerarJsonContextualParaIA(proponenteAtual, false);

        // Montagem de JSON estruturado usando Text Blocks (Java 25)
        String jsonFinal = """
                {
                  "documento": {
                    "id": %d,
                    "tipo": "%s",
                    "verificado": %b,
                    "hash": "%s"
                  },
                  "contextoCliente": %s
                }
                """.formatted(
                doc.getId(),
                doc.getTipoDocumento(),
                Boolean.TRUE.equals(doc.getVerificado()),
                doc.getHashSha256(),
                contextoClienteJson);

        ClipboardContent content = new ClipboardContent();
        content.putString(jsonFinal);
        Clipboard.getSystemClipboard().setContent(content);

        log.info("{} [AUDITORIA] Contexto híbrido (Documento + Cliente) copiado para o Clipboard.", LOG_PREFIX);
        navigator.notificarSucesso("Contexto completo copiado para IA.");
    }

    private void solicitarExclusao(DocumentoProponenteModel doc) {
        log.info("{} [TELEMETRIA] Solicitando confirmação de exclusão. ID: {}", LOG_PREFIX, doc.getId());
        navigator.solicitarConfirmacao("🗑️ Excluir Documento",
                "Deseja apagar permanentemente este arquivo do sistema?",
                "Sim, Excluir", "#c62828", () -> executarExclusao(doc));
    }

    private void executarExclusao(DocumentoProponenteModel doc) {
        log.info("{} [TELEMETRIA] Iniciando exclusão assíncrona. ID: {}", LOG_PREFIX, doc.getId());
        navigator.mostrarLoading("Removendo arquivo...");

        AsyncUtils.executarTaskAsync(() -> {
            documentoFacade.excluirDocumento(doc.getId());
            return null;
        }, sucesso -> {
            navigator.ocultarLoading();
            log.info("{} [AUDITORIA] Documento ID {} removido com sucesso.", LOG_PREFIX, doc.getId());
            tableDocumentos.getItems().remove(doc);
            navigator.notificarSucesso("Documento excluído.");
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [AUDITORIA] Erro ao excluir documento: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso("Erro ao apagar: " + erro.getMessage());
        });
    }
}
