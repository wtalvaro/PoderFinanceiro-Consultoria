package br.com.poderfinanceiro.app.presentation.controller.suporte;

import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.application.facade.IImportadorTabelasFacade;
import br.com.poderfinanceiro.app.common.util.*;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.presentation.ui.state.IAModelRegistry;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h1>ImportadorTabelasController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por orquestrar a extração de
 * tabelas via OCR (IA).
 * Implementa o padrão <b>Humble Object</b>, utilizando o IAModelRegistry para
 * padronização
 * de modelos e AsyncUtils para processamento pesado em Virtual Threads (Java
 * 25).
 * </p>
 */
@Component
@Scope("prototype")
public class ImportadorTabelasController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(ImportadorTabelasController.class);
    private static final String LOG_PREFIX = "[ImportadorTabelasController]";

    private static final String MSG_LOADING_IA = "Analisando layout e extraindo regras de negócio via Gemini...";
    private static final String MSG_LOADING_GRAVACAO = "Persistindo lote e ativando novas vigências...";
    private static final String STATUS_REVISADO = "🟢";
    private static final String STATUS_PENDENTE = "🟡";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IImportadorTabelasFacade importadorFacade;
    private final Navigator navigator;
    private final IAModelRegistry modelRegistry;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private ComboBox<String> cmbModeloIA;
    @FXML
    private ComboBox<String> cmbBancoLote;
    @FXML
    private ComboBox<String> cmbConvenioLote;
    @FXML
    private Button btnAplicarLote;
    @FXML
    private Button btnGravarLote;
    @FXML
    private Button btnProcessarImagem;

    @FXML
    private TableView<TabelaImportadaDTO> tableMaster;
    @FXML
    private TableColumn<TabelaImportadaDTO, String> colStatus, colBanco, colNome;

    @FXML
    private ComboBox<String> cmbBanco;
    @FXML
    private ComboBox<String> cmbConvenio;
    @FXML
    private TextField txtNomeTabela;
    @FXML
    private TextField txtValorMin, txtValorMax, txtPrazoMin, txtPrazoMax, txtIdadeMin, txtIdadeMax;
    @FXML
    private TextField txtTaxa, txtComissao;
    @FXML
    private Button btnConfirmarTabela;
    @FXML
    private Label lblTotalTabelas;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<TabelaImportadaDTO> listaTabelas = FXCollections.observableArrayList();
    private TabelaImportadaDTO tabelaEmEdicao;

    public ImportadorTabelasController(IImportadorTabelasFacade importadorFacade,
            Navigator navigator,
            IAModelRegistry modelRegistry) {
        this.importadorFacade = importadorFacade;
        this.navigator = navigator;
        this.modelRegistry = modelRegistry;
        log.info("{} [SISTEMA] Controlador do Importador instanciado com suporte a Registry Global.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface do Importador de Tabelas.", LOG_PREFIX);

        configurarTabelaMaster();
        configurarModelosIA();
        carregarCombosDeApoio();
        configurarListenersDeEstado();

        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos do Importador.", LOG_PREFIX);
        listaTabelas.clear();
        tabelaEmEdicao = null;
    }

    private void configurarModelosIA() {
        log.debug("{} [SISTEMA] Vinculando ComboBox ao Registro Global de Modelos.", LOG_PREFIX);
        cmbModeloIA.setItems(modelRegistry.getModelosDisponiveis());

        // Seleção inicial baseada na especialidade de OCR
        String ocrModel = modelRegistry.getModeloParaOCR();
        cmbModeloIA.getSelectionModel().select(ocrModel);

        modelRegistry.carregarModelos();
    }

    private void configurarListenersDeEstado() {
        log.trace("{} [SISTEMA] Configurando listeners de reatividade da lista.", LOG_PREFIX);
        listaTabelas.addListener((ListChangeListener<TabelaImportadaDTO>) c -> {
            boolean temDados = !listaTabelas.isEmpty();
            btnGravarLote.setDisable(!temDados);
            btnAplicarLote.setDisable(!temDados);
            lblTotalTabelas.setText(temDados ? String.valueOf(listaTabelas.size()) : "0");
        });
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarTabelaMaster() {
        log.trace("{} [SISTEMA] Configurando colunas da TableView master.", LOG_PREFIX);
        tableMaster.setItems(listaTabelas);

        colStatus.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().isRevisado() ? STATUS_REVISADO : STATUS_PENDENTE));
        colBanco.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBanco()));
        colNome.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomeTabela()));

        tableMaster.getSelectionModel().selectedItemProperty().addListener((obs, old, newSelection) -> {
            if (newSelection != null) {
                log.trace("{} [TELEMETRIA] Tabela selecionada para revisão: {}", LOG_PREFIX,
                        newSelection.getNomeTabela());
                carregarTabelaParaEdicao(newSelection);
            }
        });
    }

    private void carregarCombosDeApoio() {
        log.trace("{} [SISTEMA] Carregando domínios de Bancos e Convênios.", LOG_PREFIX);
        List<String> convenios = Arrays.stream(TipoConvenioModel.values()).map(Enum::name).toList();
        cmbConvenio.getItems().setAll(convenios);
        cmbConvenioLote.getItems().setAll(convenios);

        AsyncUtils.executarTaskAsync(
                importadorFacade::listarBancosAtivos,
                bancos -> {
                    List<String> nomesBancos = bancos.stream().map(BancoModel::getNome).toList();
                    cmbBanco.getItems().setAll(nomesBancos);
                    cmbBancoLote.getItems().setAll(nomesBancos);
                    log.debug("{} [SISTEMA] {} bancos ativos carregados para apoio.", LOG_PREFIX, nomesBancos.size());
                },
                erro -> log.error("{} [SISTEMA] Erro ao carregar bancos ativos: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: INTELIGÊNCIA ARTIFICIAL (OCR)
    // ==========================================================================================
    @FXML
    private void processarImagemIA() {
        log.info("{} [TELEMETRIA] Solicitando processamento de imagem via IA.", LOG_PREFIX);

        FileChooser fc = new FileChooser();
        fc.setTitle("Selecione a Imagem da Tabela");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File arquivo = fc.showOpenDialog(btnProcessarImagem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            String modelo = cmbModeloIA.getValue();
            navigator.mostrarLoading(MSG_LOADING_IA);

            AsyncUtils.executarTaskAsync(
                    () -> {
                        log.debug("{} [NEGOCIO] Invocando extração OCR com modelo: {}", LOG_PREFIX, modelo);
                        return importadorFacade.extrairTabelasDeImagem(arquivo, modelo);
                    },
                    tabelasExtraidas -> {
                        navigator.ocultarLoading();
                        log.info("{} [AUDITORIA] IA extraiu {} tabelas com sucesso.", LOG_PREFIX,
                                tabelasExtraidas.size());

                        aplicarPrecedenciaManual(tabelasExtraidas);
                        listaTabelas.setAll(tabelasExtraidas);
                        navigator.notificarSucesso(tabelasExtraidas.size() + " tabelas extraídas!");

                        if (!listaTabelas.isEmpty()) {
                            selecionarProximaPendente();
                        }
                    },
                    erro -> {
                        log.error("{} [AUDITORIA] Falha na extração da IA: {}", LOG_PREFIX, erro.getMessage());
                        navigator.ocultarLoading();
                        navigator.notificarAviso("Erro na extração: " + erro.getMessage());
                    });
        }
    }

    private void aplicarPrecedenciaManual(List<TabelaImportadaDTO> extraidas) {
        String bancoManual = cmbBancoLote.getValue();
        String convenioManual = cmbConvenioLote.getValue();

        for (TabelaImportadaDTO dto : extraidas) {
            if (ValidationUtils.isPreenchido(bancoManual))
                dto.setBanco(bancoManual);
            if (ValidationUtils.isPreenchido(convenioManual))
                dto.setTipoConvenio(convenioManual);
        }
    }

    // ==========================================================================================
    // MÓDULO 8: REVISÃO E EDIÇÃO INDIVIDUAL
    // ==========================================================================================
    private void carregarTabelaParaEdicao(TabelaImportadaDTO dto) {
        this.tabelaEmEdicao = dto;
        cmbBanco.setValue(dto.getBanco());
        cmbConvenio.setValue(dto.getTipoConvenio());
        txtNomeTabela.setText(dto.getNomeTabela());
        txtValorMin.setText(FinanceiroUtils.formatarParaExibicao(dto.getValorMinimo()));
        txtValorMax.setText(FinanceiroUtils.formatarParaExibicao(dto.getValorMaximo()));
        txtPrazoMin.setText(String.valueOf(dto.getPrazoMinimo()));
        txtPrazoMax.setText(String.valueOf(dto.getPrazoMaximo()));
        txtIdadeMin.setText(String.valueOf(dto.getIdadeMinima()));
        txtIdadeMax.setText(String.valueOf(dto.getIdadeMaxima()));
        txtTaxa.setText(FinanceiroUtils.formatarParaExibicao(dto.getTaxaMensal()));
        txtComissao.setText(FinanceiroUtils.formatarParaExibicao(dto.getComissaoPercentual()));
        btnConfirmarTabela.setDisable(false);
    }

    @FXML
    private void handleConfirmarEdicao() {
        if (tabelaEmEdicao == null)
            return;

        log.trace("{} [NEGOCIO] Confirmando revisão da tabela: {}", LOG_PREFIX, tabelaEmEdicao.getNomeTabela());

        tabelaEmEdicao.setBanco(cmbBanco.getValue());
        tabelaEmEdicao.setTipoConvenio(cmbConvenio.getValue());
        tabelaEmEdicao.setNomeTabela(txtNomeTabela.getText());
        tabelaEmEdicao.setValorMinimo(FinanceiroUtils.extrairValorParaBanco(txtValorMin.getText()));
        tabelaEmEdicao.setValorMaximo(FinanceiroUtils.extrairValorParaBanco(txtValorMax.getText()));
        tabelaEmEdicao.setPrazoMinimo(FinanceiroUtils.parseSafeInt(txtPrazoMin.getText()));
        tabelaEmEdicao.setPrazoMaximo(FinanceiroUtils.parseSafeInt(txtPrazoMax.getText()));
        tabelaEmEdicao.setIdadeMinima(FinanceiroUtils.parseSafeInt(txtIdadeMin.getText()));
        tabelaEmEdicao.setIdadeMaxima(FinanceiroUtils.parseSafeInt(txtIdadeMax.getText()));
        tabelaEmEdicao.setTaxaMensal(FinanceiroUtils.extrairValorParaBanco(txtTaxa.getText()));
        tabelaEmEdicao.setComissaoPercentual(FinanceiroUtils.extrairValorParaBanco(txtComissao.getText()));

        tabelaEmEdicao.setRevisado(true);
        tableMaster.refresh();
        selecionarProximaPendente();
    }

    private void selecionarProximaPendente() {
        for (TabelaImportadaDTO dto : listaTabelas) {
            if (!dto.isRevisado()) {
                tableMaster.getSelectionModel().select(dto);
                tableMaster.scrollTo(dto);
                return;
            }
        }
        limparFormularioEdicao();
    }

    @FXML
    private void removerTabelaAtual() {
        if (tabelaEmEdicao == null)
            return;
        log.info("{} [TELEMETRIA] Removendo tabela da lista de importação: {}", LOG_PREFIX,
                tabelaEmEdicao.getNomeTabela());
        listaTabelas.remove(tabelaEmEdicao);
        tableMaster.refresh();
        selecionarProximaPendente();
    }

    private void limparFormularioEdicao() {
        this.tabelaEmEdicao = null;
        cmbBanco.setValue(null);
        cmbConvenio.setValue(null);
        txtNomeTabela.clear();
        txtValorMin.clear();
        txtValorMax.clear();
        txtPrazoMin.clear();
        txtPrazoMax.clear();
        txtIdadeMin.clear();
        txtIdadeMax.clear();
        txtTaxa.clear();
        txtComissao.clear();
        btnConfirmarTabela.setDisable(true);
    }

    // ==========================================================================================
    // MÓDULO 9: AÇÕES EM LOTE E PERSISTÊNCIA
    // ==========================================================================================
    @FXML
    private void handleAplicarLote() {
        String bancoLote = cmbBancoLote.getValue();
        String convenioLote = cmbConvenioLote.getValue();

        log.info("{} [TELEMETRIA] Aplicando valores em lote. Banco: {}, Convênio: {}", LOG_PREFIX, bancoLote,
                convenioLote);

        if (!ValidationUtils.isPreenchido(bancoLote) && !ValidationUtils.isPreenchido(convenioLote)) {
            navigator.notificarAviso("Selecione um Banco ou Convênio para aplicar em lote.");
            return;
        }

        for (TabelaImportadaDTO dto : listaTabelas) {
            if (ValidationUtils.isPreenchido(bancoLote))
                dto.setBanco(bancoLote);
            if (ValidationUtils.isPreenchido(convenioLote))
                dto.setTipoConvenio(convenioLote);
        }
        tableMaster.refresh();

        if (tabelaEmEdicao != null) {
            if (ValidationUtils.isPreenchido(bancoLote))
                cmbBanco.setValue(bancoLote);
            if (ValidationUtils.isPreenchido(convenioLote))
                cmbConvenio.setValue(convenioLote);
        }
    }

    @FXML
    private void handleGravarLote() {
        log.info("{} [TELEMETRIA] Solicitando gravação do lote de tabelas.", LOG_PREFIX);

        long pendentes = listaTabelas.stream().filter(t -> !t.isRevisado()).count();

        if (pendentes > 0) {
            navigator.solicitarConfirmacao("⚠️ Tabelas não revisadas",
                    "Existem " + pendentes + " tabelas pendentes de revisão. Deseja gravar o lote mesmo assim?",
                    "Sim, Gravar Tudo", "#f57c00", this::executarGravacaoLote);
        } else {
            executarGravacaoLote();
        }
    }

    private void executarGravacaoLote() {
        navigator.mostrarLoading(MSG_LOADING_GRAVACAO);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando persistência em lote via Facade.", LOG_PREFIX);
                    importadorFacade.salvarLoteTabelas(new ArrayList<>(listaTabelas));
                    return null;
                },
                sucesso -> {
                    log.info("{} [AUDITORIA] Lote de tabelas gravado com sucesso.", LOG_PREFIX);
                    navigator.ocultarLoading();
                    navigator.notificarSucesso("Lote gravado com sucesso!");
                    listaTabelas.clear();
                    limparFormularioEdicao();
                },
                erro -> {
                    log.error("{} [AUDITORIA] Falha crítica ao gravar lote: {}", LOG_PREFIX, erro.getMessage());
                    navigator.ocultarLoading();
                    navigator.notificarAviso("Erro ao gravar lote: " + erro.getMessage());
                });
    }
}
