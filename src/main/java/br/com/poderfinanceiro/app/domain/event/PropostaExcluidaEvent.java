package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PropostaExcluidaEvent(Long idProposta) {

    private static final Logger log = LoggerFactory.getLogger(PropostaExcluidaEvent.class);

    public PropostaExcluidaEvent {
        log.debug("[EVENT] PropostaExcluidaEvent criado para ID: {}", idProposta);
    }
}