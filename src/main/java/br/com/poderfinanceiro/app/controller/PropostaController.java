package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Scope("prototype")
public class PropostaController {

    private final PropostaViewModel viewModel;
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;

    // --- NOVOS ELEMENTOS DA TRIAGEM ---
    @FXML
    private ComboBox<TipoConvenioModel> cbConvenio;

    @FXML
    private ComboBox<BancoModel> cbBanco;
    @FXML
    private ComboBox<TabelaJurosModel> cbTabela;
    @FXML
    private ComboBox<StatusPropostaModel> cbStatus;
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
    @FXML
    private Label lblTituloComissao;
    @FXML
    private Label lblTotalPago;

    // Caches da Memória
    private List<TabelaJurosModel> todasTabelasAtivas;
    private List<TabelaJurosModel> tabelasElegiveisDaTriagem; // Resultado do Filtro

    public PropostaController(PropostaViewModel viewModel,
            PropostaService propostaService, TabelaJurosService tabelaJurosService) {
        this.viewModel = viewModel;
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
    }

    @FXML
    public void initialize() {
        carregarListasBase();
        configurarFormatadoresEBindings();
        configurarGatilhosDaTriagem();
        configurarAutoSelecao();
    }

    private void carregarListasBase() {
        todasTabelasAtivas = tabelaJurosService.listarAtivas();
        cbStatus.setItems(FXCollections.observableArrayList(StatusPropostaModel.values()));

        // Inicializa o Combo de Convênio (Se você adicionou no FXML)
        if (cbConvenio != null) {
            cbConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));

            // Sugere o convênio automaticamente baseado no perfil do cliente
            viewModel.proponenteProperty().addListener((obs, old, paciente) -> {
                if (paciente != null && paciente.getConvenioOrgao() != null && cbConvenio.getValue() == null) {
                    cbConvenio.setValue(paciente.getConvenioOrgao());
                }
            });
        }

        // Conversores Visuais
        cbBanco.setConverter(new StringConverter<>() {
            @Override
            public String toString(BancoModel b) {
                return b != null ? b.getNome() : "Aguardando Triagem...";
            }

            @Override
            public BancoModel fromString(String s) {
                return null;
            }
        });

        cbTabela.setConverter(new StringConverter<>() {
            @Override
            public String toString(TabelaJurosModel t) {
                return t != null ? t.getNomeTabela() + " (" + t.getComissaoPercentual() + "%)"
                        : "Selecione a Tabela...";
            }

            @Override
            public TabelaJurosModel fromString(String s) {
                return null;
            }
        });
    }

    private void configurarFormatadoresEBindings() {
        cbStatus.valueProperty().bindBidirectional(viewModel.statusProperty());
        txtObservacoes.textProperty().bindBidirectional(viewModel.observacoesProperty());

        // --- CONFIGURAÇÃO DO PRAZO (SPINNER) ---
        spinPrazo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 0));
        spinPrazo.setEditable(true); // Permite digitação livre

        // "Hack" nativo de UX do JavaFX:
        // Garante que se ela digitar "84" e clicar em outro campo, o valor é salvo sem
        // precisar dar Enter.
        spinPrazo.getEditor().focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
            if (!agoraFocado) {
                spinPrazo.increment(0); // Força o JavaFX a fazer o "commit" do texto digitado para a ValueFactory
            }
        });

        spinPrazo.getValueFactory().valueProperty().bindBidirectional(viewModel.quantidadeParcelasProperty());

        // Máscaras Monetárias
        TextFormatter<BigDecimal> fmtSolicitado = FinanceiroUtils.criarFormatadorMoeda();
        txtValorSolicitado.setTextFormatter(fmtSolicitado);
        fmtSolicitado.valueProperty().bindBidirectional(viewModel.valorSolicitadoProperty());

        TextFormatter<BigDecimal> fmtAprovado = FinanceiroUtils.criarFormatadorMoeda();
        txtValorAprovado.setTextFormatter(fmtAprovado);
        fmtAprovado.valueProperty().bindBidirectional(viewModel.valorAprovadoProperty());

        TextFormatter<BigDecimal> fmtParcela = FinanceiroUtils.criarFormatadorMoeda();
        txtParcela.setTextFormatter(fmtParcela);
        fmtParcela.valueProperty().bindBidirectional(viewModel.valorParcelaProperty());

        // Quando carrega uma proposta do banco, ajusta a UI
        viewModel.tabelaIdProperty().addListener((obs, old, idNovo) -> {
            if (idNovo != null) {
                TabelaJurosModel tab = todasTabelasAtivas.stream().filter(t -> t.getId().equals(idNovo)).findFirst()
                        .orElse(null);
                if (tab != null) {
                    if (cbConvenio != null && cbConvenio.getValue() == null)
                        cbConvenio.setValue(tab.getTipoConvenio());
                    cbTabela.setValue(tab);
                    cbBanco.setValue(tab.getBanco());
                }
            } else {
                cbTabela.setValue(null);
            }
        });

        cbTabela.valueProperty().addListener((obs, old, novaTabela) -> {
            viewModel.tabelaIdProperty().set(novaTabela != null ? novaTabela.getId() : null);
            dispararCalculo(); // Recalcula a comissão imediatamente ao escolher a tabela
        });

        // Garante que o Banco selecionado no combo atualize o ViewModel (Para salvar no
        // banco de dados)
        cbBanco.valueProperty().bindBidirectional(viewModel.bancoProperty());

        // ==========================================================
        // MONITORES DE SINAIS VITAIS (UX Dinâmica)
        // ==========================================================

        // 1. O Título Dinâmico da Comissão (Psicologia de Vendas)
        lblTituloComissao.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    BigDecimal aprovado = viewModel.valorAprovadoProperty().get();
                    // Se o valor aprovado for maior que zero, é certeza. Senão, é estimativa.
                    if (aprovado != null && aprovado.compareTo(BigDecimal.ZERO) > 0) {
                        return "Valor da Comissão";
                    }
                    return "Comissão Estimada";
                }, viewModel.valorAprovadoProperty()));

        // 2. O Calculadora de Transparência (Total a Pagar)
        lblTotalPago.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    Integer prazo = viewModel.quantidadeParcelasProperty().get();
                    BigDecimal parcela = viewModel.valorParcelaProperty().get();

                    // Só calcula se os dois campos estiverem preenchidos e maiores que zero
                    if (prazo != null && prazo > 0 && parcela != null && parcela.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal total = parcela.multiply(new BigDecimal(prazo));
                        String formatado = FinanceiroUtils.formatarParaExibicao(total);
                        return "Total a Pagar: " + formatado;
                    }

                    return "Total a Pagar: R$ 0,00";
                }, viewModel.quantidadeParcelasProperty(), viewModel.valorParcelaProperty()));
    }

    // ========================================================================
    // O CORAÇÃO DA TRIAGEM (A Mágica dos Filtros)
    // ========================================================================

    private void configurarGatilhosDaTriagem() {
        // Se o valor solicitado ou o convênio mudarem, refaz a triagem
        viewModel.valorSolicitadoProperty().addListener((obs, old, val) -> realizarTriagem());
        if (cbConvenio != null) {
            cbConvenio.valueProperty().addListener((obs, old, val) -> realizarTriagem());
        }

        // Se o banco mudar, atualiza as tabelas disponíveis (dentro do que passou na
        // triagem)
        cbBanco.valueProperty().addListener((obs, old, bancoNovo) -> {
            if (bancoNovo != null && tabelasElegiveisDaTriagem != null) {

                // Filtra as tabelas do banco selecionado e ORDENA pela maior comissão!
                List<TabelaJurosModel> tabelasDoBanco = tabelasElegiveisDaTriagem.stream()
                        .filter(t -> t.getBanco().getId().equals(bancoNovo.getId()))
                        .sorted(java.util.Comparator.comparing(TabelaJurosModel::getComissaoPercentual).reversed())
                        .toList();

                TabelaJurosModel tabelaAtual = cbTabela.getValue();
                cbTabela.setItems(FXCollections.observableArrayList(tabelasDoBanco));

                // 🧠 A Mágica do Piloto Automático para Tabelas
                if (tabelasDoBanco.size() == 1) {
                    // Se só tem uma via, vira o volante sozinho
                    cbTabela.setValue(tabelasDoBanco.get(0));
                } else if (tabelaAtual != null && tabelasDoBanco.contains(tabelaAtual)) {
                    // Se ela alterou o valor, mas a tabela que ela já tinha escolhido continua
                    // válida, mantém!
                    cbTabela.setValue(tabelaAtual);
                } else {
                    // Se tem várias opções e ela ainda não escolheu, deixa em branco (A mais
                    // lucrativa estará no topo!)
                    cbTabela.setValue(null);
                }
            } else {
                cbTabela.getItems().clear();
                cbTabela.setValue(null);
            }
        });

        // Recalcula se o valor aprovado for digitado
        viewModel.valorAprovadoProperty().addListener((obs, old, val) -> dispararCalculo());
    }

    private void realizarTriagem() {
        TipoConvenioModel convenio = (cbConvenio != null) ? cbConvenio.getValue() : null;
        BigDecimal valorSolicitado = viewModel.valorSolicitadoProperty().get();

        if (convenio == null || valorSolicitado == null || valorSolicitado.compareTo(BigDecimal.ZERO) <= 0) {
            cbBanco.getItems().clear();
            cbTabela.getItems().clear();
            tabelasElegiveisDaTriagem = null;
            return;
        }

        // 1. Filtra as Tabelas Elegíveis (Poder do Stream Nativo)
        tabelasElegiveisDaTriagem = todasTabelasAtivas.stream()
                .filter(t -> t.getTipoConvenio() == convenio)
                .filter(t -> {
                    BigDecimal min = t.getValorMinimoEmprestimo() != null ? t.getValorMinimoEmprestimo()
                            : BigDecimal.ZERO;
                    return valorSolicitado.compareTo(min) >= 0;
                })
                .filter(t -> t.getValorMaximoEmprestimo() == null
                        || t.getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) <= 0
                        || valorSolicitado.compareTo(t.getValorMaximoEmprestimo()) <= 0)
                .toList();

        // 2. Extrai os Bancos (Sem HashMap! Agora o Hibernate confia no distinct do
        // Java)
        List<BancoModel> bancosElegiveis = tabelasElegiveisDaTriagem.stream()
                .map(TabelaJurosModel::getBanco)
                .filter(java.util.Objects::nonNull) // Evita nulos caso alguma tabela esteja sem banco
                .distinct() // O Java usa o equals() nativo da entidade perfeitamente agora!
                .toList();

        // 3. Atualiza o ComboBox de Bancos mantendo a seleção se for válida
        BancoModel bancoAtual = cbBanco.getValue();
        cbBanco.setItems(FXCollections.observableArrayList(bancosElegiveis));

        // 4. 🧠 A Mágica do Piloto Automático para Bancos
        if (bancosElegiveis.size() == 1) {
            // Rota única: Auto-seleciona o banco parceiro
            cbBanco.setValue(bancosElegiveis.get(0));
        } else if (bancoAtual != null && bancosElegiveis.contains(bancoAtual)) {
            // O paciente alterou o valor, mas o banco que estava selecionado ainda aceita a
            // operação
            cbBanco.setValue(bancoAtual);
        } else {
            // Múltiplos bancos disponíveis: Força a Consultora a avaliar as opções
            cbBanco.setValue(null);
        }

        dispararCalculo();
    }

    /**
     * Calcula a Comissão e mostra o R$ e a % na UI.
     * Prioriza o Valor Liberado (Aprovado), mas usa o Solicitado se o Aprovado
     * estiver vazio.
     */
    private void dispararCalculo() {
        BigDecimal vAprovado = viewModel.valorAprovadoProperty().get();
        BigDecimal vSolicitado = viewModel.valorSolicitadoProperty().get();
        TabelaJurosModel tabela = cbTabela.getValue();

        // Qual valor usar para calcular? Se tem liberado, usa liberado. Senão, usa
        // solicitado.
        BigDecimal valorBase = (vAprovado != null && vAprovado.compareTo(BigDecimal.ZERO) > 0) ? vAprovado
                : vSolicitado;

        if (valorBase != null && valorBase.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            // Calcula no Backend/Service
            BigDecimal comissaoCalculada = propostaService.calcularComissaoEstimada(valorBase, tabela.getId());
            viewModel.comissaoEstimadaProperty().set(comissaoCalculada);

            // Atualiza a UI (Valor + Porcentagem)
            String formatado = FinanceiroUtils.formatarParaExibicao(comissaoCalculada);
            lblComissaoEstimada.setText(String.format("%s (%s%%)", formatado, tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); // Dá um destaque clínico
        } else {
            viewModel.comissaoEstimadaProperty().set(BigDecimal.ZERO);
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-default;");
        }
    }

    private void configurarAutoSelecao() {
        TextField[] camposFinanceiros = { txtValorSolicitado, txtValorAprovado, txtParcela };
        for (TextField campo : camposFinanceiros) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    javafx.application.Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    public PropostaViewModel getViewModel() {
        return viewModel;
    }
}