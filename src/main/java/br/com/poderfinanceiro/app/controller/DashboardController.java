package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.model.StatusProposta;
import br.com.poderfinanceiro.app.model.TipoVinculo;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DashboardController {

    @Autowired
    private PropostaRepository propostaRepository;

    @Autowired
    private ComissaoRepository comissaoRepository;

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

    /**
     * Inicializa o dashboard configurando a tabela e carregando os dados do banco.
     */
    @FXML
    public void initialize() {
        // Nome fixo conforme perfil do proprietário da Poder Financeiro
        lblNomeConsultor.setText("Wagner Teles Alvaro");

        configurarTabela();
        carregarDadosReais();
    }

    /**
     * Configura o mapeamento das colunas da TableView com os modelos de dados.
     */
    private void configurarTabela() {
        // Acesso ao nome do proponente (Entidade Proponente)
        colCliente.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getProponente().getNomeCompleto()));

        // Acesso ao nome do banco (Entidade Banco)
        colBanco.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getBanco().getNomeBanco()));

        colConvenio.setCellValueFactory(cellData -> {
            // 1. Pegamos o enum do proponente vinculado à proposta
            TipoVinculo vinculo = cellData.getValue().getProponente().getTipoVinculo();

            // 2. Retornamos a descrição amigável ou vazio se estiver nulo
            return new SimpleStringProperty(vinculo != null ? vinculo.getLabel() : "");
        });
        
        // Mapeamento direto de campos da Proposta
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorAprovado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Formatação monetária personalizada para a coluna de valor
        colValor.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(FinanceiroUtils.formatarParaExibicao(item));
                }
            }
        });
    }

    /**
     * Busca informações reais no banco de dados e atualiza os KPIs e a Tabela.
     */
    @FXML
    public void carregarDadosReais() {
        List<Proposta> propostas = propostaRepository.findAll();

        // 1. Contador de propostas aguardando (Status DIGITADA ou PENDENTE)
        long aguardando = propostas.stream()
                .filter(p -> p.getStatus() == StatusProposta.DIGITADA ||
                        p.getStatus() == StatusProposta.PENDENTE)
                .count();

        // 2. Volume total aprovado (Status PAGO)
        BigDecimal volumePago = propostas.stream()
                .filter(p -> p.getStatus() == StatusProposta.PAGO)
                .map(p -> p.getValorAprovado() != null ? p.getValorAprovado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Soma das comissões pendentes (Tabela Comissoes)
        BigDecimal comissaoTotal = comissaoRepository.findAll().stream()
                .filter(c -> "Pendente".equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorLiquidoConsultor() != null ? c.getValorLiquidoConsultor() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Atualização dos Labels na UI usando formatador oficial[cite: 13]
        lblQtdAguardando.setText(String.valueOf(aguardando));
        lblVolumeAprovado.setText(FinanceiroUtils.formatarParaExibicao(volumePago));
        lblComissaoEstimada.setText(FinanceiroUtils.formatarParaExibicao(comissaoTotal));

        // Atualiza a lista da tabela
        tabelaPropostas.setItems(FXCollections.observableArrayList(propostas));
    }
}