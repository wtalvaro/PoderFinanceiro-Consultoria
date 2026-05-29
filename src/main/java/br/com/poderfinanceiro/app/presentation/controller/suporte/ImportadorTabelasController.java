package br.com.poderfinanceiro.app.presentation.controller.suporte;

import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.application.facade.IImportadorTabelasFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * <h1>ImportadorTabelasController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por orquestrar a extração de
 * tabelas via OCR (IA). Implementa o padrão <b>Humble Object</b>, delegando a
 * comunicação com a IA, parsing de JSON e persistência em lote para a
 * {@link IImportadorTabelasFacade}.
 * </p>
 */
@Component @Scope("prototype")
public class ImportadorTabelasController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(ImportadorTabelasController.class);
    private static final String LOG_PREFIX = "[ImportadorTabelasController]";

    private static final String MODELO_IA_PADRAO = "gemini-3.5-flash";
    private static final String MSG_LOADING_IA = "Analisando layout e extraindo regras de negócio...";
    private static final String MSG_LOADING_GRAVACAO = "Persistindo lote e ativando novas vigências...";
    private static final String MSG_ERRO_GRAVACAO = "Erro ao gravar lote: ";

    private static final String STATUS_REVISADO = "🟢";
    private static final String STATUS_PENDENTE = "🟡";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IImportadorTabelasFacade importadorFacade;
    private final Navigator navigator;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private ComboBox<String> cmbModeloIA;
    @FXML private ComboBox<String> cmbBancoLote;
    @FXML private ComboBox<String> cmbConvenioLote;
    @FXML private Button btnAplicarLote;
    @FXML private Button btnGravarLote;
    @FXML private Button btnProcessarImagem;

    @FXML private TableView<TabelaImportadaDTO> tableMaster;
    @FXML private TableColumn<TabelaImportadaDTO, String> colStatus, colBanco, colNome;

    @FXML private ComboBox<String> cmbBanco;
    @FXML private ComboBox<String> cmbConvenio;
    @FXML private TextField txtNomeTabela;
    @FXML private TextField txtValorMin, txtValorMax, txtPrazoMin, txtPrazoMax, txtIdadeMin, txtIdadeMax;
    @FXML private TextField txtTaxa, txtComissao;
    @FXML private Button btnConfirmarTabela;
    @FXML private Label lblTotalTabelas;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<TabelaImportadaDTO> listaTabelas = FXCollections.observableArrayList();
    private TabelaImportadaDTO tabelaEmEdicao;
    private boolean modelosCarregados = false;

    public ImportadorTabelasController(IImportadorTabelasFacade importadorFacade, Navigator navigator) {
        this.importadorFacade = importadorFacade;
        this.navigator = navigator;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface do Importador de Tabelas...", LOG_PREFIX);
        configurarTabelaMaster();
        carregarCombosDeApoio();
        carregarModelosGemini();
        configurarListenersDeEstado();
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    private void configurarListenersDeEstado() {
        log.trace("{} [UI] Configurando listeners de estado da lista.", LOG_PREFIX);
        listaTabelas.addListener((ListChangeListener<TabelaImportadaDTO>) c -> {
            boolean temDados = !listaTabelas.isEmpty();
            btnGravarLote.setDisable(!temDados);
            btnAplicarLote.setDisable(!temDados);
            lblTotalTabelas.setText(temDados ? "Total: " + listaTabelas.size() + " tabela(s)" : "");
        });
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarTabelaMaster() {
        log.trace("{} [UI] Configurando colunas da tabela master.", LOG_PREFIX);
        tableMaster.setItems(listaTabelas);

        // CORREÇÃO: isRevisado() em vez de isRevisada()
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isRevisado() ? STATUS_REVISADO : STATUS_PENDENTE));
        colBanco.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBanco()));
        colNome.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomeTabela()));

        tableMaster.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                log.trace("{} [UI] Tabela selecionada para revisão: {}", LOG_PREFIX, newSelection.getNomeTabela());
                carregarTabelaParaEdicao(newSelection);
            }
        });
    }

    private void carregarCombosDeApoio() {
        log.trace("{} [UI] Carregando combos de apoio (Bancos e Convênios).", LOG_PREFIX);
        List<String> convenios = Arrays.stream(TipoConvenioModel.values()).map(Enum::name).toList();
        cmbConvenio.getItems().setAll(convenios);
        cmbConvenioLote.getItems().setAll(convenios);

        AsyncUtils.executarTaskAsync(importadorFacade::listarBancosAtivos, bancos -> {
            List<String> nomesBancos = bancos.stream().map(BancoModel::getNome).toList();
            cmbBanco.getItems().setAll(nomesBancos);
            cmbBancoLote.getItems().setAll(nomesBancos);
        }, erro -> log.error("{} [SISTEMA] Erro ao carregar bancos: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void carregarModelosGemini() {
        if (cmbModeloIA == null || modelosCarregados)
            return;

        cmbModeloIA.getItems().add(MODELO_IA_PADRAO);
        cmbModeloIA.getSelectionModel().selectFirst();

        AsyncUtils.executarTaskAsync(importadorFacade::listarModelosIADisponiveis, modelos -> {
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

    // ==========================================================================================
    // MÓDULO 7: INTELIGÊNCIA ARTIFICIAL (OCR)
    // ==========================================================================================
    @FXML private void processarImagemIA() {
        log.info("{} [TELEMETRIA] Usuário solicitou processamento de imagem via IA.", LOG_PREFIX);
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecione a Imagem da Tabela");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File arquivo = fc.showOpenDialog(btnProcessarImagem.getScene().getWindow());

        if (arquivo != null && arquivo.exists()) {
            String modelo = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : MODELO_IA_PADRAO;
            navigator.mostrarLoading(MSG_LOADING_IA);

            AsyncUtils.executarTaskAsync(() -> importadorFacade.extrairTabelasDeImagem(arquivo, modelo),
                    tabelasExtraidas -> {
                        navigator.ocultarLoading();
                        log.info("{} [AUDITORIA] IA extraiu {} tabelas. Iniciando aplicação de precedência.",
                                LOG_PREFIX, tabelasExtraidas.size());

                        // LÓGICA DE PRECEDÊNCIA: Captura seleções manuais
                        // pré-processamento
                        String bancoManual = cmbBancoLote.getSelectionModel().getSelectedItem();
                        String convenioManual = cmbConvenioLote.getSelectionModel().getSelectedItem();

                        for (TabelaImportadaDTO dto : tabelasExtraidas) {
                            // Só sobrescreve se o usuário selecionou algo real
                            // (não vazio/null)
                            if (bancoManual != null && !bancoManual.isBlank()) {
                                log.trace("{} [NEGOCIO] Aplicando Banco manual: {}", LOG_PREFIX, bancoManual);
                                dto.setBanco(bancoManual);
                            }
                            if (convenioManual != null && !convenioManual.isBlank()) {
                                log.trace("{} [NEGOCIO] Aplicando Convênio manual: {}", LOG_PREFIX, convenioManual);
                                dto.setTipoConvenio(convenioManual);
                            }

                            // LOG DE RASTREABILIDADE: Mostra como o DTO ficou
                            // após a extração
                            log.info("{} [TELEMETRIA] DTO Extraído: {}", LOG_PREFIX, dto);
                        }

                        listaTabelas.setAll(tabelasExtraidas);
                        navigator.notificarSucesso(tabelasExtraidas.size() + " tabelas extraídas com sucesso!");

                        if (!listaTabelas.isEmpty()) {
                            selecionarProximaPendente();
                        }

                    }, erro -> {
                        log.error("{} [AUDITORIA] Falha na extração da IA: {}", LOG_PREFIX, erro.getMessage());
                        navigator.ocultarLoading();
                        navigator.notificarAviso("Erro na extração: " + erro.getMessage());
                    });
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
        txtValorMin.setText(formatarBigDecimal(dto.getValorMinimo()));
        txtValorMax.setText(formatarBigDecimal(dto.getValorMaximo()));
        txtPrazoMin.setText(String.valueOf(dto.getPrazoMinimo()));
        txtPrazoMax.setText(String.valueOf(dto.getPrazoMaximo()));
        txtIdadeMin.setText(String.valueOf(dto.getIdadeMinima()));
        txtIdadeMax.setText(String.valueOf(dto.getIdadeMaxima()));
        txtTaxa.setText(formatarBigDecimal(dto.getTaxaMensal()));
        txtComissao.setText(formatarBigDecimal(dto.getComissaoPercentual()));
        btnConfirmarTabela.setDisable(false);
    }

    @FXML private void handleConfirmarEdicao() {
        if (tabelaEmEdicao == null)
            return;
        log.trace("{} [UI] Confirmando edição da tabela: {}", LOG_PREFIX, tabelaEmEdicao.getNomeTabela());

        tabelaEmEdicao.setBanco(cmbBanco.getValue());
        tabelaEmEdicao.setTipoConvenio(cmbConvenio.getValue());
        tabelaEmEdicao.setNomeTabela(txtNomeTabela.getText());
        tabelaEmEdicao.setValorMinimo(parseBigDecimal(txtValorMin.getText()));
        tabelaEmEdicao.setValorMaximo(parseBigDecimal(txtValorMax.getText()));
        tabelaEmEdicao.setPrazoMinimo(parseInt(txtPrazoMin.getText()));
        tabelaEmEdicao.setPrazoMaximo(parseInt(txtPrazoMax.getText()));
        tabelaEmEdicao.setIdadeMinima(parseInt(txtIdadeMin.getText()));
        tabelaEmEdicao.setIdadeMaxima(parseInt(txtIdadeMax.getText()));
        tabelaEmEdicao.setTaxaMensal(parseBigDecimal(txtTaxa.getText()));
        tabelaEmEdicao.setComissaoPercentual(parseBigDecimal(txtComissao.getText()));

        // CORREÇÃO: setRevisado() em vez de setRevisada()
        tabelaEmEdicao.setRevisado(true);

        tableMaster.refresh();
        selecionarProximaPendente();
    }

    private void selecionarProximaPendente() {
        for (TabelaImportadaDTO dto : listaTabelas) {
            if (!dto.isRevisado()) {
                tableMaster.getSelectionModel().select(dto);
                // CORREÇÃO 2: Força a tabela a rolar (scroll) até o item
                // selecionado
                tableMaster.scrollTo(dto);
                return;
            }
        }
        limparFormularioEdicao();
    }

    @FXML private void removerTabelaAtual() {
        if (tabelaEmEdicao == null)
            return;
        log.info("{} [TELEMETRIA] Removendo tabela da lista de importação: {}", LOG_PREFIX, tabelaEmEdicao.getNomeTabela());

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
    @FXML private void handleAplicarLote() {
        String bancoLote = cmbBancoLote.getValue();
        String convenioLote = cmbConvenioLote.getValue();
        log.info("{} [TELEMETRIA] Aplicando valores em lote. Banco: {}, Convênio: {}", LOG_PREFIX, bancoLote, convenioLote);

        if (bancoLote == null && convenioLote == null) {
            navigator.notificarAviso("Selecione um Banco ou Convênio para aplicar em lote.");
            return;
        }

        for (TabelaImportadaDTO dto : listaTabelas) {
            if (bancoLote != null)
                dto.setBanco(bancoLote);
            if (convenioLote != null)
                dto.setTipoConvenio(convenioLote);
        }
        tableMaster.refresh();

        if (tabelaEmEdicao != null) {
            if (bancoLote != null)
                cmbBanco.setValue(bancoLote);
            if (convenioLote != null)
                cmbConvenio.setValue(convenioLote);
        }
    }

    @FXML private void handleGravarLote() {
        log.info("{} [TELEMETRIA] Solicitando gravação do lote de tabelas.", LOG_PREFIX);

        // CORREÇÃO: isRevisado() em vez de isRevisada()
        long pendentes = listaTabelas.stream().filter(t -> !t.isRevisado()).count();

        if (pendentes > 0) {
            // CORREÇÃO: Removido o 6º parâmetro (null) da chamada do Navigator
            navigator.solicitarConfirmacao("⚠️ Tabelas não revisadas",
                    "Existem " + pendentes + " tabelas que não foram marcadas como revisadas. Deseja gravar mesmo assim?",
                    "Sim, Gravar Tudo", "#f57c00", this::executarGravacaoLote);
        } else {
            executarGravacaoLote();
        }
    }

    private void executarGravacaoLote() {
        navigator.mostrarLoading(MSG_LOADING_GRAVACAO);

        AsyncUtils.executarTaskAsync(() -> {
            importadorFacade.salvarLoteTabelas(new ArrayList<>(listaTabelas));
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] Lote gravado com sucesso.", LOG_PREFIX);
            navigator.ocultarLoading();
            navigator.notificarSucesso("Lote gravado com sucesso! As tabelas já estão disponíveis para uso.");
            listaTabelas.clear();
            limparFormularioEdicao();
        }, erro -> {
            log.error("{} [AUDITORIA] Erro ao gravar lote: {}", LOG_PREFIX, erro.getMessage());
            navigator.ocultarLoading();
            navigator.notificarAviso(MSG_ERRO_GRAVACAO + erro.getMessage());
        });
    }

    // ==========================================================================================
    // MÓDULO 10: UTILITÁRIOS DE PARSING
    // ==========================================================================================
    private String formatarBigDecimal(BigDecimal valor) {
        return valor != null ? valor.toString().replace(".", ",") : "";
    }

    private BigDecimal parseBigDecimal(String texto) {
        if (texto == null || texto.isBlank())
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(texto.replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int parseInt(String texto) {
        if (texto == null || texto.isBlank())
            return 0;
        try {
            return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
