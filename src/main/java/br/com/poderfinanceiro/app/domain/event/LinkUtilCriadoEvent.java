package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record LinkUtilCriadoEvent(Long idLink) {
    private static final Logger log = LoggerFactory.getLogger(LinkUtilCriadoEvent.class);

    public LinkUtilCriadoEvent {
        log.debug("[EVENT] LinkUtilCriadoEvent criado para ID: {}", idLink);
    }
}