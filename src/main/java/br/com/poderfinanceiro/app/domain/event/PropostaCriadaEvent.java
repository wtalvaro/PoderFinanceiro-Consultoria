package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PropostaCriadaEvent(Long idProposta) {

    private static final Logger log = LoggerFactory.getLogger(PropostaCriadaEvent.class);

    public PropostaCriadaEvent {
        log.debug("[EVENT] PropostaCriadaEvent criado para ID: {}", idProposta);
    }
}