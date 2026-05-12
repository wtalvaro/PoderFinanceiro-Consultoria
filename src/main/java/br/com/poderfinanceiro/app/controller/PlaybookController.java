package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.PlaybookService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class PlaybookController implements Initializable {

    @FXML
    private TextField txtBusca;
    @FXML
    private TreeView<String> treeViewScripts;

    // Novos campos editáveis
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtCategoria;
    @FXML
    private TextArea txtConteudo;
    @FXML
    private TextField txtDica;

    // Controles de visibilidade/ação
    @FXML
    private Button btnCopiar;
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnExcluir;
    @FXML
    private HBox boxAcoesTopo;
    @FXML
    private HBox boxAcoesEdicao;

    private final PlaybookService playbookService;
    private final AuthService authService;
    private List<PlaybookItemModel> todosOsItens;

    // Estado da tela
    private boolean modoEdicao = false;
    private PlaybookItemModel itemSelecionadoAtual;
    private boolean criandoNovo = false;

    public PlaybookController(PlaybookService playbookService, AuthService authService) {
        this.playbookService = playbookService;
        this.authService = authService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.todosOsItens = playbookService.listarTudoParaOPlaybook();
        construirArvore(this.todosOsItens, false);
        configurarSelecaoNaArvore();
        configurarFiltroDeBusca();
        alternarModoVisualizacao(false); // Inicia bloqueado
    }

    private void configurarFiltroDeBusca() {
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                construirArvore(this.todosOsItens, false);
                return;
            }
            String termo = newValue.toLowerCase().trim();
            List<PlaybookItemModel> itensFiltrados = todosOsItens.stream()
                    .filter(item -> item.getTitulo().toLowerCase().contains(termo)
                            || item.getCategoria().toLowerCase().contains(termo)
                            || item.getConteudo().toLowerCase().contains(termo))
                    .collect(Collectors.toList());
            construirArvore(itensFiltrados, true);
        });
    }

    private void construirArvore(List<PlaybookItemModel> itensParaExibir, boolean expandirPastas) {
        if (itensParaExibir == null || itensParaExibir.isEmpty()) {
            treeViewScripts.setRoot(new TreeItem<>("Nenhum resultado encontrado"));
            return;
        }

        Map<String, List<PlaybookItemModel>> itensPorCategoria = itensParaExibir.stream()
                .collect(
                        Collectors.groupingBy(PlaybookItemModel::getCategoria, java.util.TreeMap::new, Collectors.toList()));

        TreeItem<String> rootItem = new TreeItem<>("Playbook");
        rootItem.setExpanded(true);

        for (Map.Entry<String, List<PlaybookItemModel>> entry : itensPorCategoria.entrySet()) {
            TreeItem<String> categoriaNode = new TreeItem<>(entry.getKey());
            categoriaNode.setExpanded(expandirPastas);

            List<PlaybookItemModel> scriptsOrdenados = entry.getValue().stream()
                    .sorted(java.util.Comparator.comparing(PlaybookItemModel::getTitulo, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            for (PlaybookItemModel item : scriptsOrdenados) {
                TreeItem<String> scriptNode = new TreeItem<>(item.getTitulo());
                categoriaNode.getChildren().add(scriptNode);
            }
            rootItem.getChildren().add(categoriaNode);
        }
        treeViewScripts.setRoot(rootItem);
    }

    private void configurarSelecaoNaArvore() {
        treeViewScripts.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (modoEdicao)
                return; // Não muda seleção se estiver editando

            if (newValue != null && newValue.isLeaf() && newValue.getParent() != null
                    && newValue.getParent().getValue() != null) {
                exibirDetalhesDoScript(newValue.getValue(), newValue.getParent().getValue());
            } else {
                limparPainelDeDetalhes();
            }
        });
    }

    private void exibirDetalhesDoScript(String tituloDoScript, String categoriaDoScript) {
        String nomeDoConsultor = authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor(a)";

        todosOsItens.stream()
                .filter(item -> item.getTitulo().equals(tituloDoScript)
                        && item.getCategoria().equals(categoriaDoScript))
                .findFirst()
                .ifPresent(item -> {
                    this.itemSelecionadoAtual = item;
                    txtTitulo.setText(item.getTitulo());
                    txtCategoria.setText(item.getCategoria());

                    // Exibe com o nome substituído no modo leitura
                    txtConteudo.setText(item.getConteudo().replace("%CONSULTOR%", nomeDoConsultor));
                    txtDica.setText(item.getDica() != null ? item.getDica() : "");

                    btnCopiar.setDisable(false);
                    btnEditar.setDisable(false);
                    btnExcluir.setDisable(false);
                });
    }

    // ==========================================
    // LÓGICA DE CRUD E EDIÇÃO
    // ==========================================

    @FXML
    private void handleNovo() {
        this.itemSelecionadoAtual = new PlaybookItemModel();
        this.criandoNovo = true;

        txtTitulo.setText("");
        txtCategoria.setText("Nova Categoria / Subcategoria");
        txtConteudo.setText("Olá, me chamo %CONSULTOR%...");
        txtDica.setText("");

        alternarModoVisualizacao(true);
        txtTitulo.requestFocus();
    }

    @FXML
    private void handleEditar() {
        if (itemSelecionadoAtual == null)
            return;
        this.criandoNovo = false;

        // Retorna a tag crua para o TextArea para o usuário não salvar o próprio nome
        // fixo no JSON
        txtConteudo.setText(itemSelecionadoAtual.getConteudo());
        alternarModoVisualizacao(true);
    }

    @FXML
    private void handleExcluir() {
        if (itemSelecionadoAtual != null) {
            todosOsItens.remove(itemSelecionadoAtual);
            salvarAlteracoesNoService();
            limparPainelDeDetalhes();
            construirArvore(todosOsItens, false);
        }
    }

    @FXML
    private void handleSalvar() {
        if (txtTitulo.getText().trim().isEmpty() || txtCategoria.getText().trim().isEmpty()) {
            return; // Ideal adicionar um Alerta aqui
        }

        itemSelecionadoAtual.setTitulo(txtTitulo.getText().trim());
        itemSelecionadoAtual.setCategoria(txtCategoria.getText().trim());
        itemSelecionadoAtual.setConteudo(txtConteudo.getText().trim());
        itemSelecionadoAtual.setDica(txtDica.getText().trim());

        if (criandoNovo) {
            todosOsItens.add(itemSelecionadoAtual);
        }

        salvarAlteracoesNoService();
        construirArvore(todosOsItens, true); // Reconstrói com pastas abertas

        alternarModoVisualizacao(false);
        exibirDetalhesDoScript(itemSelecionadoAtual.getTitulo(), itemSelecionadoAtual.getCategoria());
    }

    @FXML
    private void handleCancelar() {
        alternarModoVisualizacao(false);
        if (criandoNovo) {
            limparPainelDeDetalhes();
        } else {
            exibirDetalhesDoScript(itemSelecionadoAtual.getTitulo(), itemSelecionadoAtual.getCategoria());
        }
    }

    private void salvarAlteracoesNoService() {
        playbookService.salvarTodos(todosOsItens);
    }

    private void alternarModoVisualizacao(boolean editando) {
        this.modoEdicao = editando;

        // Habilita/Desabilita os campos
        txtTitulo.setEditable(editando);
        txtCategoria.setEditable(editando);
        txtConteudo.setEditable(editando);
        txtDica.setEditable(editando);
        treeViewScripts.setDisable(editando); // Trava a navegação

        // Estilos para mostrar que é editável
        String bordaAtiva = "-fx-border-color: #ced4da; -fx-background-color: white;";
        String bordaInativa = "-fx-border-color: transparent; -fx-background-color: transparent;";

        txtTitulo.setStyle(txtTitulo.getStyle() + (editando ? bordaAtiva : bordaInativa));
        txtCategoria.setStyle(txtCategoria.getStyle() + (editando ? bordaAtiva : bordaInativa));
        txtDica.setStyle(txtDica.getStyle() + (editando ? bordaAtiva : bordaInativa));

        // Alterna os botões visíveis
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
        txtDica.setText("");
        btnCopiar.setDisable(true);
        btnEditar.setDisable(true);
        btnExcluir.setDisable(true);
    }

    @FXML
    private void handleCopiar() {
        // Lógica original de copiar (mantida igual)
        String textoParaCopiar = txtConteudo.getText();
        if (textoParaCopiar != null && !textoParaCopiar.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(textoParaCopiar);
            clipboard.setContent(content);

            String textoOriginal = btnCopiar.getText();
            btnCopiar.setText("Copiado! ✓");
            btnCopiar.setStyle(
                    "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> {
                        btnCopiar.setText(textoOriginal);
                        btnCopiar.setStyle(
                                "-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");
                    });
                }
            }, 2000);
        }
    }
}