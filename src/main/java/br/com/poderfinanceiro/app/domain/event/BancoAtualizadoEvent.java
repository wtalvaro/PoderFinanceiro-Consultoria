package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record BancoAtualizadoEvent(Long idBanco) {
    private static final Logger log = LoggerFactory.getLogger(BancoAtualizadoEvent.class);

    public BancoAtualizadoEvent {
        log.debug("[EVENT] BancoAtualizadoEvent criado para ID: {}", idBanco);
    }
}