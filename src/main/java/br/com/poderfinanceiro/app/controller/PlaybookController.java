package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.facade.IPlaybookFacade;
import br.com.poderfinanceiro.app.util.AsyncUtils;
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
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>PlaybookController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pelo Playbook de Vendas. Implementa
 * o padrão <b>Humble Object</b>, delegando a persistência, filtros e
 * estruturação via IA para a {@link IPlaybookFacade}.
 * </p>
 */
@Controller
public class PlaybookController implements Initializable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(PlaybookController.class);
    private static final String LOG_PREFIX = "[PlaybookController]";

    private static final String MODELO_PADRAO = "gemini-3.5-flash";
    private static final String NOME_CONSULTOR_PLACEHOLDER = "%CONSULTOR%";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IPlaybookFacade playbookFacade;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TextField txtBusca;
    @FXML private TreeView<String> treeViewScripts;

    @FXML private TextField txtTitulo;
    @FXML private TextField txtCategoria;
    @FXML private TextArea txtConteudo;

    @FXML private StackPane stackDica;
    @FXML private Label lblDica;
    @FXML private TextArea txtDica;

    @FXML private Button btnCopiar, btnEditar, btnExcluir;
    @FXML private HBox boxAcoesTopo, boxAcoesEdicao;

    @FXML private VBox overlayIA;
    @FXML private TextArea txtInputIA;
    @FXML private Button btnProcessarIA;
    @FXML private ComboBox<String> cmbModeloIA;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private List<PlaybookItemModel> todosOsItens;
    private PlaybookItemModel itemSelecionadoAtual;
    private boolean modoEdicao = false;
    private boolean criandoNovo = false;
    private boolean modelosCarregados = false;

    public PlaybookController(IPlaybookFacade playbookFacade) {
        this.playbookFacade = playbookFacade;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @Override public void initialize(URL location, ResourceBundle resources) {
        log.info("{} [TELEMETRIA] Inicializando interface do Playbook...", LOG_PREFIX);

        AsyncUtils.executarTaskAsync(playbookFacade::listarTodosOsScripts, itens -> {
            this.todosOsItens = itens;
            construirArvore(this.todosOsItens, false);
            log.info("{} [TELEMETRIA] Playbook inicializado com {} itens.", LOG_PREFIX, itens.size());
        }, erro -> log.error("{} [SISTEMA] Erro ao carregar playbook: {}", LOG_PREFIX, erro.getMessage()));

        configurarSelecaoNaArvore();
        configurarFiltroDeBusca();
        alternarModoVisualizacao(false);

        if (cmbModeloIA != null) {
            cmbModeloIA.getItems().add(MODELO_PADRAO);
            cmbModeloIA.getSelectionModel().selectFirst();
        }
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DA ÁRVORE E FILTROS
    // ==========================================================================================
    private void configurarFiltroDeBusca() {
        log.trace("{} [UI] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("{} [UI] Termo de busca alterado: '{}'", LOG_PREFIX, newVal);
            AsyncUtils.executarTaskAsync(() -> playbookFacade.filtrarScripts(newVal),
                    itensFiltrados -> construirArvore(itensFiltrados, newVal != null && !newVal.trim().isEmpty()),
                    erro -> log.error("{} [SISTEMA] Erro ao filtrar scripts: {}", LOG_PREFIX, erro.getMessage()));
        });
    }

    private void construirArvore(List<PlaybookItemModel> itensParaExibir, boolean expandirPastas) {
        log.trace("{} [UI] Construindo árvore visual com {} itens.", LOG_PREFIX, itensParaExibir != null ? itensParaExibir.size() : 0);
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
        log.trace("{} [UI] Configurando listener de seleção na árvore.", LOG_PREFIX);
        treeViewScripts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (modoEdicao)
                return;

            if (newVal != null && newVal.isLeaf() && newVal.getParent() != null && newVal.getParent().getValue() != null) {
                log.debug("{} [UI] Script selecionado: '{}'", LOG_PREFIX, newVal.getValue());
                exibirDetalhesDoScript(newVal.getValue(), newVal.getParent().getValue());
            } else {
                limparPainelDeDetalhes();
            }
        });
    }

    private void exibirDetalhesDoScript(String titulo, String categoria) {
        log.trace("{} [UI] Exibindo detalhes do script: {}", LOG_PREFIX, titulo);
        String nomeConsultor = playbookFacade.obterNomeConsultorLogado();

        todosOsItens.stream().filter(i -> i.getTitulo().equals(titulo) && i.getCategoria().equals(categoria)).findFirst()
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
    @FXML private void handleNovo() {
        log.info("{} [TELEMETRIA] Usuário solicitou criação de novo script.", LOG_PREFIX);
        this.itemSelecionadoAtual = new PlaybookItemModel();
        this.criandoNovo = true;
        txtTitulo.setText("");
        txtCategoria.setText("Nova Categoria / Subcategoria");
        txtConteudo.setText("Olá, me chamo " + NOME_CONSULTOR_PLACEHOLDER + "...");
        lblDica.setText("");

        alternarModoVisualizacao(true);
        txtTitulo.requestFocus();
    }

    @FXML private void handleEditar() {
        if (itemSelecionadoAtual == null)
            return;
        log.info("{} [TELEMETRIA] Usuário solicitou edição do script: {}", LOG_PREFIX, itemSelecionadoAtual.getTitulo());
        this.criandoNovo = false;
        txtConteudo.setText(itemSelecionadoAtual.getConteudo());
        alternarModoVisualizacao(true);
    }

    @FXML private void handleExcluir() {
        if (itemSelecionadoAtual != null) {
            log.warn("{} [TELEMETRIA] Usuário solicitou exclusão do script: {}", LOG_PREFIX, itemSelecionadoAtual.getTitulo());
            todosOsItens.remove(itemSelecionadoAtual);

            AsyncUtils.executarTaskAsync(() -> {
                playbookFacade.salvarTodosOsScripts(todosOsItens);
                return null;
            }, sucesso -> {
                log.info("{} [AUDITORIA] Script excluído com sucesso.", LOG_PREFIX);
                limparPainelDeDetalhes();
                construirArvore(todosOsItens, false);
            }, erro -> log.error("{} [AUDITORIA] Erro ao excluir script: {}", LOG_PREFIX, erro.getMessage()));
        }
    }

    @FXML private void handleSalvar() {
        if (txtTitulo.getText().trim().isEmpty() || txtCategoria.getText().trim().isEmpty()) {
            log.warn("{} [NEGOCIO] Salvamento bloqueado: Título ou categoria vazios.", LOG_PREFIX);
            return;
        }

        log.info("{} [TELEMETRIA] Salvando script: {}", LOG_PREFIX, txtTitulo.getText());
        itemSelecionadoAtual.setTitulo(txtTitulo.getText().trim());
        itemSelecionadoAtual.setCategoria(txtCategoria.getText().trim());
        itemSelecionadoAtual.setConteudo(txtConteudo.getText().trim());
        itemSelecionadoAtual.setDica(txtDica.getText().trim());

        if (criandoNovo)
            todosOsItens.add(itemSelecionadoAtual);

        AsyncUtils.executarTaskAsync(() -> {
            playbookFacade.salvarTodosOsScripts(todosOsItens);
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] Script salvo com sucesso.", LOG_PREFIX);
            construirArvore(todosOsItens, true);
            alternarModoVisualizacao(false);
            exibirDetalhesDoScript(itemSelecionadoAtual.getTitulo(), itemSelecionadoAtual.getCategoria());
        }, erro -> log.error("{} [AUDITORIA] Erro ao salvar script: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML private void handleCancelar() {
        log.trace("{} [UI] Edição cancelada pelo usuário.", LOG_PREFIX);
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

    @FXML private void handleCopiar() {
        String textoParaCopiar = txtConteudo.getText();
        if (textoParaCopiar != null && !textoParaCopiar.isEmpty()) {
            log.info("{} [TELEMETRIA] Usuário copiou o conteúdo do script.", LOG_PREFIX);
            ClipboardContent content = new ClipboardContent();
            content.putString(textoParaCopiar);
            Clipboard.getSystemClipboard().setContent(content);

            String textoOriginal = btnCopiar.getText();
            btnCopiar.setText("Copiado! ✓");
            btnCopiar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");

            new Timer().schedule(new TimerTask() {
                @Override public void run() {
                    Platform.runLater(() -> {
                        btnCopiar.setText(textoOriginal);
                        btnCopiar.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");
                    });
                }
            }, 2000);
        }
    }

    // ==========================================================================================
    // MÓDULO 8: INTELIGÊNCIA ARTIFICIAL (GEMINI)
    // ==========================================================================================
    @FXML private void handleGerarComIA() {
        log.info("{} [TELEMETRIA] Abrindo overlay de geração com IA.", LOG_PREFIX);
        txtInputIA.clear();
        overlayIA.setVisible(true);
        txtInputIA.requestFocus();

        if (!modelosCarregados && cmbModeloIA != null) {
            AsyncUtils.executarTaskAsync(playbookFacade::listarModelosIADisponiveis, modelos -> {
                if (!modelos.isEmpty()) {
                    cmbModeloIA.getItems().setAll(modelos);
                    cmbModeloIA.getSelectionModel().select(modelos.contains(MODELO_PADRAO) ? MODELO_PADRAO : modelos.get(0));
                    modelosCarregados = true;
                }
            }, erro -> log.error("{} [SISTEMA] Erro ao carregar modelos Gemini: {}", LOG_PREFIX, erro.getMessage()));
        }
    }

    @FXML private void fecharOverlayIA() {
        log.trace("{} [UI] Fechando overlay de IA.", LOG_PREFIX);
        overlayIA.setVisible(false);
    }

    @FXML private void processarTextoComIA() {
        String textoBruto = txtInputIA.getText();
        if (textoBruto == null || textoBruto.trim().isEmpty()) {
            log.warn("{} [NEGOCIO] Processamento bloqueado: Texto de entrada vazio.", LOG_PREFIX);
            return;
        }

        String modelo = (cmbModeloIA != null && cmbModeloIA.getValue() != null) ? cmbModeloIA.getValue() : MODELO_PADRAO;
        log.info("{} [TELEMETRIA] Enviando texto para estruturação via IA. Modelo: {}", LOG_PREFIX, modelo);

        btnProcessarIA.setDisable(true);
        btnProcessarIA.setText("Processando...");

        AsyncUtils.executarTaskAsync(() -> playbookFacade.estruturarTextoComIA(textoBruto, modelo), this::aplicarEstruturaIA,
                erro -> aplicarErroIA(erro, textoBruto));
    }

    private void aplicarEstruturaIA(JsonNode node) {
        log.info("{} [AUDITORIA] Estruturação via IA concluída com sucesso.", LOG_PREFIX);
        this.itemSelecionadoAtual = new PlaybookItemModel();
        this.criandoNovo = true;

        txtCategoria.setText(node.has("categoria") ? node.get("categoria").asText() : "Geral");
        txtTitulo.setText(node.has("titulo") ? node.get("titulo").asText() : "Script via IA");
        txtConteudo.setText(node.has("conteudo") ? node.get("conteudo").asText() : "");
        lblDica.setText(node.has("dica") ? node.get("dica").asText() : "");

        fecharOverlayIA();
        alternarModoVisualizacao(true);
        btnProcessarIA.setDisable(false);
        btnProcessarIA.setText("✨ Estruturar com Gemini");
    }

    private void aplicarErroIA(Throwable erro, String textoBruto) {
        log.error("{} [AUDITORIA] Falha na estruturação via IA: {}", LOG_PREFIX, erro.getMessage());
        txtInputIA.setText("⚠️ Falha ao estruturar dados ou servidores ocupados. Tente outro modelo.\n\n" + textoBruto);
        btnProcessarIA.setDisable(false);
        btnProcessarIA.setText("✨ Estruturar com Gemini");
    }
}
