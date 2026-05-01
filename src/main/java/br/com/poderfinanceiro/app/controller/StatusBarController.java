package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class StatusBarController {

    private final AuthService authService;

    @FXML
    private Label lblUsuario;

    // Injeção de dependência via construtor para o Spring
    public StatusBarController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        System.out.println("StatusBarController carregado com sucesso!");
        
        atualizarStatusUsuario();
    }

    /**
     * Atualiza o texto da barra de status com o nome do consultor logado
     */
    public void atualizarStatusUsuario() {
        if (authService.estaLogado() && lblUsuario != null) {
            String nomeConsultor = authService.getUsuarioLogado().getNome();
            lblUsuario.setText("👤 Consultor: " + nomeConsultor);
        } else if (lblUsuario != null) {
            lblUsuario.setText("👤 Usuário não identificado");
        }
    }
}