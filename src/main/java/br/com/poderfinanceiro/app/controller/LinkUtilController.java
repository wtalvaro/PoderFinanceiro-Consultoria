package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.model.LinkUtilModel;
import br.com.poderfinanceiro.app.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.repository.LinkUtilRepository;
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

@Component
public class LinkUtilController {

    @FXML
    private ComboBox<CategoriaLinkModel> comboCategoria;

    // Adicionado txtTags aqui:
    @FXML
    private TextField txtTitulo, txtUrl, txtDescricao, txtTags;

    @FXML
    private TextField txtBusca;

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

    private final LinkUtilRepository repository;
    private final MainController mainController;

    private LinkUtilModel linkEmEdicao;

    private final ObservableList<LinkUtilModel> masterData = FXCollections.observableArrayList();

    public LinkUtilController(LinkUtilRepository repository, MainController mainController) {
        this.repository = repository;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        configurarCombo(comboCategoria, CategoriaLinkModel.values(), CategoriaLinkModel::fromString);

        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getLabel()));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colDescricao.setCellValueFactory(data -> {
            String desc = data.getValue().getDescricao();
            return new SimpleStringProperty(desc != null ? desc : "");
        });

        configurarColunaAcoes();
        configurarBuscaReativa();
        recarregarLinks();
    }

    private void configurarBuscaReativa() {
        FilteredList<LinkUtilModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(link -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String filtro = newValue.toLowerCase();

                if (link.getTitulo() != null && link.getTitulo().toLowerCase().contains(filtro)) {
                    return true;
                }
                if (link.getDescricao() != null && link.getDescricao().toLowerCase().contains(filtro)) {
                    return true;
                }
                if (link.getCategoria() != null && link.getCategoria().getLabel().toLowerCase().contains(filtro)) {
                    return true;
                }
                // NOVO: Permite que a Solange ache links pesquisando pelas tags na tela de
                // gestão
                if (link.getTags() != null && link.getTags().toLowerCase().contains(filtro)) {
                    return true;
                }

                return false;
            });
        });

        SortedList<LinkUtilModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableLinks.comparatorProperty());

        tableLinks.setItems(sortedData);
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

    private void configurarColunaAcoes() {
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("🌐");
            private final Button btnEditar = new Button("✏️");
            private final Button btnExcluir = new Button("🗑️");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> {
                    LinkUtilModel item = getTableRow().getItem();
                    if (item != null)
                        mainController.getHostServices().showDocument(item.getUrl());
                });

                btnAbrir.setCursor(Cursor.HAND);
                btnEditar.setCursor(Cursor.HAND);
                btnExcluir.setCursor(Cursor.HAND);

                btnEditar.setOnAction(e -> {
                    LinkUtilModel item = getTableRow().getItem();
                    if (item != null)
                        prepararEdicao(item);
                });

                btnExcluir.setOnAction(e -> {
                    LinkUtilModel item = getTableRow().getItem();
                    if (item != null)
                        handleExcluir(item);
                });

                btnAbrir.getStyleClass().add("flat");
                btnEditar.getStyleClass().add("flat");
                btnExcluir.getStyleClass().addAll("flat", "danger");
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

    @FXML
    private void handleSalvar() {
        if (txtTitulo.getText().isEmpty() || txtUrl.getText().isEmpty() || comboCategoria.getValue() == null) {
            return;
        }

        LinkUtilModel link = (linkEmEdicao != null) ? linkEmEdicao : new LinkUtilModel();
        link.setTitulo(txtTitulo.getText());
        link.setUrl(txtUrl.getText());
        link.setDescricao(txtDescricao.getText());
        link.setCategoria(comboCategoria.getValue());

        // NOVO: Adiciona a string de tags digitadas
        link.setTags(txtTags.getText());

        repository.save(link);
        limparFormulario();
        recarregarLinks();
    }

    private void prepararEdicao(LinkUtilModel link) {
        this.linkEmEdicao = link;
        txtTitulo.setText(link.getTitulo());
        txtUrl.setText(link.getUrl());
        txtDescricao.setText(link.getDescricao());
        comboCategoria.setValue(link.getCategoria());

        // NOVO: Preenche a caixa de texto de tags ao editar
        txtTags.setText(link.getTags() != null ? link.getTags() : "");

        paneFormulario.setText("✏️ Editando: " + link.getTitulo());
        paneFormulario.setExpanded(true);
        scrollPrincipal.setVvalue(0.0);
        txtTitulo.requestFocus();
    }

    private void handleExcluir(LinkUtilModel link) {
        repository.delete(link);
        recarregarLinks();
    }

    @FXML
    private void limparFormulario() {
        this.linkEmEdicao = null;
        txtTitulo.clear();
        txtUrl.clear();
        txtDescricao.clear();

        // NOVO: Limpa o campo de tags
        txtTags.clear();

        comboCategoria.getSelectionModel().clearSelection();
        paneFormulario.setText("🔗 Gestão de Links e Atalhos");
    }

    public void recarregarLinks() {
        masterData.setAll(repository.findAllByOrderByCategoriaAscTituloAsc());
    }
}