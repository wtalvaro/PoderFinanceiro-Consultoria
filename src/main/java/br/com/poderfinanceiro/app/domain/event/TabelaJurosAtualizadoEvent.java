package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record TabelaJurosAtualizadoEvent(Long idTabela) {
    private static final Logger log = LoggerFactory.getLogger(TabelaJurosAtualizadoEvent.class);

    public TabelaJurosAtualizadoEvent {
        log.debug("[EVENT] TabelaJurosAtualizadoEvent criado para ID: {}", idTabela);
    }
}