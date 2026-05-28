package br.com.poderfinanceiro.app.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        private static final Logger log = LoggerFactory.getLogger(ViaCepResponse.class);

        public ViaCepResponse {
                log.debug("[VIA_CEP_RESPONSE] Criado: cep={}, logradouro={}, localidade={}, uf={}, erro={}",
                                cep, logradouro, localidade, uf, erro);
        }
}