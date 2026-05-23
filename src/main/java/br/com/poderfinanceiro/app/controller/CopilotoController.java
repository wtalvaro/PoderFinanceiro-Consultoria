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
import javafx.concurrent.Task;
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

    // 🔥 NOVO: Armazena o índice (-1 significa que nenhum foi escolhido ainda)
    private int indiceRecomendadoIA = -1;

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

    // 🚀 NOVO MÉTODO: Carrega os modelos na ComboBox
    private void carregarModelosGemini() {
        if (cmbModeloIA == null)
            return;

        cmbModeloIA.getItems().add("gemini-3.5-flash"); // Fallback imediato
        cmbModeloIA.getSelectionModel().selectFirst();

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token != null && !token.isBlank()) {
            AsyncUtils.executarTask(new Task<List<String>>() {
                @Override
                protected List<String> call() {
                    return geminiService.listarModelosMultimodais(token);
                }
            }, modelos -> {
                String atual = cmbModeloIA.getValue();
                cmbModeloIA.getItems().setAll(modelos);
                if (modelos.contains(atual))
                    cmbModeloIA.getSelectionModel().select(atual);
                else
                    cmbModeloIA.getSelectionModel().selectFirst();
            }, null);
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

                    // 🔥 NOVO: Verifica se o índice atual da célula é o escolhido pela IA
                    controller.setRecomendadoIA(getIndex() == indiceRecomendadoIA);

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

    private void extrairMargemAssincrona(File arquivo) {
        btnExtrairMargem.setDisable(true);
        btnExtrairMargem.setText("⏳");
        mainController.mostrarLoading("A IA está lendo o documento...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return copilotoService.extrairMargemDocumento(arquivo);
            }
        };

        // 🔥 USO DO ASYNC UTILS
        AsyncUtils.executarTask(task,
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

    private void iniciarSimulacaoAssincrona() {
        btnSimular.setText("⏳ Buscando...");
        btnSimular.setDisable(true);
        indiceRecomendadoIA = -1; // 🔥 NOVO: Reseta o destaque

        Task<List<ResultadoSimulacaoDTO>> task = new Task<>() {
            @Override
            protected List<ResultadoSimulacaoDTO> call() {
                return copilotoService.processarSimulacaoRapida(rascunhoAtual);
            }
        };

        // 🔥 USO DO ASYNC UTILS: Desacoplado da renderização nativa de nós
        AsyncUtils.executarTask(task,
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

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Passa o modelo para o serviço
                return copilotoService.gerarRecomendacaoInteligenteIA(rascunhoAtual, rankingAtual, modeloEscolhido);
            }
        };

        AsyncUtils.executarTask(task,
                resposta -> {
                    // (Lógica do Regex [ESCOLHA: X])
                    Pattern p = Pattern.compile("\\[ESCOLHA:\\s*(\\d+)\\]");
                    Matcher m = p.matcher(resposta);

                    if (m.find()) {
                        try {
                            indiceRecomendadoIA = Integer.parseInt(m.group(1)) - 1;
                            resposta = resposta.replace(m.group(0), "").trim();
                        } catch (Exception ex) {
                            indiceRecomendadoIA = -1;
                        }
                    } else {
                        indiceRecomendadoIA = -1;
                    }

                    lblRespostaIA.setText(resposta);
                    btnPedirConselho.setText("✨ Atualizar Conselho");
                    btnPedirConselho.setDisable(false);
                    listaRanking.refresh();

                    // 🚀 REPETE A SOLUÇÃO 2 AQUI DENTRO!
                    // Como o texto do Gemini pode ter 3 ou 4 linhas, a caixa vai crescer de novo.
                    // Precisamos re-ancorar no topo após o texto ser renderizado.
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

    private void resetarBotoes() {
        btnSimular.setText("🔍 Buscar Melhores Tabelas");
        btnSimular.setDisable(false);
    }
}