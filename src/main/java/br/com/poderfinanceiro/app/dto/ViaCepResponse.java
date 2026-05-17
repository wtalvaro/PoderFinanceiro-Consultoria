package br.com.poderfinanceiro.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignora campos que a API mandar e nós não quisermos (ex: ibge, gia, ddd)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade, // A API chama 'cidade' de 'localidade'
        String uf,
        Boolean erro // A API devolve { "erro": true } se o CEP não existir
) {
}