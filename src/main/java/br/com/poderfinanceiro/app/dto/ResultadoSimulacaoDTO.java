package br.com.poderfinanceiro.app.dto;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import java.math.BigDecimal;

public record ResultadoSimulacaoDTO(
                TabelaJurosModel tabela,
                BigDecimal comissaoEstimada,
                BigDecimal valorParcela) {
}