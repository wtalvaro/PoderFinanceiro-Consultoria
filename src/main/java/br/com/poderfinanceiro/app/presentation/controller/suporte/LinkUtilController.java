package br.com.poderfinanceiro.app.presentation.controller.suporte;

import br.com.poderfinanceiro.app.application.facade.ILinkUtilFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.domain.event.LinkUtilUIEventHub;
import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.domain.model.enums.LabeledModel;
import javafx.application.HostServices;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.scene.Cursor;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>LinkUtilController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela gestão de Links e Atalhos.
 * Implementa o padrão <b>Humble Object</b>, delegando a persistência e filtros
 * para a {@link ILinkUtilFacade}.
 * </p>
 */
@Component
public class LinkUtilController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(LinkUtilController.class);
    private static final String LOG_PREFIX = "[LinkUtilController]";

    private static final String MSG_TITULO_PADRAO = "🔗 Gestão de Links e Atalhos";
    private static final String MSG_TITULO_EDICAO = "✏️ Editando: ";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ILinkUtilFacade linkFacade;
    private final LinkUtilUIEventHub eventHub;
    private final HostServices hostServices;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private ComboBox<CategoriaLinkModel> comboCategoria;
    @FXML private TextField txtTitulo, txtUrl, txtDescricao, txtTags, txtBusca;
    @FXML private TableView<LinkUtilModel> tableLinks;
    @FXML private TableColumn<LinkUtilModel, String> colCategoria, colTitulo, colDescricao;
    @FXML private TableColumn<LinkUtilModel, Void> colAcao;
    @FXML private TitledPane paneFormulario;
    @FXML private ScrollPane scrollPrincipal;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<LinkUtilModel> listaLinks = FXCollections.observableArrayList();
    private LinkUtilModel linkEmEdicao;

    public LinkUtilController(ILinkUtilFacade linkFacade, LinkUtilUIEventHub eventHub, HostServices hostServices) {
        this.linkFacade = linkFacade;
        this.eventHub = eventHub;
        this.hostServices = hostServices;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Links Úteis...", LOG_PREFIX);
        configurarComboCategoria();
        configurarTabela();
        configurarBuscaReativa();

        eventHub.inscrever(this::recarregarLinks);
        recarregarLinks();

        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo do hub de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::recarregarLinks);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    public void recarregarLinks() {
        log.trace("{} [TELEMETRIA] Recarregando lista de links.", LOG_PREFIX);
        filtrarLinks(txtBusca.getText());
    }

    private void configurarBuscaReativa() {
        log.trace("{} [UI] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("{} [UI] Termo de busca alterado: '{}'", LOG_PREFIX, newVal);
            filtrarLinks(newVal);
        });
    }

    private void filtrarLinks(String termo) {
        AsyncUtils.executarTaskAsync(() -> linkFacade.filtrarLinks(termo), linksFiltrados -> {
            listaLinks.setAll(linksFiltrados);
            tableLinks.setItems(listaLinks);
            log.info("{} [TELEMETRIA] Tabela atualizada. {} registro(s) exibido(s).", LOG_PREFIX, linksFiltrados.size());
        }, erro -> log.error("{} [SISTEMA] Erro ao filtrar links: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarComboCategoria() {
        log.trace("{} [UI] Carregando categorias no ComboBox.", LOG_PREFIX);
        configurarCombo(comboCategoria, CategoriaLinkModel.values(), CategoriaLinkModel::fromString);
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values, Function<String, T> searcher) {
        combo.getItems().setAll(values);
        combo.setConverter(new StringConverter<T>() {
            @Override public String toString(T obj) {
                return (obj != null) ? obj.getLabel() : "";
            }

            @Override public T fromString(String str) {
                return searcher.apply(str);
            }
        });
    }

    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da tabela.", LOG_PREFIX);
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getLabel()));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colDescricao.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getDescricao() != null ? data.getValue().getDescricao() : ""));
        configurarColunaAcoes();
    }

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

            @Override protected void updateItem(Void item, boolean empty) {
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

    // ==========================================================================================
    // MÓDULO 8: AÇÕES DE NEGÓCIO (CRUD)
    // ==========================================================================================
    @FXML private void handleSalvar() {
        log.info("{} [TELEMETRIA] Tentativa de salvar link.", LOG_PREFIX);
        if (isFormularioInvalido()) {
            log.warn("{} [NEGOCIO] Salvamento bloqueado: Campos obrigatórios ausentes.", LOG_PREFIX);
            return;
        }

        LinkUtilModel link = (linkEmEdicao != null) ? linkEmEdicao : new LinkUtilModel();
        preencherModeloComFormulario(link);

        AsyncUtils.executarTaskAsync(() -> linkFacade.salvarLink(link), salvo -> {
            log.info("{} [AUDITORIA] Link salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
            limparFormulario();
            recarregarLinks();
        }, erro -> log.error("{} [AUDITORIA] Falha ao persistir link: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void handleExcluir(LinkUtilModel link) {
        if (link == null)
            return;
        log.warn("{} [TELEMETRIA] Usuário solicitou exclusão do link ID: {}", LOG_PREFIX, link.getId());

        AsyncUtils.executarTaskAsync(() -> {
            linkFacade.excluirLink(link.getId());
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] Link ID {} excluído com sucesso.", LOG_PREFIX, link.getId());
            recarregarLinks();
        }, erro -> log.error("{} [AUDITORIA] Falha ao excluir link ID {}: {}", LOG_PREFIX, link.getId(), erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO FORMULÁRIO
    // ==========================================================================================
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
        log.info("{} [UI] Preparando formulário para edição do link ID: {}", LOG_PREFIX, link.getId());

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

    @FXML private void limparFormulario() {
        log.trace("{} [UI] Resetando formulário.", LOG_PREFIX);
        this.linkEmEdicao = null;
        txtTitulo.clear();
        txtUrl.clear();
        txtDescricao.clear();
        txtTags.clear();
        comboCategoria.getSelectionModel().clearSelection();
        paneFormulario.setText(MSG_TITULO_PADRAO);
    }

    // ==========================================================================================
    // MÓDULO 10: INTEGRAÇÕES EXTERNAS
    // ==========================================================================================
    private void abrirUrlNoNavegador(LinkUtilModel link) {
        if (link != null && link.getUrl() != null && !link.getUrl().isBlank()) {
            log.info("{} [TELEMETRIA] Abrindo URL no navegador: {}", LOG_PREFIX, link.getUrl());
            hostServices.showDocument(link.getUrl());
        } else {
            log.warn("{} [NEGOCIO] Tentativa de abrir URL inválida.", LOG_PREFIX);
        }
    }
}
