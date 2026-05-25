package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Scope("prototype")
public class ImportadorTabelasController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String MODELO_IA_PADRAO = "gemini-3.5-flash";
    private static final String MSG_LOADING_IA = "Analisando layout e extraindo regras de negócio...";
    private static final String MSG_LOADING_GRAVACAO = "Persistindo lote e ativando novas vigências...";
    private static final String MSG_ERRO_IA = "Falha na extração da IA: ";
    private static final String MSG_ERRO_GRAVACAO = "Erro ao gravar lote: ";

    private static final String STATUS_REVISADO = "🟢";
    private static final String STATUS_PENDENTE = "🟡";

    private static final Logger log = LoggerFactory.getLogger(ImportadorTabelasController.class);

    // =========================================================================
    // COMPONENTES DE UI GLOBAIS E LOTE
    // =========================================================================
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

    // =========================================================================
    // COMPONENTES DE UI (TABELA MASTER)
    // =========================================================================
    @FXML
    private TableView<TabelaImportadaDTO> tableMaster;
    @FXML
    private TableColumn<TabelaImportadaDTO, String> colStatus, colBanco, colNome;

    // =========================================================================
    // COMPONENTES DE UI (REVISÃO INDIVIDUAL)
    // =========================================================================
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

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final GeminiService geminiService;
    private final TabelaJurosService tabelaJurosService;
    private final AuthService authService;
    private final Navigator navigator;
    private final BancoRepository bancoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private final ObservableList<TabelaImportadaDTO> loteAtual = FXCollections.observableArrayList();
    private TabelaImportadaDTO selecionadaAtual;

    public ImportadorTabelasController(GeminiService geminiService, TabelaJurosService tabelaJurosService,
            AuthService authService, Navigator navigator, BancoRepository bancoRepository) {
        this.geminiService = geminiService;
        this.tabelaJurosService = tabelaJurosService;
        this.authService = authService;
        this.navigator = navigator;
        this.bancoRepository = bancoRepository;
        log.debug("[IMPORTADOR] Construtor: Controller instanciado (escopo prototype)");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[IMPORTADOR] initialize: Iniciando configuração do importador de tabelas");
        carregarConvenios();
        carregarBancos();
        carregarModelosIA();

        configurarTabelaMaster();
        configurarListenersDeEstado();
        alternarBloqueioFormularioRevisao(true);
        log.info("[IMPORTADOR] initialize: Configuração concluída");
    }

    private void carregarConvenios() {
        List<String> convenios = Arrays.stream(TipoConvenioModel.values()).map(Enum::name).toList();
        cmbConvenio.getItems().setAll(convenios);
        if (cmbConvenioLote != null)
            cmbConvenioLote.getItems().setAll(convenios);
        sincronizarEditorCombo(cmbConvenio);
        log.debug("[IMPORTADOR] carregarConvenios: {} convênios carregados", convenios.size());
    }

    // 🚀 PATCH: carregarBancos
    private void carregarBancos() {
        log.debug("[IMPORTADOR] carregarBancos: Buscando bancos ativos");
        AsyncUtils.executarTaskAsync(
                () -> {
                    log.trace("[IMPORTADOR] carregarBancos: Chamando bancoRepository.findByAtivoTrueOrderByNomeAsc");
                    return bancoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(BancoModel::getNome).toList();
                },
                bancos -> {
                    log.info("[IMPORTADOR] carregarBancos: {} bancos carregados", bancos.size());
                    cmbBanco.getItems().setAll(bancos);
                    if (cmbBancoLote != null)
                        cmbBancoLote.getItems().setAll(bancos);
                },
                erro -> log.error("[IMPORTADOR] carregarBancos: Erro ao carregar bancos", erro));
        sincronizarEditorCombo(cmbBanco);
    }

    private void carregarModelosIA() {
        log.debug("[IMPORTADOR] carregarModelosIA: Verificando autenticação para carregar modelos");
        if (authService.estaLogado()) {
            String token = authService.getUsuarioLogado().getGeminiApiKey();
            log.debug("[IMPORTADOR] carregarModelosIA: Token encontrado, buscando modelos");
            executarTaskAsync(
                    () -> {
                        log.trace("[IMPORTADOR] carregarModelosIA: Chamando geminiService.listarModelosMultimodais");
                        return geminiService.listarModelosMultimodais(token);
                    },
                    modelos -> {
                        log.info("[IMPORTADOR] carregarModelosIA: {} modelos carregados", modelos.size());
                        cmbModeloIA.getItems().setAll(modelos);
                        cmbModeloIA.getSelectionModel().selectFirst();
                        log.debug("[IMPORTADOR] carregarModelosIA: Modelo selecionado: {}", cmbModeloIA.getValue());
                    },
                    erro -> log.error("[IMPORTADOR] carregarModelosIA: Erro ao carregar modelos", erro));
        } else {
            log.warn("[IMPORTADOR] carregarModelosIA: Usuário não logado, não será possível carregar modelos IA");
        }
    }

    private void configurarTabelaMaster() {
        log.debug("[IMPORTADOR] configurarTabelaMaster: Configurando tabela master de lotes");
        tableMaster.setItems(loteAtual);
        colStatus.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().isRevisado() ? STATUS_REVISADO : STATUS_PENDENTE));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco()));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeTabela()));

        tableMaster.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("[IMPORTADOR] Seleção alterada na tabela: old={}, new={}",
                    oldVal != null ? oldVal.getNomeTabela() : "null",
                    newVal != null ? newVal.getNomeTabela() : "null");
            selecionadaAtual = newVal;
            if (newVal != null) {
                preencherFormularioRevisao(newVal);
                alternarBloqueioFormularioRevisao(false);
            } else {
                alternarBloqueioFormularioRevisao(true);
            }
        });
        log.info("[IMPORTADOR] configurarTabelaMaster: Tabela configurada");
    }

    private void configurarListenersDeEstado() {
        log.debug("[IMPORTADOR] configurarListenersDeEstado: Configurando listeners de estado dos botões");
        btnGravarLote.setDisable(true);
        if (btnAplicarLote != null)
            btnAplicarLote.setDisable(true);

        loteAtual.addListener((ListChangeListener.Change<? extends TabelaImportadaDTO> c) -> {
            validarBotoesDeAcao();
            if (lblTotalTabelas != null) {
                lblTotalTabelas.setText(String.valueOf(loteAtual.size()));
                log.trace("[IMPORTADOR] Listener: total de tabelas atualizado para {}", loteAtual.size());
            }
        });
    }

    private void validarBotoesDeAcao() {
        boolean temItens = !loteAtual.isEmpty();
        boolean todosRevisados = temItens && loteAtual.stream().allMatch(TabelaImportadaDTO::isRevisado);
        btnGravarLote.setDisable(!todosRevisados);
        if (btnAplicarLote != null)
            btnAplicarLote.setDisable(!temItens);
        log.trace("[IMPORTADOR] validarBotoesDeAcao: temItens={}, todosRevisados={}, btnGravarLote disable={}",
                temItens, todosRevisados, btnGravarLote.isDisable());
    }

    // =========================================================================
    // AÇÕES GLOBAIS E LOTE (DRY APLICADO)
    // =========================================================================
    @FXML
    public void handleAplicarLote() {
        log.info("[IMPORTADOR] handleAplicarLote: Iniciando aplicação em lote");
        if (loteAtual.isEmpty()) {
            log.warn("[IMPORTADOR] handleAplicarLote: Lote vazio, operação cancelada");
            return;
        }

        if (isSelecaoGlobalVazia()) {
            log.error(
                    ("[IMPORTADOR] handleAplicarLote: Selecione pelo menos um Banco ou Convênio para aplicar em lote."));
            return;
        }

        aplicarPropriedadesGlobaisAoLote(loteAtual);
        tableMaster.refresh();
        log.info("[IMPORTADOR] handleAplicarLote: Propriedades globais aplicadas a {} tabelas", loteAtual.size());

        if (selecionadaAtual != null) {
            preencherFormularioRevisao(selecionadaAtual);
            log.debug("[IMPORTADOR] handleAplicarLote: Formulário de revisão atualizado");
        }
        log.info("✅ Aplicação em lote concluída! Revisão individual necessária.");
    }

    private boolean isSelecaoGlobalVazia() {
        String banco = cmbBancoLote != null ? cmbBancoLote.getValue() : null;
        String convenio = cmbConvenioLote != null ? cmbConvenioLote.getValue() : null;
        boolean vazia = (banco == null || banco.isBlank()) && (convenio == null || convenio.isBlank());
        log.trace("[IMPORTADOR] isSelecaoGlobalVazia: banco='{}', convenio='{}', vazia={}", banco, convenio, vazia);
        return vazia;
    }

    private void aplicarPropriedadesGlobaisAoLote(Iterable<TabelaImportadaDTO> lote) {
        String bancoGlobal = cmbBancoLote != null ? cmbBancoLote.getValue() : null;
        String convenioGlobal = cmbConvenioLote != null ? cmbConvenioLote.getValue() : null;
        log.debug("[IMPORTADOR] aplicarPropriedadesGlobaisAoLote: bancoGlobal='{}', convenioGlobal='{}'",
                bancoGlobal, convenioGlobal);

        lote.forEach(tabela -> {
            if (bancoGlobal != null && !bancoGlobal.isBlank())
                tabela.setBanco(bancoGlobal);
            if (convenioGlobal != null && !convenioGlobal.isBlank())
                tabela.setTipoConvenio(convenioGlobal);
        });
    }

    // =========================================================================
    // MOTOR DE IA (GEMINI)
    // =========================================================================
    // 🚀 PATCH: processarImagemIA
    @FXML
    public void processarImagemIA() {
        log.info("[IMPORTADOR] processarImagemIA: Iniciando processamento de imagem/PDF via IA");
        File arquivo = escolherArquivoImagemOuPdf();
        if (arquivo == null) {
            log.warn("[IMPORTADOR] processarImagemIA: Nenhum arquivo selecionado pelo usuário");
            return;
        }

        navigator.mostrarLoading(MSG_LOADING_IA);
        String token = authService.getUsuarioLogado().getGeminiApiKey();
        String modelo = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : MODELO_IA_PADRAO;
        log.debug("[IMPORTADOR] processarImagemIA: Arquivo='{}', modelo='{}'", arquivo.getName(), modelo);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("[IMPORTADOR] processarImagemIA: Chamando geminiService.extrairTabelasEmLote");
                    return processarChamadaIA(arquivo, token, modelo);
                },
                this::finalizarImportacaoComSucesso,
                erro -> finalizarComErro(MSG_ERRO_IA + erro.getMessage()));
    }

    private List<TabelaImportadaDTO> processarChamadaIA(File arquivo, String token, String modelo) throws Exception {
        log.trace("[IMPORTADOR] processarChamadaIA: Executando extração via IA");
        String jsonResposta = geminiService.extrairTabelasEmLote(arquivo, token, modelo);
        log.debug("[IMPORTADOR] processarChamadaIA: JSON recebido tamanho={}", jsonResposta.length());
        return objectMapper.readValue(jsonResposta, new TypeReference<>() {
        });
    }

    private void finalizarImportacaoComSucesso(List<TabelaImportadaDTO> tabelas) {
        log.info("[IMPORTADOR] finalizarImportacaoComSucesso: {} tabelas extraídas com sucesso", tabelas.size());
        navigator.ocultarLoading();
        aplicarPropriedadesGlobaisAoLote(tabelas);
        loteAtual.setAll(tabelas);
        if (!loteAtual.isEmpty()) {
            tableMaster.getSelectionModel().selectFirst();
            log.debug("[IMPORTADOR] Primeira tabela selecionada");
        }
    }

    // =========================================================================
    // REVISÃO INDIVIDUAL E SALVAMENTO
    // =========================================================================
    @FXML
    public void confirmarTabelaAtual() {
        log.debug("[IMPORTADOR] confirmarTabelaAtual: Confirmando revisão da tabela selecionada");
        if (selecionadaAtual == null) {
            log.warn("[IMPORTADOR] confirmarTabelaAtual: Nenhuma tabela selecionada para confirmar");
            return;
        }

        try {
            mapearCamposParaDTO(selecionadaAtual);
            selecionadaAtual.setRevisado(true);
            log.info("[IMPORTADOR] Tabela '{}' marcada como revisada", selecionadaAtual.getNomeTabela());

            tableMaster.refresh();
            validarBotoesDeAcao();
            avancarSelecaoTabela();
        } catch (NumberFormatException e) {
            log.error(("[IMPORTADOR] confirmarTabelaAtual: Verifique os campos numéricos. " + e.getMessage()), e);
        }
    }

    private void mapearCamposParaDTO(TabelaImportadaDTO dto) {
        log.trace("[IMPORTADOR] mapearCamposParaDTO: Mapeando campos do formulário para DTO");
        dto.setBanco(cmbBanco.getEditor().getText());
        dto.setTipoConvenio(cmbConvenio.getEditor().getText());
        dto.setNomeTabela(txtNomeTabela.getText());
        dto.setValorMinimo(new BigDecimal(txtValorMin.getText()));
        dto.setValorMaximo(new BigDecimal(txtValorMax.getText()));
        dto.setPrazoMinimo(Integer.parseInt(txtPrazoMin.getText()));
        dto.setPrazoMaximo(Integer.parseInt(txtPrazoMax.getText()));
        dto.setIdadeMinima(Integer.parseInt(txtIdadeMin.getText()));
        dto.setIdadeMaxima(Integer.parseInt(txtIdadeMax.getText()));
        dto.setTaxaMensal(new BigDecimal(txtTaxa.getText()));
        dto.setComissaoPercentual(new BigDecimal(txtComissao.getText()));
    }

    private void avancarSelecaoTabela() {
        int nextIndex = tableMaster.getSelectionModel().getSelectedIndex() + 1;
        if (nextIndex < loteAtual.size()) {
            log.debug("[IMPORTADOR] avancarSelecaoTabela: Avançando para tabela índice {}", nextIndex);
            tableMaster.getSelectionModel().select(nextIndex);
            tableMaster.scrollTo(nextIndex);
        } else {
            log.debug("[IMPORTADOR] avancarSelecaoTabela: Última tabela do lote, nenhum avanço");
        }
    }

    // 🚀 PATCH: gravarLoteNoBanco
    @FXML
    public void gravarLoteNoBanco() {
        log.info("[IMPORTADOR] gravarLoteNoBanco: Iniciando persistência do lote de {} tabelas", loteAtual.size());
        navigator.mostrarLoading(MSG_LOADING_GRAVACAO);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug(
                            "[IMPORTADOR] gravarLoteNoBanco: Chamando tabelaJurosService.salvarLoteTabelasImportadas");
                    tabelaJurosService.salvarLoteTabelasImportadas(loteAtual);
                    return null;
                },
                sucesso -> {
                    navigator.ocultarLoading();
                    loteAtual.clear();
                    alternarBloqueioFormularioRevisao(true);
                    log.info("[IMPORTADOR] gravarLoteNoBanco: ✅ Lote gravado com sucesso!");
                },
                erro -> finalizarComErro(MSG_ERRO_GRAVACAO + erro.getMessage()));
    }

    // =========================================================================
    // MÉTODOS UTILITÁRIOS E UI DE APOIO
    // =========================================================================
    private void preencherFormularioRevisao(TabelaImportadaDTO dto) {
        log.debug("[IMPORTADOR] preencherFormularioRevisao: Preenchendo campos com dados da tabela '{}'",
                dto.getNomeTabela());
        cmbBanco.getEditor().setText(dto.getBanco());
        cmbConvenio.getEditor().setText(dto.getTipoConvenio());
        txtNomeTabela.setText(dto.getNomeTabela());
        txtValorMin.setText(String.valueOf(dto.getValorMinimo()));
        txtValorMax.setText(String.valueOf(dto.getValorMaximo()));
        txtPrazoMin.setText(String.valueOf(dto.getPrazoMinimo()));
        txtPrazoMax.setText(String.valueOf(dto.getPrazoMaximo()));
        txtIdadeMin.setText(String.valueOf(dto.getIdadeMinima()));
        txtIdadeMax.setText(String.valueOf(dto.getIdadeMaxima()));
        txtTaxa.setText(String.valueOf(dto.getTaxaMensal()));
        txtComissao.setText(String.valueOf(dto.getComissaoPercentual()));
    }

    private void alternarBloqueioFormularioRevisao(boolean bloquear) {
        log.trace("[IMPORTADOR] alternarBloqueioFormularioRevisao: bloquear={}", bloquear);
        cmbBanco.setDisable(bloquear);
        cmbConvenio.setDisable(bloquear);
        txtNomeTabela.setDisable(bloquear);
        txtValorMin.setDisable(bloquear);
        txtValorMax.setDisable(bloquear);
        txtPrazoMin.setDisable(bloquear);
        txtPrazoMax.setDisable(bloquear);
        txtIdadeMin.setDisable(bloquear);
        txtIdadeMax.setDisable(bloquear);
        txtTaxa.setDisable(bloquear);
        txtComissao.setDisable(bloquear);
        btnConfirmarTabela.setDisable(bloquear);
    }

    private File escolherArquivoImagemOuPdf() {
        log.debug("[IMPORTADOR] escolherArquivoImagemOuPdf: Abrindo seletor de arquivo");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Print/Imagem da Tabela");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Imagens/PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File arquivo = fileChooser.showOpenDialog(tableMaster.getScene().getWindow());
        if (arquivo != null) {
            log.debug("[IMPORTADOR] Arquivo selecionado: {}", arquivo.getAbsolutePath());
        }
        return arquivo;
    }

    private void sincronizarEditorCombo(ComboBox<String> comboBox) {
        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> comboBox.getEditor().setText(newVal));
                log.trace("[IMPORTADOR] sincronizarEditorCombo: Editor do combo atualizado para '{}'", newVal);
            }
        });
    }

    private void finalizarComErro(String mensagem) {
        navigator.ocultarLoading();
        log.error(("[IMPORTADOR] " + mensagem));
    }

    /**
     * Motor utilitário para concorrência segura em UI (JavaFX)
     */
    private <T> void executarTaskAsync(Callable<T> acao, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        log.trace("[IMPORTADOR] executarTaskAsync: Iniciando task assíncrona");
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                log.trace("[IMPORTADOR] executarTaskAsync: Executando callable");
                return acao.call();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            log.trace("[IMPORTADOR] executarTaskAsync: Task finalizada com sucesso");
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            log.error("[IMPORTADOR] executarTaskAsync: Task falhou", task.getException());
            if (onError != null)
                onError.accept(task.getException());
        }));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void removerTabelaAtual() {
        if (selecionadaAtual != null) {
            log.info("[IMPORTADOR] removerTabelaAtual: Removendo tabela '{}' do lote",
                    selecionadaAtual.getNomeTabela());
            loteAtual.remove(selecionadaAtual);
            tableMaster.refresh();
        } else {
            log.warn("[IMPORTADOR] removerTabelaAtual: Nenhuma tabela selecionada para remover");
        }
    }

}