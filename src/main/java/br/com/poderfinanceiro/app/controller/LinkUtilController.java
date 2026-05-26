package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.LinkUtilUIEventHub;
import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.domain.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.domain.service.LinkUtilService;
import br.com.poderfinanceiro.app.util.Disposable;
import javafx.application.HostServices;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LinkUtilController implements Disposable {

    private static final String MSG_TITULO_PADRAO = "🔗 Gestão de Links e Atalhos";
    private static final String MSG_TITULO_EDICAO = "✏️ Editando: ";

    private static final Logger log = LoggerFactory.getLogger(LinkUtilController.class);

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

    private final LinkUtilService service;
    private final LinkUtilUIEventHub eventHub;
    private final HostServices hostServices;

    private final ObservableList<LinkUtilModel> masterData = FXCollections.observableArrayList();
    private LinkUtilModel linkEmEdicao;

    public LinkUtilController(LinkUtilService service, LinkUtilUIEventHub eventHub, HostServices hostServices) {
        this.service = service;
        this.eventHub = eventHub;
        this.hostServices = hostServices;
        log.debug("[LINK_UTIL] Construtor: Controller instanciado");
    }

    @FXML
    public void initialize() {
        log.debug("[LINK_UTIL] initialize: Iniciando configuração da gestão de links");
        configurarComboCategoria();
        configurarTabela();
        configurarBuscaReativa();

        eventHub.inscrever(this::recarregarLinks);
        recarregarLinks();

        log.info("[LINK_UTIL] initialize: Configuração concluída e inscrita no event hub");
    }

    private void configurarComboCategoria() {
        log.debug("[LINK_UTIL] configurarComboCategoria: Carregando categorias no combobox");
        configurarCombo(comboCategoria, CategoriaLinkModel.values(), CategoriaLinkModel::fromString);
        log.trace("[LINK_UTIL] configurarComboCategoria: {} categorias disponíveis",
                CategoriaLinkModel.values().length);
    }

    private void configurarTabela() {
        log.debug("[LINK_UTIL] configurarTabela: Configurando colunas da tabela de links");
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getLabel()));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDescricao() != null ? data.getValue().getDescricao() : ""));

        configurarColunaAcoes();
        log.debug("[LINK_UTIL] configurarTabela: Tabela configurada");
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
        log.trace("[LINK_UTIL] configurarCombo: Combo configurado com {} itens", values.length);
    }

    private void configurarColunaAcoes() {
        log.debug("[LINK_UTIL] configurarColunaAcoes: Configurando coluna de ações (abrir, editar, excluir)");
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotao("🌐", "flat");
            private final Button btnEditar = criarBotao("✏️", "flat");
            private final Button btnExcluir = criarBotao("🗑️", "flat", "danger");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> {
                    log.debug("[LINK_UTIL] Botão 'Abrir' clicado para link: {}",
                            getLinkAtual() != null ? getLinkAtual().getTitulo() : "null");
                    abrirUrlNoNavegador(getLinkAtual());
                });
                btnEditar.setOnAction(e -> {
                    log.debug("[LINK_UTIL] Botão 'Editar' clicado para link: {}",
                            getLinkAtual() != null ? getLinkAtual().getTitulo() : "null");
                    prepararEdicao(getLinkAtual());
                });
                btnExcluir.setOnAction(e -> {
                    log.debug("[LINK_UTIL] Botão 'Excluir' clicado para link: {}",
                            getLinkAtual() != null ? getLinkAtual().getTitulo() : "null");
                    handleExcluir(getLinkAtual());
                });
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
        log.trace("[LINK_UTIL] criarBotao: Botão com ícone '{}' criado", icone);
        return btn;
    }

    private void configurarBuscaReativa() {
        log.debug("[LINK_UTIL] configurarBuscaReativa: Configurando filtro de busca reativo");
        FilteredList<LinkUtilModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            log.debug("[LINK_UTIL] Busca alterada: '{}' -> '{}'", oldValue, newValue);
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
        boolean matches = contemTermo(link.getTitulo(), termo) ||
                contemTermo(link.getDescricao(), termo) ||
                contemTermo(link.getCategoria() != null ? link.getCategoria().getLabel() : null, termo) ||
                contemTermo(link.getTags(), termo);
        if (matches) {
            log.trace("[LINK_UTIL] atendeCriterioDeBusca: Link '{}' corresponde ao termo '{}'", link.getTitulo(),
                    termo);
        }
        return matches;
    }

    private boolean contemTermo(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    @FXML
    private void handleSalvar() {
        log.debug("[LINK_UTIL] handleSalvar: Iniciando salvamento de link");
        if (isFormularioInvalido()) {
            log.warn("[LINK_UTIL] handleSalvar: Formulário inválido (campos obrigatórios não preenchidos)");
            return;
        }

        LinkUtilModel link = (linkEmEdicao != null) ? linkEmEdicao : new LinkUtilModel();
        preencherModeloComFormulario(link);
        log.info("[LINK_UTIL] handleSalvar: Salvando link '{}' (edição={})", link.getTitulo(), linkEmEdicao != null);
        service.salvar(link);
        limparFormulario();
    }

    private boolean isFormularioInvalido() {
        boolean invalido = txtTitulo.getText().isBlank() || txtUrl.getText().isBlank()
                || comboCategoria.getValue() == null;
        if (invalido) {
            log.debug("[LINK_UTIL] isFormularioInvalido: true (titulo vazio={}, url vazia={}, categoria nula={})",
                    txtTitulo.getText().isBlank(), txtUrl.getText().isBlank(), comboCategoria.getValue() == null);
        }
        return invalido;
    }

    private void preencherModeloComFormulario(LinkUtilModel link) {
        log.trace("[LINK_UTIL] preencherModeloComFormulario: Preenchendo modelo com dados da UI");
        link.setTitulo(txtTitulo.getText());
        link.setUrl(txtUrl.getText());
        link.setDescricao(txtDescricao.getText());
        link.setCategoria(comboCategoria.getValue());
        link.setTags(txtTags.getText());
    }

    private void prepararEdicao(LinkUtilModel link) {
        if (link == null) {
            log.warn("[LINK_UTIL] prepararEdicao: Tentativa de editar link nulo");
            return;
        }

        log.info("[LINK_UTIL] prepararEdicao: Preparando edição do link '{}' (ID={})", link.getTitulo(), link.getId());
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
        if (link == null) {
            log.warn("[LINK_UTIL] handleExcluir: Tentativa de excluir link nulo");
            return;
        }

        log.info("[LINK_UTIL] handleExcluir: Excluindo link '{}' (ID={})", link.getTitulo(), link.getId());
        service.excluir(link.getId());
    }

    @FXML
    private void limparFormulario() {
        log.debug("[LINK_UTIL] limparFormulario: Resetando formulário e modo de edição");
        this.linkEmEdicao = null;

        txtTitulo.clear();
        txtUrl.clear();
        txtDescricao.clear();
        txtTags.clear();
        comboCategoria.getSelectionModel().clearSelection();

        paneFormulario.setText(MSG_TITULO_PADRAO);
    }

    public void recarregarLinks() {
        log.debug("[LINK_UTIL] recarregarLinks: Recarregando lista de links do repositório via service");
        var links = service.listarTodos();
        masterData.setAll(links);
        log.info("[LINK_UTIL] recarregarLinks: {} links carregados", links.size());
    }

    private void abrirUrlNoNavegador(LinkUtilModel link) {
        if (link != null && link.getUrl() != null && !link.getUrl().isBlank()) {
            log.info("[LINK_UTIL] abrirUrlNoNavegador: Abrindo URL '{}' para link '{}'", link.getUrl(),
                    link.getTitulo());
            hostServices.showDocument(link.getUrl());
        } else {
            log.warn("[LINK_UTIL] abrirUrlNoNavegador: Tentativa de abrir URL inválida para link {}",
                    link != null ? link.getTitulo() : "nulo");
        }
    }

    @Override
    public void dispose() {
        log.info("[LINK_UTIL] dispose: Desinscrevendo do event hub");
        eventHub.desinscrever(this::recarregarLinks);
    }
}