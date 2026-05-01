package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

@Component
public class SimulatorController {

    // As variáveis que vieram do simulator.fxml
    @FXML
    private ComboBox<?> comboProponente;
    @FXML
    private ComboBox<?> comboBanco;
    @FXML
    private ComboBox<?> comboTabela;
    @FXML
    private TextField txtValorSolicitado;

    // Aqui está o Spinner que estava causando o erro!
    @FXML
    private Spinner<Integer> spinnerParcelas;

    @FXML
    public void initialize() {
        // A lógica de configuração agora mora no lugar certo!
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 84);

        spinnerParcelas.setValueFactory(valueFactory);
    }
}