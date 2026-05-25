package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.TabelaJurosViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;
import org.controlsfx.control.MasterDetailPane;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TabelaJurosController {

    private static final Logger log = LoggerFactory.getLogger(TabelaJurosController.class);

    private final TabelaJurosService service;
    private final TabelaJurosViewModel viewModel;
    private final BancoRepository bancoRepository;

    @FXML
    private MasterDetailPane paneFormulario; // Trocado de TitledPane para MasterDetailPane
    @FXML
    private Label lblFormTitulo; // Novo label interno do formulário
    @FXML
    private Button btnToggleForm; // Novo botão mestre do cabeçalho
    @FXML
    private Label lblAviso;

    @FXML
    private TextField txtNomeTabela;
    @FXML
    private ComboBox<TipoConvenioModel> comboConvenio;
    @FXML
    private TextField txtTaxaMensal;
    @FXML
    private TextField txtComissao;
    @FXML
    private TextField txtValorMinimo;
    @FXML
    private TextField txtValorMaximo;
    @FXML
    private TextField txtPrazoMinimo;
    @FXML
    private TextField txtPrazoMaximo;
    @FXML
    private TextField txtIdadeMinima;
    @FXML
    private TextField txtIdadeMaxima;
    @FXML
    private TextField txtRendaMinima;

    @FXML
    private TextField txtBusca;
    @FXML
    private TableView<TabelaJurosModel> tableTabelas;
    @FXML
    private TableColumn<TabelaJurosModel, String> colConvenio;
    @FXML
    private TableColumn<TabelaJurosModel, String> colNome;
    @FXML
    private TableColumn<TabelaJurosModel, String> colTaxa;
    @FXML
    private TableColumn<TabelaJurosModel, String> colComissao;
    @FXML
    private TableColumn<TabelaJurosModel, String> colLimites;
    @FXML
    private TableColumn<TabelaJurosModel, String> colRenda;
    @FXML
    private TableColumn<TabelaJurosModel, String> colIdade;
    @FXML
    private TableColumn<TabelaJurosModel, String> colPrazo;
    @FXML
    private TableColumn<TabelaJurosModel, Void> colAcoes;
    @FXML
    private ComboBox<BancoModel> comboBanco;
    @FXML
    private VBox overlayArquivar;

    private SortedList<TabelaJurosModel> sortedData;
    private FilteredList<TabelaJurosModel> filteredData;

    private ObservableList<TabelaJurosModel> dadosOriginais;
    private TabelaJurosModel tabelaSelecionadaParaArquivar;

    public TabelaJurosController(TabelaJurosService service, TabelaJurosViewModel viewModel,
            BancoRepository bancoRepository) {
        this.service = service;
        this.viewModel = viewModel;
        this.bancoRepository = bancoRepository;
    }

    @FXML
    public void initialize() {
        configurarFormulario();
        configurarColunasTabela();
        carregarDados();
        configurarFiltroBusca();

        log.info("TabelaJurosController: Centro Cirúrgico Pronto com Utils!");
    }

    // =========================================================
    // CONFIGURAÇÕES DE UI E BINDINGS
    // =========================================================

    private void configurarFormulario() {
        comboBanco.setItems(FXCollections.observableArrayList(bancoRepository.findByAtivoTrueOrderByNomeAsc()));
        comboBanco.setConverter(new StringConverter<BancoModel>() {
            @Override
            public String toString(BancoModel b) {
                return b != null ? b.getNome() : "";
            }

            @Override
            public BancoModel fromString(String s) {
                return null;
            }
        });

        comboBanco.valueProperty().bindBidirectional(viewModel.getBanco());
        comboConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        comboConvenio.valueProperty().bindBidirectional(viewModel.getTipoConvenio());
        txtNomeTabela.textProperty().bindBidirectional(viewModel.getNomeTabela());

        // 🚀 BINDINGS COM O SEU FINANCEIRO UTILS (Para Moeda/Percentual)
        // Precisamos instanciar um TextFormatter NOVO para CADA campo de texto
        TextFormatter<BigDecimal> tfTaxa = FinanceiroUtils.criarFormatadorMoeda();
        txtTaxaMensal.setTextFormatter(tfTaxa);
        tfTaxa.valueProperty().bindBidirectional(viewModel.getTaxaMensal());

        TextFormatter<BigDecimal> tfComissao = FinanceiroUtils.criarFormatadorMoeda();
        txtComissao.setTextFormatter(tfComissao);
        tfComissao.valueProperty().bindBidirectional(viewModel.getComissaoPercentual());

        TextFormatter<BigDecimal> tfValorMin = FinanceiroUtils.criarFormatadorMoeda();
        txtValorMinimo.setTextFormatter(tfValorMin);
        tfValorMin.valueProperty().bindBidirectional(viewModel.getValorMinimoEmprestimo());

        TextFormatter<BigDecimal> tfValorMax = FinanceiroUtils.criarFormatadorMoeda();
        txtValorMaximo.setTextFormatter(tfValorMax);
        tfValorMax.valueProperty().bindBidirectional(viewModel.getValorMaximoEmprestimo());

        TextFormatter<BigDecimal> tfRendaMin = FinanceiroUtils.criarFormatadorMoeda();
        txtRendaMinima.setTextFormatter(tfRendaMin);
        tfRendaMin.valueProperty().bindBidirectional(viewModel.getRendaMinima());

        // 🚀 BINDINGS ROBUSTOS PARA INTEIROS (Para Idade/Prazos)
        TextFormatter<Integer> tfPrazoMin = criarFormatadorInteiroPadrao();
        txtPrazoMinimo.setTextFormatter(tfPrazoMin);
        tfPrazoMin.valueProperty().bindBidirectional(viewModel.getPrazoMinimo());

        TextFormatter<Integer> tfPrazoMax = criarFormatadorInteiroPadrao();
        txtPrazoMaximo.setTextFormatter(tfPrazoMax);
        tfPrazoMax.valueProperty().bindBidirectional(viewModel.getPrazoMaximo());

        TextFormatter<Integer> tfIdadeMin = criarFormatadorInteiroPadrao();
        txtIdadeMinima.setTextFormatter(tfIdadeMin);
        tfIdadeMin.valueProperty().bindBidirectional(viewModel.getIdadeMinima());

        TextFormatter<Integer> tfIdadeMax = criarFormatadorInteiroPadrao();
        txtIdadeMaxima.setTextFormatter(tfIdadeMax);
        tfIdadeMax.valueProperty().bindBidirectional(viewModel.getIdadeMaxima());
    }

    /**
     * Motor nativo de Formatação de Inteiros para impedir travamentos e letras
     */
    private TextFormatter<Integer> criarFormatadorInteiroPadrao() {
        return new TextFormatter<>(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object != null ? object.toString() : "";
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.trim().isEmpty())
                    return null;
                try {
                    return Integer.parseInt(string.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }, null, change -> {
            // Permite APENAS números no teclado
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        });
    }

    private void configurarColunasTabela() {
        colConvenio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipoConvenio().name()));
        colNome.setCellValueFactory(cell -> {
            String bancoNome = cell.getValue().getBanco() != null ? cell.getValue().getBanco().getNome() : "S/B";
            return new SimpleStringProperty("[" + bancoNome + "] " + cell.getValue().getNomeTabela());
        });

        // Uso do FinanceiroUtils diretamente na tabela
        colTaxa.setCellValueFactory(cell -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(cell.getValue().getTaxaMensal()) + "%"));
        colComissao.setCellValueFactory(cell -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(cell.getValue().getComissaoPercentual()) + "%"));

        colLimites.setCellValueFactory(cell -> {
            String min = cell.getValue().getValorMinimoEmprestimo() != null
                    ? "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorMinimoEmprestimo())
                    : "R$ 0,00";
            String max = cell.getValue().getValorMaximoEmprestimo() != null
                    && cell.getValue().getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) > 0
                            ? "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorMaximoEmprestimo())
                            : "Sem teto";
            return new SimpleStringProperty(min + " - " + max);
        });

        colRenda.setCellValueFactory(cell -> {
            BigDecimal min = cell.getValue().getRendaMinima();
            String texto = (min != null && min.compareTo(BigDecimal.ZERO) > 0)
                    ? "A partir de R$ " + FinanceiroUtils.formatarParaExibicao(min)
                    : "Isento";
            return new SimpleStringProperty(texto);
        });

        colIdade.setCellValueFactory(cell -> {
            Integer min = cell.getValue().getIdadeMinima() != null ? cell.getValue().getIdadeMinima() : 18;
            Integer max = cell.getValue().getIdadeMaxima() != null ? cell.getValue().getIdadeMaxima() : 100;
            return new SimpleStringProperty(min + " a " + max + " anos");
        });

        colPrazo.setCellValueFactory(cell -> {
            Integer min = cell.getValue().getPrazoMinimo() != null ? cell.getValue().getPrazoMinimo() : 1;
            Integer max = cell.getValue().getPrazoMaximo() != null ? cell.getValue().getPrazoMaximo() : 96;
            return new SimpleStringProperty(min + " a " + max + "x");
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnArquivar = new Button("📦");
            private final HBox pane = new HBox(5, btnEditar, btnArquivar);
            {
                btnEditar.getStyleClass().add("flat");
                btnEditar.setOnAction(event -> editarTabela(getTableView().getItems().get(getIndex())));
                btnArquivar.getStyleClass().add("flat");
                btnArquivar.setStyle("-fx-text-fill: #f57c00;");
                btnArquivar
                        .setOnAction(event -> abrirConfirmacaoArquivamento(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void carregarDados() {
        List<TabelaJurosModel> novasTabelas = service.listarAtivas();

        // Se a lista não existir, cria. Se existir, apenas atualiza.
        if (dadosOriginais == null) {
            dadosOriginais = FXCollections.observableArrayList(novasTabelas);
        } else {
            dadosOriginais.setAll(novasTabelas);
        }
    }

    private void configurarFiltroBusca() {
        filteredData = new FilteredList<>(dadosOriginais, b -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableTabelas.comparatorProperty());

        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(tabela -> {
                // Se o campo de busca estiver vazio, mostra tudo
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                // Transforma o que foi digitado em minúsculo
                String termoBusca = newValue.toLowerCase().trim();

                // Compara com a nossa string de busca completa do objeto
                return gerarStringDeBusca(tabela).contains(termoBusca);
            });
        });

        tableTabelas.setItems(sortedData);
    }

    private String gerarStringDeBusca(TabelaJurosModel t) {
        // Concatenamos tudo o que está nas colunas, usando espaços para separar os
        // campos
        return ((t.getBanco() != null ? t.getBanco().getNome() : "") + " " +
                (t.getNomeTabela() != null ? t.getNomeTabela() : "") + " " +
                (t.getTipoConvenio() != null ? t.getTipoConvenio().name() : "") + " " +
                FinanceiroUtils.formatarParaExibicao(t.getTaxaMensal()) + " " +
                FinanceiroUtils.formatarParaExibicao(t.getComissaoPercentual()) + " " +
                t.getValorMinimoEmprestimo() + " " +
                t.getValorMaximoEmprestimo() + " " +
                t.getIdadeMinima() + " " +
                t.getIdadeMaxima() + " " +
                t.getPrazoMinimo() + " " +
                t.getPrazoMaximo()).toLowerCase();
    }

    // =========================================================
    // AÇÕES (MÉTODOS FXML)
    // =========================================================

    @FXML
    private void handleSalvar() {
        if (!viewModel.isDirty()) {
            mostrarMensagem("Nenhuma alteração detectada para salvar.", false);
            return;
        }
        try {
            TabelaJurosModel tabelaAtualizada = viewModel.atualizarModel(new TabelaJurosModel());
            service.salvarComRegraDeOuro(tabelaAtualizada);
            mostrarMensagem("Tabela atualizada com sucesso! A vigência antiga foi arquivada.", true);
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarMensagem("Erro ao salvar: " + e.getMessage(), false);
            log.error("[CONTROLLER][TABELAJUROS] Erro: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleToggleForm() {
        boolean mostrandoForm = paneFormulario.isShowDetailNode();
        paneFormulario.setShowDetailNode(!mostrandoForm);
        btnToggleForm.setText(!mostrandoForm ? "➖ Fechar Formulário" : "➕ Prescrever Nova Tabela");
    }

    @FXML
    private void limparFormulario() {
        viewModel.reset();
        paneFormulario.setShowDetailNode(false); // Oculta o formulário deslizando suavemente
        lblFormTitulo.setText("💊 Prescrever Nova Tabela (Cadastrar / Atualizar)");
        btnToggleForm.setText("➕ Prescrever Nova Tabela");
    }

    private void editarTabela(TabelaJurosModel tabela) {
        viewModel.loadFromModel(tabela);
        paneFormulario.setShowDetailNode(true); // Exibe o formulário com animação
        lblFormTitulo.setText("🔄 Editando Tabela: " + tabela.getNomeTabela() + " (Isso gerará uma nova versão)");
        btnToggleForm.setText("➖ Fechar Formulário");
        txtNomeTabela.requestFocus();
    }

    private void abrirConfirmacaoArquivamento(TabelaJurosModel tabela) {
        this.tabelaSelecionadaParaArquivar = tabela;
        overlayArquivar.setVisible(true);
    }

    @FXML
    private void cancelarArquivamento() {
        this.tabelaSelecionadaParaArquivar = null;
        overlayArquivar.setVisible(false);
    }

    @FXML
    private void prepararNovaVersao() {
        if (tabelaSelecionadaParaArquivar != null) {
            editarTabela(tabelaSelecionadaParaArquivar);
        }
        cancelarArquivamento();
        txtNomeTabela.requestFocus(); // Foco direto no primeiro input field do formulário
    }

    @FXML
    private void confirmarArquivamento() {
        if (tabelaSelecionadaParaArquivar != null) {
            service.arquivarTabela(tabelaSelecionadaParaArquivar);
            carregarDados();
            mostrarMensagem("Tabela arquivada! Ela não aparecerá mais para novas propostas.", true);
        }
        cancelarArquivamento();
    }

    private void mostrarMensagem(String texto, boolean sucesso) {
        lblAviso.setText(texto);
        lblAviso.setStyle(sucesso ? "-fx-text-fill: green; -fx-font-weight: bold;"
                : "-fx-text-fill: red; -fx-font-weight: bold;");
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }
}