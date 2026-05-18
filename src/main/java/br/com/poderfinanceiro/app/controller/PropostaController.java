package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.service.DocumentoService;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Scope("prototype")
public class PropostaController {

    // ==========================================================
    // DEPENDÊNCIAS (INJEÇÃO)
    // ==========================================================
    private final PropostaViewModel viewModel;
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;
    private final DocumentoService documentoService;
    private final MainController mainController;

    // ==========================================================
    // COMPONENTES DE UI (FXML)
    // ==========================================================
    @FXML
    private ComboBox<TipoConvenioModel> cbConvenio;
    @FXML
    private ComboBox<BancoModel> cbBanco;
    @FXML
    private ComboBox<TabelaJurosModel> cbTabela;
    @FXML
    private ComboBox<StatusPropostaModel> cbStatus;

    @FXML
    private TextField txtValorSolicitado;
    @FXML
    private TextField txtValorAprovado;
    @FXML
    private TextField txtParcela;
    @FXML
    private Spinner<Integer> spinPrazo;
    @FXML
    private Spinner<Integer> spinPrazoDesejado;
    @FXML
    private TextArea txtObservacoes;

    @FXML
    private Label lblComissaoEstimada;
    @FXML
    private Label lblTituloComissao;
    @FXML
    private Label lblTotalPago;

    // Tabela e Documentos
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

    // ==========================================================
    // ESTADO LOCAL (CACHE & CONTROLE)
    // ==========================================================
    private List<TabelaJurosModel> todasTabelasAtivas;
    private List<TabelaJurosModel> tabelasElegiveisDaTriagem;
    private DocumentoProponenteModel documentoParaExcluir;
    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();
    private boolean isUpdatingInterface = false;

    // ==========================================================
    // CONSTRUTOR & INITIALIZE
    // ==========================================================
    public PropostaController(PropostaViewModel viewModel, DocumentoService documentoService,
            PropostaService propostaService, TabelaJurosService tabelaJurosService, MainController mainController) {
        this.viewModel = viewModel;
        this.documentoService = documentoService;
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        carregarListasBase();
        configurarConversoresVisuais();
        configurarBindings();
        configurarFiltrosInteligentes();
        configurarTabelaDocumentos();
        configurarAutoSelecaoTextos();
        configurarIndicadoresDinamicos();
    }

    // ==========================================================
    // 1. CONFIGURAÇÕES INICIAIS DA UI E BINDINGS
    // ==========================================================
    private void carregarListasBase() {
        todasTabelasAtivas = tabelaJurosService.listarAtivas();
        cbStatus.setItems(FXCollections.observableArrayList(StatusPropostaModel.values()));
        if (cbConvenio != null) {
            cbConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        }
    }

    private void configurarConversoresVisuais() {
        if (cbConvenio != null) {
            cbConvenio.setConverter(criarConversor(TipoConvenioModel::getLabel));
        }
        cbBanco.setConverter(criarConversor(b -> b != null ? b.getNome() : "Aguardando Triagem..."));
        cbTabela.setConverter(
                criarConversor(t -> t != null ? t.getNomeTabela() + " (" + t.getComissaoPercentual() + "%)"
                        : "Selecione a Tabela..."));
    }

    private void configurarBindings() {
        cbStatus.valueProperty().bindBidirectional(viewModel.statusProperty());
        txtObservacoes.textProperty().bindBidirectional(viewModel.observacoesProperty());
        cbConvenio.valueProperty().bindBidirectional(viewModel.convenioProperty());
        cbBanco.valueProperty().bindBidirectional(viewModel.bancoProperty());

        configurarSpinner(spinPrazo, viewModel.quantidadeParcelasProperty());
        if (spinPrazoDesejado != null) {
            configurarSpinner(spinPrazoDesejado, viewModel.prazoDesejadoProperty());
        }

        vincularMascaraMoeda(txtValorSolicitado, viewModel.valorSolicitadoProperty());
        vincularMascaraMoeda(txtValorAprovado, viewModel.valorAprovadoProperty());
        vincularMascaraMoeda(txtParcela, viewModel.valorParcelaProperty());
    }

    private void configurarFiltrosInteligentes() {
        // Gatilhos de Triagem
        viewModel.valorSolicitadoProperty().addListener((obs, old, val) -> realizarTriagemSegura());
        viewModel.prazoDesejadoProperty().addListener((obs, old, val) -> realizarTriagemSegura());
        if (cbConvenio != null) {
            cbConvenio.valueProperty().addListener((obs, old, val) -> realizarTriagemSegura());
        }

        // Gatilhos de Recálculo e Carregamento
        cbBanco.valueProperty().addListener((obs, old, bancoNovo) -> atualizarTabelasDoBancoSegura(bancoNovo));
        viewModel.valorAprovadoProperty().addListener((obs, old, val) -> dispararCalculoSeguro());
        cbTabela.valueProperty().addListener((obs, old, novaTabela) -> atualizarTabelaNoViewModel(novaTabela));

        // Gatilho Principal: Quando uma proposta existente é carregada
        viewModel.tabelaIdProperty().addListener((obs, old, idNovo) -> carregarDadosDaPropostaExistente(idNovo));
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
                        + FinanceiroUtils.formatarParaExibicao(parcela.multiply(new BigDecimal(prazo)));
            }
            return "Total a Pagar: R$ 0,00";
        }, viewModel.quantidadeParcelasProperty(), viewModel.valorParcelaProperty()));
    }

    // ==========================================================
    // 2. LÓGICA DE TRIAGEM E REGRAS DE NEGÓCIO (CLEAN CODE)
    // ==========================================================
    private void realizarTriagemSegura() {
        if (!isUpdatingInterface)
            realizarTriagem();
    }

    private void realizarTriagem() {
        TipoConvenioModel convenio = (cbConvenio != null) ? cbConvenio.getValue() : null;
        BigDecimal valor = viewModel.valorSolicitadoProperty().get();
        Integer prazo = viewModel.prazoDesejadoProperty().get();
        ProponenteModel proponente = viewModel.proponenteProperty().get();

        if (!dadosMinimosParaTriagemPreenchidos(convenio, valor)) {
            limparFiltrosDeTriagem();
            return;
        }

        tabelasElegiveisDaTriagem = todasTabelasAtivas.stream()
                .filter(t -> t.getTipoConvenio() == convenio)
                .filter(t -> atendeRegrasDeValor(t, valor))
                .filter(t -> atendeRegrasDePrazo(t, prazo))
                .filter(t -> atendeRegrasDeRenda(t, proponente))
                .filter(t -> atendeRegrasDeIdade(t, proponente))
                .toList();

        List<BancoModel> bancosElegiveis = tabelasElegiveisDaTriagem.stream()
                .map(TabelaJurosModel::getBanco)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        atualizarComboBancos(bancosElegiveis);
    }

    // --- PREDICADOS DE TRIAGEM EXTRAÍDOS (SRP) ---
    private boolean dadosMinimosParaTriagemPreenchidos(TipoConvenioModel convenio, BigDecimal valor) {
        return convenio != null && valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean atendeRegrasDeValor(TabelaJurosModel t, BigDecimal valorSolicitado) {
        BigDecimal min = t.getValorMinimoEmprestimo() != null ? t.getValorMinimoEmprestimo() : BigDecimal.ZERO;
        if (valorSolicitado.compareTo(min) < 0)
            return false;

        return t.getValorMaximoEmprestimo() == null
                || t.getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) <= 0
                || valorSolicitado.compareTo(t.getValorMaximoEmprestimo()) <= 0;
    }

    private boolean atendeRegrasDePrazo(TabelaJurosModel t, Integer prazoDesejado) {
        if (prazoDesejado == null || prazoDesejado <= 0)
            return true;
        int min = t.getPrazoMinimo() != null ? t.getPrazoMinimo() : 1;
        int max = t.getPrazoMaximo() != null ? t.getPrazoMaximo() : 999;
        return prazoDesejado >= min && prazoDesejado <= max;
    }

    private boolean atendeRegrasDeRenda(TabelaJurosModel t, ProponenteModel p) {
        BigDecimal minRenda = t.getRendaMinima() != null ? t.getRendaMinima() : BigDecimal.ZERO;
        if (minRenda.compareTo(BigDecimal.ZERO) <= 0)
            return true;

        BigDecimal rendaProponente = (p != null && p.getRendaMensal() != null) ? p.getRendaMensal() : BigDecimal.ZERO;
        return rendaProponente.compareTo(minRenda) >= 0;
    }

    private boolean atendeRegrasDeIdade(TabelaJurosModel t, ProponenteModel p) {
        int idade = calcularIdade(p);
        if (idade <= 0)
            return true;

        int min = t.getIdadeMinima() != null ? t.getIdadeMinima() : 0;
        int max = t.getIdadeMaxima() != null ? t.getIdadeMaxima() : 999;
        return idade >= min && idade <= max;
    }

    // ==========================================================
    // 3. ATUALIZAÇÕES DE INTERFACE SOB DEMANDA
    // ==========================================================
    private void carregarDadosDaPropostaExistente(Long idTabelaNovo) {
        if (isUpdatingInterface)
            return;

        executarSemGatilhos(() -> {
            if (idTabelaNovo != null) {
                TabelaJurosModel tab = todasTabelasAtivas.stream()
                        .filter(t -> t.getId().equals(idTabelaNovo)).findFirst().orElse(null);

                if (tab != null) {
                    cbConvenio.setValue(tab.getTipoConvenio());
                    realizarTriagem();
                    cbBanco.setValue(tab.getBanco());
                    atualizarTabelasDoBanco(tab.getBanco());
                    cbTabela.setValue(tab);
                    dispararCalculo();

                    Long propostaId = viewModel.idProperty().get();
                    if (propostaId != null)
                        carregarDocumentosDaProposta(propostaId);
                }
            } else {
                limparFiltrosDeTriagem();
                dispararCalculo();
                Platform.runLater(this::aplicarBloqueio);
            }
        });
    }

    private void atualizarTabelasDoBancoSegura(BancoModel bancoNovo) {
        if (!isUpdatingInterface)
            atualizarTabelasDoBanco(bancoNovo);
    }

    private void atualizarTabelasDoBanco(BancoModel bancoNovo) {
        if (bancoNovo != null && tabelasElegiveisDaTriagem != null) {
            List<TabelaJurosModel> tabelasDoBanco = tabelasElegiveisDaTriagem.stream()
                    .filter(t -> t.getBanco().getId().equals(bancoNovo.getId()))
                    .sorted(Comparator.comparing(TabelaJurosModel::getComissaoPercentual).reversed())
                    .toList();

            TabelaJurosModel tabelaAtual = cbTabela.getValue();
            cbTabela.setItems(FXCollections.observableArrayList(tabelasDoBanco));

            if (!isUpdatingInterface) {
                if (tabelasDoBanco.size() == 1)
                    cbTabela.setValue(tabelasDoBanco.get(0));
                else if (tabelaAtual != null && tabelasDoBanco.contains(tabelaAtual))
                    cbTabela.setValue(tabelaAtual);
                else
                    cbTabela.setValue(null);
            }
        } else {
            cbTabela.getItems().clear();
            if (!isUpdatingInterface)
                cbTabela.setValue(null);
        }
    }

    private void atualizarTabelaNoViewModel(TabelaJurosModel novaTabela) {
        if (isUpdatingInterface)
            return;
        viewModel.tabelaIdProperty().set(novaTabela != null ? novaTabela.getId() : null);
        dispararCalculo();
    }

    private void dispararCalculoSeguro() {
        if (!isUpdatingInterface)
            dispararCalculo();
    }

    private void dispararCalculo() {
        BigDecimal vAprovado = viewModel.valorAprovadoProperty().get();
        BigDecimal vSolicitado = viewModel.valorSolicitadoProperty().get();
        TabelaJurosModel tabela = cbTabela.getValue();

        BigDecimal valorBase = (vAprovado != null && vAprovado.compareTo(BigDecimal.ZERO) > 0) ? vAprovado
                : vSolicitado;

        if (valorBase != null && valorBase.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            BigDecimal comissaoCalculada = propostaService.calcularComissaoEstimada(valorBase, tabela.getId());
            viewModel.comissaoEstimadaProperty().set(comissaoCalculada);

            String formatado = FinanceiroUtils.formatarParaExibicao(comissaoCalculada);
            lblComissaoEstimada.setText(String.format("%s (%s%%)", formatado, tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            viewModel.comissaoEstimadaProperty().set(BigDecimal.ZERO);
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-default;");
        }
    }

    // ==========================================================
    // 4. GESTÃO DE DOCUMENTOS E ARQUIVOS (FILECHOOSER NATIVO)
    // ==========================================================
    private void carregarDocumentosDaProposta(Long propostaId) {
        if (propostaId == null) {
            listaDocumentos.clear();
            return;
        }

        Task<List<DocumentoProponenteModel>> task = new Task<>() {
            @Override
            protected List<DocumentoProponenteModel> call() {
                return documentoService.buscarPorProposta(propostaId);
            }
        };

        task.setOnSucceeded(e -> {
            listaDocumentos.setAll(task.getValue());
            tableDocumentos.refresh();
        });

        task.setOnFailed(e -> {
            mainController.notificarAviso("Erro ao carregar documentos da proposta.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    @FXML
    private void handleAnexarDocumento() {
        if (!validarPropostaParaAnexo())
            return;

        Optional<String> tipoSelecionado = exibirDialogoTipoDocumento();

        tipoSelecionado.ifPresent(tipo -> {
            File arquivo = selecionarArquivoLocal(tipo);
            if (arquivo != null && arquivo.exists()) {
                realizarUploadAssincrono(arquivo, tipo);
            }
        });
    }

    private boolean validarPropostaParaAnexo() {
        if (viewModel.idProperty().get() == null) {
            mainController.notificarAviso(
                    "⚠️ Salve a proposta primeiro para gerar um número de identificação antes de anexar documentos.");
            return false;
        }
        return true;
    }

    private Optional<String> exibirDialogoTipoDocumento() {
        List<String> opcoes = List.of("RG", "CPF", "CNH", "Contracheque", "Comprovante de Residência",
                "Extrato Bancário", "Outros");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("RG", opcoes);
        dialog.setTitle("Classificação de Documento");
        dialog.setHeaderText("Qual documento deseja anexar à proposta?");
        dialog.setContentText("Selecione o Tipo:");
        return dialog.showAndWait();
    }

    private File selecionarArquivoLocal(String tipoSelecionado) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Arquivo: " + tipoSelecionado);
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos Permitidos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        return fileChooser.showOpenDialog(tableDocumentos.getScene().getWindow());
    }

    private void realizarUploadAssincrono(File arquivo, String tipoDoc) {
        Task<DocumentoProponenteModel> uploadTask = new Task<>() {
            @Override
            // 🚀 CORREÇÃO: Adicionado o "throws Exception" aqui
            protected DocumentoProponenteModel call() throws Exception {
                PropostaModel propostaAtual = viewModel.atualizarModel(new PropostaModel());
                return documentoService.processarUpload(arquivo, tipoDoc, propostaAtual.getProponente(), propostaAtual);
            }
        };

        uploadTask.setOnSucceeded(e -> carregarDocumentosDaProposta(viewModel.idProperty().get()));

        uploadTask.setOnFailed(e -> mainController
                .notificarAviso("Falha ao anexar documento: " + uploadTask.getException().getMessage()));

        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }

    // ==========================================================
    // 5. CONFIGURAÇÃO DE TABELAS (UI)
    // ==========================================================
    private void configurarTabelaDocumentos() {
        tableDocumentos.setItems(listaDocumentos);

        colTipoDocumento.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoDocumento()));

        colDataUpload.setCellValueFactory(data -> {
            var dataUpload = data.getValue().getDataUpload();
            return new SimpleStringProperty(
                    dataUpload != null ? dataUpload.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-");
        });

        configurarColunaAcoes();
        configurarDuploCliqueTabela();
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️");
            private final Button btnExcluir = new Button("🗑️");
            private final HBox container = new HBox(8, btnAbrir, btnExcluir);

            {
                btnAbrir.getStyleClass().add("flat");
                btnExcluir.getStyleClass().addAll("flat", "danger");
                container.setAlignment(Pos.CENTER);
                btnAbrir.setMinWidth(Region.USE_PREF_SIZE);
                btnExcluir.setMinWidth(Region.USE_PREF_SIZE);

                btnAbrir.setOnAction(e -> abrirDocumentoFisico(getTableView().getItems().get(getIndex())));
                btnExcluir.setOnAction(e -> exibirOverlayExclusao(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void configurarDuploCliqueTabela() {
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

    private void abrirDocumentoFisico(DocumentoProponenteModel doc) {
        if (doc != null && doc.getArquivoPath() != null) {
            File file = new File(doc.getArquivoPath());
            if (file.exists()) {
                mainController.getHostServices().showDocument(file.toURI().toString());
            } else {
                mainController.notificarAviso("Arquivo físico não encontrado no servidor.");
            }
        }
    }

    private void exibirOverlayExclusao(DocumentoProponenteModel doc) {
        this.documentoParaExcluir = doc;
        lblConfirmacaoExclusao.setText(String.format("Deseja remover '%s'?", doc.getTipoDocumento()));
        overlayExclusao.setVisible(true);
    }

    // ==========================================================
    // 6. OVERLAYS E AÇÕES DE EXCLUSÃO
    // ==========================================================
    @FXML
    private void cancelarExclusao() {
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML
    private void confirmarExclusao() {
        if (documentoParaExcluir == null)
            return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                documentoService.excluirDocumento(documentoParaExcluir.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            listaDocumentos.remove(documentoParaExcluir);
            cancelarExclusao();
        });

        task.setOnFailed(e -> mainController.notificarAviso("Erro ao excluir documento."));
        new Thread(task).start();
    }

    // ==========================================================
    // 7. TRAVAS DE SEGURANÇA E PERSISTÊNCIA
    // ==========================================================
    public void aplicarBloqueio() {
        StatusPropostaModel status = viewModel.statusProperty().get();
        boolean isTerminal = viewModel.idProperty().get() != null &&
                (status == StatusPropostaModel.PAGO || status == StatusPropostaModel.REPROVADA
                        || status == StatusPropostaModel.CANCELADO);

        cbStatus.setDisable(isTerminal);
        cbTabela.setDisable(isTerminal);
        cbBanco.setDisable(isTerminal);
        cbConvenio.setDisable(isTerminal);
        txtValorSolicitado.setDisable(isTerminal);
        txtValorAprovado.setDisable(isTerminal);
        txtParcela.setDisable(isTerminal);
        spinPrazo.setDisable(isTerminal);

        txtObservacoes.setPromptText(isTerminal ? "Proposta liquidada. Alterações não permitidas." : "");
    }

    // ==========================================================
    // 8. MÉTODOS UTILITÁRIOS (DRY)
    // ==========================================================
    private void executarSemGatilhos(Runnable acao) {
        isUpdatingInterface = true;
        try {
            acao.run();
        } finally {
            isUpdatingInterface = false;
        }
    }

    private void limparFiltrosDeTriagem() {
        cbBanco.getItems().clear();
        cbTabela.getItems().clear();
        tabelasElegiveisDaTriagem = null;
    }

    private void atualizarComboBancos(List<BancoModel> bancosElegiveis) {
        BancoModel bancoAtual = cbBanco.getValue();
        cbBanco.setItems(FXCollections.observableArrayList(bancosElegiveis));

        if (!isUpdatingInterface) {
            if (bancosElegiveis.size() == 1)
                cbBanco.setValue(bancosElegiveis.get(0));
            else if (bancoAtual != null && bancosElegiveis.contains(bancoAtual))
                cbBanco.setValue(bancoAtual);
            else
                cbBanco.setValue(null);

            dispararCalculo();
        }
    }

    private <T> StringConverter<T> criarConversor(java.util.function.Function<T, String> formatter) {
        return new StringConverter<>() {
            @Override
            public String toString(T obj) {
                return formatter.apply(obj);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    private void vincularMascaraMoeda(TextField campo, javafx.beans.property.Property<BigDecimal> prop) {
        TextFormatter<BigDecimal> formatador = FinanceiroUtils.criarFormatadorMoeda();
        campo.setTextFormatter(formatador);
        formatador.valueProperty().bindBidirectional(prop);
    }

    private void configurarSpinner(Spinner<Integer> spinner, javafx.beans.property.Property<Integer> prop) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 1));
        spinner.setEditable(true);
        spinner.getEditor().focusedProperty().addListener((obs, old, isFocused) -> {
            if (!isFocused)
                spinner.increment(0);
        });
        spinner.getValueFactory().valueProperty().bindBidirectional(prop);
    }

    private void configurarAutoSelecaoTextos() {
        for (TextField campo : new TextField[] { txtValorSolicitado, txtValorAprovado, txtParcela }) {
            campo.focusedProperty().addListener((obs, old, isFocused) -> {
                if (isFocused)
                    Platform.runLater(campo::selectAll);
            });
        }
    }

    private int calcularIdade(ProponenteModel p) {
        if (p == null || p.getDataNascimento() == null)
            return 0;
        return Period.between(p.getDataNascimento(), LocalDate.now()).getYears();
    }

    public PropostaViewModel getViewModel() {
        return viewModel;
    }
}