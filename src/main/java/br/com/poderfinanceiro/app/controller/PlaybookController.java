package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.PlaybookService;
import br.com.poderfinanceiro.app.util.AsyncUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
public class PlaybookController implements Initializable {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String MODELO_PADRAO = "gemini-3.5-flash";
    private static final String NOME_CONSULTOR_PLACEHOLDER = "%CONSULTOR%";
    private static final String PROMPT_ENGENHARIA_VENDAS = """
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
                """;

    // =========================================================================
    // COMPONENTES FXML
    // =========================================================================
    @FXML private TextField txtBusca;
    @FXML private TreeView<String> treeViewScripts;

    // Campos principais
    @FXML private TextField txtTitulo;
    @FXML private TextField txtCategoria;
    @FXML private TextArea txtConteudo;
    
    // Inline Editing Dica
    @FXML private StackPane stackDica;
    @FXML private Label lblDica;
    @FXML private TextArea txtDica;

    // Ações
    @FXML private Button btnCopiar;
    @FXML private Button btnEditar;
    @FXML private Button btnExcluir;
    @FXML private HBox boxAcoesTopo;
    @FXML private HBox boxAcoesEdicao;

    // IA Overlay
    @FXML private VBox overlayIA;
    @FXML private TextArea txtInputIA;
    @FXML private Button btnProcessarIA;
    @FXML private ComboBox<String> cmbModeloIA;

    // =========================================================================
    // DEPENDÊNCIAS E ESTADO
    // =========================================================================
    private final PlaybookService playbookService;
    private final AuthService authService;
    private final GeminiService geminiService;

    private List<PlaybookItemModel> todosOsItens;
    private PlaybookItemModel itemSelecionadoAtual;
    
    private boolean modoEdicao = false;
    private boolean criandoNovo = false;
    private boolean modelosCarregados = false;

    public PlaybookController(PlaybookService playbookService, AuthService authService, GeminiService geminiService) {
        this.playbookService = playbookService;
        this.authService = authService;
        this.geminiService = geminiService;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.todosOsItens = playbookService.listarTudoParaOPlaybook();
        construirArvore(this.todosOsItens, false);
        configurarSelecaoNaArvore();
        configurarFiltroDeBusca();
        alternarModoVisualizacao(false);

        if (cmbModeloIA != null) {
            cmbModeloIA.getItems().add(MODELO_PADRAO);
            cmbModeloIA.getSelectionModel().selectFirst();
        }
    }

    private void configurarFiltroDeBusca() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                construirArvore(this.todosOsItens, false);
                return;
            }
            String termo = newVal.toLowerCase().trim();
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
                .collect(Collectors.groupingBy(PlaybookItemModel::getCategoria, TreeMap::new, Collectors.toList()));

        TreeItem<String> rootItem = new TreeItem<>("Playbook");
        rootItem.setExpanded(true);

        for (Map.Entry<String, List<PlaybookItemModel>> entry : itensPorCategoria.entrySet()) {
            TreeItem<String> categoriaNode = new TreeItem<>(entry.getKey());
            categoriaNode.setExpanded(expandirPastas);

            List<PlaybookItemModel> scriptsOrdenados = entry.getValue().stream()
                    .sorted(Comparator.comparing(PlaybookItemModel::getTitulo, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            for (PlaybookItemModel item : scriptsOrdenados) {
                categoriaNode.getChildren().add(new TreeItem<>(item.getTitulo()));
            }
            rootItem.getChildren().add(categoriaNode);
        }
        treeViewScripts.setRoot(rootItem);
    }

    private void configurarSelecaoNaArvore() {
        treeViewScripts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (modoEdicao) return;

            if (newVal != null && newVal.isLeaf() && newVal.getParent() != null && newVal.getParent().getValue() != null) {
                exibirDetalhesDoScript(newVal.getValue(), newVal.getParent().getValue());
            } else {
                limparPainelDeDetalhes();
            }
        });
    }

    private void exibirDetalhesDoScript(String titulo, String categoria) {
        String nomeConsultor = authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor(a)";

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

    // =========================================================================
    // AÇÕES DE CRUD E INTERFACE
    // =========================================================================
    @FXML
    private void handleNovo() {
        this.itemSelecionadoAtual = new PlaybookItemModel();
        this.criandoNovo = true;
        txtTitulo.setText("");
        txtCategoria.setText("Nova Categoria / Subcategoria");
        txtConteudo.setText("Olá, me chamo " + NOME_CONSULTOR_PLACEHOLDER + "...");
        lblDica.setText("");
        
        alternarModoVisualizacao(true);
        txtTitulo.requestFocus();
    }

    @FXML
    private void handleEditar() {
        if (itemSelecionadoAtual == null) return;
        this.criandoNovo = false;
        txtConteudo.setText(itemSelecionadoAtual.getConteudo()); // Remove replaces temporarios
        alternarModoVisualizacao(true);
    }

    @FXML
    private void handleExcluir() {
        if (itemSelecionadoAtual != null) {
            todosOsItens.remove(itemSelecionadoAtual);
            playbookService.salvarTodos(todosOsItens);
            limparPainelDeDetalhes();
            construirArvore(todosOsItens, false);
        }
    }

    @FXML
    private void handleSalvar() {
        if (txtTitulo.getText().trim().isEmpty() || txtCategoria.getText().trim().isEmpty()) return;

        itemSelecionadoAtual.setTitulo(txtTitulo.getText().trim());
        itemSelecionadoAtual.setCategoria(txtCategoria.getText().trim());
        itemSelecionadoAtual.setConteudo(txtConteudo.getText().trim());
        
        // Pega do TextArea de edição
        itemSelecionadoAtual.setDica(txtDica.getText().trim());

        if (criandoNovo) todosOsItens.add(itemSelecionadoAtual);

        playbookService.salvarTodos(todosOsItens);
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

        // Inline Editing Logic (StackPane)
        if (editando) {
            txtDica.setText(lblDica.getText());
        }
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
        if (textoParaCopiar != null && !textoParaCopiar.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(textoParaCopiar);
            Clipboard.getSystemClipboard().setContent(content);

            String textoOriginal = btnCopiar.getText();
            btnCopiar.setText("Copiado! ✓");
            btnCopiar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        btnCopiar.setText(textoOriginal);
                        btnCopiar.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");
                    });
                }
            }, 2000);
        }
    }

    // =========================================================================
    // ENGENHARIA COGNITIVA (GEMINI)
    // =========================================================================
    // 🚀 PATCH: Delegação para AsyncUtils
    @FXML
    private void handleGerarComIA() {
        txtInputIA.clear();
        overlayIA.setVisible(true);
        txtInputIA.requestFocus();

        if (!modelosCarregados && cmbModeloIA != null) {
            String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
            if (token != null && !token.isBlank()) {
                AsyncUtils.executarTaskAsync(
                        () -> geminiService.listarModelosMultimodais(token),
                        modelos -> {
                            cmbModeloIA.getItems().setAll(modelos);
                            cmbModeloIA.getSelectionModel()
                                    .select(modelos.contains(MODELO_PADRAO) ? MODELO_PADRAO : modelos.get(0));
                            modelosCarregados = true;
                        },
                        Throwable::printStackTrace);
            }
        }
    }

    @FXML
    private void fecharOverlayIA() {
        overlayIA.setVisible(false);
    }

    // 🚀 PATCH: Delegação para AsyncUtils
    @FXML
    private void processarTextoComIA() {
        String textoBruto = txtInputIA.getText();
        if (textoBruto == null || textoBruto.trim().isEmpty())
            return;

        btnProcessarIA.setDisable(true);
        btnProcessarIA.setText("Processando...");

        String promptCompleto = PROMPT_ENGENHARIA_VENDAS + "--- inicio do conteudo --- \n" + textoBruto
                + "\n --- final do conteudo ---";
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        String modelo = (cmbModeloIA != null && cmbModeloIA.getValue() != null) ? cmbModeloIA.getValue()
                : MODELO_PADRAO;

        AsyncUtils.executarTaskAsync(
                () -> formatarRespostaIAParaJson(geminiService.perguntarTexto(promptCompleto, token, modelo)),
                jsonNode -> aplicarEstruturaIA(jsonNode),
                erro -> aplicarErroIA(erro, textoBruto));
    }

    private JsonNode formatarRespostaIAParaJson(String respostaBruta) throws Exception {
        if (respostaBruta == null || respostaBruta.isBlank()) {
            throw new Exception("A inteligência artificial não retornou dados.");
        }
        
        int startIndex = respostaBruta.indexOf('{');
        int endIndex = respostaBruta.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex >= startIndex) {
            String jsonPuro = respostaBruta.substring(startIndex, endIndex + 1);
            return new ObjectMapper().readTree(jsonPuro.trim());
        }
        throw new Exception("Padrão JSON estruturado não localizado.");
    }

    private void aplicarEstruturaIA(JsonNode node) {
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
        erro.printStackTrace();
        txtInputIA.setText("⚠️ Falha ao estruturar dados ou servidores ocupados. Tente outro modelo.\n\n" + textoBruto);
        btnProcessarIA.setDisable(false);
        btnProcessarIA.setText("✨ Estruturar com Gemini");
    }

}