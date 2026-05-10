package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.model.enums.StatusProposta;
import br.com.poderfinanceiro.app.model.enums.TipoVinculo;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DashboardController {

    private final PropostaRepository propostaRepository;
    private final ComissaoRepository comissaoRepository;
    private final MainController mainController;

    // Elementos de UI vinculados ao dashboard.fxml
    @FXML
    private Label lblNomeConsultor;
    @FXML
    private Label lblQtdAguardando;
    @FXML
    private Label lblVolumeAprovado;
    @FXML
    private Label lblComissaoEstimada;
    @FXML
    private TextField txtBuscaPropostas;
    @FXML
    private TableView<Proposta> tabelaPropostas;
    @FXML
    private TableColumn<Proposta, String> colCliente;
    @FXML
    private TableColumn<Proposta, String> colBanco;
    @FXML
    private TableColumn<Proposta, String> colConvenio;
    @FXML
    private TableColumn<Proposta, BigDecimal> colValor;
    @FXML
    private TableColumn<Proposta, StatusProposta> colStatus;

    private final ObservableList<Proposta> masterData = FXCollections.observableArrayList();

    // Injeção de Dependências Oficial do Spring
    public DashboardController(PropostaRepository propostaRepository,
            ComissaoRepository comissaoRepository,
            MainController mainController) {
        this.propostaRepository = propostaRepository;
        this.comissaoRepository = comissaoRepository;
        this.mainController = mainController;
    }

    /**
     * Inicializa o dashboard configurando a tabela e carregando os dados do banco.
     */
    @FXML
    public void initialize() {
        lblNomeConsultor.setText("Wagner Teles Alvaro");

        configurarTabela();
        configurarBuscaReativa(); // Liga o "Radar" de busca
        carregarDadosReais(); // Coleta os primeiros exames
    }

    /**
     * Configura o mapeamento das colunas da TableView com os modelos de dados.
     */
    private void configurarTabela() {
        colCliente.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getProponente().getNomeCompleto()));
        colBanco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBanco().getNome()));

        colConvenio.setCellValueFactory(data -> {
            TipoVinculo vinculo = data.getValue().getProponente().getTipoVinculo();
            return new SimpleStringProperty(vinculo != null ? vinculo.getLabel() : "");
        });

        colValor.setCellValueFactory(new PropertyValueFactory<>("valorAprovado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colValor.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else
                    setText(FinanceiroUtils.formatarParaExibicao(item));
            }
        });
    }

    /**
     * O Motor da Mágica: Filtra a lista instantaneamente enquanto a Solange digita.
     */
    private void configurarBuscaReativa() {
        FilteredList<Proposta> filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscaPropostas.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(proposta -> {
                if (newVal == null || newVal.isEmpty()) return true;
                
                String filtro = newVal.toLowerCase();
                String nome = proposta.getProponente().getNomeCompleto().toLowerCase();
                String cpf = proposta.getProponente().getCpf().replaceAll("[^0-9]", ""); // Limpa máscara para buscar
                String banco = proposta.getBanco().getNome().toLowerCase();

                // Procura no Nome, no CPF ou no Banco
                return nome.contains(filtro) || cpf.contains(filtro) || banco.contains(filtro);
            });
        });

        SortedList<Proposta> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tabelaPropostas.comparatorProperty());
        
        // Em vez de jogar a lista crua, jogamos a lista "Superpoderosa" na tabela
        tabelaPropostas.setItems(sortedData);
    }

    /**
     * Busca informações reais no banco de dados e atualiza os KPIs e a Tabela.
     */
    @FXML
    public void carregarDadosReais() {
        // Usa a query com JOIN FETCH (O Raio-X completo) para evitar telas brancas
        List<Proposta> propostas = propostaRepository.findAllComDetalhes();

        // Atualiza a memória. A interface se ajusta sozinha, sem perder o que estiver
        // digitado na busca!
        masterData.setAll(propostas);

        // --- Recálculo dos Monitores Vitais (KPIs) ---
        long aguardando = propostas.stream()
                .filter(p -> p.getStatus() == StatusProposta.DIGITADA || p.getStatus() == StatusProposta.PENDENTE)
                .count();

        BigDecimal volumePago = propostas.stream()
                .filter(p -> p.getStatus() == StatusProposta.PAGO)
                .map(p -> p.getValorAprovado() != null ? p.getValorAprovado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal comissaoTotal = comissaoRepository.findAll().stream()
                .filter(c -> "Pendente".equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorLiquidoConsultor() != null ? c.getValorLiquidoConsultor() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblQtdAguardando.setText(String.valueOf(aguardando));
        lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(volumePago));
        lblComissaoEstimada.setText(FinanceiroUtils.formatarParaExibicao(comissaoTotal));
    }

    /**
     * Ação do botão "Simular Novo".
     * Avisa o sistema central para abrir uma ficha limpa.
     */
    @FXML
    private void simularNovo() {
        txtBuscaPropostas.clear(); // Limpa a busca antes de sair
        mainController.abrirClienteNoWorkspace(null); // O 'null' avisa o Workspace que é um paciente NOVO
    }
}