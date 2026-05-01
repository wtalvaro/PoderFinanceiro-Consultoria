package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component; // <-- Add this import
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

@Component // <-- Add this annotation
public class DashboardController {

    @FXML
    private TableView<?> tabelaPropostas;

    @FXML
    public void initialize() {
        // Lógica de carregamento da tabela aqui
    }
}