package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PlaybookItem;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.PlaybookService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
    @FXML
    private Label labelTitulo;
    @FXML
    private Label labelCategoria;
    @FXML
    private TextArea txtConteudo;
    @FXML
    private Label labelDica;
    @FXML
    private Button btnCopiar;

    private final PlaybookService playbookService;
    private final AuthService authService; // 1. Adicionado a injeção do AuthService
    private List<PlaybookItem> todosOsItens;

    // 2. Construtor atualizado para o Spring injetar o AuthService
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
    }

    private void configurarFiltroDeBusca() {
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                construirArvore(this.todosOsItens, false);
                return;
            }

            String termo = newValue.toLowerCase().trim();

            List<PlaybookItem> itensFiltrados = todosOsItens.stream()
                    .filter(item -> item.getTitulo().toLowerCase().contains(termo)
                            || item.getCategoria().toLowerCase().contains(termo)
                            || item.getConteudo().toLowerCase().contains(termo))
                    .collect(Collectors.toList());

            construirArvore(itensFiltrados, true);
        });
    }

    private void construirArvore(List<PlaybookItem> itensParaExibir, boolean expandirPastas) {
        if (itensParaExibir == null || itensParaExibir.isEmpty()) {
            treeViewScripts.setRoot(new TreeItem<>("Nenhum resultado encontrado"));
            return;
        }

        Map<String, List<PlaybookItem>> itensPorCategoria = itensParaExibir.stream()
                .collect(Collectors.groupingBy(
                        PlaybookItem::getCategoria,
                        java.util.TreeMap::new,
                        Collectors.toList()));

        TreeItem<String> rootItem = new TreeItem<>("Playbook");
        rootItem.setExpanded(true);

        for (Map.Entry<String, List<PlaybookItem>> entry : itensPorCategoria.entrySet()) {
            TreeItem<String> categoriaNode = new TreeItem<>(entry.getKey());
            categoriaNode.setExpanded(expandirPastas);

            List<PlaybookItem> scriptsOrdenados = entry.getValue().stream()
                    .sorted(java.util.Comparator.comparing(PlaybookItem::getTitulo, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            for (PlaybookItem item : scriptsOrdenados) {
                TreeItem<String> scriptNode = new TreeItem<>(item.getTitulo());
                categoriaNode.getChildren().add(scriptNode);
            }

            rootItem.getChildren().add(categoriaNode);
        }

        treeViewScripts.setRoot(rootItem);
    }

    private void configurarSelecaoNaArvore() {
        treeViewScripts.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isLeaf() && newValue.getParent() != null
                    && newValue.getParent().getValue() != null) {
                exibirDetalhesDoScript(newValue.getValue(), newValue.getParent().getValue());
            } else {
                limparPainelDeDetalhes();
            }
        });
    }

    private void exibirDetalhesDoScript(String tituloDoScript, String categoriaDoScript) {
        // 3. Puxando o nome real do Consultor logado!
        String nomeDoConsultor = "Consultor(a)"; // Fallback de segurança
        if (authService.estaLogado()) {
            nomeDoConsultor = authService.getUsuarioLogado().getNome();
        }

        final String consultorFinal = nomeDoConsultor;

        todosOsItens.stream()
                .filter(item -> item.getTitulo().equals(tituloDoScript)
                        && item.getCategoria().equals(categoriaDoScript))
                .findFirst()
                .ifPresent(itemSelecionado -> {
                    labelTitulo.setText(itemSelecionado.getTitulo());
                    labelCategoria.setText(itemSelecionado.getCategoria());

                    // 4. A substituição real
                    String conteudoPersonalizado = itemSelecionado.getConteudo().replace("%CONSULTOR%", consultorFinal);
                    txtConteudo.setText(conteudoPersonalizado);

                    String dica = itemSelecionado.getDica() != null ? itemSelecionado.getDica() : "";
                    labelDica.setText(dica);

                    btnCopiar.setDisable(false);
                });
    }

    private void limparPainelDeDetalhes() {
        labelTitulo.setText("Selecione um Script");
        labelCategoria.setText("");
        txtConteudo.setText("");
        labelDica.setText("");
        btnCopiar.setDisable(true);
    }

    @FXML
    private void handleCopiar() {
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