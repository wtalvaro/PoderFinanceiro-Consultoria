package br.com.poderfinanceiro.app.dto;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        private static final Logger log = LoggerFactory.getLogger(SimulacaoRascunhoDTO.class);

        public SimulacaoRascunhoDTO {
                log.debug("[SIMULACAO_RASCUNHO_DTO] Criado: idade={}, renda={}, convenio='{}', valorDesejado={}, prazo={}, margem={}",
                                idade, rendaMensal, tipoConvenio, valorDesejado, prazoDesejado, margemDisponivel);
        }
}