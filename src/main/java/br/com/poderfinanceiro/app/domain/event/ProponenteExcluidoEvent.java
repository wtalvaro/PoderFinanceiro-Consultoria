package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ProponenteExcluidoEvent(Long idProponente) {

    private static final Logger log = LoggerFactory.getLogger(ProponenteExcluidoEvent.class);

    public ProponenteExcluidoEvent {
        log.debug("[EVENT] ProponenteExcluidoEvent criado para ID: {}", idProponente);
    }
}