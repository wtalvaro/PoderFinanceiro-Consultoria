package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.EnderecoProponente;
import br.com.poderfinanceiro.app.model.Uf;
import br.com.poderfinanceiro.app.model.enums.TipoLogradouro;
import br.com.poderfinanceiro.app.utils.EnderecoUtils;
import br.com.poderfinanceiro.app.viewmodel.EnderecoViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype") // Garante uma instância nova para cada aba aberta
public class EnderecoController {

    // --- ELEMENTOS FXML ---
    @FXML
    private TextField txtCep;
    @FXML
    private ComboBox<TipoLogradouro> comboTipoLogradouro;
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
    private ComboBox<Uf> comboUf;

    private final EnderecoViewModel viewModel;

    /**
     * O Spring injeta a ViewModel via construtor.
     */
    public EnderecoController(EnderecoViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        configurarInterface();
        estabelecerBindings();
    }

    /**
     * Configurações iniciais de combos e máscaras.
     */
    private void configurarInterface() {
        // Preenche as ComboBoxes com os Enums que você já tem no Model
        comboTipoLogradouro.setItems(FXCollections.observableArrayList(TipoLogradouro.values()));
        comboUf.setItems(FXCollections.observableArrayList(Uf.values()));

        // Aplica a máscara de CEP do seu EnderecoUtils (00.000-000)
        txtCep.setTextFormatter(EnderecoUtils.criarFormatadorCep());
    }

    /**
     * Vincula as propriedades da ViewModel aos campos da tela.
     */
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

    /**
     * Ação disparada pelo botão de busca na tela.
     */
    @FXML
    private void buscarCep() {
        String cepDigitado = viewModel.cepProperty().get().replaceAll("[^0-9]", "");

        if (cepDigitado.length() == 8) {
            // No futuro, aqui chamaremos o serviço do ViaCEP
            System.out.println("Solicitando busca para o CEP: " + cepDigitado);
        } else {
            // Aqui você pode usar o método de exibir mensagem que já temos no projeto
            System.out.println("CEP Inválido");
        }
    }

    // --- MÉTODOS DE PONTE PARA O HUB ---

    public void carregarEndereco(EnderecoProponente endereco) {
        viewModel.loadFromModel(endereco);
    }

    public void limparCampos() {
        viewModel.reset();
    }

    public EnderecoViewModel getViewModel() {
        return viewModel;
    }
}