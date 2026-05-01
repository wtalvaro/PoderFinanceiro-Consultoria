package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;

@Component
public class ToolbarController {

    @FXML
    public void initialize() {
        // Lógica dos botões da barra de ferramentas (Novo Lead, HISCON, etc.)
        System.out.println("ToolbarController carregado com sucesso!");
    }
}