package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ComissaoAtualizadaEvent(Long idComissao) {
    private static final Logger log = LoggerFactory.getLogger(ComissaoAtualizadaEvent.class);

    public ComissaoAtualizadaEvent {
        log.debug("[EVENT] ComissaoAtualizadaEvent criado para ID: {}", idComissao);
    }
}