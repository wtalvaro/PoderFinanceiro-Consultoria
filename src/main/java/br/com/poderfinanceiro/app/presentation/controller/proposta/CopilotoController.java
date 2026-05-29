package br.com.poderfinanceiro.app.presentation.controller.proposta;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.ICopilotoFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.DataUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.presentation.controller.layout.MainController;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
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
import org.springframework.context.ApplicationContext;
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
 * + IA). Implementa o padrão <b>Humble Object</b>, delegando cálculos e
 * orquestração para a {@link ICopilotoFacade} e interações globais para o
 * {@link Navigator}.
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
    private final Navigator navigator;
    private final ApplicationContext context;
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

    public CopilotoController(ICopilotoFacade copilotoFacade, Navigator navigator, ApplicationContext context,
            ProponenteUIEventHub eventHub) {
        this.copilotoFacade = copilotoFacade;
        this.navigator = navigator;
        this.context = context;
        this.eventHub = eventHub;
        log.info("{} [SISTEMA] Controlador do Copiloto instanciado com suporte a Navigator.", LOG_PREFIX);
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
        txtRenda.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtValor.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtMargem.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
        dpDataNascimento.getEditor().setTextFormatter(DataUtils.criarFormatadorData());
    }

    private void configurarComboClientes() {
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
                txtRenda.setText(FinanceiroUtils.formatarParaExibicao(cliente.getRendaMensal()));
            }
        });
    }

    private void atualizarListaClientes() {
        Platform.runLater(() -> {
            ProponenteModel selecionado = cbCliente.getValue();
            List<ProponenteModel> clientes = copilotoFacade.listarClientesCarteira();
            cbCliente.getItems().setAll(clientes);
            if (selecionado != null) {
                clientes.stream().filter(c -> c.getId().equals(selecionado.getId())).findFirst()
                        .ifPresent(cbCliente::setValue);
            }
        });
    }

    /**
     * Configura a renderização dinâmica dos cards de ranking. RESOLUÇÃO DO
     * ERRO: Uso de controllerFactory para injetar o Navigator nos cards.
     */
    private void configurarListViewRanking() {
        listaRanking.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ResultadoSimulacaoDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/copiloto_card.fxml"));

                        // CRÍTICO: Permite que o Spring instancie o controller
                        // do card e injete o Navigator
                        loader.setControllerFactory(context::getBean);

                        Node view = loader.load();
                        CopilotoCardController ctrl = loader.getController();

                        ctrl.setDados(item, res -> converterParaProposta(res, cbCliente.getValue()));
                        ctrl.getBtnAproveitar().disableProperty().bind(cbCliente.valueProperty().isNull());

                        int rank = recomendacoesIA.indexOf(getIndex()) + 1;
                        ctrl.setRecomendadoIA(rank);

                        setGraphic(view);
                    } catch (IOException e) {
                        log.error("{} [SISTEMA] Erro ao carregar card do ranking: {}", LOG_PREFIX, e.getMessage());
                    }
                }
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 7: INTEGRAÇÃO COM INTELIGÊNCIA ARTIFICIAL
    // ==========================================================================================
    private void carregarModelosGemini() {
        if (modelosCarregados)
            return;

        AsyncUtils.executarTaskAsync(copilotoFacade::listarModelosIADisponiveis, modelos -> {
            cmbModeloIA.getItems().setAll(modelos);
            cmbModeloIA.getSelectionModel().select(MODELO_IA_FALLBACK);
            modelosCarregados = true;
            log.info("{} [TELEMETRIA] {} modelos de IA carregados.", LOG_PREFIX, modelos.size());
        }, erro -> log.error("{} [SISTEMA] Erro ao carregar modelos IA: {}", LOG_PREFIX, erro.getMessage()));
    }

    @FXML private void handleExtrairMargem() {
        log.info("{} [TELEMETRIA] Solicitando extração de margem via IA.", LOG_PREFIX);
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Holerite / Hiscon");
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(txtMargem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            navigator.mostrarLoading("A IA está lendo o documento...");
            AsyncUtils.executarTaskAsync(() -> copilotoFacade.extrairMargemDocumento(arquivo), margem -> {
                navigator.ocultarLoading();
                if (margem != null) {
                    txtMargem.setText(margem);
                    navigator.notificarSucesso("Margem extraída com sucesso!");
                } else {
                    navigator.notificarAviso("A IA não localizou uma margem clara no documento.");
                }
            }, erro -> {
                navigator.ocultarLoading();
                log.error("{} [AUDITORIA] Erro na extração: {}", LOG_PREFIX, erro.getMessage());
                navigator.notificarAviso("Erro ao processar documento.");
            });
        }
    }

    @FXML private void handlePedirConselhoIA() {
        if (rankingAtual == null || rankingAtual.isEmpty()) {
            navigator.notificarAviso("Realize uma simulação antes de pedir conselho.");
            return;
        }

        log.info("{} [TELEMETRIA] Solicitando recomendação estratégica.", LOG_PREFIX);
        navigator.mostrarLoading("O Copiloto está analisando as opções...");
        String modelo = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : MODELO_IA_FALLBACK;

        AsyncUtils.executarTaskAsync(
                () -> copilotoFacade.gerarConselhoEReordenarRanking(rascunhoAtual, rankingAtual, modelo), conselho -> {
                    navigator.ocultarLoading();
                    rankingAtual = conselho.rankingReordenado();
                    recomendacoesIA = conselho.indicesRecomendados();
                    listaRanking.setItems(FXCollections.observableArrayList(rankingAtual));
                    lblRespostaIA.setText(conselho.textoResposta());
                    boxRespostaIA.setVisible(true);
                    boxRespostaIA.setManaged(true);
                    log.info("{} [AUDITORIA] Recomendação estratégica gerada com sucesso.", LOG_PREFIX);
                }, erro -> {
                    navigator.ocultarLoading();
                    log.error("{} [AUDITORIA] Erro no conselho IA: {}", LOG_PREFIX, erro.getMessage());
                    navigator.notificarAviso("Erro ao conectar com o Copiloto.");
                });
    }

    // ==========================================================================================
    // MÓDULO 8: SIMULAÇÃO E CONVERSÃO
    // ==========================================================================================
    @FXML private void handleSimular() {
        log.info("{} [TELEMETRIA] Iniciando simulação manual.", LOG_PREFIX);

        int idade = copilotoFacade.calcularIdade(dpDataNascimento.getValue());
        BigDecimal valor = FinanceiroUtils.extrairValorParaBanco(txtValor.getText());
        BigDecimal renda = FinanceiroUtils.extrairValorParaBanco(txtRenda.getText());
        BigDecimal margem = FinanceiroUtils.extrairValorParaBanco(txtMargem.getText());
        Integer prazo = parseSafeInt(txtPrazo.getText());
        String convenio = cbConvenio.getValue() != null ? cbConvenio.getValue().name() : "";

        this.rascunhoAtual = new SimulacaoRascunhoDTO(idade, renda, convenio, valor, prazo, margem);

        log.info("{} [TELEMETRIA] SAIDA UI -> FACADE: {}", LOG_PREFIX, rascunhoAtual);

        navigator.mostrarLoading("Buscando melhores tabelas...");

        recomendacoesIA.clear();

        AsyncUtils.executarTaskAsync(() -> copilotoFacade.processarSimulacaoRapida(rascunhoAtual), ranking -> {
            navigator.ocultarLoading();
            this.rankingAtual = ranking;
            listaRanking.setItems(FXCollections.observableArrayList(ranking));
            boxResultados.setVisible(true);
            boxResultados.setManaged(true);
            log.info("{} [AUDITORIA] Simulação concluída. {} resultados.", LOG_PREFIX, ranking.size());
        }, erro -> {
            navigator.ocultarLoading();
            log.error("{} [AUDITORIA] Erro na simulação: {}", LOG_PREFIX, erro.getMessage());
            navigator.notificarAviso("Erro ao processar simulação.");
        });
    }

    private void converterParaProposta(ResultadoSimulacaoDTO resultado, ProponenteModel cliente) {
        if (cliente == null) {
            navigator.notificarAviso("Selecione um cliente antes de gerar a proposta.");
            return;
        }

        if (navigator instanceof MainController main) {
            main.iniciarConversaoCopiloto(rascunhoAtual, resultado, cliente);
        }
    }

    private int parseSafeInt(String texto) {
        if (texto == null || texto.replaceAll("[^0-9]", "").isEmpty())
            return 0;
        try {
            return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
