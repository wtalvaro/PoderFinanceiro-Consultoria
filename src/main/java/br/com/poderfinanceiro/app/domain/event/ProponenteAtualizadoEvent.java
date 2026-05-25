package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ProponenteAtualizadoEvent(Long idProponente) {

    private static final Logger log = LoggerFactory.getLogger(ProponenteAtualizadoEvent.class);

    public ProponenteAtualizadoEvent {
        log.debug("[EVENT] ProponenteAtualizadoEvent criado para ID: {}", idProponente);
    }
}