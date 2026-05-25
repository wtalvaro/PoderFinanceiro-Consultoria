package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record BancoExcluidoEvent(Long idBanco) {
    private static final Logger log = LoggerFactory.getLogger(BancoExcluidoEvent.class);

    public BancoExcluidoEvent {
        log.debug("[EVENT] BancoExcluidoEvent criado para ID: {}", idBanco);
    }
}