package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
import br.com.poderfinanceiro.app.viewmodel.TabelaJurosViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TabelaJurosController {

    // --- DEPENDÊNCIAS (Injeção via Spring) ---
    private final TabelaJurosService service;
    private final TabelaJurosViewModel viewModel;

    // --- ELEMENTOS DO FXML ---
    @FXML
    private TitledPane paneFormulario;
    @FXML
    private Label lblAviso;

    // Campos do Formulário
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

    // Tabela e Filtro
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
    private TableColumn<TabelaJurosModel, Void> colAcoes;

    // Overlays
    @FXML
    private VBox overlayArquivar;

    // Estados Locais
    private ObservableList<TabelaJurosModel> dadosOriginais;
    private TabelaJurosModel tabelaSelecionadaParaArquivar;

    public TabelaJurosController(TabelaJurosService service, TabelaJurosViewModel viewModel) {
        this.service = service;
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        configurarFormulario();
        configurarColunasTabela();
        carregarDados();
        configurarFiltroBusca();

        System.out.println("TabelaJurosController: Centro Cirúrgico Pronto!");
    }

    // =========================================================
    // CONFIGURAÇÕES DE UI E BINDINGS
    // =========================================================

    private void configurarFormulario() {
        comboConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));

        // Conecta o FXML com a nossa "prancheta" (ViewModel)
        txtNomeTabela.textProperty().bindBidirectional(viewModel.getNomeTabela());
        comboConvenio.valueProperty().bindBidirectional(viewModel.getTipoConvenio());

        // Para os números (BigDecimal), usamos um conversor customizado simples
        StringConverter<BigDecimal> conversorDecimal = new BigDecimalConverter();
        Bindings.bindBidirectional(txtTaxaMensal.textProperty(), viewModel.getTaxaMensal(), conversorDecimal);
        Bindings.bindBidirectional(txtComissao.textProperty(), viewModel.getComissaoPercentual(), conversorDecimal);
        Bindings.bindBidirectional(txtValorMinimo.textProperty(), viewModel.getValorMinimoEmprestimo(),
                conversorDecimal);
        Bindings.bindBidirectional(txtValorMaximo.textProperty(), viewModel.getValorMaximoEmprestimo(),
                conversorDecimal);
    }

    private void configurarColunasTabela() {
        colConvenio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipoConvenio().name()));
        colNome.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomeTabela()));

        colTaxa.setCellValueFactory(
                cell -> new SimpleStringProperty(formatarPorcentagem(cell.getValue().getTaxaMensal())));
        colComissao.setCellValueFactory(
                cell -> new SimpleStringProperty(formatarPorcentagem(cell.getValue().getComissaoPercentual())));

        colLimites.setCellValueFactory(cell -> {
            String min = cell.getValue().getValorMinimoEmprestimo() != null
                    ? "R$ " + cell.getValue().getValorMinimoEmprestimo()
                    : "R$ 0,00";
            String max = cell.getValue().getValorMaximoEmprestimo() != null
                    && cell.getValue().getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) > 0
                            ? "R$ " + cell.getValue().getValorMaximoEmprestimo()
                            : "Sem teto";
            return new SimpleStringProperty(min + " - " + max);
        });

        // Configuração dos Botões de Ação na Tabela (Bisturis de Operação)
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
        dadosOriginais = FXCollections.observableArrayList(service.listarAtivas());
        tableTabelas.setItems(dadosOriginais);
    }

    private void configurarFiltroBusca() {
        FilteredList<TabelaJurosModel> filteredData = new FilteredList<>(dadosOriginais, b -> true);
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(tabela -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return tabela.getNomeTabela().toLowerCase().contains(lowerCaseFilter) ||
                        tabela.getTipoConvenio().name().toLowerCase().contains(lowerCaseFilter);
            });
        });
        tableTabelas.setItems(filteredData);
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
            // Pega os dados do formulário e gera a entidade
            TabelaJurosModel tabelaAtualizada = viewModel.atualizarModel(new TabelaJurosModel());

            // Salva usando a Regra de Ouro (Cria nova versão, inativa a velha)
            service.salvarComRegraDeOuro(tabelaAtualizada);

            mostrarMensagem("Tabela atualizada com sucesso! A vigência antiga foi arquivada.", true);
            limparFormulario();
            carregarDados(); // Atualiza a tabela da tela

        } catch (Exception e) {
            mostrarMensagem("Erro ao salvar: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    @FXML
    private void limparFormulario() {
        viewModel.reset();
        paneFormulario.setExpanded(false);
        paneFormulario.setText("💊 Prescrever Nova Tabela (Cadastrar / Atualizar)");
    }

    private void editarTabela(TabelaJurosModel tabela) {
        viewModel.loadFromModel(tabela);
        paneFormulario.setExpanded(true);
        paneFormulario.setText("🔄 Editando Tabela: " + tabela.getNomeTabela() + " (Isso gerará uma nova versão)");
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
            // Reaproveita a lógica do botão Editar!
            // Ele joga os dados pro ViewModel e abre o painel expansível.
            // Quando ela clicar em "Salvar", a Regra de Ouro fará a mágica.
            editarTabela(tabelaSelecionadaParaArquivar);
        }
        // Fecha o overlay escuro
        cancelarArquivamento();

        // Dá um scroll para o topo ou foca no painel para ela ver que o formulário
        // abriu
        paneFormulario.requestFocus();
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

    // =========================================================
    // UTILITÁRIOS INTERNOS
    // =========================================================

    private void mostrarMensagem(String texto, boolean sucesso) {
        lblAviso.setText(texto);
        lblAviso.setStyle(sucesso ? "-fx-text-fill: green; -fx-font-weight: bold;"
                : "-fx-text-fill: red; -fx-font-weight: bold;");
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }

    private String formatarPorcentagem(BigDecimal valor) {
        return valor != null ? valor.setScale(2, RoundingMode.HALF_UP).toString() + "%" : "0.00%";
    }

    /**
     * Conversor simples para transformar os textos digitados em BigDecimal para o
     * ViewModel.
     */
    private static class BigDecimalConverter extends StringConverter<BigDecimal> {
        @Override
        public String toString(BigDecimal object) {
            return object != null ? object.toString() : "";
        }

        @Override
        public BigDecimal fromString(String string) {
            if (string == null || string.trim().isEmpty()) {
                return null;
            }
            try {
                // Troca vírgula por ponto para evitar erro de parse no Java
                return new BigDecimal(string.replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}