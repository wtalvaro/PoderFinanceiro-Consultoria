package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.ViaCepService;
import br.com.poderfinanceiro.app.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.model.enums.UfModel;
import br.com.poderfinanceiro.app.utils.EnderecoUtils;
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

    private final EnderecoViewModel viewModel;
    private final ViaCepService viaCepService;

    public EnderecoController(EnderecoViewModel viewModel, ViaCepService viaCepService) {
        this.viewModel = viewModel;
        this.viaCepService = viaCepService;
    }

    @FXML
    public void initialize() {
        configurarInterface();
        estabelecerBindings();
        // ❌ O ouvinte (Listener) automático foi removido daqui para evitar disparos
        // acidentais
    }

    // 🎯 O GATILHO REAPROVEITADO (Botão e ENTER)
    @FXML
    private void buscarCep() {
        String cepDigitado = txtCep.getText();

        if (cepDigitado != null && !cepDigitado.trim().isEmpty()) {
            String apenasNumeros = cepDigitado.replaceAll("\\D", "");

            if (apenasNumeros.length() == 8) {
                System.out.println("Solicitando busca para o CEP: " + apenasNumeros);
                buscarEPreencherCep(apenasNumeros);
            } else {
                System.out.println("⚠️ O CEP deve conter exatamente 8 números.");
            }
        }
    }

    private void buscarEPreencherCep(String cepDigitado) {
        Task<ViaCepResponse> buscaTask = new Task<>() {
            @Override
            protected ViaCepResponse call() throws Exception {
                return viaCepService.buscarEnderecoPorCep(cepDigitado);
            }
        };

        buscaTask.setOnSucceeded(e -> {
            ViaCepResponse enderecoEncontrado = buscaTask.getValue();

            if (enderecoEncontrado != null) {
                txtLogradouro.setText(enderecoEncontrado.logradouro());
                txtBairro.setText(enderecoEncontrado.bairro());
                txtCidade.setText(enderecoEncontrado.localidade());

                try {
                    comboUf.setValue(UfModel.valueOf(enderecoEncontrado.uf().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    System.out.println("UF não encontrada no Enum: " + enderecoEncontrado.uf());
                }

                // txtNumero.requestFocus();
            } else {
                System.out.println("CEP Inválido ou não encontrado no ViaCEP.");
            }
        });

        new Thread(buscaTask).start();
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

    public void carregarEndereco(EnderecoProponenteModel endereco) {
        viewModel.loadFromModel(endereco);
    }

    public void limparCampos() {
        viewModel.reset();
    }

    public EnderecoViewModel getViewModel() {
        return viewModel;
    }
}