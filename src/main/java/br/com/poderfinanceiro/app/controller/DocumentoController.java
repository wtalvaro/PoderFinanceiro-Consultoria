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
    private Label lblNomeArquivo, lblTituloPanel;
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
    @FXML
    private VBox overlayExclusao;
    @FXML
    private ScrollPane scrollPrincipal;

    private DocumentoProponente documentoParaExcluir;
    private DocumentoProponente documentoEmEdicao;
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
        this.documentoEmEdicao = null; // Garante que é um documento NOVO

        this.lblTituloPanel.setText("Classifique o novo documento:");
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.getSelectionModel().clearSelection();

        abrirPainelAzul();
    }

    // NOVO MÉTODO: Abre o painel, mas com os dados do documento que já existe
    private void prepararEdicaoEmbutida(DocumentoProponente doc) {
        this.documentoEmEdicao = doc;
        this.arquivoPendenteUpload = null;

        this.lblTituloPanel.setText("✏️ Editando classificação do documento:");
        File file = new File(doc.getArquivoPath());
        this.lblNomeArquivo.setText(file.getName());
        this.comboTipoDocumento.setValue(doc.getTipoDocumento());

        abrirPainelAzul();
    }

    private void abrirPainelAzul() {
        dropZone.setVisible(false);
        dropZone.setManaged(false);
        panelClassificacao.setVisible(true);
        panelClassificacao.setManaged(true);
        mostrarAviso("", true);
        
        // O SEGREDO ESTÁ AQUI: Força a rolagem para o topo
        if (scrollPrincipal != null) {
            scrollPrincipal.setVvalue(0.0);
        }
    }

    @FXML
    private void cancelarUploadInline() {
        this.arquivoPendenteUpload = null;
        this.documentoEmEdicao = null;

        panelClassificacao.setVisible(false);
        panelClassificacao.setManaged(false);
        dropZone.setVisible(true);
        dropZone.setManaged(true);
    }

    @FXML
    private void confirmarUploadInline() {
        String tipoSelecionado = comboTipoDocumento.getValue();

        if (tipoSelecionado == null || tipoSelecionado.isEmpty()) {
            mostrarAviso("Por favor, selecione um tipo de documento.", false);
            return;
        }

        try {
            // SE FOR EDIÇÃO
            if (documentoEmEdicao != null) {
                documentoService.atualizarTipoDocumento(documentoEmEdicao.getId(), tipoSelecionado);
                mostrarAviso("Documento atualizado com sucesso!", true);
            }
            // SE FOR UPLOAD NOVO
            else {
                documentoService.processarUpload(this.arquivoPendenteUpload, tipoSelecionado, proponenteAtual);
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
            private final Button btnAbrir = new Button("👁️");
            private final Button btnEditar = new Button("✏️");
            private final Button btnExcluir = new Button("🗑️");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnExcluir);

            {
                btnAbrir.getStyleClass().add("flat");
                btnEditar.getStyleClass().add("flat");
                btnExcluir.getStyleClass().addAll("flat", "danger");

                btnAbrir.setOnAction(event -> {
                    DocumentoProponente doc = getTableView().getItems().get(getIndex());
                    File file = new File(doc.getArquivoPath());
                    if (file.exists()) {
                        mainController.getHostServices().showDocument(file.toURI().toString());
                    } else {
                        mostrarAviso("Erro: O arquivo físico não foi encontrado no disco.", false);
                    }
                });

                btnEditar.setOnAction(event -> {
                    DocumentoProponente doc = getTableView().getItems().get(getIndex());
                    prepararEdicaoEmbutida(doc);
                });

                // Ação de Excluir: Chama o OVERLAY em vez do Alert do sistema
                btnExcluir.setOnAction(event -> {
                    documentoParaExcluir = getTableView().getItems().get(getIndex());
                    overlayExclusao.setVisible(true); // Exibe a tela escura
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
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

    // ==========================================================
    // LÓGICA DO OVERLAY DE EXCLUSÃO
    // ==========================================================

    @FXML
    private void cancelarExclusao() {
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML
    private void confirmarExclusao() {
        if (this.documentoParaExcluir != null) {
            try {
                // 1. Remove do banco e do disco
                documentoService.excluirDocumento(documentoParaExcluir.getId());

                // 2. Remove da interface imediatamente
                tableDocumentos.getItems().remove(documentoParaExcluir);

                mostrarAviso("Documento apagado.", true);
            } catch (Exception e) {
                mostrarAviso("Erro ao apagar: " + e.getMessage(), false);
            } finally {
                // 3. Esconde o overlay e limpa a variável
                cancelarExclusao();
            }
        }
    }
}