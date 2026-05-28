package br.com.poderfinanceiro.app.application.dto;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ResultadoSimulacaoDTO(
        TabelaJurosModel tabela,
        BigDecimal comissaoEstimada,
        BigDecimal valorParcela) {

    private static final Logger log = LoggerFactory.getLogger(ResultadoSimulacaoDTO.class);

    public ResultadoSimulacaoDTO {
        log.debug("[RESULTADO_SIMULACAO_DTO] Criado: banco='{}', comissao={}, parcela={}",
                tabela != null && tabela.getBanco() != null ? tabela.getBanco().getNome() : "N/A",
                comissaoEstimada, valorParcela);
    }
}