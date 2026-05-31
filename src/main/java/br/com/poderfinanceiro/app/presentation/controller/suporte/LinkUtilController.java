package br.com.poderfinanceiro.app.presentation.controller.suporte;

import br.com.poderfinanceiro.app.application.facade.ILinkUtilFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>LinkUtilController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela gestão de Links e Atalhos.
 * Implementa o padrão <b>Humble Object</b>, utilizando utilitários Gold
 * Standard
 * para garantir fluidez e integridade de dados.
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
    private final SummaryGeneratorUtils summaryUtils; // Injetado para geração de contexto

    public LinkUtilController(ILinkUtilFacade linkFacade,
            LinkUtilUIEventHub eventHub,
            HostServices hostServices,
            SummaryGeneratorUtils summaryUtils) {
        this.linkFacade = linkFacade;
        this.eventHub = eventHub;
        this.hostServices = hostServices;
        this.summaryUtils = summaryUtils;
        log.debug("{} [SISTEMA] Controlador instanciado com suporte a SummaryUtils.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
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

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<LinkUtilModel> listaLinks = FXCollections.observableArrayList();
    private LinkUtilModel linkEmEdicao;

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Links Úteis...", LOG_PREFIX);
        configurarComboCategoria();
        configurarTabela();
        configurarBuscaReativa();

        eventHub.inscrever(this::recarregarLinks);
        recarregarLinks();

        log.debug("{} [SISTEMA] Inicialização da UI concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos e desinscrevendo do hub de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::recarregarLinks);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    public void recarregarLinks() {
        log.trace("{} [TELEMETRIA] Solicitando recarregamento da lista.", LOG_PREFIX);
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
            log.info("{} [TELEMETRIA] Tabela atualizada. {} registro(s) exibido(s).", LOG_PREFIX,
                    linksFiltrados.size());
        }, erro -> log.error("{} [SISTEMA] Falha na filtragem assíncrona: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarComboCategoria() {
        log.trace("{} [UI] Carregando categorias no ComboBox.", LOG_PREFIX);
        configurarCombo(comboCategoria, CategoriaLinkModel.values(), CategoriaLinkModel::fromString);
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

    private void configurarTabela() {
        log.trace("{} [UI] Configurando colunas da tabela.", LOG_PREFIX);
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getLabel()));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colDescricao.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getDescricao() != null ? data.getValue().getDescricao() : ""));
        configurarColunaAcoes();
    }

    private void configurarColunaAcoes() {
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotao("🌐", "flat");
            private final Button btnEditar = criarBotao("✏️", "flat");
            private final Button btnCopiar = criarBotao("📋", "flat"); // Novo: Copiar Contexto
            private final Button btnExcluir = criarBotao("🗑️", "flat", "danger");
            private final HBox container = new HBox(5, btnAbrir, btnEditar, btnCopiar, btnExcluir);

            {
                btnAbrir.setOnAction(e -> abrirUrlNoNavegador(getLinkAtual()));
                btnEditar.setOnAction(e -> prepararEdicao(getLinkAtual()));
                btnCopiar.setOnAction(e -> handleCopiarContexto(getLinkAtual()));
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

    // ==========================================================================================
    // MÓDULO 8: AÇÕES DE NEGÓCIO (CRUD)
    // ==========================================================================================
    @FXML
    private void handleSalvar() {
        log.info("{} [TELEMETRIA] Iniciando processo de salvamento de link.", LOG_PREFIX);
        if (isFormularioInvalido()) {
            log.warn("{} [NEGOCIO] Salvamento abortado: Validação de campos obrigatórios falhou.", LOG_PREFIX);
            return;
        }

        LinkUtilModel link = (linkEmEdicao != null) ? linkEmEdicao : new LinkUtilModel();
        preencherModeloComFormulario(link);

        AsyncUtils.executarTaskAsync(() -> linkFacade.salvarLink(link), salvo -> {
            log.info("{} [AUDITORIA] Link ID {} persistido com sucesso.", LOG_PREFIX, salvo.getId());
            limparFormulario();
            recarregarLinks();
        }, erro -> log.error("{} [AUDITORIA] Erro crítico ao salvar link: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void handleExcluir(LinkUtilModel link) {
        if (link == null)
            return;
        log.warn("{} [TELEMETRIA] Usuário solicitou exclusão do link ID: {}", LOG_PREFIX, link.getId());

        AsyncUtils.executarTaskAsync(() -> {
            linkFacade.excluirLink(link.getId());
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] Link ID {} removido do sistema.", LOG_PREFIX, link.getId());
            recarregarLinks();
        }, erro -> log.error("{} [AUDITORIA] Falha ao excluir link: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void handleCopiarContexto(LinkUtilModel link) {
        if (link == null)
            return;
        log.info("{} [TELEMETRIA] Gerando contexto JSON para o link ID: {}", LOG_PREFIX, link.getId());

        String json = summaryUtils.gerarJsonLinksUteis(List.of(link));
        ClipboardContent content = new ClipboardContent();
        content.putString(json);
        Clipboard.getSystemClipboard().setContent(content);

        log.info("{} [AUDITORIA] Contexto do link copiado para o Clipboard.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO FORMULÁRIO
    // ==========================================================================================
    private boolean isFormularioInvalido() {
        return txtTitulo.getText().isBlank() || txtUrl.getText().isBlank() || comboCategoria.getValue() == null;
    }

    private void preencherModeloComFormulario(LinkUtilModel link) {
        log.trace("{} [NEGOCIO] Saneando e preenchendo modelo com dados da UI.", LOG_PREFIX);
        link.setTitulo(txtTitulo.getText().trim());
        link.setUrl(txtUrl.getText().trim());
        link.setDescricao(txtDescricao.getText() != null ? txtDescricao.getText().trim() : "");
        link.setCategoria(comboCategoria.getValue());
        link.setTags(txtTags.getText() != null ? txtTags.getText().trim() : "");
    }

    private void prepararEdicao(LinkUtilModel link) {
        if (link == null)
            return;
        log.info("{} [UI] Carregando link ID {} para edição.", LOG_PREFIX, link.getId());

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

    @FXML
    private void limparFormulario() {
        log.trace("{} [UI] Resetando estado do formulário.", LOG_PREFIX);
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
            log.info("{} [TELEMETRIA] Disparando abertura de URL externa: {}", LOG_PREFIX, link.getUrl());
            try {
                hostServices.showDocument(link.getUrl());
            } catch (Exception e) {
                log.error("{} [SISTEMA] Falha ao invocar navegador do SO: {}", LOG_PREFIX, e.getMessage());
            }
        } else {
            log.warn("{} [NEGOCIO] Tentativa de abertura de URL nula ou inválida.", LOG_PREFIX);
        }
    }
}
