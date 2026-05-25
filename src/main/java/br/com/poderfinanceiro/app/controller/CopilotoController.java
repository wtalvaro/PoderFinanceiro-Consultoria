package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
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
import javafx.application.Platform;
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
import br.com.poderfinanceiro.app.util.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CopilotoController implements Disposable {

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

    @FXML
    private ListView<ResultadoSimulacaoDTO> listaRanking;
    @FXML
    private ComboBox<String> cmbModeloIA;
    @FXML
    private ScrollPane scrollArea;

    private final SimulacaoCopilotoService copilotoService;
    private final MainController mainController;
    private final ProponenteService proponenteService;
    private final GeminiService geminiService;
    private final AuthService authService;
    private final ProponenteUIEventHub eventHub;

    private static final Logger log = LoggerFactory.getLogger(CopilotoController.class);

    private SimulacaoRascunhoDTO rascunhoAtual;
    private List<ResultadoSimulacaoDTO> rankingAtual;
    private java.util.List<Integer> recomendacoesIA = new java.util.ArrayList<>();

    public CopilotoController(SimulacaoCopilotoService copilotoService, MainController mainController,
            ProponenteService proponenteService, GeminiService geminiService, AuthService authService,
            ProponenteUIEventHub eventHub) {
        this.copilotoService = copilotoService;
        this.mainController = mainController;
        this.proponenteService = proponenteService;
        this.geminiService = geminiService;
        this.authService = authService;
        this.eventHub = eventHub;
        log.debug("[COPILOTO] Construtor: Controller instanciado");
    }

    @FXML
    public void initialize() {
        log.debug("[COPILOTO] initialize: Iniciando configuração da tela Copiloto");

        cbConvenio.getItems().setAll(TipoConvenioModel.values());
        log.debug("[COPILOTO] initialize: Combobox convênio carregado com {} itens", TipoConvenioModel.values().length);

        txtRenda.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtValor.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtMargem.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtPrazo.setTextFormatter(createIntegerFormatter());
        log.debug("[COPILOTO] initialize: Formatadores de campos configurados");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);
        log.debug("[COPILOTO] initialize: DatePicker configurado");

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
        log.debug("[COPILOTO] initialize: Combobox clientes carregado com {} itens", cbCliente.getItems().size());

        cbCliente.valueProperty().addListener((obs, old, cliente) -> {
            if (cliente != null) {
                log.debug("[COPILOTO] initialize: Cliente selecionado '{}' (ID={})", cliente.getNomeCompleto(),
                        cliente.getId());
                dpDataNascimento.setValue(cliente.getDataNascimento());
                if (cliente.getRendaMensal() != null && cliente.getRendaMensal().compareTo(BigDecimal.ZERO) > 0) {
                    txtRenda.setText(FinanceiroUtils.formatarParaExibicao(cliente.getRendaMensal()));
                } else {
                    txtRenda.setText("");
                }
            } else {
                log.debug("[COPILOTO] initialize: Cliente desmarcado, limpando campos");
                dpDataNascimento.setValue(null);
                txtRenda.setText("");
            }
        });

        configurarListViewRanking();
        carregarModelosGemini();

        eventHub.inscrever(this::atualizarListaClientes);
        atualizarListaClientes();
        log.info("[COPILOTO] initialize: Configuração finalizada com sucesso");
    }

    private void atualizarListaClientes() {
        log.debug("[COPILOTO] atualizarListaClientes: Iniciando atualização da lista de clientes");
        Platform.runLater(() -> {
            ProponenteModel selecionado = cbCliente.getValue();
            List<ProponenteModel> clientes = proponenteService.listarMinhaCarteira();
            log.debug("[COPILOTO] atualizarListaClientes: {} clientes disponíveis na carteira", clientes.size());

            cbCliente.getItems().setAll(clientes);

            if (selecionado != null) {
                clientes.stream()
                        .filter(c -> c.getId().equals(selecionado.getId()))
                        .findFirst()
                        .ifPresent(c -> {
                            cbCliente.setValue(c);
                            log.debug("[COPILOTO] atualizarListaClientes: Seleção anterior mantida (cliente ID={})",
                                    c.getId());
                        });
            }
        });
    }

    private void carregarModelosGemini() {
        log.debug("[COPILOTO] carregarModelosGemini: Iniciando carregamento de modelos disponíveis");
        if (cmbModeloIA == null) {
            log.warn("[COPILOTO] carregarModelosGemini: cmbModeloIA está nulo, pulando carregamento");
            return;
        }

        cmbModeloIA.getItems().add("gemini-3.5-flash");
        cmbModeloIA.getSelectionModel().selectFirst();
        log.debug("[COPILOTO] carregarModelosGemini: Modelo fallback 'gemini-3.5-flash' adicionado");

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token != null && !token.isBlank()) {
            log.debug("[COPILOTO] carregarModelosGemini: Token encontrado, buscando modelos da API");
            AsyncUtils.executarTaskAsync(
                    () -> geminiService.listarModelosMultimodais(token),
                    modelos -> {
                        log.info("[COPILOTO] carregarModelosGemini: {} modelos carregados da API", modelos.size());
                        String atual = cmbModeloIA.getValue();
                        cmbModeloIA.getItems().setAll(modelos);
                        if (modelos.contains(atual))
                            cmbModeloIA.getSelectionModel().select(atual);
                        else
                            cmbModeloIA.getSelectionModel().selectFirst();
                        log.debug("[COPILOTO] carregarModelosGemini: Modelo selecionado: '{}'", cmbModeloIA.getValue());
                    },
                    erro -> log.error("[COPILOTO] carregarModelosGemini: Erro ao carregar modelos da API", erro));
        } else {
            log.warn("[COPILOTO] carregarModelosGemini: Nenhum token válido disponível, usando apenas fallback");
        }
    }

    private void configurarListViewRanking() {
        log.debug("[COPILOTO] configurarListViewRanking: Configurando ListView de ranking");
        listaRanking.setCellFactory(lv -> new ListCell<>() {
            private Node view;
            private CopilotoCardController controller;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/copiloto_card.fxml"));
                    view = loader.load();
                    controller = loader.getController();
                    log.trace("[COPILOTO] configurarListViewRanking: Card FXML carregado com sucesso");
                } catch (IOException e) {
                    log.error("[COPILOTO][RANKING] Erro ao carregar copiloto_card.fxml: {}", e.getMessage(), e);
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
                    int rank = recomendacoesIA.indexOf(getIndex()) + 1;
                    controller.setRecomendadoIA(rank);
                    setGraphic(view);
                }
            }
        });
        log.info("[COPILOTO] configurarListViewRanking: ListView configurada");
    }

    private TextFormatter<String> createIntegerFormatter() {
        return new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null);
    }

    @FXML
    private void handleSimular() {
        log.debug("[COPILOTO] handleSimular: Iniciando simulação manual");

        lblRespostaIA.setText("");
        boxRespostaIA.setVisible(false);
        boxRespostaIA.setManaged(false);
        btnPedirConselho.setDisable(false);
        btnPedirConselho.setText("✨ Analisar com IA");

        int idade = 0;
        LocalDate nascimento = dpDataNascimento.getValue();
        if (nascimento != null) {
            idade = Period.between(nascimento, LocalDate.now()).getYears();
            log.debug("[COPILOTO] handleSimular: Idade calculada = {} anos", idade);
        } else {
            log.warn("[COPILOTO] handleSimular: Data de nascimento não informada, idade será 0");
        }

        int prazo = parseSafeInt(txtPrazo.getText());
        BigDecimal renda = FinanceiroUtils.extrairValorParaBanco(txtRenda.getText());
        BigDecimal valor = FinanceiroUtils.extrairValorParaBanco(txtValor.getText());
        BigDecimal margem = FinanceiroUtils.extrairValorParaBanco(txtMargem.getText());
        String convenio = cbConvenio.getValue() != null ? cbConvenio.getValue().name() : "";

        log.info(
                "[COPILOTO] handleSimular: Parâmetros - idade={}, renda={}, convenio={}, valor={}, prazo={}, margem={}",
                idade, renda, convenio, valor, prazo, margem);

        this.rascunhoAtual = new SimulacaoRascunhoDTO(idade, renda, convenio, valor, prazo, margem);
        iniciarSimulacaoAssincrona();
    }

    @FXML
    private void handleExtrairMargem() {
        log.debug("[COPILOTO] handleExtrairMargem: Iniciando extração de margem via arquivo");
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Holerite / Hiscon");
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(txtMargem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            log.info("[COPILOTO] handleExtrairMargem: Arquivo selecionado: {}", arquivo.getAbsolutePath());
            extrairMargemAssincrona(arquivo);
        } else {
            log.warn("[COPILOTO] handleExtrairMargem: Nenhum arquivo válido selecionado");
        }
    }

    @FXML
    private void handlePedirConselhoIA() {
        log.debug("[COPILOTO] handlePedirConselhoIA: Iniciando consulta à IA");

        if (rankingAtual == null || rankingAtual.isEmpty()) {
            log.warn("[COPILOTO] handlePedirConselhoIA: rankingAtual está nulo ou vazio, abortando consulta");
            return;
        }

        log.info("[COPILOTO] handlePedirConselhoIA: ranking possui {} resultados", rankingAtual.size());

        if (scrollArea != null) {
            scrollArea.requestFocus();
        }

        btnPedirConselho.setText("✨ Analisando...");
        btnPedirConselho.setDisable(true);
        boxRespostaIA.setVisible(true);
        boxRespostaIA.setManaged(true);
        lblRespostaIA.setText("Pensando em estratégias comerciais...");

        if (scrollArea != null) {
            Platform.runLater(() -> {
                scrollArea.applyCss();
                scrollArea.layout();
                scrollArea.setVvalue(0.0);
            });
        }

        String modeloEscolhido = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : "gemini-3.5-flash";
        log.debug("[COPILOTO] handlePedirConselhoIA: Modelo IA selecionado: {}", modeloEscolhido);

        AsyncUtils.executarTaskAsync(
                () -> copilotoService.gerarRecomendacaoInteligenteIA(rascunhoAtual, rankingAtual, modeloEscolhido),
                resposta -> {
                    log.debug("[COPILOTO] handlePedirConselhoIA: Resposta recebida da IA (tamanho={})",
                            resposta.length());
                    Pattern p = Pattern.compile("\\[TOP:\\s*([\\d,\\s]+)\\]", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(resposta);

                    recomendacoesIA.clear();

                    if (m.find()) {
                        try {
                            String[] numerosExtraidos = m.group(1).split(",");
                            log.debug("[COPILOTO] handlePedirConselhoIA: Números extraídos do TOP: {}",
                                    (Object[]) numerosExtraidos);
                            for (String numeroStr : numerosExtraidos) {
                                int indiceReal = Integer.parseInt(numeroStr.trim()) - 1;
                                if (indiceReal >= 0 && indiceReal < rankingAtual.size()) {
                                    recomendacoesIA.add(indiceReal);
                                }
                            }
                            resposta = resposta.replace(m.group(0), "").trim();

                            if (!recomendacoesIA.isEmpty()) {
                                java.util.List<ResultadoSimulacaoDTO> topChoices = recomendacoesIA.stream()
                                        .map(arg0 -> rankingAtual.get(arg0))
                                        .toList();
                                java.util.List<ResultadoSimulacaoDTO> remainingChoices = java.util.stream.IntStream
                                        .range(0, rankingAtual.size())
                                        .filter(i -> !recomendacoesIA.contains(i))
                                        .mapToObj(rankingAtual::get)
                                        .toList();

                                rankingAtual.clear();
                                rankingAtual.addAll(topChoices);
                                rankingAtual.addAll(remainingChoices);

                                recomendacoesIA.clear();
                                for (int i = 0; i < topChoices.size(); i++) {
                                    recomendacoesIA.add(i);
                                }

                                listaRanking.setItems(FXCollections.observableArrayList(rankingAtual));
                                log.info(
                                        "[COPILOTO] handlePedirConselhoIA: Ranking reordenado com {} recomendações no topo",
                                        topChoices.size());
                            }
                        } catch (Exception ex) {
                            log.error("[COPILOTO] handlePedirConselhoIA: Erro ao processar ranking da IA", ex);
                        }
                    } else {
                        log.warn("[COPILOTO] handlePedirConselhoIA: Padrão [TOP: ...] não encontrado na resposta");
                    }

                    lblRespostaIA.setText(resposta);
                    btnPedirConselho.setText("✨ Atualizar Conselho");
                    btnPedirConselho.setDisable(false);
                    listaRanking.refresh();

                    if (scrollArea != null) {
                        Platform.runLater(() -> {
                            scrollArea.applyCss();
                            scrollArea.layout();
                            scrollArea.setVvalue(0.0);
                        });
                    }
                    log.info("[COPILOTO] handlePedirConselhoIA: Conselho finalizado com sucesso");
                },
                erro -> {
                    log.error("[COPILOTO] handlePedirConselhoIA: Erro ao chamar serviço de IA", erro);
                    lblRespostaIA.setText("Erro ao conectar com o Copiloto.");
                    btnPedirConselho.setDisable(false);
                });
    }

    private void extrairMargemAssincrona(File arquivo) {
        log.debug("[COPILOTO] extrairMargemAssincrona: Iniciando extração assíncrona do arquivo: {}",
                arquivo.getName());
        btnExtrairMargem.setDisable(true);
        btnExtrairMargem.setText("⏳");
        mainController.mostrarLoading("A IA está lendo o documento...");

        AsyncUtils.executarTaskAsync(
                () -> copilotoService.extrairMargemDocumento(arquivo),
                respostaCompleta -> {
                    mainController.ocultarLoading();
                    btnExtrairMargem.setDisable(false);
                    btnExtrairMargem.setText("📎 IA");
                    log.info("[COPILOTO] extrairMargemAssincrona: Extração concluída. Resposta tamanho: {}",
                            respostaCompleta != null ? respostaCompleta.length() : 0);

                    if (respostaCompleta != null && !respostaCompleta.isBlank()) {
                        mainController.notificarAviso("Análise concluída! O log está no console.");
                        if (respostaCompleta.contains("RESULTADO FINAL:")) {
                            String[] partes = respostaCompleta.split("RESULTADO FINAL:");
                            String margemLimpa = partes[1].trim().replaceAll("[^0-9,]", "");
                            if (!margemLimpa.isEmpty() && !margemLimpa.equals("0") && !margemLimpa.equals("0,00")) {
                                txtMargem.setText(margemLimpa);
                                log.debug("[COPILOTO] extrairMargemAssincrona: Margem extraída e preenchida: {}",
                                        margemLimpa);
                            } else {
                                log.warn("[COPILOTO] extrairMargemAssincrona: Margem extraída é inválida: '{}'",
                                        margemLimpa);
                            }
                        }
                    } else {
                        log.warn("[COPILOTO] extrairMargemAssincrona: Resposta da IA está vazia ou nula");
                        mainController.notificarAviso("A IA não conseguiu analisar o documento.");
                    }
                },
                erro -> {
                    log.error("[COPILOTO] extrairMargemAssincrona: Erro durante extração", erro);
                    mainController.ocultarLoading();
                    btnExtrairMargem.setDisable(false);
                    btnExtrairMargem.setText("📎 IA");
                    mainController.notificarAviso("Erro na extração de documento.");
                });
    }

    private void iniciarSimulacaoAssincrona() {
        log.debug("[COPILOTO] iniciarSimulacaoAssincrona: Iniciando simulação assíncrona");
        btnSimular.setText("⏳ Buscando...");
        btnSimular.setDisable(true);
        recomendacoesIA.clear();

        AsyncUtils.executarTaskAsync(
                () -> copilotoService.processarSimulacaoRapida(rascunhoAtual),
                ranking -> {
                    rankingAtual = ranking;
                    listaRanking.setItems(FXCollections.observableArrayList(ranking));
                    boxResultados.setVisible(true);
                    boxResultados.setManaged(true);
                    resetarBotoes();
                    log.info("[COPILOTO] iniciarSimulacaoAssincrona: Simulação finalizada com {} resultados",
                            ranking != null ? ranking.size() : 0);
                },
                erro -> {
                    log.error("[COPILOTO] iniciarSimulacaoAssincrona: Erro na simulação", erro);
                    mainController.notificarAviso("Erro ao processar. Verifique os valores.");
                    resetarBotoes();
                });
    }

    private int parseSafeInt(String texto) {
        if (texto == null || texto.replaceAll("[^0-9]", "").isEmpty()) {
            log.debug("[COPILOTO] parseSafeInt: Texto vazio ou nulo, retornando 0");
            return 0;
        }
        try {
            int valor = Integer.parseInt(texto.replaceAll("[^0-9]", ""));
            log.trace("[COPILOTO] parseSafeInt: Convertido '{}' para {}", texto, valor);
            return valor;
        } catch (NumberFormatException e) {
            log.warn("[COPILOTO] parseSafeInt: Erro ao converter '{}' para inteiro, retornando 0", texto, e);
            return 0;
        }
    }

    private void converterParaProposta(ResultadoSimulacaoDTO resultadoEscolhido, ProponenteModel cliente) {
        log.debug("[COPILOTO] converterParaProposta: Iniciando conversão para proposta. Cliente informado? {}",
                cliente != null);
        if (cliente == null) {
            log.warn("[COPILOTO] converterParaProposta: Cliente nulo, abortando conversão");
            mainController.notificarAviso("Selecione um cliente no campo acima antes de gerar a proposta.");
            return;
        }
        log.info(
                "[COPILOTO] converterParaProposta: Gerando proposta para cliente {} com resultado banco={}, parcela={}",
                cliente.getNomeCompleto(),
                resultadoEscolhido.tabela().getBanco().getNome(),
                FinanceiroUtils.formatarParaExibicao(resultadoEscolhido.valorParcela()));
        mainController.iniciarConversaoCopiloto(rascunhoAtual, resultadoEscolhido, cliente);
    }

    private void resetarBotoes() {
        log.debug("[COPILOTO] resetarBotoes: Restaurando estado dos botões");
        btnSimular.setText("🔍 Buscar Melhores Tabelas");
        btnSimular.setDisable(false);
    }

    @Override
    public void dispose() {
        log.info("[COPILOTO] dispose: Liberando recursos e desinscrevendo do event hub");
        eventHub.desinscrever(this::atualizarListaClientes);
    }
}