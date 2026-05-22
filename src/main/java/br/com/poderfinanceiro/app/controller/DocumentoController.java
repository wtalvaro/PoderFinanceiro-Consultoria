package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.DocumentoService;
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

@Component
public class DocumentoController {

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
    private final MainController mainController;

    private ProponenteModel proponenteAtual;
    private File arquivoPendenteUpload;
    private DocumentoProponenteModel documentoEmEdicao;
    private DocumentoProponenteModel documentoParaExcluir;

    public DocumentoController(DocumentoService documentoService, MainController mainController) {
        this.documentoService = documentoService;
        this.mainController = mainController;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarTabela();
        comboTipoDocumento.setItems(FXCollections.observableArrayList(TIPOS_DOCUMENTO));
    }

    public void carregarDocumentos(ProponenteModel proponente) {
        this.proponenteAtual = proponente;
        cancelarUploadInline();

        if (proponente != null && proponente.getId() != null) {
            atualizarTabela();
            dropZone.setDisable(false);
        } else {
            tableDocumentos.getItems().clear();
            dropZone.setDisable(true);
            mostrarAviso("Salve o atendimento primeiro para poder anexar documentos.", false);
        }
    }

    private void atualizarTabela() {
        if (proponenteAtual != null) {
            List<DocumentoProponenteModel> docs = documentoService.listarDoProponente(proponenteAtual.getId());
            tableDocumentos.setItems(FXCollections.observableArrayList(docs));
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
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        dropZone.setStyle(STYLE_DROP_INACTIVE);

        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            success = true;
            File file = event.getDragboard().getFiles().get(0);
            prepararUploadEmbutido(file);
        }

        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleFileSelection() {
        if (!isProponenteValido())
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Documento");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

        File file = fileChooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            prepararUploadEmbutido(file);
        }
    }

    private boolean isProponenteValido() {
        return proponenteAtual != null && proponenteAtual.getId() != null;
    }

    // =========================================================================
    // FLUXO DO PAINEL NATIVO (UPLOAD E EDIÇÃO)
    // =========================================================================
    private void prepararUploadEmbutido(File file) {
        this.arquivoPendenteUpload = file;
        this.documentoEmEdicao = null;

        this.lblTituloPanel.setText("Classifique o novo documento:");
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.getSelectionModel().clearSelection();

        abrirPainelAzul();
    }

    private void prepararEdicaoEmbutida(DocumentoProponenteModel doc) {
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

        if (scrollPrincipal != null) {
            scrollPrincipal.setVvalue(0.0);
        }
    }

    @FXML
    private void cancelarUploadInline() {
        double posicaoScroll = scrollPrincipal != null ? scrollPrincipal.getVvalue() : 0.0;

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

        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            mostrarAviso("Por favor, selecione um tipo de documento.", false);
            return;
        }

        try {
            if (documentoEmEdicao != null) {
                documentoService.atualizarTipoDocumento(documentoEmEdicao.getId(), tipoSelecionado);
                mostrarAviso("Documento atualizado com sucesso!", true);
            } else {
                documentoService.processarUpload(this.arquivoPendenteUpload, tipoSelecionado, proponenteAtual, null);
                mostrarAviso("Documento salvo com sucesso!", true);
            }

            atualizarTabela();
            cancelarUploadInline();

        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage(), false);
        } catch (Exception e) {
            mostrarAviso("Erro ao processar arquivo.", false);
            e.printStackTrace();
        }
    }

    private void alternarVisibilidadePainel(boolean mostrarDropZone) {
        dropZone.setVisible(mostrarDropZone);
        dropZone.setManaged(mostrarDropZone);
        panelClassificacao.setVisible(!mostrarDropZone);
        panelClassificacao.setManaged(!mostrarDropZone);
    }

    // =========================================================================
    // CONFIGURAÇÃO DA TABELA (SRP Aplicado)
    // =========================================================================
    private void configurarTabela() {
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
            try {
                DocumentoProponenteModel atualizado = documentoService.alternarVerificacao(doc.getId());
                doc.setVerificado(atualizado.getVerificado());
                tableDocumentos.refresh();
            } catch (Exception e) {
                mostrarAviso("Erro ao atualizar status: " + e.getMessage(), false);
            }
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
        File file = new File(doc.getArquivoPath());
        if (file.exists()) {
            mainController.getHostServices().showDocument(file.toURI().toString());
        } else {
            mostrarAviso("Erro: O arquivo físico não foi encontrado no disco.", false);
        }
    }

    private void solicitarExclusaoDeDocumento(DocumentoProponenteModel doc) {
        this.documentoParaExcluir = doc;
        overlayExclusao.setVisible(true);
    }

    // =========================================================================
    // LÓGICA DO OVERLAY DE EXCLUSÃO E AVISOS
    // =========================================================================
    @FXML
    private void cancelarExclusao() {
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML
    private void confirmarExclusao() {
        if (this.documentoParaExcluir != null) {
            try {
                // Prevenção da "Edição Fantasma"
                if (documentoEmEdicao != null && documentoEmEdicao.getId().equals(documentoParaExcluir.getId())) {
                    cancelarUploadInline();
                }

                documentoService.excluirDocumento(documentoParaExcluir.getId());
                tableDocumentos.getItems().remove(documentoParaExcluir);
                mostrarAviso("Documento apagado.", true);

            } catch (Exception e) {
                mostrarAviso("Erro ao apagar: " + e.getMessage(), false);
            } finally {
                cancelarExclusao();
            }
        }
    }

    private void mostrarAviso(String msg, boolean sucesso) {
        if (msg == null || msg.isBlank()) {
            lblAviso.setVisible(false);
            lblAviso.setManaged(false);
            return;
        }
        lblAviso.setText(msg);
        lblAviso.setStyle(sucesso ? STYLE_MSG_SUCESSO : STYLE_MSG_ERRO);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }
}