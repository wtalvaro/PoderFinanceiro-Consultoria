package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.enums.Labeled;
import br.com.poderfinanceiro.app.model.LinkUtil;
import br.com.poderfinanceiro.app.model.enums.CategoriaLink;
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
    private ComboBox<CategoriaLink> comboCategoria;
    @FXML
    private TextField txtTitulo, txtUrl, txtDescricao;

    // NOVO: Campo de busca
    @FXML
    private TextField txtBusca;

    @FXML
    private TableView<LinkUtil> tableLinks;
    @FXML
    private TableColumn<LinkUtil, String> colCategoria, colTitulo, colDescricao;
    @FXML
    private TableColumn<LinkUtil, Void> colAcao;
    @FXML
    private TitledPane paneFormulario;
    @FXML
    private ScrollPane scrollPrincipal;

    private final LinkUtilRepository repository;
    private final MainController mainController;

    private LinkUtil linkEmEdicao;

    // NOVO: Lista mestre que guarda todos os dados para o filtro não perder
    // informação
    private final ObservableList<LinkUtil> masterData = FXCollections.observableArrayList();

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
        colDescricao.setCellValueFactory(data -> {
            String desc = data.getValue().getDescricao();
            return new SimpleStringProperty(desc != null ? desc : ""); // Previne null pointer na interface
        });

        configurarColunaAcoes();

        // 3. NOVO: Configura a mágica do filtro
        configurarBuscaReativa();

        // 4. Carrega os dados do banco
        recarregarLinks();
    }

    /**
     * Configura o FilteredList para buscar instantaneamente no cliente.
     */
    private void configurarBuscaReativa() {
        // Envolve nossa ObservableList em um FilteredList (inicialmente mostra tudo)
        FilteredList<LinkUtil> filteredData = new FilteredList<>(masterData, p -> true);

        // Adiciona um listener (escutador) ao texto da barra de busca
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(link -> {
                // Se o texto de busca for vazio, mostra o link
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Coloca tudo em minúsculo para a busca não ser sensível a maiúsculas
                String filtro = newValue.toLowerCase();

                // Busca no Título
                if (link.getTitulo() != null && link.getTitulo().toLowerCase().contains(filtro)) {
                    return true;
                }
                // Busca na Descrição
                if (link.getDescricao() != null && link.getDescricao().toLowerCase().contains(filtro)) {
                    return true;
                }
                // Busca na Categoria (pelo label Bonito)
                if (link.getCategoria() != null && link.getCategoria().getLabel().toLowerCase().contains(filtro)) {
                    return true;
                }

                return false; // Não deu match em nada, esconde da tabela
            });
        });

        // Envolve o FilteredList em um SortedList para permitir que o usuário ainda
        // clique nos cabeçalhos da tabela para ordenar
        SortedList<LinkUtil> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableLinks.comparatorProperty());

        // Adiciona a lista final "superpoderosa" na tabela
        tableLinks.setItems(sortedData);
    }

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
        recarregarLinks(); // Recarrega do banco e atualiza a masterData
    }

    private void prepararEdicao(LinkUtil link) {
        this.linkEmEdicao = link;
        txtTitulo.setText(link.getTitulo());
        txtUrl.setText(link.getUrl());
        txtDescricao.setText(link.getDescricao());
        comboCategoria.setValue(link.getCategoria());
        paneFormulario.setText("✏️ Editando: " + link.getTitulo());
        paneFormulario.setExpanded(true);
        scrollPrincipal.setVvalue(0.0);
        txtTitulo.requestFocus();
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
        paneFormulario.setText("🔗 Gestão de Links e Atalhos");
    }

    public void recarregarLinks() {
        // ATUALIZADO: Em vez de jogar direto na tabela, atualizamos a masterData.
        // A interface vai reagir automaticamente mantendo o filtro de texto se houver
        // algum!
        masterData.setAll(repository.findAllByOrderByCategoriaAscTituloAsc());
    }
}