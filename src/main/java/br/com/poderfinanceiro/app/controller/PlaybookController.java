package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.model.PlaybookItem;
import br.com.poderfinanceiro.app.service.PlaybookService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Scope("prototype")
public class PlaybookController {

    private final PlaybookService playbookService;

    @FXML
    private TreeView<Object> treeViewScripts; // Usamos Object para aceitar String (categoria) e PlaybookItem
    @FXML
    private Label labelTitulo, labelCategoria, labelDica;
    @FXML
    private TextArea txtConteudo;
    @FXML
    private Button btnCopiar;

    @FXML
    public void initialize() {
        configurarArvore();
    }

    private void configurarArvore() {
        // 1. Criar o nó raiz (invisível conforme o FXML)
        TreeItem<Object> root = new TreeItem<>("Root");

        // 2. Agrupar os scripts por categoria
        List<PlaybookItem> todosScripts = playbookService.listarTudoParaOPlaybook();
        Map<String, List<PlaybookItem>> agrupados = todosScripts.stream()
                .collect(Collectors.groupingBy(PlaybookItem::getCategoria));

        // 3. Construir a hierarquia
        agrupados.forEach((categoria, scripts) -> {
            TreeItem<Object> categoriaNode = new TreeItem<>(categoria);
            categoriaNode.setExpanded(true); // Deixa as categorias abertas por padrão

            scripts.forEach(script -> {
                categoriaNode.getChildren().add(new TreeItem<>(script));
            });

            root.getChildren().add(categoriaNode);
        });

        treeViewScripts.setRoot(root);

        // 4. Customizar a exibição (mostrar apenas o título para o script e o nome para
        // a categoria)
        treeViewScripts.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item instanceof String) {
                    setText((String) item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1976d2;"); // Destaque para categorias
                } else if (item instanceof PlaybookItem) {
                    setText(((PlaybookItem) item).getTitulo());
                    setStyle("-fx-padding: 0 0 0 10;"); // Indentação para os scripts
                }
            }
        });

        // 5. Ouvinte de seleção
        treeViewScripts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof PlaybookItem) {
                exibirDetalhes((PlaybookItem) newVal.getValue());
            }
        });
    }

    private void exibirDetalhes(PlaybookItem item) {
        labelTitulo.setText(item.getTitulo());
        labelCategoria.setText("Categoria: " + item.getCategoria());
        txtConteudo.setText(item.getConteudo());
        labelDica.setText("💡 Dica: " + item.getDicaTecnica());
    }

    @FXML
    private void handleCopiar() {
        String textoParaCopiar = txtConteudo.getText();

        // RESOLUÇÃO DO ERRO: Obtemos o item diretamente da seleção da ListView
        PlaybookItem itemSelecionado = treeViewScripts.getSelectionModel().getSelectedItem() != null
                && treeViewScripts.getSelectionModel().getSelectedItem().getValue() instanceof PlaybookItem
                ? (PlaybookItem) treeViewScripts.getSelectionModel().getSelectedItem().getValue()
                : null;

        if (textoParaCopiar != null && !textoParaCopiar.isEmpty()) {
            // 1. Ação de Cópia
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(textoParaCopiar);
            clipboard.setContent(content);

            // 2. Feedback Visual Imediato (o efeito de "pulo" que conversamos)
            String textoOriginal = btnCopiar.getText();
            String estiloOriginal = btnCopiar.getStyle();

            btnCopiar.setText("✅ Copiado para o Clipboard!");
            btnCopiar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

            // 3. Temporizador para voltar ao normal (1.5 segundos)
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1.5),
                    ae -> {
                        btnCopiar.setText(textoOriginal);
                        btnCopiar.setStyle(estiloOriginal);
                    }));
            timeline.setCycleCount(1);
            timeline.play();

            // Log de depuração (agora com a variável resolvida)
            if (itemSelecionado != null) {
                System.out.println("Conteúdo copiado: " + itemSelecionado.getTitulo());
            }
        }
    }
}