package br.com.poderfinanceiro.app.presentation.controller.suporte;

import br.com.poderfinanceiro.app.application.facade.IPlaybookFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.presentation.ui.state.IAModelRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>PlaybookController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pelo Playbook de Vendas.
 * Implementa o padrão <b>Humble Object</b>, utilizando o IAModelRegistry para
 * padronização
 * de modelos e AsyncUtils para orquestração de I/O em Virtual Threads (Java
 * 25).
 * </p>
 */
@Component
@Scope("prototype")
public class PlaybookController implements Initializable, Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(PlaybookController.class);
    private static final String LOG_PREFIX = "[PlaybookController]";
    private static final String NOME_CONSULTOR_PLACEHOLDER = "%CONSULTOR%";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IPlaybookFacade playbookFacade;
    private final IAModelRegistry modelRegistry;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private TextField txtBusca;
    @FXML
    private TreeView<String> treeViewScripts;
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtCategoria;
    @FXML
    private TextArea txtConteudo;
    @FXML
    private StackPane stackDica;
    @FXML
    private Label lblDica;
    @FXML
    private TextArea txtDica;
    @FXML
    private Button btnCopiar, btnEditar, btnExcluir;
    @FXML
    private HBox boxAcoesTopo, boxAcoesEdicao;
    @FXML
    private VBox overlayIA;
    @FXML
    private TextArea txtInputIA;
    @FXML
    private Button btnProcessarIA;
    @FXML
    private ComboBox<String> cmbModeloIA;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private List<PlaybookItemModel> todosOsItens;
    private PlaybookItemModel itemSelecionadoAtual;
    private boolean modoEdicao = false;
    private boolean criandoNovo = false;

    public PlaybookController(IPlaybookFacade playbookFacade, IAModelRegistry modelRegistry) {
        this.playbookFacade = playbookFacade;
        this.modelRegistry = modelRegistry;
        log.info("{} [SISTEMA] Controlador do Playbook instanciado com suporte a Registry Global.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("{} [SISTEMA] Inicializando interface do Playbook Cognitivo.", LOG_PREFIX);

        carregarDadosIniciais();
        configurarSelecaoNaArvore();
        configurarFiltroDeBusca();
        configurarModelosIA();
        alternarModoVisualizacao(false);

        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos do Playbook.", LOG_PREFIX);
        if (todosOsItens != null)
            todosOsItens.clear();
        itemSelecionadoAtual = null;
    }

    private void carregarDadosIniciais() {
        log.info("{} [TELEMETRIA] Solicitando carga assíncrona de scripts.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(
                playbookFacade::listarTodosOsScripts,
                itens -> {
                    this.todosOsItens = new ArrayList<>(itens);
                    construirArvore(this.todosOsItens, false);
                    log.info("{} [AUDITORIA] Playbook carregado com {} scripts.", LOG_PREFIX, itens.size());
                },
                erro -> log.error("{} [SISTEMA] Falha ao carregar base de scripts: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void configurarModelosIA() {
        log.debug("{} [SISTEMA] Vinculando ComboBox ao Registro Global de Modelos.", LOG_PREFIX);
        cmbModeloIA.setItems(modelRegistry.getModelosDisponiveis());
        cmbModeloIA.getSelectionModel().select(modelRegistry.getModeloPadrao());
        modelRegistry.carregarModelos();
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DA ÁRVORE E FILTROS
    // ==========================================================================================
    private void configurarFiltroDeBusca() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.trace("{} [TELEMETRIA] Filtro de busca alterado: '{}'", LOG_PREFIX, newVal);
            AsyncUtils.executarTaskAsync(
                    () -> playbookFacade.filtrarScripts(newVal),
                    itensFiltrados -> construirArvore(itensFiltrados, ValidationUtils.isPreenchido(newVal)),
                    erro -> log.error("{} [SISTEMA] Erro ao filtrar scripts: {}", LOG_PREFIX, erro.getMessage()));
        });
    }

    private void construirArvore(List<PlaybookItemModel> itensParaExibir, boolean expandirPastas) {
        if (itensParaExibir == null || itensParaExibir.isEmpty()) {
            treeViewScripts.setRoot(new TreeItem<>("Nenhum resultado encontrado"));
            return;
        }

        Map<String, List<PlaybookItemModel>> itensPorCategoria = itensParaExibir.stream()
                .collect(Collectors.groupingBy(PlaybookItemModel::getCategoria, TreeMap::new, Collectors.toList()));

        TreeItem<String> rootItem = new TreeItem<>("Playbook");
        rootItem.setExpanded(true);

        for (Map.Entry<String, List<PlaybookItemModel>> entry : itensPorCategoria.entrySet()) {
            TreeItem<String> categoriaNode = new TreeItem<>(entry.getKey());
            categoriaNode.setExpanded(expandirPastas);

            List<PlaybookItemModel> scriptsOrdenados = entry.getValue().stream()
                    .sorted(Comparator.comparing(PlaybookItemModel::getTitulo, String.CASE_INSENSITIVE_ORDER)).toList();

            for (PlaybookItemModel item : scriptsOrdenados) {
                categoriaNode.getChildren().add(new TreeItem<>(item.getTitulo()));
            }
            rootItem.getChildren().add(categoriaNode);
        }
        treeViewScripts.setRoot(rootItem);
    }

    private void configurarSelecaoNaArvore() {
        treeViewScripts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (modoEdicao)
                return;

            if (newVal != null && newVal.isLeaf() && newVal.getParent() != null
                    && newVal.getParent().getValue() != null) {
                log.debug("{} [TELEMETRIA] Script selecionado na árvore: '{}'", LOG_PREFIX, newVal.getValue());
                exibirDetalhesDoScript(newVal.getValue(), newVal.getParent().getValue());
            } else {
                limparPainelDeDetalhes();
            }
        });
    }

    private void exibirDetalhesDoScript(String titulo, String categoria) {
        String nomeConsultor = playbookFacade.obterNomeConsultorLogado();

        todosOsItens.stream()
                .filter(i -> i.getTitulo().equals(titulo) && i.getCategoria().equals(categoria))
                .findFirst()
                .ifPresent(item -> {
                    this.itemSelecionadoAtual = item;
                    txtTitulo.setText(item.getTitulo());
                    txtCategoria.setText(item.getCategoria());
                    txtConteudo.setText(item.getConteudo().replace(NOME_CONSULTOR_PLACEHOLDER, nomeConsultor));
                    lblDica.setText(item.getDica() != null ? item.getDica() : "");

                    btnCopiar.setDisable(false);
                    btnEditar.setDisable(false);
                    btnExcluir.setDisable(false);
                });
    }

    // ==========================================================================================
    // MÓDULO 7: AÇÕES DE CRUD E INTERFACE
    // ==========================================================================================
    @FXML
    private void handleNovo() {
        log.info("{} [TELEMETRIA] Iniciando criação de novo script manual.", LOG_PREFIX);
        this.itemSelecionadoAtual = new PlaybookItemModel();
        this.criandoNovo = true;
        txtTitulo.setText("");
        txtCategoria.setText("Nova Categoria");
        txtConteudo.setText("Olá, me chamo " + NOME_CONSULTOR_PLACEHOLDER + "...");
        lblDica.setText("");

        alternarModoVisualizacao(true);
        txtTitulo.requestFocus();
    }

    @FXML
    private void handleEditar() {
        if (itemSelecionadoAtual == null)
            return;
        log.info("{} [TELEMETRIA] Editando script: {}", LOG_PREFIX, itemSelecionadoAtual.getTitulo());
        this.criandoNovo = false;
        txtConteudo.setText(itemSelecionadoAtual.getConteudo());
        alternarModoVisualizacao(true);
    }

    @FXML
    private void handleExcluir() {
        if (itemSelecionadoAtual == null)
            return;

        log.warn("{} [AUDITORIA] Solicitando exclusão do script: {}", LOG_PREFIX, itemSelecionadoAtual.getTitulo());
        todosOsItens.remove(itemSelecionadoAtual);

        AsyncUtils.executarTaskAsync(
                () -> {
                    playbookFacade.salvarTodosOsScripts(new ArrayList<>(todosOsItens));
                    return null;
                },
                sucesso -> {
                    log.info("{} [AUDITORIA] Script removido com sucesso.", LOG_PREFIX);
                    limparPainelDeDetalhes();
                    construirArvore(todosOsItens, false);
                },
                erro -> log.error("{} [SISTEMA] Falha ao excluir script: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML
    private void handleSalvar() {
        if (!ValidationUtils.isPreenchido(txtTitulo.getText())
                || !ValidationUtils.isPreenchido(txtCategoria.getText())) {
            log.warn("{} [NEGOCIO] Salvamento abortado: Título ou Categoria ausentes.", LOG_PREFIX);
            return;
        }

        log.info("{} [AUDITORIA] Iniciando persistência do script: {}", LOG_PREFIX, txtTitulo.getText());
        itemSelecionadoAtual.setTitulo(txtTitulo.getText().trim());
        itemSelecionadoAtual.setCategoria(txtCategoria.getText().trim());
        itemSelecionadoAtual.setConteudo(txtConteudo.getText().trim());
        itemSelecionadoAtual.setDica(txtDica.getText().trim());

        if (criandoNovo)
            todosOsItens.add(itemSelecionadoAtual);

        AsyncUtils.executarTaskAsync(
                () -> {
                    playbookFacade.salvarTodosOsScripts(new ArrayList<>(todosOsItens));
                    return null;
                },
                sucesso -> {
                    log.info("{} [AUDITORIA] Script persistido com sucesso.", LOG_PREFIX);
                    construirArvore(todosOsItens, true);
                    alternarModoVisualizacao(false);
                    exibirDetalhesDoScript(itemSelecionadoAtual.getTitulo(), itemSelecionadoAtual.getCategoria());
                },
                erro -> log.error("{} [SISTEMA] Erro ao salvar playbook: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML
    private void handleCancelar() {
        log.trace("{} [UI] Operação cancelada pelo usuário.", LOG_PREFIX);
        alternarModoVisualizacao(false);
        if (criandoNovo)
            limparPainelDeDetalhes();
        else
            exibirDetalhesDoScript(itemSelecionadoAtual.getTitulo(), itemSelecionadoAtual.getCategoria());
    }

    private void alternarModoVisualizacao(boolean editando) {
        this.modoEdicao = editando;
        txtTitulo.setEditable(editando);
        txtCategoria.setEditable(editando);
        txtConteudo.setEditable(editando);
        treeViewScripts.setDisable(editando);

        String bordaAtiva = "-fx-border-color: #ced4da; -fx-background-color: white;";
        String bordaInativa = "-fx-border-color: transparent; -fx-background-color: transparent;";

        txtTitulo.setStyle(txtTitulo.getStyle() + (editando ? bordaAtiva : bordaInativa));
        txtCategoria.setStyle(txtCategoria.getStyle() + (editando ? bordaAtiva : bordaInativa));

        if (editando)
            txtDica.setText(lblDica.getText());

        lblDica.setVisible(!editando);
        lblDica.setManaged(!editando);
        txtDica.setVisible(editando);
        txtDica.setManaged(editando);

        btnCopiar.setVisible(!editando);
        btnCopiar.setManaged(!editando);
        boxAcoesEdicao.setVisible(editando);
        boxAcoesEdicao.setManaged(editando);
        boxAcoesTopo.setDisable(editando);
    }

    private void limparPainelDeDetalhes() {
        this.itemSelecionadoAtual = null;
        txtTitulo.setText("Selecione um Script");
        txtCategoria.setText("");
        txtConteudo.setText("");
        lblDica.setText("");
        btnCopiar.setDisable(true);
        btnEditar.setDisable(true);
        btnExcluir.setDisable(true);
    }

    @FXML
    private void handleCopiar() {
        String textoParaCopiar = txtConteudo.getText();
        if (ValidationUtils.isPreenchido(textoParaCopiar)) {
            log.info("{} [TELEMETRIA] Script copiado para a área de transferência.", LOG_PREFIX);
            ClipboardContent content = new ClipboardContent();
            content.putString(textoParaCopiar);
            Clipboard.getSystemClipboard().setContent(content);

            String textoOriginal = btnCopiar.getText();
            btnCopiar.setText("Copiado! ✓");
            btnCopiar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

            AsyncUtils.executarTaskAsync(() -> {
                Thread.sleep(2000);
                return null;
            },
                    s -> {
                        btnCopiar.setText(textoOriginal);
                        btnCopiar.setStyle(
                                "-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold;");
                    }, null);
        }
    }

    // ==========================================================================================
    // MÓDULO 8: INTELIGÊNCIA ARTIFICIAL (GEMINI)
    // ==========================================================================================
    @FXML
    private void handleGerarComIA() {
        log.info("{} [TELEMETRIA] Abrindo overlay de Engenharia Cognitiva.", LOG_PREFIX);
        txtInputIA.clear();
        overlayIA.setVisible(true);
        txtInputIA.requestFocus();
    }

    @FXML
    private void fecharOverlayIA() {
        log.trace("{} [UI] Fechando overlay de IA.", LOG_PREFIX);
        overlayIA.setVisible(false);
    }

    @FXML
    private void processarTextoComIA() {
        String textoBruto = txtInputIA.getText();
        if (!ValidationUtils.isPreenchido(textoBruto)) {
            log.warn("{} [NEGOCIO] Processamento IA abortado: Texto bruto vazio.", LOG_PREFIX);
            return;
        }

        String modelo = cmbModeloIA.getValue();
        log.info("{} [TELEMETRIA] Solicitando estruturação cognitiva. Modelo: {}", LOG_PREFIX, modelo);

        btnProcessarIA.setDisable(true);
        btnProcessarIA.setText("Processando...");

        AsyncUtils.executarTaskAsync(
                () -> playbookFacade.estruturarTextoComIA(textoBruto, modelo),
                this::aplicarEstruturaIA,
                erro -> aplicarErroIA(erro, textoBruto));
    }

    private void aplicarEstruturaIA(JsonNode node) {
        log.info("{} [AUDITORIA] Estrutura cognitiva recebida. Mapeando para o modelo.", LOG_PREFIX);

        String categoria = node.path("categoria").asText("Geral");
        String titulo = node.path("titulo").asText("Script via IA");
        String conteudo = node.path("conteudo").asText("");
        String dica = node.path("dica").asText("Dica de Ouro");

        Platform.runLater(() -> {
            this.itemSelecionadoAtual = new PlaybookItemModel();
            this.criandoNovo = true;
            txtCategoria.setText(categoria);
            txtTitulo.setText(titulo);
            txtConteudo.setText(conteudo);
            lblDica.setText(dica);
            txtDica.setText(dica);
            fecharOverlayIA();
            alternarModoVisualizacao(true);
            btnProcessarIA.setDisable(false);
            btnProcessarIA.setText("✨ Estruturar com Gemini");
            log.info("{} [AUDITORIA] Script estruturado aplicado à interface.", LOG_PREFIX);
        });
    }

    private void aplicarErroIA(Throwable erro, String textoBruto) {
        log.error("{} [AUDITORIA] Falha na estruturação cognitiva: {}", LOG_PREFIX, erro.getMessage());
        txtInputIA.setText("⚠️ Falha ao estruturar dados. Tente outro modelo.\n\n" + textoBruto);
        btnProcessarIA.setDisable(false);
        btnProcessarIA.setText("✨ Estruturar com Gemini");
    }
}
