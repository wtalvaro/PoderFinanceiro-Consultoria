package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.BancoUIEventHub;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosUIEventHub;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.facade.ITabelaJurosFacade;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.TabelaJurosViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.controlsfx.control.MasterDetailPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * <h1>TabelaJurosController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar as Tabelas de Juros.
 * Implementa o padrão <b>Humble Object</b>, delegando a persistência, filtros e
 * formatações para a {@link ITabelaJurosFacade}.
 * </p>
 */
@Component
public class TabelaJurosController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(TabelaJurosController.class);
    private static final String LOG_PREFIX = "[TabelaJurosController]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ITabelaJurosFacade tabelaFacade;
    private final TabelaJurosViewModel viewModel;
    private final BancoUIEventHub bancoEventHub;
    private final TabelaJurosUIEventHub tabelaEventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private MasterDetailPane paneFormulario;
    @FXML private Label lblFormTitulo;
    @FXML private Button btnToggleForm;
    @FXML private Label lblAviso;

    @FXML private TextField txtNomeTabela;
    @FXML private ComboBox<TipoConvenioModel> comboConvenio;
    @FXML private TextField txtTaxaMensal;
    @FXML private TextField txtComissao;
    @FXML private TextField txtValorMinimo;
    @FXML private TextField txtValorMaximo;
    @FXML private TextField txtPrazoMinimo;
    @FXML private TextField txtPrazoMaximo;
    @FXML private TextField txtIdadeMinima;
    @FXML private TextField txtIdadeMaxima;
    @FXML private TextField txtRendaMinima;

    @FXML private TextField txtBusca;
    @FXML private TableView<TabelaJurosModel> tableTabelas;
    @FXML private TableColumn<TabelaJurosModel, String> colConvenio, colNome, colTaxa, colComissao, colLimites, colRenda, colIdade,
            colPrazo;
    @FXML private TableColumn<TabelaJurosModel, Void> colAcoes;
    @FXML private ComboBox<BancoModel> comboBanco;
    @FXML private VBox overlayArquivar;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<TabelaJurosModel> listaTabelas = FXCollections.observableArrayList();
    private TabelaJurosModel tabelaSelecionadaParaArquivar;

    public TabelaJurosController(ITabelaJurosFacade tabelaFacade, TabelaJurosViewModel viewModel, BancoUIEventHub bancoEventHub,
            TabelaJurosUIEventHub tabelaEventHub) {
        this.tabelaFacade = tabelaFacade;
        this.viewModel = viewModel;
        this.bancoEventHub = bancoEventHub;
        this.tabelaEventHub = tabelaEventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring (Injeção por Construtor).", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Tabelas de Juros...", LOG_PREFIX);
        configurarFormulario();
        configurarColunasTabela();
        carregarDados();
        configurarFiltroReativo();

        bancoEventHub.inscrever(this::recarregarBancos);
        tabelaEventHub.inscrever(this::carregarDados);

        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d tabela(s)", Bindings.size(listaTabelas)));
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo dos hubs de eventos.", LOG_PREFIX);
        tabelaEventHub.desinscrever(this::carregarDados);
        bancoEventHub.desinscrever(this::recarregarBancos);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    private void carregarDados() {
        log.trace("{} [TELEMETRIA] Recarregando lista de tabelas.", LOG_PREFIX);
        filtrarTabelas(txtBusca.getText());
    }

    private void recarregarBancos() {
        log.trace("{} [TELEMETRIA] Atualizando combo de bancos.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(tabelaFacade::listarBancosAtivos,
                bancos -> comboBanco.setItems(FXCollections.observableArrayList(bancos)),
                erro -> log.error("{} [SISTEMA] Erro ao carregar bancos: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void configurarFiltroReativo() {
        log.trace("{} [UI] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("{} [UI] Termo de busca alterado: '{}'", LOG_PREFIX, newVal);
            filtrarTabelas(newVal);
        });
    }

    private void filtrarTabelas(String termo) {
        AsyncUtils.executarTaskAsync(() -> tabelaFacade.filtrarTabelas(termo), tabelasFiltradas -> {
            listaTabelas.setAll(tabelasFiltradas);
            tableTabelas.setItems(listaTabelas);
            log.info("{} [TELEMETRIA] Tabela atualizada. {} registro(s) exibido(s).", LOG_PREFIX, tabelasFiltradas.size());
        }, erro -> log.error("{} [SISTEMA] Erro ao filtrar tabelas: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: CONFIGURAÇÕES DE UI E BINDINGS
    // ==========================================================================================
    private void configurarFormulario() {
        log.trace("{} [UI] Configurando combos, bindings e formatadores.", LOG_PREFIX);
        recarregarBancos();

        comboBanco.setConverter(new StringConverter<BancoModel>() {
            @Override public String toString(BancoModel b) {
                return b != null ? b.getNome() : "";
            }

            @Override public BancoModel fromString(String s) {
                return null;
            }
        });

        comboBanco.valueProperty().bindBidirectional(viewModel.getBanco());
        comboConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        comboConvenio.valueProperty().bindBidirectional(viewModel.getTipoConvenio());
        txtNomeTabela.textProperty().bindBidirectional(viewModel.getNomeTabela());

        vincularMascaraMoeda(txtTaxaMensal, viewModel.getTaxaMensal());
        vincularMascaraMoeda(txtComissao, viewModel.getComissaoPercentual());
        vincularMascaraMoeda(txtValorMinimo, viewModel.getValorMinimoEmprestimo());
        vincularMascaraMoeda(txtValorMaximo, viewModel.getValorMaximoEmprestimo());
        vincularMascaraMoeda(txtRendaMinima, viewModel.getRendaMinima());

        vincularMascaraInteiro(txtPrazoMinimo, viewModel.getPrazoMinimo());
        vincularMascaraInteiro(txtPrazoMaximo, viewModel.getPrazoMaximo());
        vincularMascaraInteiro(txtIdadeMinima, viewModel.getIdadeMinima());
        vincularMascaraInteiro(txtIdadeMaxima, viewModel.getIdadeMaxima());
    }

    private void configurarColunasTabela() {
        log.trace("{} [UI] Configurando colunas da tabela.", LOG_PREFIX);
        colConvenio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipoConvenio().name()));
        colNome.setCellValueFactory(cell -> {
            String bancoNome = cell.getValue().getBanco() != null ? cell.getValue().getBanco().getNome() : "S/B";
            return new SimpleStringProperty("[" + bancoNome + "] " + cell.getValue().getNomeTabela());
        });

        colTaxa.setCellValueFactory(
                cell -> new SimpleStringProperty(FinanceiroUtils.formatarParaExibicao(cell.getValue().getTaxaMensal()) + "%"));
        colComissao.setCellValueFactory(
                cell -> new SimpleStringProperty(FinanceiroUtils.formatarParaExibicao(cell.getValue().getComissaoPercentual()) + "%"));

        colLimites.setCellValueFactory(cell -> {
            String min = cell.getValue().getValorMinimoEmprestimo() != null
                    ? "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorMinimoEmprestimo())
                    : "R$ 0,00";
            String max = cell.getValue().getValorMaximoEmprestimo() != null
                    && cell.getValue().getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) > 0
                            ? "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorMaximoEmprestimo())
                            : "Sem teto";
            return new SimpleStringProperty(min + " - " + max);
        });

        colRenda.setCellValueFactory(cell -> {
            BigDecimal min = cell.getValue().getRendaMinima();
            return new SimpleStringProperty(
                    (min != null && min.compareTo(BigDecimal.ZERO) > 0) ? "A partir de R$ " + FinanceiroUtils.formatarParaExibicao(min)
                            : "Isento");
        });

        colIdade.setCellValueFactory(cell -> {
            Integer min = cell.getValue().getIdadeMinima() != null ? cell.getValue().getIdadeMinima() : 18;
            Integer max = cell.getValue().getIdadeMaxima() != null ? cell.getValue().getIdadeMaxima() : 100;
            return new SimpleStringProperty(min + " a " + max + " anos");
        });

        colPrazo.setCellValueFactory(cell -> {
            Integer min = cell.getValue().getPrazoMinimo() != null ? cell.getValue().getPrazoMinimo() : 1;
            Integer max = cell.getValue().getPrazoMaximo() != null ? cell.getValue().getPrazoMaximo() : 96;
            return new SimpleStringProperty(min + " a " + max + "x");
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnArquivar = new Button("📦");
            private final HBox pane = new HBox(5, btnEditar, btnArquivar);
            {
                btnEditar.getStyleClass().add("flat");
                btnEditar.setOnAction(event -> editarTabela(getTableView().getItems().get(getIndex())));

                btnArquivar.getStyleClass().add("flat");
                btnArquivar.setStyle("-fx-text-fill: #f57c00;");
                btnArquivar.setOnAction(event -> abrirConfirmacaoArquivamento(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 8: AÇÕES DE NEGÓCIO (CRUD)
    // ==========================================================================================
    @FXML private void handleSalvar() {
        log.info("{} [TELEMETRIA] Tentativa de salvar tabela.", LOG_PREFIX);
        if (!viewModel.isDirty()) {
            log.warn("{} [NEGOCIO] Salvamento bloqueado: Nenhuma alteração detectada.", LOG_PREFIX);
            mostrarMensagem("Nenhuma alteração detectada para salvar.", false);
            return;
        }

        TabelaJurosModel tabelaAtualizada = viewModel.atualizarModel(new TabelaJurosModel());

        AsyncUtils.executarTaskAsync(() -> tabelaFacade.salvarTabela(tabelaAtualizada), salva -> {
            log.info("{} [AUDITORIA] Tabela salva com sucesso. ID: {}", LOG_PREFIX, salva.getId());
            mostrarMensagem("Tabela atualizada com sucesso! A vigência antiga foi arquivada.", true);
            limparFormulario();
            carregarDados();
        }, erro -> {
            log.error("{} [AUDITORIA] Falha ao persistir tabela: {}", LOG_PREFIX, erro.getMessage());
            mostrarMensagem("Erro ao salvar: " + erro.getMessage(), false);
        });
    }

    @FXML private void confirmarArquivamento() {
        if (tabelaSelecionadaParaArquivar != null) {
            Long idTabela = tabelaSelecionadaParaArquivar.getId();
            log.info("{} [TELEMETRIA] Confirmando arquivamento da tabela ID: {}", LOG_PREFIX, idTabela);

            AsyncUtils.executarTaskAsync(() -> {
                tabelaFacade.arquivarTabela(tabelaSelecionadaParaArquivar);
                return null;
            }, sucesso -> {
                log.info("{} [AUDITORIA] Tabela ID {} arquivada com sucesso.", LOG_PREFIX, idTabela);
                mostrarMensagem("Tabela arquivada! Ela não aparecerá mais para novas propostas.", true);
                carregarDados();
            }, erro -> {
                log.error("{} [AUDITORIA] Falha ao arquivar tabela ID {}: {}", LOG_PREFIX, idTabela, erro.getMessage());
                mostrarMensagem("Erro ao arquivar: " + erro.getMessage(), false);
            });
        }
        cancelarArquivamento();
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO MODAL (FORMULÁRIO)
    // ==========================================================================================
    @FXML private void handleToggleForm() {
        boolean mostrandoForm = paneFormulario.isShowDetailNode();
        log.trace("{} [UI] Alternando formulário. Mostrando antes? {}", LOG_PREFIX, mostrandoForm);
        paneFormulario.setShowDetailNode(!mostrandoForm);
        btnToggleForm.setText(!mostrandoForm ? "➖ Fechar Formulário" : "➕ Prescrever Nova Tabela");
    }

    @FXML private void limparFormulario() {
        log.trace("{} [UI] Resetando formulário.", LOG_PREFIX);
        viewModel.reset();
        paneFormulario.setShowDetailNode(false);
        lblFormTitulo.setText("💊 Prescrever Nova Tabela (Cadastrar / Atualizar)");
        btnToggleForm.setText("➕ Prescrever Nova Tabela");
    }

    private void editarTabela(TabelaJurosModel tabela) {
        log.info("{} [UI] Editando tabela ID={}, nome='{}'", LOG_PREFIX, tabela.getId(), tabela.getNomeTabela());
        viewModel.loadFromModel(tabela);
        paneFormulario.setShowDetailNode(true);
        lblFormTitulo.setText("🔄 Editando Tabela: " + tabela.getNomeTabela() + " (Isso gerará uma nova versão)");
        btnToggleForm.setText("➖ Fechar Formulário");
        txtNomeTabela.requestFocus();
    }

    private void abrirConfirmacaoArquivamento(TabelaJurosModel tabela) {
        log.trace("{} [UI] Abrindo overlay de arquivamento para tabela ID={}", LOG_PREFIX, tabela.getId());
        this.tabelaSelecionadaParaArquivar = tabela;
        overlayArquivar.setVisible(true);
    }

    @FXML private void cancelarArquivamento() {
        log.trace("{} [UI] Arquivamento cancelado.", LOG_PREFIX);
        this.tabelaSelecionadaParaArquivar = null;
        overlayArquivar.setVisible(false);
    }

    @FXML private void prepararNovaVersao() {
        if (tabelaSelecionadaParaArquivar != null) {
            log.info("{} [UI] Preparando nova versão a partir da tabela ID={}", LOG_PREFIX, tabelaSelecionadaParaArquivar.getId());
            editarTabela(tabelaSelecionadaParaArquivar);
        }
        cancelarArquivamento();
        txtNomeTabela.requestFocus();
    }

    private void mostrarMensagem(String texto, boolean sucesso) {
        log.trace("{} [UI] Exibindo aviso no formulário: '{}'", LOG_PREFIX, texto);
        lblAviso.setText(texto);
        lblAviso.setStyle(sucesso ? "-fx-text-fill: green; -fx-font-weight: bold;" : "-fx-text-fill: red; -fx-font-weight: bold;");
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }

    // ==========================================================================================
    // MÓDULO 10: UTILITÁRIOS
    // ==========================================================================================
    private void vincularMascaraMoeda(TextField campo, javafx.beans.property.Property<BigDecimal> prop) {
        TextFormatter<BigDecimal> fmt = FinanceiroUtils.criarFormatadorMoeda();
        campo.setTextFormatter(fmt);
        fmt.valueProperty().bindBidirectional(prop);
    }

    private void vincularMascaraInteiro(TextField campo, javafx.beans.property.Property<Integer> prop) {
        TextFormatter<Integer> fmt = new TextFormatter<>(new StringConverter<Integer>() {
            @Override public String toString(Integer object) {
                return object != null ? object.toString() : "";
            }

            @Override public Integer fromString(String string) {
                if (string == null || string.trim().isEmpty())
                    return null;
                try {
                    return Integer.parseInt(string.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }, null, change -> change.getControlNewText().matches("\\d*") ? change : null);

        campo.setTextFormatter(fmt);
        fmt.valueProperty().bindBidirectional(prop);
    }
}
