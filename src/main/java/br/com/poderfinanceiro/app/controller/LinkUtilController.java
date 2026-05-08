package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.enums.Labeled;
import br.com.poderfinanceiro.app.model.LinkUtil;
import br.com.poderfinanceiro.app.model.enums.CategoriaLink;
import br.com.poderfinanceiro.app.repository.LinkUtilRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.scene.Cursor;
import org.springframework.stereotype.Component;

@Component
public class LinkUtilController {

    @FXML
    private ComboBox<CategoriaLink> comboCategoria;
    @FXML
    private TextField txtTitulo, txtUrl, txtDescricao;
    @FXML
    private TableView<LinkUtil> tableLinks;
    @FXML
    private TableColumn<LinkUtil, String> colCategoria, colTitulo, colDescricao;
    @FXML
    private TableColumn<LinkUtil, Void> colAcao;

    private final LinkUtilRepository repository;
    private final MainController mainController;

    private LinkUtil linkEmEdicao;

    public LinkUtilController(LinkUtilRepository repository, MainController mainController) {
        this.repository = repository;
        this.mainController = mainController;
    }

@FXML
    public void initialize() {
        // 1. Usa o seu método genérico para configurar a ComboBox
        configurarCombo(comboCategoria, CategoriaLink.values(), CategoriaLink::fromString);

        // 2. Configuração das colunas
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getLabel()));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescricao()));

        configurarColunaAcoes();
        recarregarLinks();
    }

    /**
     * Reutilizando o seu método genérico para manter a consistência do projeto.
     */
    private <T extends Enum<T> & Labeled> void configurarCombo(ComboBox<T> combo, T[] values,
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
                    LinkUtil item = getTableRow().getItem();
                    if (item != null)
                        mainController.getHostServices().showDocument(item.getUrl());
                });

                // Define o cursor de mãozinha para todos os botões
                btnAbrir.setCursor(Cursor.HAND);
                btnEditar.setCursor(Cursor.HAND);
                btnExcluir.setCursor(Cursor.HAND);
                
                btnEditar.setOnAction(e -> {
                    LinkUtil item = getTableRow().getItem();
                    if (item != null)
                        prepararEdicao(item);
                });

                btnExcluir.setOnAction(e -> {
                    LinkUtil item = getTableRow().getItem();
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

        LinkUtil link = (linkEmEdicao != null) ? linkEmEdicao : new LinkUtil();
        link.setTitulo(txtTitulo.getText());
        link.setUrl(txtUrl.getText());
        link.setDescricao(txtDescricao.getText());
        link.setCategoria(comboCategoria.getValue());

        repository.save(link);
        limparFormulario();
        recarregarLinks();
    }

    private void prepararEdicao(LinkUtil link) {
        this.linkEmEdicao = link;
        txtTitulo.setText(link.getTitulo());
        txtUrl.setText(link.getUrl());
        txtDescricao.setText(link.getDescricao());
        comboCategoria.setValue(link.getCategoria());
    }

    private void handleExcluir(LinkUtil link) {
        repository.delete(link);
        recarregarLinks();
    }

    @FXML
    private void limparFormulario() {
        this.linkEmEdicao = null;
        txtTitulo.clear();
        txtUrl.clear();
        txtDescricao.clear();
        comboCategoria.getSelectionModel().clearSelection();
    }

    public void recarregarLinks() {
        tableLinks.setItems(FXCollections.observableArrayList(repository.findAllByOrderByCategoriaAscTituloAsc()));
    }
}