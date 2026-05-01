package br.com.poderfinanceiro.app.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;

@Component
public class StatusBarController {

    @FXML
    public void initialize() {
        // Lógica para monitorar o status do banco de dados, alertas e usuário atual
        System.out.println("StatusBarController carregado com sucesso!");
    }
}