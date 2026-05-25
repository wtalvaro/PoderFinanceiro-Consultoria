package br.com.poderfinanceiro.app.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

/**
 * Utilitário responsável pelo Motor de Estados Temporal do sistema.
 * Define os ciclos financeiros com corte rígido na Quarta-feira às 18:00.
 */
public class CicloFinanceiroUtils {

    private static final Logger log = LoggerFactory.getLogger(CicloFinanceiroUtils.class);

    /**
     * Retorna o ID do Ciclo (Ex: 2026-W20) baseado na data da operação.
     *
     * @param dataOperacao Data e hora em que a proposta foi aprovada/paga.
     * @return String no formato YYYY-Www.
     */
    public static String identificarCiclo(LocalDateTime dataOperacao) {
        log.debug("[CICLO_FINANCEIRO] identificarCiclo: dataOperacao={}", dataOperacao);
        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);
        log.trace("[CICLO_FINANCEIRO] Quarta-feira de fechamento calculada: {}", quartaFeiraDeFechamento);

        int ano = quartaFeiraDeFechamento.get(WeekFields.ISO.weekBasedYear());
        int semana = quartaFeiraDeFechamento.get(WeekFields.ISO.weekOfWeekBasedYear());

        String ciclo = String.format("%04d-W%02d", ano, semana);
        log.info("[CICLO_FINANCEIRO] Ciclo identificado: {} (baseado em {})", ciclo, dataOperacao);
        return ciclo;
    }

    /**
     * Define o prazo máximo que o consultor tem para contestar (Quinta-feira às
     * 15:00).
     *
     * @param dataOperacao Data e hora original da operação.
     * @return Timestamp exato do limite para o botão de "conferido" funcionar.
     */
    public static LocalDateTime calcularLimiteContestacao(LocalDateTime dataOperacao) {
        log.debug("[CICLO_FINANCEIRO] calcularLimiteContestacao: dataOperacao={}", dataOperacao);
        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);
        log.trace("[CICLO_FINANCEIRO] Quarta-feira de fechamento: {}", quartaFeiraDeFechamento);

        LocalDateTime limite = quartaFeiraDeFechamento.plusDays(1)
                .withHour(15)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        log.info("[CICLO_FINANCEIRO] Limite de contestação calculado: {}", limite);
        return limite;
    }

    /**
     * Calcula a Sexta-feira de liquidação (Pagamento) correspondente ao ciclo.
     * É sempre 2 dias após a Quarta-feira de fechamento do ciclo atual.
     *
     * @param dataOperacao Data e hora original da operação.
     * @return LocalDateTime apontando para a Sexta-feira correta.
     */
    public static LocalDateTime calcularSextaDePagamento(LocalDateTime dataOperacao) {
        log.debug("[CICLO_FINANCEIRO] calcularSextaDePagamento: dataOperacao={}", dataOperacao);
        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);
        log.trace("[CICLO_FINANCEIRO] Quarta-feira de fechamento: {}", quartaFeiraDeFechamento);

        LocalDateTime pagamento = quartaFeiraDeFechamento.plusDays(2)
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        log.info("[CICLO_FINANCEIRO] Sexta de pagamento calculada: {}", pagamento);
        return pagamento;
    }

    /**
     * Lógica central da janela de corte. Encontra a Quarta-feira exata a qual
     * esta operação pertence financeiramente.
     */
    public static LocalDateTime obterQuartaDeFechamento(LocalDateTime dataOperacao) {
        log.debug("[CICLO_FINANCEIRO] obterQuartaDeFechamento: dataOperacao={}", dataOperacao);
        DayOfWeek diaDaSemana = dataOperacao.getDayOfWeek();
        log.trace("[CICLO_FINANCEIRO] Dia da semana: {}, hora: {}", diaDaSemana, dataOperacao.getHour());

        LocalDateTime quarta;

        if (diaDaSemana == DayOfWeek.MONDAY ||
                diaDaSemana == DayOfWeek.TUESDAY ||
                (diaDaSemana == DayOfWeek.WEDNESDAY && dataOperacao.getHour() < 18)) {

            quarta = dataOperacao.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
            log.debug("[CICLO_FINANCEIRO] Caso: Segunda/Terça/Quarta antes das 18h. Quarta de fechamento = {}", quarta);
        } else {
            quarta = dataOperacao.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
            log.debug(
                    "[CICLO_FINANCEIRO] Caso: Quarta pós 18h ou Quinta/Sexta/Sábado/Domingo. Quarta de fechamento = {}",
                    quarta);
        }

        return quarta;
    }
}