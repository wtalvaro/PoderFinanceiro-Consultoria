package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.facade.ICopilotoFacade;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.DataUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>CopilotoController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pelo Copiloto de Vendas (Simulador
 * + IA). Implementa o padrão <b>Humble Object</b>, delegando cálculos, parsing
 * de IA e orquestração de serviços para a {@link ICopilotoFacade}.
 * </p>
 */
@Component
public class CopilotoController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(CopilotoController.class);
    private static final String LOG_PREFIX = "[CopilotoController]";
    private static final String MODELO_IA_FALLBACK = "gemini-3.5-flash";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ICopilotoFacade copilotoFacade;
    private final MainController mainController;
    private final ProponenteUIEventHub eventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TextField txtRenda, txtValor, txtPrazo, txtMargem;
    @FXML private ComboBox<TipoConvenioModel> cbConvenio;
    @FXML private Button btnSimular, btnPedirConselho, btnExtrairMargem;
    @FXML private VBox boxResultados, boxRespostaIA;
    @FXML private Label lblRespostaIA;
    @FXML private ComboBox<ProponenteModel> cbCliente;
    @FXML private DatePicker dpDataNascimento;
    @FXML private ListView<ResultadoSimulacaoDTO> listaRanking;
    @FXML private ComboBox<String> cmbModeloIA;
    @FXML private ScrollPane scrollArea;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private SimulacaoRascunhoDTO rascunhoAtual;
    private List<ResultadoSimulacaoDTO> rankingAtual;
    private List<Integer> recomendacoesIA = new ArrayList<>();
    private boolean modelosCarregados = false;

    public CopilotoController(ICopilotoFacade copilotoFacade, MainController mainController, ProponenteUIEventHub eventHub) {
        this.copilotoFacade = copilotoFacade;
        this.mainController = mainController;
        this.eventHub = eventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface do Copiloto...", LOG_PREFIX);

        cbConvenio.getItems().setAll(TipoConvenioModel.values());
        configurarFormatadores();
        configurarComboClientes();
        configurarListViewRanking();
        carregarModelosGemini();

        eventHub.inscrever(this::atualizarListaClientes);
        atualizarListaClientes();

        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo do hub de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::atualizarListaClientes);
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarFormatadores() {
        log.trace("{} [UI] Configurando formatadores de moeda e data.", LOG_PREFIX);
        txtRenda.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtValor.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtMargem.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtPrazo.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
        dpDataNascimento.getEditor().setTextFormatter(DataUtils.criarFormatadorData());
    }

    private void configurarComboClientes() {
        log.trace("{} [UI] Configurando ComboBox de clientes.", LOG_PREFIX);
        cbCliente.setConverter(new StringConverter<>() {
            @Override public String toString(ProponenteModel p) {
                return p != null ? p.getNomeCompleto() : "";
            }

            @Override public ProponenteModel fromString(String s) {
                return null;
            }
        });

        cbCliente.valueProperty().addListener((obs, old, cliente) -> {
            if (cliente != null) {
                dpDataNascimento.setValue(cliente.getDataNascimento());
                if (cliente.getRendaMensal() != null && cliente.getRendaMensal().compareTo(BigDecimal.ZERO) > 0) {
                    txtRenda.setText(FinanceiroUtils.formatarParaExibicao(cliente.getRendaMensal()));
                } else {
                    txtRenda.setText("");
                }
            } else {
                dpDataNascimento.setValue(null);
                txtRenda.setText("");
            }
        });
    }

    private void atualizarListaClientes() {
        log.trace("{} [TELEMETRIA] Atualizando lista de clientes na UI.", LOG_PREFIX);
        Platform.runLater(() -> {
            ProponenteModel selecionado = cbCliente.getValue();
            List<ProponenteModel> clientes = copilotoFacade.listarClientesCarteira();
            cbCliente.getItems().setAll(clientes);

            if (selecionado != null) {
                clientes.stream().filter(c -> c.getId().equals(selecionado.getId())).findFirst().ifPresent(cbCliente::setValue);
            }
        });
    }

    private void configurarListViewRanking() {
        log.trace("{} [UI] Configurando ListView de Ranking.", LOG_PREFIX);
        listaRanking.setCellFactory(lv -> new ListCell<>() {
            private Node view;
            private CopilotoCardController controller;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/copiloto_card.fxml"));
                    view = loader.load();
                    controller = loader.getController();
                } catch (IOException e) {
                    log.error("{} [SISTEMA] Erro ao carregar copiloto_card.fxml: {}", LOG_PREFIX, e.getMessage());
                }
            }

            @Override protected void updateItem(ResultadoSimulacaoDTO item, boolean empty) {
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
    }

    // ==========================================================================================
    // MÓDULO 7: INTELIGÊNCIA ARTIFICIAL (GEMINI)
    // ==========================================================================================
    private void carregarModelosGemini() {
        if (cmbModeloIA == null || modelosCarregados)
            return;

        cmbModeloIA.getItems().add(MODELO_IA_FALLBACK);
        cmbModeloIA.getSelectionModel().selectFirst();

        AsyncUtils.executarTaskAsync(copilotoFacade::listarModelosIADisponiveis, modelos -> {
            if (!modelos.isEmpty()) {
                String atual = cmbModeloIA.getValue();
                cmbModeloIA.getItems().setAll(modelos);
                if (modelos.contains(atual))
                    cmbModeloIA.getSelectionModel().select(atual);
                else
                    cmbModeloIA.getSelectionModel().selectFirst();
                modelosCarregados = true;
                log.info("{} [TELEMETRIA] {} modelos de IA carregados.", LOG_PREFIX, modelos.size());
            }
        }, erro -> log.error("{} [SISTEMA] Erro ao carregar modelos da API: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML private void handleExtrairMargem() {
        log.info("{} [TELEMETRIA] Usuário solicitou extração de margem via documento.", LOG_PREFIX);
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Holerite / Hiscon");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(txtMargem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            btnExtrairMargem.setDisable(true);
            btnExtrairMargem.setText("⏳");
            mainController.mostrarLoading("A IA está lendo o documento...");

            AsyncUtils.executarTaskAsync(() -> copilotoFacade.extrairMargemDocumento(arquivo), margemExtraida -> {
                mainController.ocultarLoading();
                btnExtrairMargem.setDisable(false);
                btnExtrairMargem.setText("📎 IA");

                if (margemExtraida != null) {
                    txtMargem.setText(margemExtraida);
                    mainController.notificarSucesso("Margem extraída com sucesso!");
                } else {
                    mainController.notificarAviso("A IA não conseguiu encontrar uma margem válida no documento.");
                }
            }, erro -> {
                log.error("{} [AUDITORIA] Erro na extração de margem: {}", LOG_PREFIX, erro.getMessage());
                mainController.ocultarLoading();
                btnExtrairMargem.setDisable(false);
                btnExtrairMargem.setText("📎 IA");
                mainController.notificarAviso("Erro na extração de documento.");
            });
        }
    }

    @FXML private void handlePedirConselhoIA() {
        log.info("{} [TELEMETRIA] Usuário solicitou conselho da IA.", LOG_PREFIX);

        if (rankingAtual == null || rankingAtual.isEmpty())
            return;

        if (scrollArea != null)
            scrollArea.requestFocus();

        btnPedirConselho.setText("✨ Analisando...");
        btnPedirConselho.setDisable(true);
        boxRespostaIA.setVisible(true);
        boxRespostaIA.setManaged(true);
        lblRespostaIA.setText("Pensando em estratégias comerciais...");

        String modeloEscolhido = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : MODELO_IA_FALLBACK;

        AsyncUtils.executarTaskAsync(() -> copilotoFacade.gerarConselhoEReordenarRanking(rascunhoAtual, rankingAtual, modeloEscolhido),
                conselho -> {
                    log.info("{} [AUDITORIA] Conselho da IA recebido e ranking reordenado.", LOG_PREFIX);
                    rankingAtual = conselho.rankingReordenado();
                    recomendacoesIA = conselho.indicesRecomendados();

                    listaRanking.setItems(FXCollections.observableArrayList(rankingAtual));
                    lblRespostaIA.setText(conselho.textoResposta());

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
                }, erro -> {
                    log.error("{} [AUDITORIA] Erro ao pedir conselho à IA: {}", LOG_PREFIX, erro.getMessage());
                    lblRespostaIA.setText("Erro ao conectar com o Copiloto.");
                    btnPedirConselho.setDisable(false);
                });
    }

    // ==========================================================================================
    // MÓDULO 8: SIMULAÇÃO E CONVERSÃO
    // ==========================================================================================
    @FXML private void handleSimular() {
        log.info("{} [TELEMETRIA] Iniciando simulação manual.", LOG_PREFIX);

        lblRespostaIA.setText("");
        boxRespostaIA.setVisible(false);
        boxRespostaIA.setManaged(false);
        btnPedirConselho.setDisable(false);
        btnPedirConselho.setText("✨ Analisar com IA");

        int idade = copilotoFacade.calcularIdade(dpDataNascimento.getValue());
        int prazo = parseSafeInt(txtPrazo.getText());
        BigDecimal renda = FinanceiroUtils.extrairValorParaBanco(txtRenda.getText());
        BigDecimal valor = FinanceiroUtils.extrairValorParaBanco(txtValor.getText());
        BigDecimal margem = FinanceiroUtils.extrairValorParaBanco(txtMargem.getText());
        String convenio = cbConvenio.getValue() != null ? cbConvenio.getValue().name() : "";

        this.rascunhoAtual = new SimulacaoRascunhoDTO(idade, renda, convenio, valor, prazo, margem);

        btnSimular.setText("⏳ Buscando...");
        btnSimular.setDisable(true);
        recomendacoesIA.clear();

        AsyncUtils.executarTaskAsync(() -> copilotoFacade.processarSimulacaoRapida(rascunhoAtual), ranking -> {
            log.info("{} [AUDITORIA] Simulação concluída. {} resultados encontrados.", LOG_PREFIX, ranking.size());
            rankingAtual = ranking;
            listaRanking.setItems(FXCollections.observableArrayList(ranking));
            boxResultados.setVisible(true);
            boxResultados.setManaged(true);
            resetarBotoes();
        }, erro -> {
            log.error("{} [AUDITORIA] Erro na simulação: {}", LOG_PREFIX, erro.getMessage());
            mainController.notificarAviso("Erro ao processar. Verifique os valores.");
            resetarBotoes();
        });
    }

    private void converterParaProposta(ResultadoSimulacaoDTO resultadoEscolhido, ProponenteModel cliente) {
        log.info("{} [TELEMETRIA] Solicitando conversão de simulação para proposta.", LOG_PREFIX);
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

    private int parseSafeInt(String texto) {
        if (texto == null || texto.replaceAll("[^0-9]", "").isEmpty())
            return 0;
        try {
            return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
