package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Banco;
import br.com.poderfinanceiro.app.model.TabelaJuros;
import br.com.poderfinanceiro.app.model.enums.StatusProposta;
import br.com.poderfinanceiro.app.repository.BancoRepository;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Scope("prototype") // Uma instância por paciente!
public class PropostaController {

    private final PropostaViewModel viewModel;
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;
    private final BancoRepository bancoRepository;

    @FXML
    private ComboBox<Banco> cbBanco;
    @FXML
    private ComboBox<TabelaJuros> cbTabela;
    @FXML
    private ComboBox<StatusProposta> cbStatus;

    @FXML
    private TextField txtValorSolicitado;
    @FXML
    private TextField txtValorAprovado;
    @FXML
    private TextField txtParcela;
    @FXML
    private Spinner<Integer> spinPrazo;

    @FXML
    private TextArea txtObservacoes;
    @FXML
    private Label lblComissaoEstimada;

    // Cache de tabelas ativas para filtrar conforme o banco selecionado
    private List<TabelaJuros> todasTabelasAtivas;

    public PropostaController(PropostaViewModel viewModel,
            PropostaService propostaService,
            TabelaJurosService tabelaJurosService,
            BancoRepository bancoRepository) {
        this.viewModel = viewModel;
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
        this.bancoRepository = bancoRepository;
    }

    @FXML
    public void initialize() {
        carregarListasBase();
        configurarFormatadoresEBindings();
        configurarCalculoAutomatico();
    }

    private void carregarListasBase() {
        // Bancos e Status
        cbBanco.setItems(FXCollections.observableArrayList(bancoRepository.findAll()));
        cbStatus.setItems(FXCollections.observableArrayList(StatusProposta.values()));

        // Conversores de visualização para os ComboBoxes
        cbBanco.setConverter(new StringConverter<>() {
            @Override
            public String toString(Banco b) {
                return b != null ? b.getNome() : "Selecione o Banco...";
            }

            @Override
            public Banco fromString(String s) {
                return null;
            }
        });

        cbTabela.setConverter(new StringConverter<>() {
            @Override
            public String toString(TabelaJuros t) {
                return t != null ? t.getNomeTabela() + " (" + t.getComissaoPercentual() + "%)"
                        : "Selecione a Tabela...";
            }

            @Override
            public TabelaJuros fromString(String s) {
                return null;
            }
        });

        // Carrega as tabelas e filtra quando o banco mudar
        todasTabelasAtivas = tabelaJurosService.listarAtivas();
        cbBanco.valueProperty().addListener((obs, old, bancoNovo) -> {
            if (bancoNovo != null) {
                List<TabelaJuros> filtradas = todasTabelasAtivas.stream()
                        .filter(t -> t.getBanco().getId().equals(bancoNovo.getId()))
                        .toList();
                cbTabela.setItems(FXCollections.observableArrayList(filtradas));
            } else {
                cbTabela.getItems().clear();
            }
        });
    }

    private void configurarFormatadoresEBindings() {
        // --- BINDINGS DIRETOS ---
        cbBanco.valueProperty().bindBidirectional(viewModel.bancoProperty());
        cbStatus.valueProperty().bindBidirectional(viewModel.statusProperty());
        txtObservacoes.textProperty().bindBidirectional(viewModel.observacoesProperty());

        // --- SPINNER (Prazo) ---
        spinPrazo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 0));
        spinPrazo.getValueFactory().valueProperty().bindBidirectional(viewModel.quantidadeParcelasProperty());

        // --- MÁSCARAS DE MOEDA (TextFormatter) ---
        TextFormatter<BigDecimal> fmtSolicitado = FinanceiroUtils.criarFormatadorMoeda();
        txtValorSolicitado.setTextFormatter(fmtSolicitado);
        fmtSolicitado.valueProperty().bindBidirectional(viewModel.valorSolicitadoProperty());

        TextFormatter<BigDecimal> fmtAprovado = FinanceiroUtils.criarFormatadorMoeda();
        txtValorAprovado.setTextFormatter(fmtAprovado);
        fmtAprovado.valueProperty().bindBidirectional(viewModel.valorAprovadoProperty());

        TextFormatter<BigDecimal> fmtParcela = FinanceiroUtils.criarFormatadorMoeda();
        txtParcela.setTextFormatter(fmtParcela);
        fmtParcela.valueProperty().bindBidirectional(viewModel.valorParcelaProperty());

        // Sincroniza o ID da tabela escolhida com o ViewModel
        cbTabela.valueProperty().addListener((obs, old, novaTabela) -> {
            if (novaTabela != null) {
                viewModel.tabelaIdProperty().set(novaTabela.getId().intValue());
            } else {
                viewModel.tabelaIdProperty().set(null);
            }
        });

        // Binding do Label de Comissão
        lblComissaoEstimada.textProperty().bind(Bindings.createStringBinding(
                () -> FinanceiroUtils.formatarParaExibicao(viewModel.comissaoEstimadaProperty().get()),
                viewModel.comissaoEstimadaProperty()));
    }

    /**
     * A Mágica da Anamnese: Recalcula a comissão sempre que o valor ou a tabela
     * mudar.
     */
    private void configurarCalculoAutomatico() {
        // Observa mudanças no Valor Aprovado e na Tabela
        viewModel.valorAprovadoProperty().addListener((obs, old, novoValor) -> dispararCalculo());
        viewModel.tabelaIdProperty().addListener((obs, old, novaTab) -> dispararCalculo());
    }

    private void dispararCalculo() {
        BigDecimal valor = viewModel.valorAprovadoProperty().get();
        Integer tabId = viewModel.tabelaIdProperty().get();

        if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0 && tabId != null) {
            // Usa o "Cérebro" para calcular
            BigDecimal comissao = propostaService.calcularComissaoEstimada(valor, tabId.longValue());
            viewModel.comissaoEstimadaProperty().set(comissao);
        } else {
            viewModel.comissaoEstimadaProperty().set(BigDecimal.ZERO);
        }
    }

    public PropostaViewModel getViewModel() {
        return viewModel;
    }
}