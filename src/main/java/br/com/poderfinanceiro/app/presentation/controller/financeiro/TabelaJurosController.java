package br.com.poderfinanceiro.app.presentation.controller.financeiro;

import br.com.poderfinanceiro.app.application.facade.ITabelaJurosFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.domain.event.BancoUIEventHub;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosUIEventHub;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.presentation.viewmodel.TabelaJurosViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.controlsfx.control.MasterDetailPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * <h1>TabelaJurosController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar as Tabelas de Juros.
 * Implementa o padrão <b>Humble Object</b>, utilizando utilitários Gold Standard
 * para garantir integridade de dados e fluidez na UI Thread via Virtual Threads.
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
    private final Navigator navigator;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private MasterDetailPane paneFormulario;
    @FXML private Label lblFormTitulo;
    @FXML private Button btnToggleForm;

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
    @FXML private TableColumn<TabelaJurosModel, String> colConvenio, colNome, colTaxa, colComissao, colLimites,
            colRenda, colIdade, colPrazo;
    @FXML private TableColumn<TabelaJurosModel, Void> colAcoes;
    @FXML private ComboBox<BancoModel> comboBanco;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<TabelaJurosModel> listaTabelas = FXCollections.observableArrayList();
    private TabelaJurosModel tabelaSelecionadaParaArquivar;

    public TabelaJurosController(ITabelaJurosFacade tabelaFacade, TabelaJurosViewModel viewModel,
            BancoUIEventHub bancoEventHub, TabelaJurosUIEventHub tabelaEventHub, Navigator navigator) {
        this.tabelaFacade = tabelaFacade;
        this.viewModel = viewModel;
        this.bancoEventHub = bancoEventHub;
        this.tabelaEventHub = tabelaEventHub;
        this.navigator = navigator;
        log.info("{} [SISTEMA] Controlador instanciado com suporte a Navigator e Virtual Threads.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML 
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface de Tabelas de Juros.", LOG_PREFIX);

        inicializarValoresPadraoViewModel();
        configurarFormulario();
        configurarColunasTabela();
        carregarDados();
        configurarFiltroReativo();

        log.debug("{} [SISTEMA] Inscrevendo controlador nos hubs de eventos.", LOG_PREFIX);
        bancoEventHub.inscrever(this::recarregarBancos);
        tabelaEventHub.inscrever(this::carregarDados);

        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d tabela(s)", Bindings.size(listaTabelas)));
        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override 
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos e desinscrevendo de eventos.", LOG_PREFIX);
        tabelaEventHub.desinscrever(this::carregarDados);
        bancoEventHub.desinscrever(this::recarregarBancos);
    }

    private void inicializarValoresPadraoViewModel() {
        log.trace("{} [NEGOCIO] Normalizando propriedades monetárias para evitar nulos.", LOG_PREFIX);
        viewModel.getTaxaMensal().set(BigDecimal.ZERO);
        viewModel.getComissaoPercentual().set(BigDecimal.ZERO);
        viewModel.getValorMinimoEmprestimo().set(BigDecimal.ZERO);
        viewModel.getValorMaximoEmprestimo().set(BigDecimal.ZERO);
        viewModel.getRendaMinima().set(BigDecimal.ZERO);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    public void carregarDados() {
        log.info("{} [TELEMETRIA] Iniciando carga de tabelas de juros.", LOG_PREFIX);
        filtrarTabelas(txtBusca.getText());
    }

    private void recarregarBancos() {
        log.debug("{} [TELEMETRIA] Recarregando lista de bancos ativos.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(
            tabelaFacade::listarBancosAtivos,
            bancos -> {
                comboBanco.setItems(FXCollections.observableArrayList(bancos));
                log.trace("{} [SISTEMA] Combo de bancos atualizado com {} registros.", LOG_PREFIX, bancos.size());
            },
            erro -> log.error("{} [SISTEMA] Erro ao carregar bancos: {}", LOG_PREFIX, erro.getMessage())
        );
    }

    private void configurarFiltroReativo() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.trace("{} [UI] Filtro de busca alterado para: '{}'", LOG_PREFIX, newVal);
            filtrarTabelas(newVal);
        });
    }

    private void filtrarTabelas(String termo) {
        AsyncUtils.executarTaskAsync(
            () -> tabelaFacade.filtrarTabelas(termo), 
            tabelasFiltradas -> {
                listaTabelas.setAll(tabelasFiltradas);
                tableTabelas.setItems(listaTabelas);
                log.info("{} [AUDITORIA] Listagem atualizada com {} tabelas.", LOG_PREFIX, tabelasFiltradas.size());
            }, 
            erro -> log.error("{} [SISTEMA] Falha ao filtrar tabelas: {}", LOG_PREFIX, erro.getMessage())
        );
    }

    // ==========================================================================================
    // MÓDULO 7: CONFIGURAÇÕES DE UI E BINDINGS
    // ==========================================================================================
    private void configurarFormulario() {
        log.debug("{} [SISTEMA] Configurando bindings e formatadores do formulário.", LOG_PREFIX);
        recarregarBancos();

        comboBanco.setConverter(criarConversor(b -> b != null ? b.getNome() : ""));
        comboBanco.valueProperty().bindBidirectional(viewModel.getBanco());

        comboConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));
        comboConvenio.valueProperty().bindBidirectional(viewModel.getTipoConvenio());

        txtNomeTabela.textProperty().bindBidirectional(viewModel.getNomeTabela());

        // Bindings Monetários via FinanceiroUtils (Garante Dirty Checking correto)
        vincularMascaraMoeda(txtTaxaMensal, viewModel.getTaxaMensal());
        vincularMascaraMoeda(txtComissao, viewModel.getComissaoPercentual());
        vincularMascaraMoeda(txtValorMinimo, viewModel.getValorMinimoEmprestimo());
        vincularMascaraMoeda(txtValorMaximo, viewModel.getValorMaximoEmprestimo());
        vincularMascaraMoeda(txtRendaMinima, viewModel.getRendaMinima());

        // Bindings Inteiros
        vincularMascaraInteiro(txtPrazoMinimo, viewModel.getPrazoMinimo());
        vincularMascaraInteiro(txtPrazoMaximo, viewModel.getPrazoMaximo());
        vincularMascaraInteiro(txtIdadeMinima, viewModel.getIdadeMinima());
        vincularMascaraInteiro(txtIdadeMaxima, viewModel.getIdadeMaxima());
    }

    private void configurarColunasTabela() {
        log.trace("{} [UI] Aplicando formatadores nas colunas da TableView.", LOG_PREFIX);
        
        colConvenio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipoConvenio().name()));
        colNome.setCellValueFactory(cell -> {
            String bancoNome = cell.getValue().getBanco() != null ? cell.getValue().getBanco().getNome() : "S/B";
            return new SimpleStringProperty("[" + bancoNome + "] " + cell.getValue().getNomeTabela());
        });

        colTaxa.setCellValueFactory(cell -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(cell.getValue().getTaxaMensal()) + "%"));
        
        colComissao.setCellValueFactory(cell -> new SimpleStringProperty(
                FinanceiroUtils.formatarParaExibicao(cell.getValue().getComissaoPercentual()) + "%"));

        colLimites.setCellValueFactory(cell -> {
            String min = "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorMinimoEmprestimo());
            String max = (cell.getValue().getValorMaximoEmprestimo() != null
                    && cell.getValue().getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) > 0)
                            ? "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getValorMaximoEmprestimo())
                            : "Sem teto";
            return new SimpleStringProperty(min + " - " + max);
        });

        colRenda.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRendaMinima() != null
                ? "R$ " + FinanceiroUtils.formatarParaExibicao(cell.getValue().getRendaMinima())
                : "Isento"));
        
        colIdade.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getIdadeMinima() + " a " + cell.getValue().getIdadeMaxima() + " anos"));
        
        colPrazo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPrazoMinimo() + " a " + cell.getValue().getPrazoMaximo() + "x"));

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
    @FXML 
    public void handleSalvar() {
        log.info("{} [TELEMETRIA] Iniciando processo de salvamento de tabela.", LOG_PREFIX);
        
        if (!viewModel.isDirty()) {
            log.warn("{} [NEGOCIO] Salvamento abortado: Nenhuma alteração detectada.", LOG_PREFIX);
            navigator.notificarAviso("Nenhuma alteração detectada para salvar.");
            return;
        }

        TabelaJurosModel snapshot = viewModel.atualizarModel(new TabelaJurosModel());
        navigator.mostrarLoading("Prescrevendo tabela...");

        AsyncUtils.executarTaskAsync(
            () -> tabelaFacade.salvarTabela(snapshot), 
            salva -> {
                navigator.ocultarLoading();
                log.info("{} [AUDITORIA] Tabela ID {} persistida com sucesso.", LOG_PREFIX, salva.getId());
                navigator.notificarSucesso("Tabela atualizada! A vigência antiga foi arquivada automaticamente.");
                limparFormulario();
                carregarDados();
            }, 
            erro -> {
                navigator.ocultarLoading();
                log.error("{} [AUDITORIA] Falha ao persistir tabela: {}", LOG_PREFIX, erro.getMessage());
                navigator.notificarAviso("Erro ao salvar: " + erro.getMessage());
            }
        );
    }

    private void abrirConfirmacaoArquivamento(TabelaJurosModel tabela) {
        log.info("{} [TELEMETRIA] Solicitando confirmação para arquivamento. ID: {}", LOG_PREFIX, tabela.getId());
        this.tabelaSelecionadaParaArquivar = tabela;

        navigator.solicitarConfirmacao("📦 Arquivar Tabela",
                "Deseja inativar definitivamente a tabela '" + tabela.getNomeTabela()
                        + "'?\nEla não poderá mais ser usada em novas propostas.",
                "Sim, Arquivar", "#f57c00", this::confirmarArquivamento);
    }

    private void confirmarArquivamento() {
        if (tabelaSelecionadaParaArquivar == null) return;

        Long idTabela = tabelaSelecionadaParaArquivar.getId();
        log.info("{} [TELEMETRIA] Executando arquivamento da tabela ID: {}", LOG_PREFIX, idTabela);
        navigator.mostrarLoading("Arquivando...");

        AsyncUtils.executarTaskAsync(
            () -> {
                tabelaFacade.arquivarTabela(tabelaSelecionadaParaArquivar);
                return null;
            }, 
            sucesso -> {
                navigator.ocultarLoading();
                log.info("{} [AUDITORIA] Tabela ID {} arquivada com sucesso.", LOG_PREFIX, idTabela);
                navigator.notificarSucesso("Tabela arquivada com sucesso.");
                carregarDados();
                this.tabelaSelecionadaParaArquivar = null;
            }, 
            erro -> {
                navigator.ocultarLoading();
                log.error("{} [AUDITORIA] Falha ao arquivar: {}", LOG_PREFIX, erro.getMessage());
                navigator.notificarAviso("Erro ao arquivar: " + erro.getMessage());
            }
        );
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO FORMULÁRIO
    // ==========================================================================================
    @FXML 
    public void handleToggleForm() {
        boolean mostrandoForm = paneFormulario.isShowDetailNode();
        log.trace("{} [UI] Alternando visibilidade do formulário. Novo estado: {}", LOG_PREFIX, !mostrandoForm);
        paneFormulario.setShowDetailNode(!mostrandoForm);
        btnToggleForm.setText(!mostrandoForm ? "➖ Fechar Formulário" : "➕ Prescrever Nova Tabela");
    }

    @FXML 
    public void limparFormulario() {
        log.info("{} [SISTEMA] Resetando formulário de tabela de juros.", LOG_PREFIX);
        viewModel.reset();
        inicializarValoresPadraoViewModel();

        paneFormulario.setShowDetailNode(false);
        lblFormTitulo.setText("💊 Prescrever Nova Tabela (Cadastrar / Atualizar)");
        btnToggleForm.setText("➕ Prescrever Nova Tabela");
    }

    private void editarTabela(TabelaJurosModel tabela) {
        log.info("{} [NEGOCIO] Carregando tabela ID {} para edição.", LOG_PREFIX, tabela.getId());
        viewModel.loadFromModel(tabela);

        // Normalização de nulos para garantir estabilidade da UI
        if (viewModel.getTaxaMensal().get() == null) viewModel.getTaxaMensal().set(BigDecimal.ZERO);
        if (viewModel.getComissaoPercentual().get() == null) viewModel.getComissaoPercentual().set(BigDecimal.ZERO);
        if (viewModel.getValorMinimoEmprestimo().get() == null) viewModel.getValorMinimoEmprestimo().set(BigDecimal.ZERO);
        if (viewModel.getValorMaximoEmprestimo().get() == null) viewModel.getValorMaximoEmprestimo().set(BigDecimal.ZERO);
        if (viewModel.getRendaMinima().get() == null) viewModel.getRendaMinima().set(BigDecimal.ZERO);

        paneFormulario.setShowDetailNode(true);
        lblFormTitulo.setText("🔄 Editando Tabela: " + tabela.getNomeTabela() + " (Isso gerará uma nova versão)");
        btnToggleForm.setText("➖ Fechar Formulário");
        txtNomeTabela.requestFocus();
    }

    // ==========================================================================================
    // MÓDULO 10: UTILITÁRIOS DE BINDING
    // ==========================================================================================
    private void vincularMascaraMoeda(TextField campo, javafx.beans.property.Property<BigDecimal> prop) {
        TextFormatter<BigDecimal> fmt = FinanceiroUtils.criarFormatadorMoeda();
        campo.setTextFormatter(fmt);
        // Vinculação via valueProperty garante que o Dirty Checking da ViewModel funcione
        fmt.valueProperty().bindBidirectional(prop);
    }

    private void vincularMascaraInteiro(TextField campo, javafx.beans.property.Property<Integer> prop) {
        log.trace("{} [SISTEMA] Criando formatador de inteiro para o campo: {}", LOG_PREFIX, campo.getId());
        
        StringConverter<Integer> converter = new StringConverter<>() {
            @Override public String toString(Integer object) {
                return object != null ? object.toString() : "";
            }

            @Override public Integer fromString(String string) {
                if (string == null || string.trim().isEmpty()) return 0;
                return FinanceiroUtils.parseSafeInt(string);
            }
        };

        TextFormatter<Integer> fmt = new TextFormatter<>(converter, 0, change -> 
            change.getControlNewText().matches("\\d*") ? change : null
        );
        
        campo.setTextFormatter(fmt);
        fmt.valueProperty().bindBidirectional(prop);
    }

    private <T> StringConverter<T> criarConversor(Function<T, String> formatter) {
        return new StringConverter<>() {
            @Override public String toString(T obj) { return formatter.apply(obj); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
