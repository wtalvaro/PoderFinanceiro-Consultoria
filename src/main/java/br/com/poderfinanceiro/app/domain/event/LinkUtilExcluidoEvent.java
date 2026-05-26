package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record LinkUtilExcluidoEvent(Long idLink) {
    private static final Logger log = LoggerFactory.getLogger(LinkUtilExcluidoEvent.class);

    public LinkUtilExcluidoEvent {
        log.debug("[EVENT] LinkUtilExcluidoEvent criado para ID: {}", idLink);
    }
}