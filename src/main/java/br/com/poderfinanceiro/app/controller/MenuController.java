package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;

@Component
public class MenuController {

    @FXML
    public void initialize() {
        // Lógica dos menus suspensos e atalhos de teclado (Ctrl+N, Ctrl+S)
        System.out.println("MenuController carregado com sucesso!");
    }
}