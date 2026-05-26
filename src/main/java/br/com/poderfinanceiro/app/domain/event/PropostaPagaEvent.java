package br.com.poderfinanceiro.app.domain.event;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PropostaPagaEvent(PropostaModel proposta) {
    private static final Logger log = LoggerFactory.getLogger(PropostaPagaEvent.class);

    public PropostaPagaEvent {
        log.info("[EVENT] PropostaPagaEvent disparado para Proposta ID={}",
                proposta != null ? proposta.getId() : "null");
    }
}