package br.com.poderfinanceiro.app.presentation.controller.proposta;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.ICopilotoFacade;
import br.com.poderfinanceiro.app.common.util.*;
import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.presentation.controller.layout.MainController;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.presentation.ui.state.IAModelRegistry;
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
 * + IA).
 * Implementa o padrão <b>Humble Object</b>, utilizando o IAModelRegistry para
 * padronização
 * de modelos e AsyncUtils para orquestração de I/O em Virtual Threads.
 * </p>
 */
@Component
public class CopilotoController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(CopilotoController.class);
    private static final String LOG_PREFIX = "[CopilotoController]";
    private static final String PADRAO_DATA = "dd/MM/yyyy";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ICopilotoFacade copilotoFacade;
    private final Navigator navigator;
    private final ApplicationContext context;
    private final ProponenteUIEventHub eventHub;
    private final IAModelRegistry modelRegistry;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
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

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private SimulacaoRascunhoDTO rascunhoAtual;
    private List<ResultadoSimulacaoDTO> rankingAtual;
    private List<Integer> recomendacoesIA = new ArrayList<>();

    public CopilotoController(ICopilotoFacade copilotoFacade,
            Navigator navigator,
            ApplicationContext context,
            ProponenteUIEventHub eventHub,
            IAModelRegistry modelRegistry) {
        this.copilotoFacade = copilotoFacade;
        this.navigator = navigator;
        this.context = context;
        this.eventHub = eventHub;
        this.modelRegistry = modelRegistry;
        log.info("{} [SISTEMA] Controlador do Copiloto instanciado com suporte a Registry Global.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface do Copiloto de Vendas.", LOG_PREFIX);

        cbConvenio.getItems().setAll(TipoConvenioModel.values());
        configurarFormatadores();
        configurarComboClientes();
        configurarListViewRanking();
        configurarModelosIA();

        log.debug("{} [SISTEMA] Inscrevendo controlador no Hub de Eventos de Proponentes.", LOG_PREFIX);
        eventHub.inscrever(this::atualizarListaClientes);
        atualizarListaClientes();

        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos e desinscrevendo de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::atualizarListaClientes);
    }

    private void configurarModelosIA() {
        log.debug("{} [SISTEMA] Vinculando ComboBox ao Registro Global de Modelos.", LOG_PREFIX);
        cmbModeloIA.setItems(modelRegistry.getModelosDisponiveis());
        cmbModeloIA.getSelectionModel().select(modelRegistry.getModeloPadrao());

        // Dispara a carga global se ainda não foi realizada
        modelRegistry.carregarModelos();
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarFormatadores() {
        log.trace("{} [SISTEMA] Aplicando formatadores monetários e de data.", LOG_PREFIX);
        txtRenda.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtValor.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtMargem.setTextFormatter(FinanceiroUtils.criarFormatadorMoeda());
        txtPrazo.setTextFormatter(FinanceiroUtils.criarFormatadorInteiro());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PADRAO_DATA);
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
        dpDataNascimento.getEditor().setTextFormatter(DataUtils.criarFormatadorData());
    }

    private void configurarComboClientes() {
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

        cbCliente.valueProperty().addListener((obs, old, cliente) -> {
            if (cliente != null) {
                log.debug("{} [NEGOCIO] Cliente selecionado: {}. Sincronizando dados básicos.", LOG_PREFIX,
                        cliente.getNomeCompleto());
                dpDataNascimento.setValue(cliente.getDataNascimento());
                txtRenda.setText(FinanceiroUtils.formatarParaExibicao(cliente.getRendaMensal()));
            }
        });
    }

    private void atualizarListaClientes() {
        log.trace("{} [TELEMETRIA] Atualizando lista de clientes no combo.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(
                copilotoFacade::listarClientesCarteira,
                clientes -> {
                    ProponenteModel selecionado = cbCliente.getValue();
                    cbCliente.getItems().setAll(clientes);
                    if (selecionado != null) {
                        clientes.stream()
                                .filter(c -> c.getId().equals(selecionado.getId()))
                                .findFirst()
                                .ifPresent(cbCliente::setValue);
                    }
                },
                erro -> log.error("{} [SISTEMA] Falha ao atualizar lista de clientes: {}", LOG_PREFIX,
                        erro.getMessage()));
    }

    private void configurarListViewRanking() {
        log.debug("{} [SISTEMA] Configurando renderizador dinâmico de cards de ranking.", LOG_PREFIX);
        listaRanking.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ResultadoSimulacaoDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/copiloto_card.fxml"));
                        loader.setControllerFactory(context::getBean);
                        Node view = loader.load();
                        CopilotoCardController ctrl = loader.getController();

                        ctrl.setDados(item, res -> converterParaProposta(res, cbCliente.getValue()));
                        ctrl.getBtnAproveitar().disableProperty().bind(cbCliente.valueProperty().isNull());

                        int rank = recomendacoesIA.indexOf(getIndex()) + 1;
                        ctrl.setRecomendadoIA(rank);

                        setGraphic(view);
                    } catch (IOException e) {
                        log.error("{} [SISTEMA] Erro ao renderizar card do ranking: {}", LOG_PREFIX, e.getMessage());
                    }
                }
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 7: INTEGRAÇÃO COM INTELIGÊNCIA ARTIFICIAL
    // ==========================================================================================
    @FXML
    private void handleExtrairMargem() {
        log.info("{} [TELEMETRIA] Iniciando fluxo de extração de margem via IA.", LOG_PREFIX);

        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Holerite / Hiscon");
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));
        File arquivo = fc.showOpenDialog(txtMargem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            navigator.mostrarLoading("A IA está analisando o documento...");

            AsyncUtils.executarTaskAsync(
                    () -> {
                        log.debug("{} [NEGOCIO] Enviando arquivo para processamento OCR/IA.", LOG_PREFIX);
                        return copilotoFacade.extrairMargemDocumento(arquivo);
                    },
                    margem -> {
                        navigator.ocultarLoading();
                        if (ValidationUtils.isPreenchido(margem)) {
                            txtMargem.setText(margem);
                            log.info("{} [AUDITORIA] Margem extraída com sucesso: {}", LOG_PREFIX, margem);
                            navigator.notificarSucesso("Margem extraída com sucesso!");
                        } else {
                            log.warn("{} [NEGOCIO] IA não identificou margem no documento.", LOG_PREFIX);
                            navigator.notificarAviso("A IA não localizou uma margem clara no documento.");
                        }
                    },
                    erro -> {
                        navigator.ocultarLoading();
                        log.error("{} [AUDITORIA] Falha crítica no processamento do documento: {}", LOG_PREFIX,
                                erro.getMessage());
                        navigator.notificarAviso("Erro ao processar documento.");
                    });
        }
    }

    @FXML
    private void handlePedirConselhoIA() {
        if (rankingAtual == null || rankingAtual.isEmpty()) {
            log.warn("{} [NEGOCIO] Pedido de conselho abortado: Ranking vazio.", LOG_PREFIX);
            navigator.notificarAviso("Realize uma simulação antes de pedir conselho.");
            return;
        }

        log.info("{} [TELEMETRIA] Solicitando recomendação estratégica ao Copiloto.", LOG_PREFIX);
        navigator.mostrarLoading("O Copiloto está analisando as opções...");
        String modelo = cmbModeloIA.getValue();

        AsyncUtils.executarTaskAsync(
                () -> copilotoFacade.gerarConselhoEReordenarRanking(rascunhoAtual, rankingAtual, modelo),
                conselho -> {
                    navigator.ocultarLoading();
                    rankingAtual = conselho.rankingReordenado();
                    recomendacoesIA = conselho.indicesRecomendados();
                    listaRanking.setItems(FXCollections.observableArrayList(rankingAtual));
                    lblRespostaIA.setText(conselho.textoResposta());
                    boxRespostaIA.setVisible(true);
                    boxRespostaIA.setManaged(true);
                    log.info("{} [AUDITORIA] Recomendação estratégica gerada e ranking reordenado.", LOG_PREFIX);
                },
                erro -> {
                    navigator.ocultarLoading();
                    log.error("{} [AUDITORIA] Erro na análise cognitiva: {}", LOG_PREFIX, erro.getMessage());
                    navigator.notificarAviso("Erro ao conectar com o Copiloto.");
                });
    }

    // ==========================================================================================
    // MÓDULO 8: SIMULAÇÃO E CONVERSÃO
    // ==========================================================================================
    @FXML
    private void handleSimular() {
        log.info("{} [TELEMETRIA] Iniciando orquestração de simulação manual.", LOG_PREFIX);

        if (!validarCamposSimulacao())
            return;

        int idade = copilotoFacade.calcularIdade(dpDataNascimento.getValue());
        BigDecimal valor = FinanceiroUtils.extrairValorParaBanco(txtValor.getText());
        BigDecimal renda = FinanceiroUtils.extrairValorParaBanco(txtRenda.getText());
        BigDecimal margem = FinanceiroUtils.extrairValorParaBanco(txtMargem.getText());
        Integer prazo = FinanceiroUtils.parseSafeInt(txtPrazo.getText());
        String convenio = cbConvenio.getValue() != null ? cbConvenio.getValue().name() : "";

        this.rascunhoAtual = new SimulacaoRascunhoDTO(idade, renda, convenio, valor, prazo, margem);

        navigator.mostrarLoading("Buscando melhores tabelas no mercado...");
        recomendacoesIA.clear();

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Executando motor de cálculo de tabelas.", LOG_PREFIX);
                    return copilotoFacade.processarSimulacaoRapida(rascunhoAtual);
                },
                ranking -> {
                    navigator.ocultarLoading();
                    this.rankingAtual = ranking;
                    listaRanking.setItems(FXCollections.observableArrayList(ranking));
                    boxResultados.setVisible(true);
                    boxResultados.setManaged(true);
                    log.info("{} [AUDITORIA] Simulação finalizada com {} resultados compatíveis.", LOG_PREFIX,
                            ranking.size());
                },
                erro -> {
                    navigator.ocultarLoading();
                    log.error("{} [AUDITORIA] Falha no motor de simulação: {}", LOG_PREFIX, erro.getMessage());
                    navigator.notificarAviso("Erro ao processar simulação.");
                });
    }

    private boolean validarCamposSimulacao() {
        if (!ValidationUtils.isPreenchido(txtValor.getText()) || !ValidationUtils.isPreenchido(txtRenda.getText())) {
            log.warn("{} [NEGOCIO] Simulação abortada: Campos obrigatórios vazios.", LOG_PREFIX);
            navigator.notificarAviso("Preencha o valor pretendido e a renda para simular.");
            return false;
        }
        return true;
    }

    private void converterParaProposta(ResultadoSimulacaoDTO resultado, ProponenteModel cliente) {
        if (cliente == null) {
            log.warn("{} [NEGOCIO] Conversão abortada: Nenhum cliente selecionado.", LOG_PREFIX);
            navigator.notificarAviso("Selecione um cliente antes de gerar a proposta.");
            return;
        }

        log.info("{} [TELEMETRIA] Iniciando conversão de rascunho para proposta real. Cliente: {}", LOG_PREFIX,
                cliente.getNomeCompleto());
        if (navigator instanceof MainController main) {
            main.iniciarConversaoCopiloto(rascunhoAtual, resultado, cliente);
        }
    }
}
