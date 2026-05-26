package br.com.poderfinanceiro.app.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ComissaoCriadaEvent(Long idComissao) {
    private static final Logger log = LoggerFactory.getLogger(ComissaoCriadaEvent.class);

    public ComissaoCriadaEvent {
        log.debug("[EVENT] ComissaoCriadaEvent criado para ID: {}", idComissao);
    }
}