package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.repository.BancoRepository;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.GeminiService;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
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
    // COMPONENTES DE UI GLOBAIS (LOTE & IA)
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
    // COMPONENTES DE UI (FORMULÁRIO DE REVISÃO INDIVIDUAL)
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

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final GeminiService geminiService;
    private final TabelaJurosService tabelaJurosService;
    private final AuthService authService;
    private final MainController mainController;
    private final BancoRepository bancoRepository;

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
        carregarDependenciasBasicas();
        configurarTabelaMaster();
        configurarListenersDeEstado();
        alternarFormularioIndividual(true);
    }

    private void carregarDependenciasBasicas() {
        // Carrega Convênios (Síncrono pois é Enum local)
        List<String> convenios = Arrays.stream(TipoConvenioModel.values()).map(Enum::name).toList();
        cmbConvenio.getItems().setAll(convenios);
        if (cmbConvenioLote != null)
            cmbConvenioLote.getItems().setAll(convenios);

        // Carrega Bancos (Assíncrono para não travar UI)
        executarTaskAsync(
                () -> bancoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(BancoModel::getNome).toList(),
                bancos -> {
                    cmbBanco.getItems().setAll(bancos);
                    if (cmbBancoLote != null)
                        cmbBancoLote.getItems().setAll(bancos);
                },
                null);

        // Carrega Modelos de IA
        if (authService.estaLogado()) {
            executarTaskAsync(
                    () -> geminiService.listarModelosMultimodais(authService.getUsuarioLogado().getGeminiApiKey()),
                    modelos -> {
                        cmbModeloIA.getItems().setAll(modelos);
                        cmbModeloIA.getSelectionModel().selectFirst();
                    },
                    null);
        }

        sincronizarEditorCombo(cmbBanco);
        sincronizarEditorCombo(cmbConvenio);
    }

    private void configurarTabelaMaster() {
        tableMaster.setItems(loteAtual);
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isRevisado() ? "🟢" : "🟡"));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco()));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeTabela()));

        tableMaster.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selecionadaAtual = newVal;
            if (newVal != null) {
                preencherFormularioRevisao(newVal);
                alternarFormularioIndividual(false);
            } else {
                alternarFormularioIndividual(true);
            }
        });
    }

    private void configurarListenersDeEstado() {
        btnGravarLote.setDisable(true);
        if (btnAplicarLote != null)
            btnAplicarLote.setDisable(true);

        // O listener agora apenas delega para o método principal
        loteAtual.addListener((ListChangeListener.Change<? extends TabelaImportadaDTO> c) -> {
            validarBotoesDeAcao();
        });
    }

    // 🚀 NOVO MÉTODO PARA FORÇAR A VALIDAÇÃO DO ESTADO
    private void validarBotoesDeAcao() {
        boolean temItens = !loteAtual.isEmpty();
        boolean todosRevisados = temItens && loteAtual.stream().allMatch(TabelaImportadaDTO::isRevisado);

        btnGravarLote.setDisable(!todosRevisados);
        if (btnAplicarLote != null)
            btnAplicarLote.setDisable(!temItens);
    }

    // =========================================================================
    // AÇÕES EM LOTE (NOVIDADE)
    // =========================================================================
    @FXML
    public void handleAplicarLote() {
        if (loteAtual.isEmpty())
            return;

        String bancoGlobal = cmbBancoLote.getValue();
        String convenioGlobal = cmbConvenioLote.getValue();

        if (bancoGlobal == null && convenioGlobal == null) {
            System.err.println("Selecione pelo menos um Banco ou Convênio para aplicar em lote.");
            return;
        }

        loteAtual.forEach(tabela -> {
            if (bancoGlobal != null && !bancoGlobal.isBlank())
                tabela.setBanco(bancoGlobal);
            if (convenioGlobal != null && !convenioGlobal.isBlank())
                tabela.setTipoConvenio(convenioGlobal);
        });

        tableMaster.refresh();

        // Se houver uma tabela aberta no formulário de revisão, atualiza ela
        // visualmente
        if (selecionadaAtual != null) {
            preencherFormularioRevisao(selecionadaAtual);
        }

        System.out.println("✅ Aplicação em lote concluída! Revisão individual necessária.");
    }

    // =========================================================================
    // MOTOR DE IA (GEMINI)
    // =========================================================================
    @FXML
    public void processarImagemIA() {
        File arquivo = escolherArquivo();
        if (arquivo == null)
            return;

        mainController.mostrarLoading("Analisando layout e extraindo regras de negócio...");

        String token = authService.getUsuarioLogado().getGeminiApiKey();
        String modelo = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : "gemini-3.5-flash";

        executarTaskAsync(
                () -> processarChamadaIA(arquivo, token, modelo),
                tabelas -> finalizarImportacaoComSucesso(tabelas),
                erro -> finalizarComErro("Falha na extração da IA: " + erro.getMessage()));
    }

    private List<TabelaImportadaDTO> processarChamadaIA(File arquivo, String token, String modelo) throws Exception {
        String jsonResposta = geminiService.extrairTabelasEmLote(arquivo, token, modelo);
        return new ObjectMapper().readValue(jsonResposta, new TypeReference<List<TabelaImportadaDTO>>() {
        });
    }

    private void finalizarImportacaoComSucesso(List<TabelaImportadaDTO> tabelas) {
        mainController.ocultarLoading();

        // 🚀 AUTO-APLICAR LOTE SE JÁ ESTIVER SELECIONADO ANTES DO PROCESSAMENTO
        String bancoGlobal = cmbBancoLote != null ? cmbBancoLote.getValue() : null;
        String convenioGlobal = cmbConvenioLote != null ? cmbConvenioLote.getValue() : null;

        tabelas.forEach(t -> {
            if (bancoGlobal != null && !bancoGlobal.isBlank())
                t.setBanco(bancoGlobal);
            if (convenioGlobal != null && !convenioGlobal.isBlank())
                t.setTipoConvenio(convenioGlobal);
        });

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
            selecionadaAtual.setBanco(cmbBanco.getEditor().getText());
            selecionadaAtual.setTipoConvenio(cmbConvenio.getEditor().getText());
            selecionadaAtual.setNomeTabela(txtNomeTabela.getText());
            selecionadaAtual.setValorMinimo(new BigDecimal(txtValorMin.getText()));
            selecionadaAtual.setValorMaximo(new BigDecimal(txtValorMax.getText()));
            selecionadaAtual.setPrazoMinimo(Integer.parseInt(txtPrazoMin.getText()));
            selecionadaAtual.setPrazoMaximo(Integer.parseInt(txtPrazoMax.getText()));
            selecionadaAtual.setIdadeMinima(Integer.parseInt(txtIdadeMin.getText()));
            selecionadaAtual.setIdadeMaxima(Integer.parseInt(txtIdadeMax.getText()));
            selecionadaAtual.setTaxaMensal(new BigDecimal(txtTaxa.getText()));
            selecionadaAtual.setComissaoPercentual(new BigDecimal(txtComissao.getText()));

            selecionadaAtual.setRevisado(true);
            tableMaster.refresh();

            // 🚀 CORREÇÃO AQUI: Valida o estado dos botões a cada confirmação
            validarBotoesDeAcao();

            // Avanço automático produtivo
            int nextIndex = tableMaster.getSelectionModel().getSelectedIndex() + 1;
            if (nextIndex < loteAtual.size()) {
                tableMaster.getSelectionModel().select(nextIndex);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Verifique os campos numéricos. " + e.getMessage());
        }
    }

    @FXML
    public void gravarLoteNoBanco() {
        mainController.mostrarLoading("Persistindo lote e ativando novas vigências...");

        executarTaskAsync(
                () -> {
                    tabelaJurosService.salvarLoteTabelasImportadas(loteAtual);
                    return null;
                },
                sucesso -> {
                    mainController.ocultarLoading();
                    loteAtual.clear();
                    alternarFormularioIndividual(true);
                    System.out.println("✅ Lote gravado com sucesso!");
                },
                erro -> finalizarComErro("Erro ao gravar lote: " + erro.getMessage()));
    }

    // =========================================================================
    // MÉTODOS UTILITÁRIOS E UI
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

    private void alternarFormularioIndividual(boolean disable) {
        cmbBanco.setDisable(disable);
        cmbConvenio.setDisable(disable);
        txtNomeTabela.setDisable(disable);
        txtValorMin.setDisable(disable);
        txtValorMax.setDisable(disable);
        txtPrazoMin.setDisable(disable);
        txtPrazoMax.setDisable(disable);
        txtIdadeMin.setDisable(disable);
        txtIdadeMax.setDisable(disable);
        txtTaxa.setDisable(disable);
        txtComissao.setDisable(disable);
        btnConfirmarTabela.setDisable(disable);
    }

    private File escolherArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Print/Imagem da Tabela");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Imagens/PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        return fileChooser.showOpenDialog(tableMaster.getScene().getWindow());
    }

    private void sincronizarEditorCombo(ComboBox<String> comboBox) {
        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                Platform.runLater(() -> comboBox.getEditor().setText(newVal));
        });
    }

    private void finalizarComErro(String mensagem) {
        mainController.ocultarLoading();
        System.err.println("❌ " + mensagem);
    }

    /**
     * Motor utilitário DRY para concorrência
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
        new Thread(task).start();
    }
}