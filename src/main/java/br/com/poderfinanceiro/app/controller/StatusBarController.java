package br.com.poderfinanceiro.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.domain.service.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StatusBarController {

    private static final Logger log = LoggerFactory.getLogger(StatusBarController.class);
    
    private final AuthService authService;

    @FXML
    private Label lblUsuario;

    // Injeção de dependência via construtor para o Spring
    public StatusBarController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        log.info("StatusBarController carregado com sucesso!");
        
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