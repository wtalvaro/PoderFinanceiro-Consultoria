package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.enums.LabeledModel;
import br.com.poderfinanceiro.app.domain.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.DataUtils;
import br.com.poderfinanceiro.app.util.DocumentoUtils;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Scope("prototype")
public class ProponenteController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final String MSG_NOVO_CONTATO = "Cadastrar Novo Contato";
    private static final String MSG_EDITAR_CONTATO = "Editando Contato: ";
    private static final String PADRAO_DATA = "dd/MM/yyyy";

    private static final Logger log = LoggerFactory.getLogger(ProponenteController.class);

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private Label lblTituloTela;
    @FXML
    private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML
    private ComboBox<OrigemLeadModel> cbOrigem;
    @FXML
    private ComboBox<TipoVinculoModel> cbVinculo;
    @FXML
    private ComboBox<TipoRelacionamentoModel> cbClassificacao;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private ScrollPane scrollPrincipal;

    // =========================================================================
    // ESTADO DA CLASSE E INJEÇÕES
    // =========================================================================
    private final LeadViewModel viewModel;

    public ProponenteController(LeadViewModel viewModel) {
        this.viewModel = viewModel;
        log.debug("[PROPONENTE] Construtor: Controller instanciado (escopo prototype)");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[PROPONENTE] initialize: Iniciando configuração do formulário de proponente");
        configurarTituloDinamico();
        configurarListasEFormatadoresDeData();
        estabelecerBindings();
        configurarAutoSelecao(txtRenda);
        log.info("[PROPONENTE] initialize: Configuração concluída");
    }

    private void configurarTituloDinamico() {
        log.debug("[PROPONENTE] configurarTituloDinamico: Vinculando título dinâmico ao nome do ViewModel");
        lblTituloTela.textProperty().bind(Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            String titulo = (nome == null || nome.isBlank()) ? MSG_NOVO_CONTATO : MSG_EDITAR_CONTATO + nome;
            log.trace("[PROPONENTE] Título dinâmico atualizado: '{}'", titulo);
            return titulo;
        }, viewModel.nomeProperty()));
    }

    private void configurarListasEFormatadoresDeData() {
        log.debug("[PROPONENTE] configurarListasEFormatadoresDeData: Carregando combos e formatador de data");
        configurarCombo(cbOrigem, OrigemLeadModel.values(), OrigemLeadModel::fromString);
        configurarCombo(cbVinculo, TipoVinculoModel.values(), TipoVinculoModel::fromString);
        configurarCombo(cbClassificacao, TipoRelacionamentoModel.values(), TipoRelacionamentoModel::fromString);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PADRAO_DATA);
        dpDataNascimento.setConverter(new LocalDateStringConverter(formatter, formatter));
        log.trace("[PROPONENTE] Formatador de data configurado com padrão '{}'", PADRAO_DATA);
    }

    private <T extends Enum<T> & LabeledModel> void configurarCombo(ComboBox<T> combo, T[] values,
            Function<String, T> searcher) {
        combo.getItems().setAll(values);
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T obj) {
                return (obj != null) ? obj.getLabel() : "";
            }

            @Override
            public T fromString(String str) {
                return searcher.apply(str);
            }
        });
        log.trace("[PROPONENTE] Combo '{}' configurado com {} itens", combo.getId(), values.length);
    }

    // =========================================================================
    // BINDINGS (SRP Aplicado)
    // =========================================================================
    private void estabelecerBindings() {
        log.debug("[PROPONENTE] estabelecerBindings: Configurando bindings bidirecionais");
        vincularCamposSimples();
        vincularCamposComFormatacao();
    }

    private void vincularCamposSimples() {
        log.trace("[PROPONENTE] vinculando campos simples");
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());
    }

    private void vincularCamposComFormatacao() {
        log.trace("[PROPONENTE] vinculando campos com formatação especial (data, renda, CPF, telefone)");
        // Data de Nascimento
        TextFormatter<LocalDate> dataFormatter = DataUtils.criarFormatadorData();
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

        // Renda
        TextFormatter<BigDecimal> rendaFormatter = FinanceiroUtils.criarFormatadorMoeda();
        txtRenda.setTextFormatter(rendaFormatter);
        rendaFormatter.valueProperty().bindBidirectional(viewModel.rendaProperty());

        // CPF
        TextFormatter<String> cpfFormatter = DocumentoUtils.criarFormatadorCpf();
        txtCpf.setTextFormatter(cpfFormatter);
        cpfFormatter.valueProperty().bindBidirectional(viewModel.cpfProperty());

        // Telefone
        TextFormatter<String> telefoneFormatter = ContatoUtils.criarFormatadorTelefone();
        txtTelefone.setTextFormatter(telefoneFormatter);
        telefoneFormatter.valueProperty().bindBidirectional(viewModel.telefoneProperty());
    }

    // =========================================================================
    // UTILITÁRIOS DE UI (DRY Aplicado)
    // =========================================================================
    private void configurarAutoSelecao(TextField... campos) {
        log.debug("[PROPONENTE] configurarAutoSelecao: Ativando seleção automática ao focar campos");
        for (TextField campo : campos) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    log.trace("[PROPONENTE] Campo '{}' ganhou foco, selecionando todo o texto", campo.getId());
                    Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    public LeadViewModel getViewModel() {
        log.trace("[PROPONENTE] getViewModel: Retornando ViewModel atual");
        return viewModel;
    }
}