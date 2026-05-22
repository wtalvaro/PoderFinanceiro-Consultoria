package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import br.com.poderfinanceiro.app.domain.service.ViaCepService;
import br.com.poderfinanceiro.app.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.util.EnderecoUtils;
import br.com.poderfinanceiro.app.viewmodel.EnderecoViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class EnderecoController {

    // =========================================================================
    // CONSTANTES (Clean Code & DRY)
    // =========================================================================
    private static final int TAMANHO_CEP_VALIDO = 8;
    private static final String REGEX_SOMENTE_NUMEROS = "\\D";

    private static final String MSG_LOG_BUSCA = "Solicitando busca para o CEP: %s";
    private static final String MSG_AVISO_TAMANHO = "⚠️ O CEP deve conter exatamente 8 números.";
    private static final String MSG_ERRO_NAO_ENCONTRADO = "CEP Inválido ou não encontrado no ViaCEP.";
    private static final String MSG_ERRO_UF_INVALIDA = "UF não encontrada no Enum: %s";

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
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        configurarInterface();
        estabelecerBindings();
    }

    private void configurarInterface() {
        comboTipoLogradouro.setItems(FXCollections.observableArrayList(TipoLogradouroModel.values()));
        comboUf.setItems(FXCollections.observableArrayList(UfModel.values()));
        txtCep.setTextFormatter(EnderecoUtils.criarFormatadorCep());
    }

    private void estabelecerBindings() {
        viewModel.cepProperty().bindBidirectional(txtCep.textProperty());
        viewModel.tipoLogradouroProperty().bindBidirectional(comboTipoLogradouro.valueProperty());
        viewModel.logradouroProperty().bindBidirectional(txtLogradouro.textProperty());
        viewModel.numeroProperty().bindBidirectional(txtNumero.textProperty());
        viewModel.complementoProperty().bindBidirectional(txtComplemento.textProperty());
        viewModel.bairroProperty().bindBidirectional(txtBairro.textProperty());
        viewModel.cidadeProperty().bindBidirectional(txtCidade.textProperty());
        viewModel.ufProperty().bindBidirectional(comboUf.valueProperty());
    }

    // =========================================================================
    // EVENTOS E LÓGICA DE BUSCA DE CEP
    // =========================================================================
    @FXML
    private void buscarCep() {
        String cepDigitado = txtCep.getText();

        if (isCepVazioOuNulo(cepDigitado))
            return;

        String apenasNumeros = extrairSomenteNumeros(cepDigitado);

        if (apenasNumeros.length() == TAMANHO_CEP_VALIDO) {
            System.out.printf((MSG_LOG_BUSCA) + "%n", apenasNumeros);
            executarBuscaAssincrona(apenasNumeros);
        } else {
            System.out.println(MSG_AVISO_TAMANHO);
        }
    }

    private void executarBuscaAssincrona(String cep) {
        Task<ViaCepResponse> buscaTask = new Task<>() {
            @Override
            protected ViaCepResponse call() throws Exception {
                return viaCepService.buscarEnderecoPorCep(cep);
            }
        };

        buscaTask.setOnSucceeded(e -> processarResultadoBusca(buscaTask.getValue()));
        buscaTask.setOnFailed(e -> processarErroBusca(buscaTask.getException()));

        Thread thread = new Thread(buscaTask);
        thread.setDaemon(true); // Garante que a thread não impeça o fechamento do sistema
        thread.start();
    }

    private void processarResultadoBusca(ViaCepResponse enderecoEncontrado) {
        if (enderecoEncontrado != null) {
            preencherCamposEndereco(enderecoEncontrado);
        } else {
            System.out.println(MSG_ERRO_NAO_ENCONTRADO);
        }
    }

    private void processarErroBusca(Throwable excecao) {
        System.out.println("Erro ao comunicar com a API do ViaCEP.");
        if (excecao != null)
            excecao.printStackTrace();
    }

    private void preencherCamposEndereco(ViaCepResponse endereco) {
        txtLogradouro.setText(endereco.logradouro());
        txtBairro.setText(endereco.bairro());
        txtCidade.setText(endereco.localidade());
        selecionarUfSeguro(endereco.uf());
    }

    private void selecionarUfSeguro(String ufSigla) {
        if (isCepVazioOuNulo(ufSigla))
            return;

        try {
            comboUf.setValue(UfModel.valueOf(ufSigla.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            System.out.printf((MSG_ERRO_UF_INVALIDA) + "%n", ufSigla);
        }
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS (Contratos do Controller) E UTILITÁRIOS
    // =========================================================================
    public void carregarEndereco(EnderecoProponenteModel endereco) {
        viewModel.loadFromModel(endereco);
    }

    public void limparCampos() {
        viewModel.reset();
    }

    public EnderecoViewModel getViewModel() {
        return viewModel;
    }

    private boolean isCepVazioOuNulo(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String extrairSomenteNumeros(String valor) {
        return valor.replaceAll(REGEX_SOMENTE_NUMEROS, "");
    }
}