package br.com.poderfinanceiro.app.common.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitário para manipulação de valores monetários e inteiros.
 * Otimizada para Project Loom via ThreadLocal para garantir thread-safety
 * sem bloqueios (lock-free) na formatação de números.
 */
public final class FinanceiroUtils {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroUtils.class);
    private static final String LOG_PREFIX = "[FinanceiroUtils]";
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    /**
     * Cache de formatadores por Thread.
     * Justificativa: DecimalFormat não é thread-safe. O uso de ThreadLocal
     * minimiza a pressão do Garbage Collector e garante isolamento entre Virtual
     * Threads.
     */
    private static final ThreadLocal<DecimalFormat> CURRENCY_FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(LOCALE_BR);
        df.setPositivePrefix("");
        df.setNegativePrefix("-");
        return df;
    });

    private FinanceiroUtils() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada.");
    }

    static {
        log.info("{} [SISTEMA] Utilitário financeiro inicializado com suporte a Virtual Threads.", LOG_PREFIX);
    }

    /**
     * Formata um BigDecimal para exibição textual brasileira (ex: 1.234,56).
     */
    public static String formatarParaExibicao(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        log.trace("{} [TELEMETRIA] Formatando valor para exibição: {}", LOG_PREFIX, valor);
        return CURRENCY_FORMATTER.get().format(valor).trim();
    }

    /**
     * Converte uma String da UI para BigDecimal saneado para o banco de dados.
     */
    public static BigDecimal extrairValorParaBanco(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        try {
            // Remove pontos de milhar e troca vírgula por ponto decimal
            String limpo = texto.replace(".", "").replace(",", ".");
            limpo = limpo.replaceAll("[^0-9\\.-]", "");

            if (limpo.isEmpty() || limpo.equals("-")) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal valor = new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
            log.trace("{} [NEGOCIO] Valor extraído para persistência: {}", LOG_PREFIX, valor);
            return valor;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica na conversão financeira de '{}': {}", LOG_PREFIX, texto,
                    e.getMessage());
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Cria um TextFormatter para campos de moeda no JavaFX.
     */
    public static TextFormatter<BigDecimal> criarFormatadorMoeda() {
        log.debug("{} [SISTEMA] Criando formatador de moeda para UI.", LOG_PREFIX);

        StringConverter<BigDecimal> converter = new StringConverter<>() {
            @Override
            public String toString(BigDecimal valor) {
                return formatarParaExibicao(valor);
            }

            @Override
            public BigDecimal fromString(String string) {
                return extrairValorParaBanco(string);
            }
        };

        return new TextFormatter<>(converter, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), change -> {
            if (change.getControlNewText().matches("([\\d\\.,-]*)")) {
                return change;
            }
            return null;
        });
    }

    /**
     * Parse seguro de String para Inteiro, removendo caracteres não numéricos.
     */
    public static int parseSafeInt(String texto) {
        if (texto == null || texto.isBlank())
            return 0;

        try {
            String apenasNumeros = texto.replaceAll("\\D", "");
            return apenasNumeros.isEmpty() ? 0 : Integer.parseInt(apenasNumeros);
        } catch (NumberFormatException e) {
            log.warn("{} [NEGOCIO] Falha ao converter '{}' para inteiro.", LOG_PREFIX, texto);
            return 0;
        }
    }

    /**
     * Cria um TextFormatter que permite apenas dígitos numéricos.
     */
    public static TextFormatter<String> criarFormatadorInteiro() {
        log.debug("{} [SISTEMA] Criando formatador de inteiros para UI.", LOG_PREFIX);
        return new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        });
    }
}
