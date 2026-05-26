package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record LinkUtilAtualizadoEvent(Long idLink) {
    private static final Logger log = LoggerFactory.getLogger(LinkUtilAtualizadoEvent.class);

    public LinkUtilAtualizadoEvent {
        log.debug("[EVENT] LinkUtilAtualizadoEvent criado para ID: {}", idLink);
    }
}