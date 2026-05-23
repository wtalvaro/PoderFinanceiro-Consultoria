package br.com.poderfinanceiro.app.dto;

import java.math.BigDecimal;

/**
 * Representa os dados mínimos necessários para o sistema (e a IA)
 * encontrarem a melhor tabela e banco, sem precisar cadastrar o cliente.
 */
public record SimulacaoRascunhoDTO(
        Integer idade,
        BigDecimal rendaMensal,
        String tipoConvenio, // Ex: INSS, SIAPE, FORÇAS ARMADAS
        BigDecimal valorDesejado,
        Integer prazoDesejado,
        BigDecimal margemDisponivel) {
}