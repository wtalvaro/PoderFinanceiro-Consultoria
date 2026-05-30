package br.com.poderfinanceiro.app.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

/**
 * Utilitário responsável pelo Motor de Estados Temporal do sistema.
 * Orquestra os ciclos financeiros da Poder Financeiro.
 * 
 * <p>
 * Regras de Negócio:
 * </p>
 * <ul>
 * <li><b>Corte do Ciclo:</b> Quarta-feira às 23:59:59.</li>
 * <li><b>Limite de Contestação:</b> Quinta-feira às 15:00:00 (Regra da
 * Correspondente).</li>
 * <li><b>Liquidação (Pagamento):</b> Sexta-feira às 18:00:00.</li>
 * </ul>
 */
public class CicloFinanceiroUtils {

    private static final Logger log = LoggerFactory.getLogger(CicloFinanceiroUtils.class);
    private static final String LOG_PREFIX = "[CicloFinanceiroUtils]";

    /**
     * Retorna o ID do Ciclo (Ex: 2026-W20) baseado na data da operação.
     *
     * @param dataOperacao Data e hora em que a proposta foi aprovada/paga.
     * @return String no formato YYYY-Www.
     */
    public static String identificarCiclo(LocalDateTime dataOperacao) {
        log.debug("{} [TELEMETRIA] Identificando ciclo para a data: {}", LOG_PREFIX, dataOperacao);

        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);

        int ano = quartaFeiraDeFechamento.get(WeekFields.ISO.weekBasedYear());
        int semana = quartaFeiraDeFechamento.get(WeekFields.ISO.weekOfWeekBasedYear());

        String ciclo = String.format("%04d-W%02d", ano, semana);
        log.info("{} [NEGOCIO] Ciclo identificado: {} para a operação de {}", LOG_PREFIX, ciclo, dataOperacao);
        return ciclo;
    }

    /**
     * Define o prazo máximo que o consultor tem para contestar valores.
     * Conforme regra da Poder Financeiro, o prazo expira na Quinta-feira às
     * 15:00:00.
     *
     * @param dataOperacao Data e hora original da operação.
     * @return Timestamp exato do limite para contestação.
     */
    public static LocalDateTime calcularLimiteContestacao(LocalDateTime dataOperacao) {
        log.debug("{} [TELEMETRIA] Calculando limite de contestação para: {}", LOG_PREFIX, dataOperacao);

        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);

        // Regra Específica: Quinta-feira subsequente à quarta de fechamento às 15:00:00
        LocalDateTime limite = quartaFeiraDeFechamento.plusDays(1)
                .withHour(15)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        log.info("{} [NEGOCIO] Limite de contestação definido: {}", LOG_PREFIX, limite);
        return limite;
    }

    /**
     * Calcula a Sexta-feira de liquidação (Pagamento) correspondente ao ciclo.
     * É sempre 2 dias após a Quarta-feira de fechamento do ciclo atual.
     *
     * @param dataOperacao Data e hora original da operação.
     * @return LocalDateTime apontando para a Sexta-feira às 18:00:00.
     */
    public static LocalDateTime calcularSextaDePagamento(LocalDateTime dataOperacao) {
        log.debug("{} [TELEMETRIA] Calculando data de pagamento para: {}", LOG_PREFIX, dataOperacao);

        LocalDateTime quartaFeiraDeFechamento = obterQuartaDeFechamento(dataOperacao);

        // Pagamento ocorre na Sexta-feira às 18:00:00
        LocalDateTime pagamento = quartaFeiraDeFechamento.plusDays(2)
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        log.info("{} [AUDITORIA] Previsão de pagamento calculada: {}", LOG_PREFIX, pagamento);
        return pagamento;
    }

    /**
     * Lógica central da janela de corte (Deadline).
     * Encontra a Quarta-feira de fechamento à qual esta operação pertence.
     * 
     * <p>
     * Refatorado para o corte de 23:59:59.
     * </p>
     */
    public static LocalDateTime obterQuartaDeFechamento(LocalDateTime dataOperacao) {
        if (dataOperacao == null) {
            log.warn("{} [SISTEMA] Data de operação nula recebida. Utilizando data atual.", LOG_PREFIX);
            dataOperacao = LocalDateTime.now();
        }

        log.trace("{} [TELEMETRIA] Determinando quarta-feira de fechamento. Entrada: {}", LOG_PREFIX, dataOperacao);

        // Se for de Segunda a Quarta, retorna a Quarta desta semana.
        // Se for de Quinta a Domingo, retorna a Quarta da próxima semana.
        LocalDateTime quarta = dataOperacao.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999999999);

        log.debug("{} [NEGOCIO] Fechamento do ciclo determinado para: {}", LOG_PREFIX, quarta);
        return quarta;
    }
}
