package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import java.math.BigDecimal;
import java.util.List;

public interface IDashboardFacade {

    // --- DTO de Transporte ---
    record MetricasDashboardDTO(List<PropostaModel> propostas, long qtdAguardando, BigDecimal volumeAprovado, BigDecimal comissaoPendente,
            BigDecimal comissaoPaga) {
    }

    // --- Consultas e Cálculos ---
    MetricasDashboardDTO calcularMetricasGerais();

    String obterNomeConsultorLogado();

    // --- Regras de Negócio e Filtros ---
    List<PropostaModel> filtrarPropostas(List<PropostaModel> propostas, String termoBusca);
}
