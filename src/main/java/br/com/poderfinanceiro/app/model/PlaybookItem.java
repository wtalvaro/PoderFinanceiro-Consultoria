package br.com.poderfinanceiro.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaybookItem {
    private String categoria;
    private String titulo;
    private String conteudo;
    private String dica;
}