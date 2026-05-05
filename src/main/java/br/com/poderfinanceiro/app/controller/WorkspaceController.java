package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceController {

    @FXML
    private TabPane tabPanePrincipal;

    public WorkspaceController(ApplicationContext context) {
    }
}