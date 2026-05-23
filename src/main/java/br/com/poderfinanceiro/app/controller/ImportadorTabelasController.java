package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;

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
    private final MainController mainController;
    private final BancoRepository bancoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private final ObservableList<TabelaImportadaDTO> loteAtual = FXCollections.observableArrayList();
    private TabelaImportadaDTO selecionadaAtual;

    public ImportadorTabelasController(GeminiService geminiService, TabelaJurosService tabelaJurosService,
            AuthService authService, MainController mainController, BancoRepository bancoRepository) {
        this.geminiService = geminiService;
        this.tabelaJurosService = tabelaJurosService;
        this.authService = authService;
        this.mainController = mainController;
        this.bancoRepository = bancoRepository;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        carregarConvenios();
        carregarBancos();
        carregarModelosIA();

        configurarTabelaMaster();
        configurarListenersDeEstado();
        alternarBloqueioFormularioRevisao(true);
    }

    private void carregarConvenios() {
        List<String> convenios = Arrays.stream(TipoConvenioModel.values()).map(Enum::name).toList();
        cmbConvenio.getItems().setAll(convenios);
        if (cmbConvenioLote != null)
            cmbConvenioLote.getItems().setAll(convenios);
        sincronizarEditorCombo(cmbConvenio);
    }

    private void carregarBancos() {
        executarTaskAsync(
                () -> bancoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(BancoModel::getNome).toList(),
                bancos -> {
                    cmbBanco.getItems().setAll(bancos);
                    if (cmbBancoLote != null)
                        cmbBancoLote.getItems().setAll(bancos);
                }, null);
        sincronizarEditorCombo(cmbBanco);
    }

    private void carregarModelosIA() {
        if (authService.estaLogado()) {
            executarTaskAsync(
                    () -> geminiService.listarModelosMultimodais(authService.getUsuarioLogado().getGeminiApiKey()),
                    modelos -> {
                        cmbModeloIA.getItems().setAll(modelos);
                        cmbModeloIA.getSelectionModel().selectFirst();
                    }, null);
        }
    }

    private void configurarTabelaMaster() {
        tableMaster.setItems(loteAtual);
        colStatus.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().isRevisado() ? STATUS_REVISADO : STATUS_PENDENTE));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco()));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeTabela()));

        tableMaster.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selecionadaAtual = newVal;
            if (newVal != null) {
                preencherFormularioRevisao(newVal);
                alternarBloqueioFormularioRevisao(false);
            } else {
                alternarBloqueioFormularioRevisao(true);
            }
        });
    }

    private void configurarListenersDeEstado() {
        btnGravarLote.setDisable(true);
        if (btnAplicarLote != null)
            btnAplicarLote.setDisable(true);

        loteAtual.addListener((ListChangeListener.Change<? extends TabelaImportadaDTO> c) -> {
            validarBotoesDeAcao();
            if (lblTotalTabelas != null) {
                lblTotalTabelas.setText(String.valueOf(loteAtual.size()));
            }
        });
    }

    private void validarBotoesDeAcao() {
        boolean temItens = !loteAtual.isEmpty();
        boolean todosRevisados = temItens && loteAtual.stream().allMatch(TabelaImportadaDTO::isRevisado);

        btnGravarLote.setDisable(!todosRevisados);
        if (btnAplicarLote != null)
            btnAplicarLote.setDisable(!temItens);
    }

    // =========================================================================
    // AÇÕES GLOBAIS E LOTE (DRY APLICADO)
    // =========================================================================
    @FXML
    public void handleAplicarLote() {
        if (loteAtual.isEmpty())
            return;

        if (isSelecaoGlobalVazia()) {
            System.err.println("Selecione pelo menos um Banco ou Convênio para aplicar em lote.");
            return;
        }

        aplicarPropriedadesGlobaisAoLote(loteAtual);
        tableMaster.refresh();

        if (selecionadaAtual != null) {
            preencherFormularioRevisao(selecionadaAtual);
        }
        System.out.println("✅ Aplicação em lote concluída! Revisão individual necessária.");
    }

    private boolean isSelecaoGlobalVazia() {
        String banco = cmbBancoLote != null ? cmbBancoLote.getValue() : null;
        String convenio = cmbConvenioLote != null ? cmbConvenioLote.getValue() : null;
        return (banco == null || banco.isBlank()) && (convenio == null || convenio.isBlank());
    }

    private void aplicarPropriedadesGlobaisAoLote(Iterable<TabelaImportadaDTO> lote) {
        String bancoGlobal = cmbBancoLote != null ? cmbBancoLote.getValue() : null;
        String convenioGlobal = cmbConvenioLote != null ? cmbConvenioLote.getValue() : null;

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
    @FXML
    public void processarImagemIA() {
        File arquivo = escolherArquivoImagemOuPdf();
        if (arquivo == null)
            return;

        mainController.mostrarLoading(MSG_LOADING_IA);
        String token = authService.getUsuarioLogado().getGeminiApiKey();
        String modelo = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : MODELO_IA_PADRAO;

        executarTaskAsync(
                () -> processarChamadaIA(arquivo, token, modelo),
                this::finalizarImportacaoComSucesso,
                erro -> finalizarComErro(MSG_ERRO_IA + erro.getMessage()));
    }

    private List<TabelaImportadaDTO> processarChamadaIA(File arquivo, String token, String modelo) throws Exception {
        String jsonResposta = geminiService.extrairTabelasEmLote(arquivo, token, modelo);
        return objectMapper.readValue(jsonResposta, new TypeReference<>() {
        });
    }

    private void finalizarImportacaoComSucesso(List<TabelaImportadaDTO> tabelas) {
        mainController.ocultarLoading();
        aplicarPropriedadesGlobaisAoLote(tabelas); // Reaproveitamento da função de lote

        loteAtual.setAll(tabelas);
        if (!loteAtual.isEmpty())
            tableMaster.getSelectionModel().selectFirst();
    }

    // =========================================================================
    // REVISÃO INDIVIDUAL E SALVAMENTO
    // =========================================================================
    @FXML
    public void confirmarTabelaAtual() {
        if (selecionadaAtual == null)
            return;

        try {
            mapearCamposParaDTO(selecionadaAtual);
            selecionadaAtual.setRevisado(true);

            tableMaster.refresh();
            validarBotoesDeAcao();
            avancarSelecaoTabela();
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Verifique os campos numéricos. " + e.getMessage());
        }
    }

    private void mapearCamposParaDTO(TabelaImportadaDTO dto) {
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
            tableMaster.getSelectionModel().select(nextIndex);
            tableMaster.scrollTo(nextIndex); // 🚀 Força a tabela a rolar acompanhando a seleção
        }
    }

    @FXML
    public void gravarLoteNoBanco() {
        mainController.mostrarLoading(MSG_LOADING_GRAVACAO);

        executarTaskAsync(
                () -> {
                    tabelaJurosService.salvarLoteTabelasImportadas(loteAtual);
                    return null;
                },
                sucesso -> {
                    mainController.ocultarLoading();
                    loteAtual.clear();
                    alternarBloqueioFormularioRevisao(true);
                    System.out.println("✅ Lote gravado com sucesso!");
                },
                erro -> finalizarComErro(MSG_ERRO_GRAVACAO + erro.getMessage()));
    }

    // =========================================================================
    // MÉTODOS UTILITÁRIOS E UI DE APOIO
    // =========================================================================
    private void preencherFormularioRevisao(TabelaImportadaDTO dto) {
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Print/Imagem da Tabela");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Imagens/PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        return fileChooser.showOpenDialog(tableMaster.getScene().getWindow());
    }

    private void sincronizarEditorCombo(ComboBox<String> comboBox) {
        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> comboBox.getEditor().setText(newVal));
            }
        });
    }

    private void finalizarComErro(String mensagem) {
        mainController.ocultarLoading();
        System.err.println("❌ " + mensagem);
    }

    /**
     * Motor utilitário para concorrência segura em UI (JavaFX)
     */
    private <T> void executarTaskAsync(Callable<T> acao, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return acao.call();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (onError != null)
                onError.accept(task.getException());
        }));

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Garante que a JVM possa encerrar mesmo com a task rodando
        thread.start();
    }

    @FXML
    public void removerTabelaAtual() {
        if (selecionadaAtual != null) {
            loteAtual.remove(selecionadaAtual);
            tableMaster.refresh();
        }
    }
    
}