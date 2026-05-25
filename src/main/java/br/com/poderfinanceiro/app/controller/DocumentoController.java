package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.DocumentoService;
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
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DocumentoController {
    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final List<String> TIPOS_DOCUMENTO = List.of(
            "RG", "CNH", "CPF", "Comprovante de Residência", "Contracheque", "Outros");

    // Estilos Visuais
    private static final String STYLE_DROP_ACTIVE = "-fx-border-color: #2196F3; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #e3f2fd;";
    private static final String STYLE_DROP_INACTIVE = "-fx-border-color: #aaaaaa; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #fafafa;";
    private static final String STYLE_STATUS_OK = "-fx-text-fill: #2e7d32; -fx-underline: false; -fx-font-weight: bold;";
    private static final String STYLE_STATUS_PENDENTE = "-fx-text-fill: #ffa000; -fx-underline: false; -fx-font-weight: bold;";
    private static final String STYLE_MSG_SUCESSO = "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
    private static final String STYLE_MSG_ERRO = "-fx-text-fill: #c62828; -fx-font-weight: bold;";

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private VBox dropZone;
    @FXML
    private VBox panelClassificacao;
    @FXML
    private Label lblNomeArquivo, lblTituloPanel, lblAviso;
    @FXML
    private ComboBox<String> comboTipoDocumento;
    @FXML
    private ScrollPane scrollPrincipal;
    @FXML
    private VBox overlayExclusao;

    @FXML
    private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colTipo;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colStatus;
    @FXML
    private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final DocumentoService documentoService;
    private final HostServices hostServices;

    private ProponenteModel proponenteAtual;
    private File arquivoPendenteUpload;
    private DocumentoProponenteModel documentoEmEdicao;
    private DocumentoProponenteModel documentoParaExcluir;

    public DocumentoController(DocumentoService documentoService, HostServices hostServices) {
        this.documentoService = documentoService;
        this.hostServices = hostServices;
        log.debug("[DOCUMENTO] Construtor: Controller instanciado");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[DOCUMENTO] initialize: Configurando componentes UI");
        configurarTabela();
        comboTipoDocumento.setItems(FXCollections.observableArrayList(TIPOS_DOCUMENTO));
        log.info("[DOCUMENTO] initialize: Configuração concluída");
    }

    public void carregarDocumentos(ProponenteModel proponente) {
        log.debug("[DOCUMENTO] carregarDocumentos: Carregando documentos para proponente ID={}",
                proponente != null ? proponente.getId() : "null");
        this.proponenteAtual = proponente;
        cancelarUploadInline();

        if (proponente != null && proponente.getId() != null) {
            atualizarTabela();
            dropZone.setDisable(false);
            log.debug("[DOCUMENTO] carregarDocumentos: Drop zone habilitada para proponente ativo");
        } else {
            tableDocumentos.getItems().clear();
            dropZone.setDisable(true);
            mostrarAviso("Salve o atendimento primeiro para poder anexar documentos.", false);
            log.warn("[DOCUMENTO] carregarDocumentos: Proponente inválido, drop zone desabilitada");
        }
    }

    private void atualizarTabela() {
        if (proponenteAtual != null) {
            List<DocumentoProponenteModel> docs = documentoService.listarDoProponente(proponenteAtual.getId());
            tableDocumentos.setItems(FXCollections.observableArrayList(docs));
            log.debug("[DOCUMENTO] atualizarTabela: {} documentos carregados para proponente ID={}", docs.size(),
                    proponenteAtual.getId());
        } else {
            log.warn("[DOCUMENTO] atualizarTabela: proponenteAtual nulo, não foi possível carregar documentos");
        }
    }

    // =========================================================================
    // LÓGICA DE ARRASTAR E SOLTAR (DRAG & DROP) E SELEÇÃO
    // =========================================================================
    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles() && isProponenteValido()) {
            event.acceptTransferModes(TransferMode.COPY);
            dropZone.setStyle(STYLE_DROP_ACTIVE);
            log.trace("[DOCUMENTO] handleDragOver: Drag aceito");
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        dropZone.setStyle(STYLE_DROP_INACTIVE);
        log.debug("[DOCUMENTO] handleDragDropped: Evento de drop recebido");

        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            success = true;
            File file = event.getDragboard().getFiles().get(0);
            log.info("[DOCUMENTO] handleDragDropped: Arquivo arrastado: {}", file.getName());
            prepararUploadEmbutido(file);
        } else {
            log.warn("[DOCUMENTO] handleDragDropped: Drop sem arquivos");
        }

        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleFileSelection() {
        if (!isProponenteValido()) {
            log.warn("[DOCUMENTO] handleFileSelection: Tentativa de seleção sem proponente válido");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Documento");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

        File file = fileChooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            log.info("[DOCUMENTO] handleFileSelection: Arquivo selecionado: {}", file.getName());
            prepararUploadEmbutido(file);
        } else {
            log.debug("[DOCUMENTO] handleFileSelection: Nenhum arquivo selecionado");
        }
    }

    private boolean isProponenteValido() {
        boolean valido = proponenteAtual != null && proponenteAtual.getId() != null;
        log.trace("[DOCUMENTO] isProponenteValido: {}", valido);
        return valido;
    }

    // =========================================================================
    // FLUXO DO PAINEL NATIVO (UPLOAD E EDIÇÃO)
    // =========================================================================
    private void prepararUploadEmbutido(File file) {
        log.debug("[DOCUMENTO] prepararUploadEmbutido: Novo upload - arquivo='{}'", file.getName());
        this.arquivoPendenteUpload = file;
        this.documentoEmEdicao = null;

        this.lblTituloPanel.setText("Classifique o novo documento:");
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.getSelectionModel().clearSelection();

        abrirPainelAzul();
    }

    private void prepararEdicaoEmbutida(DocumentoProponenteModel doc) {
        log.debug("[DOCUMENTO] prepararEdicaoEmbutida: Editando documento ID={}", doc.getId());
        this.documentoEmEdicao = doc;
        this.arquivoPendenteUpload = null;

        this.lblTituloPanel.setText("✏️ Editando classificação do documento:");
        this.lblNomeArquivo.setText(new File(doc.getArquivoPath()).getName());
        this.comboTipoDocumento.setValue(doc.getTipoDocumento());

        abrirPainelAzul();
    }

    private void abrirPainelAzul() {
        log.debug("[DOCUMENTO] abrirPainelAzul: Exibindo painel de classificação");
        alternarVisibilidadePainel(false);
        mostrarAviso("", true);

        if (scrollPrincipal != null) {
            scrollPrincipal.setVvalue(0.0);
        }
    }

    @FXML
    private void cancelarUploadInline() {
        double posicaoScroll = scrollPrincipal != null ? scrollPrincipal.getVvalue() : 0.0;
        log.debug("[DOCUMENTO] cancelarUploadInline: Cancelando operação atual (scroll={})", posicaoScroll);

        this.arquivoPendenteUpload = null;
        this.documentoEmEdicao = null;

        alternarVisibilidadePainel(true);
        dropZone.requestFocus();

        if (scrollPrincipal != null) {
            Platform.runLater(() -> scrollPrincipal.setVvalue(posicaoScroll));
        }
    }

    @FXML
    private void confirmarUploadInline() {
        String tipoSelecionado = comboTipoDocumento.getValue();
        log.debug("[DOCUMENTO] confirmarUploadInline: Tipo selecionado='{}'", tipoSelecionado);

        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            log.warn("[DOCUMENTO] confirmarUploadInline: Tipo de documento não selecionado");
            mostrarAviso("Por favor, selecione um tipo de documento.", false);
            return;
        }

        try {
            if (documentoEmEdicao != null) {
                log.info("[DOCUMENTO] confirmarUploadInline: Atualizando tipo do documento ID={} para '{}'",
                        documentoEmEdicao.getId(), tipoSelecionado);
                documentoService.atualizarTipoDocumento(documentoEmEdicao.getId(), tipoSelecionado);
                mostrarAviso("Documento atualizado com sucesso!", true);
            } else {
                log.info("[DOCUMENTO] confirmarUploadInline: Realizando upload do arquivo '{}' como tipo '{}'",
                        arquivoPendenteUpload.getName(), tipoSelecionado);
                documentoService.processarUpload(this.arquivoPendenteUpload, tipoSelecionado, proponenteAtual, null);
                mostrarAviso("Documento salvo com sucesso!", true);
            }

            atualizarTabela();
            cancelarUploadInline();

        } catch (IllegalArgumentException ex) {
            log.warn("[DOCUMENTO] confirmarUploadInline: Argumento inválido - {}", ex.getMessage());
            mostrarAviso(ex.getMessage(), false);
        } catch (Exception ex) {
            log.error("[DOCUMENTO] confirmarUploadInline: Erro ao processar arquivo", ex);
            mostrarAviso("Erro ao processar arquivo.", false);
        }
    }

    private void alternarVisibilidadePainel(boolean mostrarDropZone) {
        log.trace("[DOCUMENTO] alternarVisibilidadePainel: mostrarDropZone={}", mostrarDropZone);
        dropZone.setVisible(mostrarDropZone);
        dropZone.setManaged(mostrarDropZone);
        panelClassificacao.setVisible(!mostrarDropZone);
        panelClassificacao.setManaged(!mostrarDropZone);
    }

    // =========================================================================
    // CONFIGURAÇÃO DA TABELA (SRP Aplicado)
    // =========================================================================
    private void configurarTabela() {
        log.debug("[DOCUMENTO] configurarTabela: Configurando colunas da tabela de documentos");
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoDocumento()));
        configurarColunaStatus();
        configurarColunaAcoes();
    }

    private void configurarColunaStatus() {
        log.debug("[DOCUMENTO] configurarColunaStatus: Configurando coluna de status com hyperlinks");
        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    DocumentoProponenteModel doc = getTableRow().getItem();
                    setGraphic(criarLinkStatus(doc));
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
            log.info("[DOCUMENTO] Alternando status de verificação do documento ID={} (atual={})", doc.getId(),
                    verificado);
            try {
                DocumentoProponenteModel atualizado = documentoService.alternarVerificacao(doc.getId());
                doc.setVerificado(atualizado.getVerificado());
                tableDocumentos.refresh();
                log.debug("[DOCUMENTO] Status alterado para verificado={}", atualizado.getVerificado());
            } catch (Exception e) {
                log.error("[DOCUMENTO] Erro ao alternar status do documento ID={}", doc.getId(), e);
                mostrarAviso("Erro ao atualizar status: " + e.getMessage(), false);
            }
        });

        return linkStatus;
    }

    private void configurarColunaAcoes() {
        log.debug("[DOCUMENTO] configurarColunaAcoes: Configurando coluna de ações (visualizar, editar, excluir)");
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotaoAcao("👁️", "flat");
            private final Button btnEditar = criarBotaoAcao("✏️", "flat");
            private final Button btnExcluir = criarBotaoAcao("🗑️", "flat", "danger");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> {
                    e.consume();
                    DocumentoProponenteModel doc = getDocAtual();
                    log.debug("[DOCUMENTO] Botão 'Visualizar' clicado para documento ID={}",
                            doc != null ? doc.getId() : "null");
                    handleVisualizarArquivo(doc);
                });
                btnEditar.setOnAction(e -> {
                    e.consume();
                    DocumentoProponenteModel doc = getDocAtual();
                    log.debug("[DOCUMENTO] Botão 'Editar' clicado para documento ID={}",
                            doc != null ? doc.getId() : "null");
                    prepararEdicaoEmbutida(doc);
                });
                btnExcluir.setOnAction(e -> {
                    e.consume();
                    DocumentoProponenteModel doc = getDocAtual();
                    log.debug("[DOCUMENTO] Botão 'Excluir' clicado para documento ID={}",
                            doc != null ? doc.getId() : "null");
                    solicitarExclusaoDeDocumento(doc);
                });
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

    private Button criarBotaoAcao(String icone, String... classesCss) {
        Button btn = new Button(icone);
        btn.getStyleClass().addAll(classesCss);
        return btn;
    }

    private void handleVisualizarArquivo(DocumentoProponenteModel doc) {
        if (doc == null) {
            log.warn("[DOCUMENTO] handleVisualizarArquivo: Documento nulo");
            return;
        }
        File file = new File(doc.getArquivoPath());
        log.info("[DOCUMENTO] handleVisualizarArquivo: Tentando abrir arquivo '{}' para documento ID={}",
                file.getAbsolutePath(), doc.getId());
        if (file.exists()) {
            hostServices.showDocument(file.toURI().toString());
            log.debug("[DOCUMENTO] handleVisualizarArquivo: Arquivo aberto com sucesso");
        } else {
            log.warn("[DOCUMENTO] handleVisualizarArquivo: Arquivo não encontrado no disco: {}",
                    file.getAbsolutePath());
            mostrarAviso("Erro: O arquivo físico não foi encontrado no disco.", false);
        }
    }

    private void solicitarExclusaoDeDocumento(DocumentoProponenteModel doc) {
        log.info("[DOCUMENTO] solicitarExclusaoDeDocumento: Solicitando confirmação para exclusão do documento ID={}",
                doc.getId());
        this.documentoParaExcluir = doc;
        overlayExclusao.setVisible(true);
    }

    // =========================================================================
    // LÓGICA DO OVERLAY DE EXCLUSÃO E AVISOS
    // =========================================================================
    @FXML
    private void cancelarExclusao() {
        log.debug("[DOCUMENTO] cancelarExclusao: Exclusão cancelada pelo usuário");
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML
    private void confirmarExclusao() {
        if (this.documentoParaExcluir != null) {
            log.info("[DOCUMENTO] confirmarExclusao: Confirmando exclusão do documento ID={}",
                    documentoParaExcluir.getId());
            try {
                // Prevenção da "Edição Fantasma"
                if (documentoEmEdicao != null && documentoEmEdicao.getId().equals(documentoParaExcluir.getId())) {
                    log.debug(
                            "[DOCUMENTO] confirmarExclusao: Cancelando edição inline devido à exclusão do documento em edição");
                    cancelarUploadInline();
                }

                documentoService.excluirDocumento(documentoParaExcluir.getId());
                tableDocumentos.getItems().remove(documentoParaExcluir);
                mostrarAviso("Documento apagado.", true);
                log.info("[DOCUMENTO] confirmarExclusao: Documento ID={} excluído com sucesso",
                        documentoParaExcluir.getId());

            } catch (Exception e) {
                log.error("[DOCUMENTO] confirmarExclusao: Erro ao excluir documento ID={}",
                        documentoParaExcluir.getId(), e);
                mostrarAviso("Erro ao apagar: " + e.getMessage(), false);
            } finally {
                cancelarExclusao();
            }
        } else {
            log.warn("[DOCUMENTO] confirmarExclusao: Nenhum documento selecionado para exclusão");
        }
    }

    private void mostrarAviso(String msg, boolean sucesso) {
        if (msg == null || msg.isBlank()) {
            log.trace("[DOCUMENTO] mostrarAviso: Mensagem vazia, ocultando aviso");
            lblAviso.setVisible(false);
            lblAviso.setManaged(false);
            return;
        }
        log.debug("[DOCUMENTO] mostrarAviso: {} - {}", sucesso ? "SUCESSO" : "ERRO", msg);
        lblAviso.setText(msg);
        lblAviso.setStyle(sucesso ? STYLE_MSG_SUCESSO : STYLE_MSG_ERRO);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }
}