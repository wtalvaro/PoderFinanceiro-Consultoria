package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import br.com.poderfinanceiro.app.domain.service.ViaCepService;
import br.com.poderfinanceiro.app.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.EnderecoUtils;
import br.com.poderfinanceiro.app.viewmodel.EnderecoViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Scope("prototype")
public class EnderecoController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final int TAMANHO_CEP_VALIDO = 8;
    private static final String REGEX_SOMENTE_NUMEROS = "\\D";

    private static final String MSG_LOG_BUSCA = "Solicitando busca para o CEP: {}";
    private static final String MSG_AVISO_TAMANHO = "⚠️ O CEP deve conter exatamente 8 números.";
    private static final String MSG_ERRO_NAO_ENCONTRADO = "CEP Inválido ou não encontrado no ViaCEP.";
    private static final String MSG_ERRO_UF_INVALIDA = "UF não encontrada no Enum: {}";

    private static final Logger log = LoggerFactory.getLogger(EnderecoController.class);

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private TextField txtCep;
    @FXML
    private ComboBox<TipoLogradouroModel> comboTipoLogradouro;
    @FXML
    private TextField txtLogradouro;
    @FXML
    private TextField txtNumero;
    @FXML
    private TextField txtComplemento;
    @FXML
    private TextField txtBairro;
    @FXML
    private TextField txtCidade;
    @FXML
    private ComboBox<UfModel> comboUf;

    // =========================================================================
    // INJEÇÃO DE DEPENDÊNCIAS E ESTADO DA CLASSE
    // =========================================================================
    private final EnderecoViewModel viewModel;
    private final ViaCepService viaCepService;

    public EnderecoController(EnderecoViewModel viewModel, ViaCepService viaCepService) {
        this.viewModel = viewModel;
        this.viaCepService = viaCepService;
        log.debug("[ENDERECO] Construtor: Controller instanciado (escopo prototype)");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[ENDERECO] initialize: Iniciando configuração do formulário de endereço");
        configurarInterface();
        estabelecerBindings();
        log.info("[ENDERECO] initialize: Configuração concluída");
    }

    private void configurarInterface() {
        log.debug("[ENDERECO] configurarInterface: Carregando combos e aplicando formatador de CEP");
        comboTipoLogradouro.setItems(FXCollections.observableArrayList(TipoLogradouroModel.values()));
        comboUf.setItems(FXCollections.observableArrayList(UfModel.values()));
        txtCep.setTextFormatter(EnderecoUtils.criarFormatadorCep());
        log.trace("[ENDERECO] configurarInterface: Combos carregados ({} tipos, {} UFs)",
                TipoLogradouroModel.values().length, UfModel.values().length);
    }

    private void estabelecerBindings() {
        log.debug("[ENDERECO] estabelecerBindings: Realizando bind bidirecional com ViewModel");
        viewModel.cepProperty().bindBidirectional(txtCep.textProperty());
        viewModel.tipoLogradouroProperty().bindBidirectional(comboTipoLogradouro.valueProperty());
        viewModel.logradouroProperty().bindBidirectional(txtLogradouro.textProperty());
        viewModel.numeroProperty().bindBidirectional(txtNumero.textProperty());
        viewModel.complementoProperty().bindBidirectional(txtComplemento.textProperty());
        viewModel.bairroProperty().bindBidirectional(txtBairro.textProperty());
        viewModel.cidadeProperty().bindBidirectional(txtCidade.textProperty());
        viewModel.ufProperty().bindBidirectional(comboUf.valueProperty());
        log.trace("[ENDERECO] estabelecerBindings: Todos os bindings estabelecidos");
    }

    // =========================================================================
    // EVENTOS E LÓGICA DE BUSCA DE CEP
    // =========================================================================
    @FXML
    private void buscarCep() {
        String cepDigitado = txtCep.getText();
        log.debug("[ENDERECO] buscarCep: CEP digitado = '{}'", cepDigitado);

        if (isCepVazioOuNulo(cepDigitado)) {
            log.warn("[ENDERECO] buscarCep: CEP vazio ou nulo, ignorando busca");
            return;
        }

        String apenasNumeros = extrairSomenteNumeros(cepDigitado);
        log.trace("[ENDERECO] buscarCep: Apenas números = '{}'", apenasNumeros);

        if (apenasNumeros.length() == TAMANHO_CEP_VALIDO) {
            log.info(MSG_LOG_BUSCA, apenasNumeros);
            executarBuscaAssincrona(apenasNumeros);
        } else {
            log.warn(MSG_AVISO_TAMANHO + " (tamanho atual = {})", apenasNumeros.length());
        }
    }

    // 🚀 PATCH: Refatoração para usar o utilitário assíncrono (AsyncUtils)
    private void executarBuscaAssincrona(String cep) {
        log.debug("[ENDERECO] executarBuscaAssincrona: Chamando serviço ViaCEP para CEP={}", cep);
        AsyncUtils.executarTaskAsync(
                () -> {
                    log.trace("[ENDERECO] executarBuscaAssincrona: Executando viaCepService.buscarEnderecoPorCep({})",
                            cep);
                    return viaCepService.buscarEnderecoPorCep(cep);
                },
                this::processarResultadoBusca,
                this::processarErroBusca);
    }

    private void processarResultadoBusca(ViaCepResponse enderecoEncontrado) {
        if (enderecoEncontrado != null) {
            log.info("[ENDERECO] processarResultadoBusca: Endereço encontrado para CEP={}", enderecoEncontrado.cep());
            preencherCamposEndereco(enderecoEncontrado);
        } else {
            log.warn("[ENDERECO] processarResultadoBusca: {}", MSG_ERRO_NAO_ENCONTRADO);
        }
    }

    private void processarErroBusca(Throwable excecao) {
        log.error("[ENDERECO] processarErroBusca: Erro ao comunicar com a API do ViaCEP.");
        if (excecao != null) {
            log.error("[ENDERECO][ERROBUSCA] Erro: {}", excecao.getMessage(), excecao);
        }
    }

    private void preencherCamposEndereco(ViaCepResponse endereco) {
        log.debug("[ENDERECO] preencherCamposEndereco: Preenchendo campos com os dados retornados");
        txtLogradouro.setText(endereco.logradouro());
        txtBairro.setText(endereco.bairro());
        txtCidade.setText(endereco.localidade());
        selecionarUfSeguro(endereco.uf());
        log.info(
                "[ENDERECO] preencherCamposEndereco: Endereço preenchido: logradouro='{}', bairro='{}', cidade='{}', uf='{}'",
                endereco.logradouro(), endereco.bairro(), endereco.localidade(), endereco.uf());
    }

    private void selecionarUfSeguro(String ufSigla) {
        if (isCepVazioOuNulo(ufSigla)) {
            log.warn("[ENDERECO] selecionarUfSeguro: UF vazia ou nula, não será selecionada");
            return;
        }

        try {
            comboUf.setValue(UfModel.valueOf(ufSigla.toUpperCase()));
            log.debug("[ENDERECO] selecionarUfSeguro: UF '{}' selecionada com sucesso", ufSigla);
        } catch (IllegalArgumentException ex) {
            log.error(MSG_ERRO_UF_INVALIDA, ufSigla, ex);
        }
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS (Contratos do Controller) E UTILITÁRIOS
    // =========================================================================
    public void carregarEndereco(EnderecoProponenteModel endereco) {
        log.debug("[ENDERECO] carregarEndereco: Carregando endereço no formulário. Endereco fornecido? {}",
                endereco != null);
        if (endereco == null) {
            log.warn("[ENDERECO] carregarEndereco: Endereço nulo, resetando formulário");
            viewModel.reset();
            return;
        }
        viewModel.loadFromModel(endereco);
        log.info("[ENDERECO] carregarEndereco: Endereço carregado - CEP='{}', Logradouro='{}'",
                viewModel.cepProperty(), viewModel.logradouroProperty());
    }

    public void limparCampos() {
        log.debug("[ENDERECO] limparCampos: Resetando formulário");
        viewModel.reset();
    }

    public EnderecoViewModel getViewModel() {
        log.trace("[ENDERECO] getViewModel: Retornando ViewModel atual");
        return viewModel;
    }

    private boolean isCepVazioOuNulo(String valor) {
        boolean vazio = valor == null || valor.trim().isEmpty();
        if (vazio) {
            log.trace("[ENDERECO] isCepVazioOuNulo: Valor vazio ou nulo: '{}'", valor);
        }
        return vazio;
    }

    private String extrairSomenteNumeros(String valor) {
        String numeros = valor.replaceAll(REGEX_SOMENTE_NUMEROS, "");
        log.trace("[ENDERECO] extrairSomenteNumeros: '{}' -> '{}'", valor, numeros);
        return numeros;
    }
}