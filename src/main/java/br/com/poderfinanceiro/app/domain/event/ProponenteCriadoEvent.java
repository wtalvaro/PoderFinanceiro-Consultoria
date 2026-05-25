package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ProponenteCriadoEvent(Long idProponente) {

    private static final Logger log = LoggerFactory.getLogger(ProponenteCriadoEvent.class);

    public ProponenteCriadoEvent {
        log.debug("[EVENT] ProponenteCriadoEvent criado para ID: {}", idProponente);
    }
}