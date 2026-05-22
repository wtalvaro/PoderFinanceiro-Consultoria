package br.com.poderfinanceiro.app.domain.model;

public record PlaybookItemDTO(
        String categoria,
        String titulo,
        String conteudo,
        String dica) {
}