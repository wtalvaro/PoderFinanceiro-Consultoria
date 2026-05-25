package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record BancoCriadoEvent(Long idBanco) {
    private static final Logger log = LoggerFactory.getLogger(BancoCriadoEvent.class);

    public BancoCriadoEvent {
        log.debug("[EVENT] BancoCriadoEvent criado para ID: {}", idBanco);
    }
}