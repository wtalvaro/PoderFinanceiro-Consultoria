package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PropostaAtualizadaEvent(Long idProposta) {

    private static final Logger log = LoggerFactory.getLogger(PropostaAtualizadaEvent.class);

    public PropostaAtualizadaEvent {
        log.debug("[EVENT] PropostaAtualizadaEvent criado para ID: {}", idProposta);
    }
}