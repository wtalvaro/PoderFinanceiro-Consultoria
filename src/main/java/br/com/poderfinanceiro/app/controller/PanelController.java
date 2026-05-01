package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;

@Component
public class PanelController {

    @FXML
    public void initialize() {
        // Lógica da barra lateral de navegação e da busca rápida
        System.out.println("PanelController carregado com sucesso!");
    }
}