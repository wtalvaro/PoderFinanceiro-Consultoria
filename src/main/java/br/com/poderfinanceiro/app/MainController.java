package br.com.poderfinanceiro.app;

import br.com.poderfinanceiro.app.model.Proposta;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    // Tabela da Aba 1
    @FXML
    private TableView<Proposta> tabelaPropostas;

    // Formulário da Aba 2
    @FXML
    private ComboBox<String> comboProponente;
    @FXML
    private ComboBox<String> comboBanco;
    @FXML
    private ComboBox<String> comboTabela;
    @FXML
    private TextField txtValorSolicitado;
    @FXML
    private Spinner<Integer> spinnerParcelas;

    @FXML
    public void initialize() {
        // Inicialização dos componentes (Spinner)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 96, 72);
        spinnerParcelas.setValueFactory(valueFactory);
    }
}