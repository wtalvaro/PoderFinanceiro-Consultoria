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
        log.debug("[STATUS_BAR] Construtor: Controller instanciado");
    }

    @FXML
    public void initialize() {
        log.debug("[STATUS_BAR] initialize: Iniciando configuração da barra de status");
        log.info("[STATUS_BAR] StatusBarController carregado com sucesso!");

        atualizarStatusUsuario();
        log.info("[STATUS_BAR] initialize: Configuração concluída");
    }

    /**
     * Atualiza o texto da barra de status com o nome do consultor logado
     */
    public void atualizarStatusUsuario() {
        log.debug("[STATUS_BAR] atualizarStatusUsuario: Verificando estado da autenticação");
        if (authService.estaLogado() && lblUsuario != null) {
            String nomeConsultor = authService.getUsuarioLogado().getNome();
            lblUsuario.setText("👤 Consultor: " + nomeConsultor);
            log.info("[STATUS_BAR] Usuário logado: '{}'", nomeConsultor);
        } else if (lblUsuario != null) {
            lblUsuario.setText("👤 Usuário não identificado");
            log.warn("[STATUS_BAR] Usuário não logado ou não identificado. lblUsuario disponível? {}",
                    lblUsuario != null);
        } else {
            log.error("[STATUS_BAR] lblUsuario está nulo, não foi possível atualizar a barra de status");
        }
    }
}