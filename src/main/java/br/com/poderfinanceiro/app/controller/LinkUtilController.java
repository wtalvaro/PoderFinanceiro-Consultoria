package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.domain.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.scene.Cursor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class LinkUtilController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String MSG_TITULO_PADRAO = "🔗 Gestão de Links e Atalhos";
    private static final String MSG_TITULO_EDICAO = "✏️ Editando: ";

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private ComboBox<CategoriaLinkModel> comboCategoria;
    @FXML
    private TextField txtTitulo, txtUrl, txtDescricao, txtTags, txtBusca;

    @FXML
    private TableView<LinkUtilModel> tableLinks;
    @FXML
    private TableColumn<LinkUtilModel, String> colCategoria, colTitulo, colDescricao;
    @FXML
    private TableColumn<LinkUtilModel, Void> colAcao;

    @FXML
    private TitledPane paneFormulario;
    @FXML
    private ScrollPane scrollPrincipal;

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final LinkUtilRepository repository;
    private final MainController mainController;
    private final ObservableList<LinkUtilModel> masterData = FXCollections.observableArrayList();

    private LinkUtilModel linkEmEdicao;

    public LinkUtilController(LinkUtilRepository repository, MainController mainController) {
        this.repository = repository;
        this.mainController = mainController;
    }

    // =========================================================================
    // INICIALIZAÇÃO E CONFIGURAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarComboCategoria();
        configurarTabela();
        configurarBuscaReativa();
        recarregarLinks();
    }

    private void configurarComboCategoria() {
        configurarCombo(comboCategoria, CategoriaLinkModel.values(), CategoriaLinkModel::fromString);
    }

    private void configurarTabela() {
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getLabel()));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDescricao() != null ? data.getValue().getDescricao() : ""));

        configurarColunaAcoes();
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values,
            Function<String, T> searcher) {
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

    // =========================================================================
    // COLUNA DE AÇÕES DA TABELA (SRP & DRY Aplicados)
    // =========================================================================
    private void configurarColunaAcoes() {
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotao("🌐", "flat");
            private final Button btnEditar = criarBotao("✏️", "flat");
            private final Button btnExcluir = criarBotao("🗑️", "flat", "danger");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> abrirUrlNoNavegador(getLinkAtual()));
                btnEditar.setOnAction(e -> prepararEdicao(getLinkAtual()));
                btnExcluir.setOnAction(e -> handleExcluir(getLinkAtual()));
            }

            private LinkUtilModel getLinkAtual() {
                return getTableRow() != null ? getTableRow().getItem() : null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getLinkAtual() == null) ? null : container);
            }
        });
    }

    private Button criarBotao(String icone, String... styleClasses) {
        Button btn = new Button(icone);
        btn.setCursor(Cursor.HAND);
        btn.getStyleClass().addAll(styleClasses);
        return btn;
    }

    // =========================================================================
    // FILTRO E BUSCA REATIVA
    // =========================================================================
    private void configurarBuscaReativa() {
        FilteredList<LinkUtilModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(link -> atendeCriterioDeBusca(link, newValue));
        });

        SortedList<LinkUtilModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableLinks.comparatorProperty());

        tableLinks.setItems(sortedData);
    }

    private boolean atendeCriterioDeBusca(LinkUtilModel link, String filtro) {
        if (filtro == null || filtro.isBlank())
            return true;

        String termo = filtro.toLowerCase();

        return contemTermo(link.getTitulo(), termo) ||
                contemTermo(link.getDescricao(), termo) ||
                contemTermo(link.getCategoria() != null ? link.getCategoria().getLabel() : null, termo) ||
                contemTermo(link.getTags(), termo);
    }

    private boolean contemTermo(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    // =========================================================================
    // FLUXO DE FORMULÁRIO (SALVAR, EDITAR, LIMPAR, EXCLUIR)
    // =========================================================================
    @FXML
    private void handleSalvar() {
        if (isFormularioInvalido())
            return;

        LinkUtilModel link = (linkEmEdicao != null) ? linkEmEdicao : new LinkUtilModel();
        preencherModeloComFormulario(link);

        repository.save(link);
        limparFormulario();
        recarregarLinks();
    }

    private boolean isFormularioInvalido() {
        return txtTitulo.getText().isBlank() || txtUrl.getText().isBlank() || comboCategoria.getValue() == null;
    }

    private void preencherModeloComFormulario(LinkUtilModel link) {
        link.setTitulo(txtTitulo.getText());
        link.setUrl(txtUrl.getText());
        link.setDescricao(txtDescricao.getText());
        link.setCategoria(comboCategoria.getValue());
        link.setTags(txtTags.getText());
    }

    private void prepararEdicao(LinkUtilModel link) {
        if (link == null)
            return;

        this.linkEmEdicao = link;
        txtTitulo.setText(link.getTitulo());
        txtUrl.setText(link.getUrl());
        txtDescricao.setText(link.getDescricao());
        comboCategoria.setValue(link.getCategoria());
        txtTags.setText(link.getTags() != null ? link.getTags() : "");

        paneFormulario.setText(MSG_TITULO_EDICAO + link.getTitulo());
        paneFormulario.setExpanded(true);
        scrollPrincipal.setVvalue(0.0);
        txtTitulo.requestFocus();
    }

    private void handleExcluir(LinkUtilModel link) {
        if (link == null)
            return;

        repository.delete(link);
        recarregarLinks();
    }

    @FXML
    private void limparFormulario() {
        this.linkEmEdicao = null;

        txtTitulo.clear();
        txtUrl.clear();
        txtDescricao.clear();
        txtTags.clear();
        comboCategoria.getSelectionModel().clearSelection();

        paneFormulario.setText(MSG_TITULO_PADRAO);
    }

    // =========================================================================
    // INTEGRAÇÕES EXTERNAS E COMUNICAÇÃO
    // =========================================================================
    public void recarregarLinks() {
        masterData.setAll(repository.findAllByOrderByCategoriaAscTituloAsc());
    }

    private void abrirUrlNoNavegador(LinkUtilModel link) {
        if (link != null && link.getUrl() != null && !link.getUrl().isBlank()) {
            mainController.getHostServices().showDocument(link.getUrl());
        }
    }
}