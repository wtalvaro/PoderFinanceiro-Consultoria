package br.com.poderfinanceiro.app.application.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PlaybookItemDTO(
                String categoria,
                String titulo,
                String conteudo,
                String dica) {

        private static final Logger log = LoggerFactory.getLogger(PlaybookItemDTO.class);

        public PlaybookItemDTO {
                log.debug("[PLAYBOOK_ITEM_DTO] Criado: categoria='{}', titulo='{}'", categoria, titulo);
        }
}