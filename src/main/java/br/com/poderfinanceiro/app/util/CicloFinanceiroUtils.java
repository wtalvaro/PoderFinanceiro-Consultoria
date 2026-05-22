package br.com.poderfinanceiro.app.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

/**
 * Utilitário responsável pelo Motor de Estados Temporal do sistema.
 * Define os ciclos financeiros com corte rígido na Quarta-feira às 18:00.
 */
public class CicloFinanceiroUtils {

    /**
     * Retorna o ID do Ciclo (Ex: 2026-W20) baseado na data da operação.
     * * @param dataOperacao Data e hora em que a proposta foi aprovada/paga.
     * 
     * @return String no formato YYYY-Www.
     */
    public static String identificarCiclo(LocalDateTime dataOperacao) {
        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);

        // Extrai o ano e a semana baseados na norma ISO (garante consistência no final
        // do ano)
        int ano = quartaFeiraDeFechamento.get(WeekFields.ISO.weekBasedYear());
        int semana = quartaFeiraDeFechamento.get(WeekFields.ISO.weekOfWeekBasedYear());

        return String.format("%04d-W%02d", ano, semana);
    }

    /**
     * Define o prazo máximo que o consultor tem para contestar (Quinta-feira às
     * 15:00).
     * * @param dataOperacao Data e hora original da operação.
     * 
     * @return Timestamp exato do limite para o botão de "conferido" funcionar.
     */
    public static LocalDateTime calcularLimiteContestacao(LocalDateTime dataOperacao) {
        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);

        // O prazo é sempre o dia seguinte (Quinta-feira) após a Quarta de fechamento,
        // às 15:00
        return quartaFeiraDeFechamento.plusDays(1)
                .withHour(15)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Calcula a Sexta-feira de liquidação (Pagamento) correspondente ao ciclo.
     * É sempre 2 dias após a Quarta-feira de fechamento do ciclo atual.
     * * @param dataOperacao Data e hora original da operação.
     * 
     * @return LocalDateTime apontando para a Sexta-feira correta.
     */
    public static LocalDateTime calcularSextaDePagamento(LocalDateTime dataOperacao) {
        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);

        // O pagamento é sempre na Sexta-feira (Quarta + 2 dias)
        return quartaFeiraDeFechamento.plusDays(2)
                .withHour(18) // Fim do expediente
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Lógica central da janela de corte. Encontra a Quarta-feira exata a qual
     * esta operação pertence financeiramente.
     */
    public static LocalDateTime obterQuartaDeFechamento(LocalDateTime dataOperacao) {
        DayOfWeek diaDaSemana = dataOperacao.getDayOfWeek();

        // Se for Segunda, Terça, ou Quarta ANTES das 18:00 -> O ciclo fecha nesta mesma
        // semana.
        if (diaDaSemana == DayOfWeek.MONDAY ||
                diaDaSemana == DayOfWeek.TUESDAY ||
                (diaDaSemana == DayOfWeek.WEDNESDAY && dataOperacao.getHour() < 18)) {

            return dataOperacao.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        }
        // Se for Quarta (18:00+), Quinta, Sexta, Sábado ou Domingo -> Pula para a
        // PRÓXIMA Quarta-feira.
        else {
            return dataOperacao.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        }
    }
}