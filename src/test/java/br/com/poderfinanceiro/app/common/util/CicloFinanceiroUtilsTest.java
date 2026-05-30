package br.com.poderfinanceiro.app.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Common] Teste de Unidade - CicloFinanceiroUtils")
class CicloFinanceiroUtilsTest {

    @Test
    @DisplayName("Deve identificar ciclo corretamente para uma quarta-feira antes do fechamento")
    void deveIdentificarCicloQuartaFeira() {
        // GIVEN: Quarta-feira, 20 de Maio de 2026, às 10:00
        LocalDateTime quartaManha = LocalDateTime.of(2026, 5, 20, 10, 0);

        // WHEN
        String ciclo = CicloFinanceiroUtils.identificarCiclo(quartaManha);

        // THEN: Deve ser a semana 21 de 2026
        assertThat(ciclo).isEqualTo("2026-W21");
    }

    @Test
    @DisplayName("Corte de Ciclo: Quarta 23:59:59 deve pertencer ao ciclo atual")
    void deveManterCicloNoUltimoSegundoDeQuarta() {
        // GIVEN: Quarta-feira no último segundo
        LocalDateTime limiteQuarta = LocalDateTime.of(2026, 5, 20, 23, 59, 59);

        // WHEN
        LocalDateTime fechamento = CicloFinanceiroUtils.obterQuartaDeFechamento(limiteQuarta);

        // THEN: O fechamento deve ser no próprio dia 20
        assertThat(fechamento.getDayOfMonth()).isEqualTo(20);
        assertThat(fechamento.getHour()).isEqualTo(23);
    }

    @Test
    @DisplayName("Corte de Ciclo: Quinta 00:00:01 deve pertencer ao próximo ciclo")
    void deveMudarCicloNoPrimeiroSegundoDeQuinta() {
        // GIVEN: Quinta-feira logo após a meia-noite
        LocalDateTime inicioQuinta = LocalDateTime.of(2026, 5, 21, 0, 0, 1);

        // WHEN
        LocalDateTime fechamento = CicloFinanceiroUtils.obterQuartaDeFechamento(inicioQuinta);

        // THEN: O fechamento deve ser na próxima quarta, dia 27
        assertThat(fechamento.getDayOfMonth()).isEqualTo(27);
        assertThat(fechamento.getHour()).isEqualTo(23);
    }

    @Test
    @DisplayName("Regra Poder Financeiro: Limite de contestação deve ser Quinta às 15:00")
    void deveCalcularLimiteContestacaoQuintaAs15h() {
        // GIVEN: Operação realizada na Terça-feira
        LocalDateTime terca = LocalDateTime.of(2026, 5, 19, 14, 0);

        // WHEN
        LocalDateTime limite = CicloFinanceiroUtils.calcularLimiteContestacao(terca);

        // THEN: Limite deve ser Quinta, dia 21, às 15:00
        assertThat(limite.getDayOfWeek().name()).isEqualTo("THURSDAY");
        assertThat(limite.getDayOfMonth()).isEqualTo(21);
        assertThat(limite.getHour()).isEqualTo(15);
        assertThat(limite.getMinute()).isZero();
    }

    @Test
    @DisplayName("Liquidação: Pagamento deve ser na Sexta-feira às 18:00")
    void deveCalcularPagamentoSextaAs18h() {
        // GIVEN: Operação na Quarta-feira
        LocalDateTime quarta = LocalDateTime.of(2026, 5, 20, 15, 0);

        // WHEN
        LocalDateTime pagamento = CicloFinanceiroUtils.calcularSextaDePagamento(quarta);

        // THEN: Pagamento na Sexta, dia 22, às 18:00
        assertThat(pagamento.getDayOfWeek().name()).isEqualTo("FRIDAY");
        assertThat(pagamento.getDayOfMonth()).isEqualTo(22);
        assertThat(pagamento.getHour()).isEqualTo(18);
    }

    @Test
    @DisplayName("Virada de Ano: Deve tratar corretamente a transição de ciclos em Dezembro")
    void deveTratarViradaDeAno() {
        // GIVEN: Quarta-feira, 31 de Dezembro de 2025
        LocalDateTime ultimoDiaAno = LocalDateTime.of(2025, 12, 31, 10, 0);

        // WHEN
        String ciclo = CicloFinanceiroUtils.identificarCiclo(ultimoDiaAno);

        // THEN: Pela regra ISO, 31/12/2025 é a semana 01 de 2026
        assertThat(ciclo).isEqualTo("2026-W01");
    }

    @Test
    @DisplayName("Robustez: Deve lidar com data nula usando a data atual")
    void deveLidarComDataNula() {
        // WHEN & THEN: Não deve lançar exceção
        LocalDateTime fechamento = CicloFinanceiroUtils.obterQuartaDeFechamento(null);
        assertThat(fechamento).isNotNull();
        assertThat(fechamento.getDayOfWeek().name()).isEqualTo("WEDNESDAY");
    }
}
