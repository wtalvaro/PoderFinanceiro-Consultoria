package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.DocumentoProponente;
import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.service.DocumentoService;
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

    @FXML
    private VBox dropZone;
    @FXML
    private VBox panelClassificacao; // Nosso novo painel
    @FXML
    private Label lblNomeArquivo;
    @FXML
    private ComboBox<String> comboTipoDocumento;

    @FXML
    private TableView<DocumentoProponente> tableDocumentos;
    @FXML
    private TableColumn<DocumentoProponente, String> colTipo;
    @FXML
    private TableColumn<DocumentoProponente, String> colStatus;
    @FXML
    private TableColumn<DocumentoProponente, Void> colAcoes;
    @FXML
    private Label lblAviso;

    private final DocumentoService documentoService;
    private final MainController mainController;

    private Proponente proponenteAtual;

    // Variável para segurar o arquivo enquanto o usuário escolhe o tipo na
    // interface
    private File arquivoPendenteUpload;

    public DocumentoController(DocumentoService documentoService, MainController mainController) {
        this.documentoService = documentoService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        configurarTabela();

        // Popula o ComboBox nativo
        comboTipoDocumento.setItems(FXCollections.observableArrayList(
                "RG", "CNH", "CPF", "Comprovante de Residência", "Contracheque", "Outros"));
    }

    public void carregarDocumentos(Proponente proponente) {
        this.proponenteAtual = proponente;
        cancelarUploadInline(); // Garante que o painel de upload é fechado se mudarmos de aba

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
            List<DocumentoProponente> docs = documentoService.listarDoProponente(proponenteAtual.getId());
            tableDocumentos.setItems(FXCollections.observableArrayList(docs));
        }
    }

    // ==========================================================
    // LÓGICA DE ARRASTAR E SOLTAR
    // ==========================================================

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles() && proponenteAtual != null && proponenteAtual.getId() != null) {
            event.acceptTransferModes(TransferMode.COPY);
            dropZone.setStyle(
                    "-fx-border-color: #2196F3; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #e3f2fd;");
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        dropZone.setStyle(
                "-fx-border-color: #aaaaaa; -fx-border-style: dashed; -fx-border-width: 2; -fx-background-color: #fafafa;");

        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            success = true;
            // Pegamos apenas o primeiro arquivo para simplificar a classificação inline
            File file = event.getDragboard().getFiles().get(0);
            prepararUploadEmbutido(file);
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleFileSelection() {
        if (proponenteAtual == null || proponenteAtual.getId() == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Documento");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

        File file = fileChooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            prepararUploadEmbutido(file);
        }
    }

    // ==========================================================
    // FLUXO DO PAINEL NATIVO (Adeus Dialogs do Sistema)
    // ==========================================================

    private void prepararUploadEmbutido(File file) {
        this.arquivoPendenteUpload = file;
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.getSelectionModel().clearSelection();

        // Alterna as views: esconde a dropzone e mostra o painel
        dropZone.setVisible(false);
        dropZone.setManaged(false);

        panelClassificacao.setVisible(true);
        panelClassificacao.setManaged(true);

        mostrarAviso("", true); // Limpa avisos anteriores
    }

    @FXML
    private void confirmarUploadInline() {
        String tipoSelecionado = comboTipoDocumento.getValue();

        if (tipoSelecionado == null || tipoSelecionado.isEmpty()) {
            mostrarAviso("Por favor, selecione um tipo de documento.", false);
            return;
        }

        try {
            documentoService.processarUpload(this.arquivoPendenteUpload, tipoSelecionado, proponenteAtual);
            atualizarTabela();
            mostrarAviso("Documento salvo com sucesso!", true);
            cancelarUploadInline(); // Volta a tela ao normal
        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage(), false);
        } catch (Exception e) {
            mostrarAviso("Erro ao processar arquivo.", false);
            e.printStackTrace();
        }
    }

    @FXML
    private void cancelarUploadInline() {
        this.arquivoPendenteUpload = null;

        // Alterna as views de volta
        panelClassificacao.setVisible(false);
        panelClassificacao.setManaged(false);

        dropZone.setVisible(true);
        dropZone.setManaged(true);
    }

    // ==========================================================
    // CONFIGURAÇÃO DA TABELA
    // ==========================================================

    private void configurarTabela() {
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoDocumento()));

        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    DocumentoProponente doc = getTableRow().getItem();

                    // Criamos um "botão" que parece um Label para ser discreto mas clicável
                    Hyperlink linkStatus = new Hyperlink(doc.getVerificado() ? "✅ Verificado" : "⏳ Pendente");

                    // Estilização dinâmica baseada no estado
                    if (doc.getVerificado()) {
                        linkStatus.setStyle("-fx-text-fill: #2e7d32; -fx-underline: false; -fx-font-weight: bold;");
                    } else {
                        linkStatus.setStyle("-fx-text-fill: #ffa000; -fx-underline: false; -fx-font-weight: bold;");
                    }

                    linkStatus.setOnAction(event -> {
                        try {
                            // 1. Atualiza no Banco via Serviço
                            DocumentoProponente atualizado = documentoService.alternarVerificacao(doc.getId());

                            // 2. Sincroniza o objeto da lista (sem recarregar do banco)
                            doc.setVerificado(atualizado.getVerificado());

                            // 3. O SEGREDO: Repinta a tabela instantaneamente
                            tableDocumentos.refresh();

                        } catch (Exception e) {
                            mostrarAviso("Erro ao atualizar status: " + e.getMessage(), false);
                        }
                    });

                    setGraphic(linkStatus);
                }
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️ Abrir");

            {
                btnAbrir.setOnAction(event -> {
                    DocumentoProponente doc = getTableView().getItems().get(getIndex());
                    File file = new File(doc.getArquivoPath());

                    if (file.exists()) {
                        // Usando o HostServices do MainController de forma elegante!
                        mainController.getHostServices().showDocument(file.toURI().toString());
                    } else {
                        mostrarAviso("Erro: O arquivo físico não foi encontrado no disco.", false);
                    }
                });
                btnAbrir.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3; -fx-cursor: hand;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else
                    setGraphic(new HBox(btnAbrir));
            }
        });
    }

    private void mostrarAviso(String msg, boolean sucesso) {
        if (msg.isEmpty()) {
            lblAviso.setVisible(false);
            lblAviso.setManaged(false);
            return;
        }
        lblAviso.setText(msg);
        lblAviso.setStyle(sucesso ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }
}