package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.PlaybookService;
import br.com.poderfinanceiro.app.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    // Campos editáveis
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtCategoria;
    @FXML
    private TextArea txtConteudo, txtDica;

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

    // Elementos FXML do Overlay de Inteligência Artificial
    @FXML
    private VBox overlayIA;
    @FXML
    private TextArea txtInputIA;
    @FXML
    private Button btnProcessarIA;

    private final PlaybookService playbookService;
    private final AuthService authService;
    private final GeminiService geminiService;

    private List<PlaybookItemModel> todosOsItens;

    // Estado da tela
    private boolean modoEdicao = false;
    private PlaybookItemModel itemSelecionadoAtual;
    private boolean criandoNovo = false;

    public PlaybookController(PlaybookService playbookService, AuthService authService, GeminiService geminiService) {
        this.playbookService = playbookService;
        this.authService = authService;
        this.geminiService = geminiService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.todosOsItens = playbookService.listarTudoParaOPlaybook();
        construirArvore(this.todosOsItens, false);
        configurarSelecaoNaArvore();
        configurarFiltroDeBusca();
        alternarModoVisualizacao(false);
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
                .collect(Collectors.groupingBy(PlaybookItemModel::getCategoria, java.util.TreeMap::new,
                        Collectors.toList()));

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
                return;

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
            return;
        }

        itemSelecionadoAtual.setTitulo(txtTitulo.getText().trim());
        itemSelecionadoAtual.setCategoria(txtCategoria.getText().trim());
        itemSelecionadoAtual.setConteudo(txtConteudo.getText().trim());
        itemSelecionadoAtual.setDica(txtDica.getText().trim());

        if (criandoNovo) {
            todosOsItens.add(itemSelecionadoAtual);
        }

        salvarAlteracoesNoService();
        construirArvore(todosOsItens, true);

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

        txtTitulo.setEditable(editando);
        txtCategoria.setEditable(editando);
        txtConteudo.setEditable(editando);
        txtDica.setEditable(editando);
        treeViewScripts.setDisable(editando);

        String bordaAtiva = "-fx-border-color: #ced4da; -fx-background-color: white;";
        String bordaInativa = "-fx-border-color: transparent; -fx-background-color: transparent;";

        txtTitulo.setStyle(txtTitulo.getStyle() + (editando ? bordaAtiva : bordaInativa));
        txtCategoria.setStyle(txtCategoria.getStyle() + (editando ? bordaAtiva : bordaInativa));
        txtDica.setStyle(txtDica.getStyle() + (editando ? bordaAtiva : bordaInativa));

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

    // =========================================================================
    // 🧠 ENGENHARIA COGNITIVA: GERADOR DE SCRIPT INTELIGENTE VIA GEMINI
    // =========================================================================

    @FXML
    private void handleGerarComIA() {
        txtInputIA.clear();
        overlayIA.setVisible(true);
        txtInputIA.requestFocus();
    }

    @FXML
    private void fecharOverlayIA() {
        overlayIA.setVisible(false);
    }

    @FXML
    private void processarTextoComIA() {
        String textoBruto = txtInputIA.getText();
        if (textoBruto == null || textoBruto.trim().isEmpty())
            return;

        btnProcessarIA.setDisable(true);
        btnProcessarIA.setText("Processando...");

        // 🚀 PROMPT DITATORIAL E INQUEBRÁVEL
        // Regras extremamente rígidas e um exemplo com Conteúdo longo e Dica curta
        // inventada.
        String prompt = """
                Você é um Diretor Comercial e Estrategista de Vendas especializado em correspondentes bancários.
                
                REGRAS ABSOLUTAS E INQUEBRÁVEIS (PUNIÇÃO SE DESCUMPRIR):
                1. "conteudo": ESTE É O CAMPO PRINCIPAL. Aqui vai 100% do texto que o cliente vai ler no WhatsApp. Extraia a mensagem de vendas na íntegra, com todos os textos, links, gatilhos, emojis e formatações originais (use \\n para quebras de linha). É PROIBIDO colocar a copy de vendas em qualquer outro lugar.
                2. "dica": INVENTE UMA DICA CURTA. É ESTRITAMENTE PROIBIDO colocar o texto da mensagem de vendas aqui.

                --- EXEMPLO PRÁTICO (SIGA EXATAMENTE ESTA PROPORÇÃO) ---
                TEXTO BRUTO DE ENTRADA:
                "Estratégia para hoje: Mandar pra quem sumiu. Texto para enviar: Olá! Tudo bem? Vi que conversamos sobre o empréstimo, mas não finalizamos. Muitos clientes na sua situação já liberaram os valores hoje! Posso dar continuidade ao seu processo para garantir seu dinheiro ainda hoje? Lembre-se: Sem consultas ao SPC/Serasa e cai em 10 minutos via PIX. Me responde com um SIM."

                JSON DE SAÍDA ESPERADO:
                {
                  "categoria": "Geral / Remarketing",
                  "titulo": "Resgate de Indecisos",
                  "conteudo": "Olá! Tudo bem? Vi que conversamos sobre o empréstimo, mas não finalizamos.\\n\\nMuitos clientes na sua situação já liberaram os valores hoje! Posso dar continuidade ao seu processo para garantir seu dinheiro ainda hoje?\\n\\nLembre-se: Sem consultas ao SPC/Serasa e cai em 10 minutos via PIX.\\n\\nMe responde com um SIM.",
                  "dica": "Dispare esta mensagem no final da tarde criando senso de escassez e foque na palavra 'PIX' para reativar o cliente."
                }
                --------------------------------------------------------

                Retorne APENAS o objeto JSON puro e válido. Não adicione crases de markdown (```json).

                Texto bruto real recebido do grupo para processar agora:
                --- inicio do conteudo ---
                """
                + "\n\"\"\"\n"
                + textoBruto
                + "\n\"\"\"\n"
                + "--- final do conteudo ---";

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;

        new Thread(() -> {
            try {
                String respostaJson = geminiService.perguntarTexto(prompt, token);

                // Validação contra NullPointerException (Aviso do Linter)
                if (respostaJson == null || respostaJson.isBlank()) {
                    throw new Exception("A resposta da inteligência artificial retornou vazia ou nula.");
                }

                // Limpeza robusta para garantir que apenas o JSON seja parseado,
                // ignorando crases de markdown (```json) ou textos extras da IA.
                int startIndex = respostaJson.indexOf('{');
                int endIndex = respostaJson.lastIndexOf('}');

                if (startIndex >= 0 && endIndex >= startIndex) {
                    respostaJson = respostaJson.substring(startIndex, endIndex + 1);
                } else {
                    throw new Exception("Não foi possível localizar o padrão JSON estruturado na resposta.");
                }

                // Parsing seguro via Jackson do Spring
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(respostaJson.trim());

                Platform.runLater(() -> {
                    this.itemSelecionadoAtual = new PlaybookItemModel();
                    this.criandoNovo = true;

                    txtCategoria.setText(node.has("categoria") ? node.get("categoria").asText() : "Geral");
                    txtTitulo.setText(node.has("titulo") ? node.get("titulo").asText() : "Script via IA");
                    txtConteudo.setText(node.has("conteudo") ? node.get("conteudo").asText() : "");
                    txtDica.setText(node.has("dica") ? node.get("dica").asText() : "");

                    fecharOverlayIA();
                    alternarModoVisualizacao(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    txtInputIA.setText(
                            "⚠️ Falha ao estruturar dados. Verifique o formato ou tente novamente.\n\n" + textoBruto);
                });
            } finally {
                Platform.runLater(() -> {
                    btnProcessarIA.setDisable(false);
                    btnProcessarIA.setText("✨ Estruturar com Gemini");
                });
            }
        }).start();
    }
}