package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.domain.service.SimulacaoCopilotoService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import javafx.util.converter.LocalDateStringConverter;
import br.com.poderfinanceiro.app.util.DataUtils;

@Component
public class CopilotoController {

    @FXML
    private TextField txtRenda, txtValor, txtPrazo, txtMargem;
    @FXML
    private ComboBox<TipoConvenioModel> cbConvenio;
    @FXML
    private Button btnSimular, btnPedirConselho, btnExtrairMargem;
    @FXML
    private VBox boxResultados, boxRespostaIA;
    @FXML
    private Label lblRespostaIA;
    @FXML
    private ComboBox<ProponenteModel> cbCliente;
    @FXML
    private DatePicker dpDataNascimento;

    // 🔥 NOVO: ListView substitui o VBox engessado
    @FXML
    private ListView<ResultadoSimulacaoDTO> listaRanking;
    // 🚀 NOVO FXML para o seletor de modelo
    @FXML
    private ComboBox<String> cmbModeloIA;
    @FXML
    private ScrollPane scrollArea;

    private final SimulacaoCopilotoService copilotoService;
    private final MainController mainController;
    private final ProponenteService proponenteService;
    private final GeminiService geminiService; // 🚀 Injetar
    private final AuthService authService; // 🚀 Injetar

    private SimulacaoRascunhoDTO rascunhoAtual;
    private List<ResultadoSimulacaoDTO> rankingAtual;

    // 🔥 Antes: private int indiceRecomendadoIA = -1;
    // 🚀 NOVO: Armazena a ordem de escolha da IA
    private java.util.List<Integer> recomendacoesIA = new java.util.ArrayList<>();

    public CopilotoController(SimulacaoCopilotoService copilotoService, MainController mainController,
            ProponenteService proponenteService, GeminiService geminiService, AuthService authService) {
        this.copilotoService = copilotoService;
        this.mainController = mainController;
        this.proponenteService = proponenteService;
        this.geminiService = geminiService;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        cbConvenio.getItems().setAll(TipoConvenioModel.values());

        txtRenda.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtValor.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtMargem.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtPrazo.setTextFormatter(createIntegerFormatter());

        // 🔥 NOVO: Formatação Robusta do DatePicker (Idêntico ao LeadController)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);

        cbCliente.getItems().setAll(proponenteService.listarMinhaCarteira());
        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProponenteModel p) {
                return p != null ? p.getNomeCompleto() : "";
            }

            @Override
            public ProponenteModel fromString(String s) {
                return null;
            }
        });

        // 🚀 NOVO: Auto-preenchimento ao selecionar o cliente
        cbCliente.valueProperty().addListener((obs, old, cliente) -> {
            if (cliente != null) {
                dpDataNascimento.setValue(cliente.getDataNascimento());
                if (cliente.getRendaMensal() != null && cliente.getRendaMensal().compareTo(BigDecimal.ZERO) > 0) {
                    txtRenda.setText(FinanceiroUtils.formatarParaExibicao(cliente.getRendaMensal()));
                } else {
                    txtRenda.setText("");
                }
            } else {
                // Limpa os campos se o consultor desmarcar o cliente
                dpDataNascimento.setValue(null);
                txtRenda.setText("");
            }
        });

        configurarListViewRanking();
        carregarModelosGemini();
    }

    // 🚀 PATCH: Refatorado para AsyncUtils com Callable enxuto
    private void carregarModelosGemini() {
        if (cmbModeloIA == null)
            return;

        cmbModeloIA.getItems().add("gemini-3.5-flash"); // Fallback imediato
        cmbModeloIA.getSelectionModel().selectFirst();

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token != null && !token.isBlank()) {
            AsyncUtils.executarTaskAsync(
                    () -> geminiService.listarModelosMultimodais(token),
                    modelos -> {
                        String atual = cmbModeloIA.getValue();
                        cmbModeloIA.getItems().setAll(modelos);
                        if (modelos.contains(atual))
                            cmbModeloIA.getSelectionModel().select(atual);
                        else
                            cmbModeloIA.getSelectionModel().selectFirst();
                    },
                    null);
        }
    }

    private void configurarListViewRanking() {
        listaRanking.setCellFactory(lv -> new ListCell<>() {
            private Node view;
            private CopilotoCardController controller;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/copiloto_card.fxml"));
                    view = loader.load();
                    controller = loader.getController();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void updateItem(ResultadoSimulacaoDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    controller.setDados(item, res -> converterParaProposta(res, cbCliente.getValue()));
                    controller.getBtnAproveitar().disableProperty().bind(cbCliente.valueProperty().isNull());

                    // 🔥 NOVO: Verifica qual é a colocação deste item no ranking da IA (0 significa
                    // que não entrou no Top 3)
                    int rank = recomendacoesIA.indexOf(getIndex()) + 1;
                    controller.setRecomendadoIA(rank);

                    setGraphic(view);
                }
            }
        });
    }

    private TextFormatter<String> createIntegerFormatter() {
        return new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null);
    }

    @FXML
    private void handleSimular() {
        lblRespostaIA.setText("");
        boxRespostaIA.setVisible(false);
        boxRespostaIA.setManaged(false);
        btnPedirConselho.setDisable(false);
        btnPedirConselho.setText("✨ Analisar com IA");

        // 🚀 NOVO: Cálculo inteligente da idade baseado na data
        int idade = 0;
        LocalDate nascimento = dpDataNascimento.getValue();
        if (nascimento != null) {
            idade = Period.between(nascimento, LocalDate.now()).getYears();
        }

        int prazo = parseSafeInt(txtPrazo.getText());
        BigDecimal renda = FinanceiroUtils.extrairValorParaBanco(txtRenda.getText());
        BigDecimal valor = FinanceiroUtils.extrairValorParaBanco(txtValor.getText());
        BigDecimal margem = FinanceiroUtils.extrairValorParaBanco(txtMargem.getText());
        String convenio = cbConvenio.getValue() != null ? cbConvenio.getValue().name() : "";

        this.rascunhoAtual = new SimulacaoRascunhoDTO(idade, renda, convenio, valor, prazo, margem);
        iniciarSimulacaoAssincrona();
    }

    @FXML
    private void handleExtrairMargem() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Holerite / Hiscon");
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(txtMargem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            extrairMargemAssincrona(arquivo);
        }
    }

    @FXML
    private void handlePedirConselhoIA() {
        if (rankingAtual == null || rankingAtual.isEmpty())
            return;

        // 🚀 SOLUÇÃO 1: Roubar o foco do botão ANTES de desativá-lo.
        // Isso evita que o JavaFX passe o foco para a ListView e puxe a tela para
        // baixo.
        if (scrollArea != null) {
            scrollArea.requestFocus();
        }

        btnPedirConselho.setText("✨ Analisando...");
        btnPedirConselho.setDisable(true);
        boxRespostaIA.setVisible(true);
        boxRespostaIA.setManaged(true);
        lblRespostaIA.setText("Pensando em estratégias comerciais...");

        // 🚀 SOLUÇÃO 2: Forçar o CSS e o Layout a se atualizarem ANTES de mover o
        // scroll
        if (scrollArea != null) {
            javafx.application.Platform.runLater(() -> {
                scrollArea.applyCss(); // Aplica estilos pendentes
                scrollArea.layout(); // Calcula a altura real da nova caixa visível
                scrollArea.setVvalue(0.0); // Crava no topo com precisão
            });
        }

        // 🚀 Pega o modelo escolhido pelo consultor
        String modeloEscolhido = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : "gemini-3.5-flash";

        // 🚀 PATCH: Delegação para o utilitário
        AsyncUtils.executarTaskAsync(
                () -> copilotoService.gerarRecomendacaoInteligenteIA(rascunhoAtual, rankingAtual, modeloEscolhido),
                resposta -> {
                    // Captura os números separados por vírgula dentro de [TOP: X, Y, Z]
                    Pattern p = Pattern.compile("\\[TOP:\\s*([\\d,\\s]+)\\]", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(resposta);

                    recomendacoesIA.clear();

                    if (m.find()) {
                        try {
                            String[] numerosExtraidos = m.group(1).split(",");
                            for (String numeroStr : numerosExtraidos) {
                                int indiceReal = Integer.parseInt(numeroStr.trim()) - 1;
                                if (indiceReal >= 0 && indiceReal < rankingAtual.size()) {
                                    recomendacoesIA.add(indiceReal);
                                }
                            }

                            // Limpa a tag para o consultor ver só o texto limpo
                            resposta = resposta.replace(m.group(0), "").trim();

                            // 🚀 PATCH DECLARATIVO: Reordena a lista baseada nas escolhas da IA
                            if (!recomendacoesIA.isEmpty()) {
                                // Separa os recomendados dos não recomendados usando Streams
                                java.util.List<ResultadoSimulacaoDTO> topChoices = recomendacoesIA.stream()
                                        .map(arg0 -> rankingAtual.get(arg0))
                                        .toList();

                                java.util.List<ResultadoSimulacaoDTO> remainingChoices = java.util.stream.IntStream
                                        .range(0, rankingAtual.size())
                                        .filter(i -> !recomendacoesIA.contains(i))
                                        .mapToObj(rankingAtual::get)
                                        .toList();

                                // Reconstrói a lista oficial e atualiza o estado
                                rankingAtual.clear();
                                rankingAtual.addAll(topChoices);
                                rankingAtual.addAll(remainingChoices);

                                // Como reordenamos a lista, os índices das recomendações mudaram (agora são os
                                // primeiros: 0, 1, 2)
                                recomendacoesIA.clear();
                                for (int i = 0; i < topChoices.size(); i++) {
                                    recomendacoesIA.add(i);
                                }

                                // Atualiza a View
                                listaRanking.setItems(FXCollections.observableArrayList(rankingAtual));
                            }
                        } catch (Exception ex) {
                            System.err.println("Erro ao processar ranking da IA: " + ex.getMessage());
                        }
                    }

                    lblRespostaIA.setText(resposta);
                    btnPedirConselho.setText("✨ Atualizar Conselho");
                    btnPedirConselho.setDisable(false);
                    listaRanking.refresh();

                    // Crava no topo com precisão
                    if (scrollArea != null) {
                        javafx.application.Platform.runLater(() -> {
                            scrollArea.applyCss();
                            scrollArea.layout();
                            scrollArea.setVvalue(0.0);
                        });
                    }
                },
                erro -> {
                    lblRespostaIA.setText("Erro ao conectar com o Copiloto.");
                    btnPedirConselho.setDisable(false);
                });
    }

    // 🚀 PATCH: Refatorado para AsyncUtils
    private void extrairMargemAssincrona(File arquivo) {
        btnExtrairMargem.setDisable(true);
        btnExtrairMargem.setText("⏳");
        mainController.mostrarLoading("A IA está lendo o documento...");

        AsyncUtils.executarTaskAsync(
                () -> copilotoService.extrairMargemDocumento(arquivo),
                respostaCompleta -> {
                    mainController.ocultarLoading();
                    btnExtrairMargem.setDisable(false);
                    btnExtrairMargem.setText("📎 IA");

                    if (respostaCompleta != null && !respostaCompleta.isBlank()) {
                        mainController.notificarAviso("Análise concluída! O log está no console.");
                        if (respostaCompleta.contains("RESULTADO FINAL:")) {
                            String[] partes = respostaCompleta.split("RESULTADO FINAL:");
                            String margemLimpa = partes[1].trim().replaceAll("[^0-9,]", "");
                            if (!margemLimpa.isEmpty() && !margemLimpa.equals("0") && !margemLimpa.equals("0,00")) {
                                txtMargem.setText(margemLimpa);
                            }
                        }
                    } else {
                        mainController.notificarAviso("A IA não conseguiu analisar o documento.");
                    }
                },
                erro -> {
                    mainController.ocultarLoading();
                    btnExtrairMargem.setDisable(false);
                    btnExtrairMargem.setText("📎 IA");
                    mainController.notificarAviso("Erro na extração de documento.");
                });
    }

    // 🚀 PATCH: Refatorado para AsyncUtils
    private void iniciarSimulacaoAssincrona() {
        btnSimular.setText("⏳ Buscando...");
        btnSimular.setDisable(true);
        recomendacoesIA.clear();

        AsyncUtils.executarTaskAsync(
                () -> copilotoService.processarSimulacaoRapida(rascunhoAtual),
                ranking -> {
                    rankingAtual = ranking;
                    listaRanking.setItems(FXCollections.observableArrayList(ranking)); // Preenche o ListView
                    boxResultados.setVisible(true);
                    boxResultados.setManaged(true);
                    resetarBotoes();
                },
                erro -> {
                    mainController.notificarAviso("Erro ao processar. Verifique os valores.");
                    resetarBotoes();
                });
    }

    private int parseSafeInt(String texto) {
        if (texto == null || texto.replaceAll("[^0-9]", "").isEmpty())
            return 0;
        try {
            return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void converterParaProposta(ResultadoSimulacaoDTO resultadoEscolhido, ProponenteModel cliente) {
        if (cliente == null) {
            mainController.notificarAviso("Selecione um cliente no campo acima antes de gerar a proposta.");
            return;
        }
        mainController.iniciarConversaoCopiloto(rascunhoAtual, resultadoEscolhido, cliente);
    }

    private void resetarBotoes() {
        btnSimular.setText("🔍 Buscar Melhores Tabelas");
        btnSimular.setDisable(false);
    }
}