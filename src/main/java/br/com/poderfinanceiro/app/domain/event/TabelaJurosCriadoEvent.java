package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record TabelaJurosCriadoEvent(Long idTabela) {
    private static final Logger log = LoggerFactory.getLogger(TabelaJurosCriadoEvent.class);

    public TabelaJurosCriadoEvent {
        log.debug("[EVENT] TabelaJurosCriadoEvent criado para ID: {}", idTabela);
    }
}