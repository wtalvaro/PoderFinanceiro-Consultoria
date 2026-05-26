package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ComissaoUIEventHub;
import br.com.poderfinanceiro.app.domain.event.PropostaUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DashboardController implements Disposable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String STATUS_PAGO_COMISSAO = "Pago";
    private static final String STATUS_LIQUIDADO_COMISSAO = "Liquidado";
    private static final String MSG_OFFLINE = "Consultor Offline";
    private static final String MSG_SEM_CONVENIO = "Sem Convênio";
    private static final String MSG_CARREGANDO = "Calculando métricas do Dashboard...";

    private static final String[] FRASES_MOTIVACIONAIS = {
            "O sucesso é a soma de pequenos esforços repetidos dia após dia.",
            "Bons negócios não caem do céu, são construídos. Ótimo dia de vendas!",
            "Cada 'não' te deixa mais perto do próximo 'sim'. Vamos em frente!",
            "Consultoria de excelência gera clientes para a vida toda.",
            "Acredite no seu potencial. O fechamento perfeito começa agora!",
            "O único lugar onde o sucesso vem antes do trabalho é no dicionário."
    };

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final PropostaRepository propostaRepository;
    private final ComissaoRepository comissaoRepository;
    private final Navigator navigator;
    private final AuthService authService;
    private final PropostaUIEventHub propostaEventHub;
    private final ComissaoUIEventHub comissaoEventHub;

    // =========================================================================
    // COMPONENTES UI (FXML)
    // =========================================================================
    @FXML
    private Label lblNomeConsultor;
    @FXML
    private Label lblQtdAguardando;
    @FXML
    private Label lblVolumeAprovado;
    @FXML
    private Label lblComissaoPendente;
    @FXML
    private Label lblComissaoPaga;
    @FXML
    private TextField txtBuscaPropostas;

    @FXML
    private TableView<PropostaModel> tabelaPropostas;
    @FXML
    private TableColumn<PropostaModel, String> colCliente;
    @FXML
    private TableColumn<PropostaModel, String> colBanco;
    @FXML
    private TableColumn<PropostaModel, String> colConvenio;
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValorSolicitado;
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colValor;
    @FXML
    private TableColumn<PropostaModel, BigDecimal> colComissao;
    @FXML
    private TableColumn<PropostaModel, StatusPropostaModel> colStatus;
    @FXML
    private TableColumn<PropostaModel, Void> colAcoes;

    private final ObservableList<PropostaModel> masterData = FXCollections.observableArrayList();
    private final Random randomGenerator = new Random();

    public DashboardController(PropostaRepository propostaRepository, ComissaoRepository comissaoRepository,
            Navigator navigator, AuthService authService, PropostaUIEventHub propostaEventHub,
            ComissaoUIEventHub comissaoEventHub) {
        this.propostaRepository = propostaRepository;
        this.comissaoRepository = comissaoRepository;
        this.navigator = navigator;
        this.authService = authService;
        this.propostaEventHub = propostaEventHub;
        this.comissaoEventHub = comissaoEventHub;
        log.debug("[DASHBOARD] Construtor: Controller instanciado com dependências injetadas");
    }

    // =========================================================================
    // INICIALIZAÇÃO E CONFIGURAÇÃO DE UI
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[DASHBOARD] initialize: Iniciando configuração do Dashboard");
        propostaEventHub.inscrever(this::carregarDadosReais);
        comissaoEventHub.inscrever(this::carregarDadosReais);
        carregarNomeConsultor();
        configurarTabela();
        configurarBuscaReativa();
        carregarDadosReais();
        log.info("[DASHBOARD] initialize: Dashboard configurado com sucesso");
    }

    private void carregarNomeConsultor() {
        UsuarioModel usuario = authService.getUsuarioLogado();
        String nome = usuario != null ? usuario.getNome() : MSG_OFFLINE;
        lblNomeConsultor.setText(nome);
        log.debug("[DASHBOARD] carregarNomeConsultor: Consultor = '{}'", nome);
    }

    private void configurarTabela() {
        log.debug("[DASHBOARD] configurarTabela: Configurando colunas da tabela de propostas");
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBanco().getNome()));
        colConvenio.setCellValueFactory(d -> {
            var convenio = d.getValue().getConvenioOrgao();
            return new SimpleStringProperty(convenio != null ? convenio.getLabel() : MSG_SEM_CONVENIO);
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        configurarColunaMoeda(colValorSolicitado, "valorSolicitado");
        configurarColunaMoeda(colValor, "valorAprovado");
        configurarColunaMoeda(colComissao, "comissaoEstimada");

        configurarColunaAcoes();
        log.debug("[DASHBOARD] configurarTabela: Tabela configurada");
    }

    private void configurarColunaMoeda(TableColumn<PropostaModel, BigDecimal> coluna, String propertyName) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "-" : FinanceiroUtils.formatarParaExibicao(item));
            }
        });
        log.trace("[DASHBOARD] configurarColunaMoeda: Coluna '{}' configurada", propertyName);
    }

    private void configurarColunaAcoes() {
        log.debug("[DASHBOARD] configurarColunaAcoes: Configurando coluna de ações (botão Abrir)");
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = criarBotaoAbrirProposta();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnAbrir);
                    setAlignment(Pos.CENTER);
                }
            }

            private Button criarBotaoAbrirProposta() {
                Button btn = new Button("📂 Abrir");
                btn.getStyleClass().addAll("flat", "accent");
                btn.setCursor(Cursor.HAND);
                btn.setOnAction(event -> {
                    PropostaModel proposta = getTableView().getItems().get(getIndex());
                    if (proposta != null && proposta.getProponente() != null) {
                        log.info("[DASHBOARD] Abrindo proposta ID={} do cliente {}", proposta.getId(),
                                proposta.getProponente().getNomeCompleto());
                        navigator.abrirPropostaNoWorkspace(proposta);
                    } else {
                        log.warn("[DASHBOARD] Tentativa de abrir proposta inválida (proposta ou proponente nulo)");
                    }
                });
                return btn;
            }
        });
    }

    private void configurarBuscaReativa() {
        log.debug("[DASHBOARD] configurarBuscaReativa: Configurando busca reativa (filtro por texto)");
        FilteredList<PropostaModel> filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscaPropostas.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("[DASHBOARD] Busca alterada: '{}' -> '{}'", oldVal, newVal);
            filteredData.setPredicate(proposta -> atendeFiltroDeBusca(proposta, newVal));
        });

        SortedList<PropostaModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tabelaPropostas.comparatorProperty());
        tabelaPropostas.setItems(sortedData);
    }

    private boolean atendeFiltroDeBusca(PropostaModel proposta, String termo) {
        if (termo == null || termo.isBlank())
            return true;

        String filtro = termo.toLowerCase();
        String nome = proposta.getProponente().getNomeCompleto().toLowerCase();
        String cpf = proposta.getProponente().getCpf().replaceAll("[^0-9]", "");
        String banco = proposta.getBanco().getNome().toLowerCase();

        boolean matches = nome.contains(filtro) || cpf.contains(filtro) || banco.contains(filtro);
        log.trace("[DASHBOARD] atendeFiltroDeBusca: termo='{}', proposta cliente='{}' -> {}", termo, nome, matches);
        return matches;
    }

    // =========================================================================
    // LÓGICA DE DADOS (ASYNC) E MÉTRICAS
    // =========================================================================
    @FXML
    public void carregarDadosReais() {
        log.debug("[DASHBOARD] carregarDadosReais: Iniciando carregamento assíncrono de propostas e comissões");
        navigator.mostrarLoading(MSG_CARREGANDO);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("[DASHBOARD] carregarDadosReais: Buscando dados no repositório");
                    List<PropostaModel> propostas = propostaRepository.findAllComDetalhes();
                    List<ComissaoModel> comissoes = comissaoRepository.findAll();
                    log.debug("[DASHBOARD] carregarDadosReais: Encontradas {} propostas e {} comissões",
                            propostas.size(), comissoes.size());
                    return calcularMetricasDoDashboard(propostas, comissoes);
                },
                this::atualizarInterfaceDoDashboard,
                erro -> {
                    navigator.ocultarLoading();
                    log.error("[DASHBOARD][DADOS] Erro ao carregar dados: {}", erro.getMessage(), erro);
                });
    }

    private ResultadoDashboard calcularMetricasDoDashboard(List<PropostaModel> propostas,
            List<ComissaoModel> comissoes) {
        log.debug("[DASHBOARD] calcularMetricasDoDashboard: Calculando métricas");
        long aguardando = propostas.stream().filter(this::isPropostaAguardando).count();
        BigDecimal volumeAprovado = somarVolumeAprovado(propostas);
        BigDecimal comissaoPendente = somarComissoes(comissoes, false);
        BigDecimal comissaoPaga = somarComissoes(comissoes, true);

        log.info(
                "[DASHBOARD] Métricas calculadas: aguardando={}, volumeAprovado={}, comissaoPendente={}, comissaoPaga={}",
                aguardando,
                FinanceiroUtils.formatarParaExibicao(volumeAprovado),
                FinanceiroUtils.formatarParaExibicao(comissaoPendente),
                FinanceiroUtils.formatarParaExibicao(comissaoPaga));

        return new ResultadoDashboard(propostas, aguardando, volumeAprovado, comissaoPendente, comissaoPaga);
    }

    private boolean isPropostaAguardando(PropostaModel p) {
        boolean aguardando = p.getStatus() == StatusPropostaModel.DIGITADA
                || p.getStatus() == StatusPropostaModel.PENDENTE;
        log.trace("[DASHBOARD] isPropostaAguardando: Proposta ID={} status={} -> {}", p.getId(), p.getStatus(),
                aguardando);
        return aguardando;
    }

    private BigDecimal somarVolumeAprovado(List<PropostaModel> propostas) {
        BigDecimal soma = propostas.stream()
                .filter(p -> p.getStatus() == StatusPropostaModel.PAGO)
                .map(p -> p.getValorFinalCliente() != null ? p.getValorFinalCliente() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("[DASHBOARD] somarVolumeAprovado: Total = {}", FinanceiroUtils.formatarParaExibicao(soma));
        return soma;
    }

    private BigDecimal somarComissoes(List<ComissaoModel> comissoes, boolean isPaga) {
        BigDecimal soma = comissoes.stream()
                .filter(c -> isComissaoPaga(c) == isPaga)
                .map(c -> {
                    BigDecimal valor = isPaga ? c.getValorPagoPelaPoder() : c.getValorBrutoComissao();
                    return valor != null ? valor : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("[DASHBOARD] somarComissoes: isPaga={}, total = {}", isPaga,
                FinanceiroUtils.formatarParaExibicao(soma));
        return soma;
    }

    private boolean isComissaoPaga(ComissaoModel c) {
        String status = c.getStatusPagamento();
        boolean paga = STATUS_PAGO_COMISSAO.equalsIgnoreCase(status)
                || STATUS_LIQUIDADO_COMISSAO.equalsIgnoreCase(status);
        log.trace("[DASHBOARD] isComissaoPaga: Comissão ID={} status='{}' -> {}", c.getId(), status, paga);
        return paga;
    }

    // =========================================================================
    // ATUALIZAÇÃO DA UI E EXPERIÊNCIA DO USUÁRIO
    // =========================================================================
    private void atualizarInterfaceDoDashboard(ResultadoDashboard res) {
        log.debug("[DASHBOARD] atualizarInterfaceDoDashboard: Atualizando componentes visuais");
        masterData.setAll(res.propostas());
        log.info("[DASHBOARD] Atualizados {} registros na tabela", res.propostas().size());

        lblQtdAguardando.setText(String.valueOf(res.qtdAguardando()));
        lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(res.volumeAprovado()));
        lblComissaoPendente.setText(FinanceiroUtils.formatarParaExibicao(res.comissaoPendente()));
        lblComissaoPaga.setText(FinanceiroUtils.formatarParaExibicao(res.comissaoPaga()));

        exibirLoadingMotivacional();
    }

    private void exibirLoadingMotivacional() {
        String fraseSorteada = FRASES_MOTIVACIONAIS[randomGenerator.nextInt(FRASES_MOTIVACIONAIS.length)];
        log.debug("[DASHBOARD] exibirLoadingMotivacional: Frase sorteada = '{}'", fraseSorteada);
        navigator.mostrarLoading("💡 " + fraseSorteada);

        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
        delay.setOnFinished(e -> {
            log.debug("[DASHBOARD] exibirLoadingMotivacional: Ocultando loading após 3.5s");
            navigator.ocultarLoading();
        });
        delay.play();
    }

    @FXML
    private void simularNovo() {
        log.info("[DASHBOARD] simularNovo: Usuário iniciou nova simulação (limpa busca e abre workspace vazio)");
        txtBuscaPropostas.clear();
        navigator.abrirClienteNoWorkspace(null);
    }

    // =========================================================================
    // RECORDS AUXILIARES
    // =========================================================================
    private record ResultadoDashboard(
            List<PropostaModel> propostas,
            long qtdAguardando,
            BigDecimal volumeAprovado,
            BigDecimal comissaoPendente,
            BigDecimal comissaoPaga) {
    }

    @Override
    public void dispose() {
        log.info("[DASHBOARD] dispose: Desinscrevendo dos hubs.");
        propostaEventHub.desinscrever(this::carregarDadosReais);
        comissaoEventHub.desinscrever(this::carregarDadosReais);
    }
}