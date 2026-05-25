package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record TabelaJurosArquivadoEvent(Long idTabela) {
    private static final Logger log = LoggerFactory.getLogger(TabelaJurosArquivadoEvent.class);

    public TabelaJurosArquivadoEvent {
        log.debug("[EVENT] TabelaJurosArquivadoEvent criado para ID: {}", idTabela);
    }
}