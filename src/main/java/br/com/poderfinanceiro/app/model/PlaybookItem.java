package br.com.poderfinanceiro.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Gera Getters, Setters, equals, hashCode e toString automaticamente
@NoArgsConstructor // Necessário para frameworks como Hibernate/Jackson
@AllArgsConstructor // Gera o construtor com todos os campos
public class PlaybookItem {
    private String categoria; // Ex: "Bolsa Família"
    private String titulo; // Ex: "Abordagem Inicial"[cite: 2]
    private String conteudo; // O script em si[cite: 2]
    private String dicaTecnica; // Regras como "mínimo R$ 100 por parcela"[cite: 2]
}