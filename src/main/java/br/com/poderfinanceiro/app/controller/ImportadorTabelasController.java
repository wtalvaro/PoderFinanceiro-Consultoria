package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.repository.BancoRepository;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.GeminiService;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

@Component
@Scope("prototype") // Segue a sua regra de arquitetura
public class ImportadorTabelasController {

    @FXML
    private ComboBox<String> cmbModeloIA;
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
    private Button btnConfirmarTabela, btnGravarLote, btnProcessarImagem;

    private final GeminiService geminiService;
    private final TabelaJurosService tabelaJurosService;
    private final AuthService authService;
    private final MainController mainController; // Para gerenciar loading
    private final BancoRepository bancoRepository;

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

    @FXML
    public void initialize() {
        carregarBancosOficiais();
        carregarTiposConvenio();

        // 🚀 PATCH PARA SINCRONIZAR CLICK COM TEXTO NO EDITOR
        sincronizarEditor(cmbBanco);
        sincronizarEditor(cmbConvenio);
        
        configurarTabela();
        configurarListeners();
        carregarModelosIA();
        btnGravarLote.setDisable(true);
        alternarFormulario(true);
    }

    /**
     * Força o editor de texto a atualizar instantaneamente quando um item é clicado
     * na lista.
     */
    private void sincronizarEditor(ComboBox<String> comboBox) {
        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // O Platform.runLater garante que o texto seja setado após o evento de clique
                // terminar
                Platform.runLater(() -> comboBox.getEditor().setText(newVal));
            }
        });
    }

    private void carregarBancosOficiais() {
        Task<List<String>> taskBancos = new Task<>() {
            @Override
            protected List<String> call() {
                return bancoRepository.findByAtivoTrueOrderByNomeAsc()
                        .stream()
                        .map(br.com.poderfinanceiro.app.model.BancoModel::getNome)
                        .toList();
            }
        };
        taskBancos.setOnSucceeded(e -> cmbBanco.getItems().setAll(taskBancos.getValue()));
        new Thread(taskBancos).start();
    }

    private void carregarTiposConvenio() {
        // Pega todos os Enums e converte para String
        List<String> convenios = java.util.Arrays
                .stream(br.com.poderfinanceiro.app.model.enums.TipoConvenioModel.values())
                .map(Enum::name)
                .toList();
        cmbConvenio.getItems().setAll(convenios);
    }

    private void carregarModelosIA() {
        if (authService.estaLogado()) {
            Task<List<String>> taskModelos = new Task<>() {
                @Override
                protected List<String> call() {
                    return geminiService.listarModelosMultimodais(authService.getUsuarioLogado().getGeminiApiKey());
                }
            };
            taskModelos.setOnSucceeded(e -> {
                cmbModeloIA.getItems().setAll(taskModelos.getValue());
                cmbModeloIA.getSelectionModel().selectFirst();
            });
            new Thread(taskModelos).start();
        }
    }

    private void configurarTabela() {
        tableMaster.setItems(loteAtual);

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isRevisado() ? "🟢" : "🟡"));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco()));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeTabela()));

        tableMaster.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selecionadaAtual = newVal;
                preencherFormulario(newVal);
                alternarFormulario(false);
            } else {
                alternarFormulario(true);
            }
        });
    }

    private void configurarListeners() {
        loteAtual.addListener((javafx.collections.ListChangeListener.Change<? extends TabelaImportadaDTO> c) -> {
            validarBotaoGravacao();
        });
    }

    private void validarBotaoGravacao() {
        boolean todosRevisados = !loteAtual.isEmpty() && loteAtual.stream().allMatch(TabelaImportadaDTO::isRevisado);
        btnGravarLote.setDisable(!todosRevisados);
    }

    private void alternarFormulario(boolean disable) {
        cmbBanco.setDisable(disable);
        txtNomeTabela.setDisable(disable);
        cmbConvenio.setDisable(disable);
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

    @FXML
    public void processarImagemIA() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o Print/Imagem da Tabela");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Imagens/PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File arquivo = fileChooser.showOpenDialog(tableMaster.getScene().getWindow());

        if (arquivo == null)
            return;

        mainController.mostrarLoading("Analisando layout e extraindo regras...");

        String token = authService.getUsuarioLogado().getGeminiApiKey();
        String modelo = cmbModeloIA.getValue() != null ? cmbModeloIA.getValue() : "gemini-3.5-flash";

        Task<List<TabelaImportadaDTO>> task = new Task<>() {
            @Override
            protected List<TabelaImportadaDTO> call() throws Exception {
                String jsonResposta = geminiService.extrairTabelasEmLote(arquivo, token, modelo);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonResposta, new TypeReference<List<TabelaImportadaDTO>>() {
                });
            }
        };

        task.setOnSucceeded(e -> {
            mainController.ocultarLoading();
            loteAtual.setAll(task.getValue());
            if (!loteAtual.isEmpty())
                tableMaster.getSelectionModel().selectFirst();
        });

        task.setOnFailed(e -> {
            mainController.ocultarLoading();
            System.err.println("Erro na IA: " + task.getException().getMessage());
            // Sugestão: Mostrar Alert de erro do JavaFX aqui
        });

        new Thread(task).start();
    }

    private void preencherFormulario(TabelaImportadaDTO dto) {
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
            tableMaster.refresh(); // Atualiza a bolinha para verde 🟢
            validarBotaoGravacao();

            // Desce automaticamente para o próximo
            int nextIndex = tableMaster.getSelectionModel().getSelectedIndex() + 1;
            if (nextIndex < loteAtual.size()) {
                tableMaster.getSelectionModel().select(nextIndex);
            }
        } catch (Exception e) {
            System.err.println("Por favor, verifique os campos numéricos. " + e.getMessage());
        }
    }

    @FXML
    public void gravarLoteNoBanco() {
        mainController.mostrarLoading("Salvando lote e ativando tabelas...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                tabelaJurosService.salvarLoteTabelasImportadas(loteAtual);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            mainController.ocultarLoading();
            loteAtual.clear();
            alternarFormulario(true);
            System.out.println("Lote gravado com sucesso!");
        });

        task.setOnFailed(e -> {
            mainController.ocultarLoading();
            System.err.println("Erro ao gravar no banco: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }
}