package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.service.DocumentoService;
import br.com.poderfinanceiro.app.utils.ContatoUtils;
import br.com.poderfinanceiro.app.utils.DataUtils;
import br.com.poderfinanceiro.app.utils.DocumentoUtils;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
public class LeadController {

    private final LeadViewModel viewModel;
    private final DocumentoService documentoService;
    private final MainController mainController;

    @FXML
    private Label lblTituloTela;
    @FXML
    private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML
    private ComboBox<OrigemLeadModel> cbOrigem;
    @FXML
    private ComboBox<TipoVinculoModel> cbVinculo;
    @FXML
    private ComboBox<TipoRelacionamentoModel> cbClassificacao;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private ScrollPane scrollPrincipal;

    // --- ELEMENTOS DE DOCUMENTAÇÃO ---
    @FXML
    private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colTipoDocumento;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colDataUpload;
    @FXML
    private TableColumn<DocumentoProponenteModel, Void> colAcoes;
    @FXML
    private VBox overlayExclusao;
    @FXML
    private Label lblConfirmacaoExclusao;

    private DocumentoProponenteModel documentoParaExcluir;
    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();

    public LeadController(LeadViewModel viewModel, DocumentoService documentoService, MainController mainController) {
        this.viewModel = viewModel;
        this.documentoService = documentoService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        tableDocumentos.setItems(listaDocumentos);
        configurarListasEFormatores();
        estabelecerBindings();
        configurarAutoSelecao();
        configurarColunasDocumentos();

        lblTituloTela.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.trim().isEmpty()) ? "Cadastrar Novo Contato" : "Editando Contato: " + nome;
        }, viewModel.nomeProperty()));

        // 🚀 GATILHO: Quando carregar um cliente, busca os documentos dele
        viewModel.idProperty().addListener((obs, old, novoId) -> {
            carregarDocumentosDoLead(novoId);
        });
    }

    private void configurarListasEFormatores() {
        configurarCombo(cbOrigem, OrigemLeadModel.values(), OrigemLeadModel::fromString);
        configurarCombo(cbVinculo, TipoVinculoModel.values(), TipoVinculoModel::fromString);
        configurarCombo(cbClassificacao, TipoRelacionamentoModel.values(), TipoRelacionamentoModel::fromString);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new javafx.util.converter.LocalDateStringConverter(formatter, formatter));
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values,
            java.util.function.Function<String, T> searcher) {
        combo.getItems().setAll(values);
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T obj) {
                return (obj != null) ? obj.getLabel() : "";
            }

            @Override
            public T fromString(String str) {
                return searcher.apply(str);
            }
        });
    }

    private void estabelecerBindings() {
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());

        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());

        TextFormatter<BigDecimal> rendaFormatter = FinanceiroUtils.criarFormatadorMoeda();
        txtRenda.setTextFormatter(rendaFormatter);
        rendaFormatter.valueProperty().bindBidirectional(viewModel.rendaProperty());

        TextFormatter<String> cpfFormatter = DocumentoUtils.criarFormatadorCpf();
        txtCpf.setTextFormatter(cpfFormatter);
        cpfFormatter.valueProperty().bindBidirectional(viewModel.cpfProperty());

        TextFormatter<String> telefoneFormatter = ContatoUtils.criarFormatadorTelefone();
        txtTelefone.setTextFormatter(telefoneFormatter);
        telefoneFormatter.valueProperty().bindBidirectional(viewModel.telefoneProperty());
    }

    private void configurarAutoSelecao() {
        TextField[] camposFinanceiros = { txtRenda };
        for (TextField campo : camposFinanceiros) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    javafx.application.Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    // ==========================================================
    // MÓDULO DE DOCUMENTAÇÃO (Transplantado da Proposta)
    // ==========================================================

    @FXML
    private void handleAnexarDocumento() {
        if (viewModel.idProperty().get() == null) {
            mainController.notificarSucesso("⚠️ Salve o cadastro do cliente antes de anexar documentos.");
            return;
        }

        List<String> opcoes = List.of("RG", "CPF", "CNH", "Comprovante de Residência", "Contracheque", "Outros");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("RG", opcoes);
        dialog.setTitle("Classificação de Documento");
        dialog.setHeaderText("Qual documento pessoal você está anexando?");
        dialog.setContentText("Tipo:");

        dialog.showAndWait().ifPresent(tipoSelecionado -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecionar Arquivo: " + tipoSelecionado);
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Arquivos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
            File file = fileChooser.showOpenDialog(tableDocumentos.getScene().getWindow());

            if (file != null) {
                realizarUploadAssincrono(file, tipoSelecionado);
            }
        });
    }

    @FXML
    private void cancelarExclusao() {
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML
    private void confirmarExclusao() {
        if (documentoParaExcluir != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    documentoService.excluirDocumento(documentoParaExcluir.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                listaDocumentos.remove(documentoParaExcluir);
                cancelarExclusao();
            });
            new Thread(task).start();
        }
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }

    private void configurarColunasDocumentos() {
        colTipoDocumento.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTipoDocumento()));

        // 🛡️ BLINDAGEM: Se a data falhar, ele mostra "Erro na data" e não quebra a
        // tabela inteira
        colDataUpload.setCellValueFactory(cellData -> {
            try {
                var data = cellData.getValue().getDataUpload();
                return new javafx.beans.property.SimpleStringProperty(
                        data != null ? data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "-");
            } catch (Exception ex) {
                System.err.println("❌ Erro ao formatar data do documento: " + ex.getMessage());
                return new javafx.beans.property.SimpleStringProperty("Data Inválida");
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️");
            private final Button btnExcluir = new Button("🗑️");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8, btnAbrir, btnExcluir);

            {
                btnAbrir.getStyleClass().add("flat");
                btnExcluir.getStyleClass().addAll("flat", "danger");
                container.setAlignment(javafx.geometry.Pos.CENTER);
                btnAbrir.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                btnExcluir.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                btnAbrir.setOnAction(event -> {
                    DocumentoProponenteModel doc = getTableView().getItems().get(getIndex());
                    if (doc != null && doc.getArquivoPath() != null) {
                        File file = new File(doc.getArquivoPath());
                        if (file.exists()) {
                            mainController.getHostServices().showDocument(file.toURI().toString());
                        } else {
                            mainController.notificarAviso("Arquivo físico não encontrado!");
                        }
                    }
                });

                btnExcluir.setOnAction(event -> {
                    documentoParaExcluir = getTableView().getItems().get(getIndex());
                    lblConfirmacaoExclusao.setText("Deseja remover '" + documentoParaExcluir.getTipoDocumento() + "'?");
                    overlayExclusao.setVisible(true);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        tableDocumentos.setRowFactory(tv -> {
            TableRow<DocumentoProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    documentoService.abrirDocumento(row.getItem());
                }
            });
            return row;
        });
    }

    private void carregarDocumentosDoLead(Long proponenteId) {
        System.out.println("🔍 Solicitando documentos para o Lead ID: " + proponenteId);

        if (proponenteId == null) {
            listaDocumentos.clear();
            return;
        }

        Task<List<DocumentoProponenteModel>> task = new Task<>() {
            @Override
            protected List<DocumentoProponenteModel> call() throws Exception {
                return documentoService.listarDoProponente(proponenteId);
            }
        };

        task.setOnSucceeded(e -> {
            List<DocumentoProponenteModel> docsEncontrados = task.getValue();
            System.out.println("✅ Banco retornou " + docsEncontrados.size() + " documentos.");

            // Injeção forçada na UI
            javafx.application.Platform.runLater(() -> {
                listaDocumentos.setAll(docsEncontrados);
                tableDocumentos.refresh();
                System.out.println("🖥️ UI: Tabela foi atualizada. Itens na lista visual: " + listaDocumentos.size());
            });
        });

        // 🛡️ BLINDAGEM: Se a busca no banco falhar, nós saberemos
        task.setOnFailed(e -> {
            System.err.println("❌ ERRO AO BUSCAR DOCUMENTOS NO BANCO:");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void realizarUploadAssincrono(File arquivo, String tipoDoc) {
        System.out.println("📤 Iniciando upload do arquivo: " + arquivo.getName());

        Task<DocumentoProponenteModel> uploadTask = new Task<>() {
            @Override
            protected DocumentoProponenteModel call() throws Exception {
                ProponenteModel proponenteAtual = viewModel.atualizarModel(new ProponenteModel());
                return documentoService.processarUpload(arquivo, tipoDoc, proponenteAtual, null);
            }
        };

        uploadTask.setOnSucceeded(e -> {
            DocumentoProponenteModel docSalvo = uploadTask.getValue();
            System.out.println(
                    "✅ BACKEND: Upload concluído. ID gerado: " + (docSalvo != null ? docSalvo.getId() : "NULO"));

            // Recarrega a tabela imediatamente
            Long idAtual = viewModel.idProperty().get();
            carregarDocumentosDoLead(idAtual);
        });

        uploadTask.setOnFailed(e -> {
            Throwable erro = uploadTask.getException();
            System.err.println("❌ ERRO NO UPLOAD: " + erro.getMessage());
            erro.printStackTrace();
            mainController.notificarAviso("Erro ao anexar documento: " + erro.getMessage());
        });

        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }
}