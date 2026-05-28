package br.com.poderfinanceiro.app.util;

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
 * <h1>FinanceiroUtils</h1>
 * <p>
 * Classe utilitária para manipulação de valores monetários e formatação de
 * campos. Otimizada para Project Loom utilizando ThreadLocal para garantir
 * thread-safety e performance na formatação de números.
 * </p>
 */
public class FinanceiroUtils {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroUtils.class);
    private static final String LOG_PREFIX = "[FinanceiroUtils]";
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    /**
     * Cache de formatadores por Thread. Justificativa: DecimalFormat não é
     * thread-safe. O uso de ThreadLocal evita a criação excessiva de objetos
     * (GC Pressure) e garante segurança em ambientes multi-thread.
     */
    private static final ThreadLocal<DecimalFormat> CURRENCY_FORMATTER = ThreadLocal.withInitial(() -> {
        log.trace("{} [SISTEMA] Inicializando nova instância de DecimalFormat para a Thread: {}", LOG_PREFIX,
                Thread.currentThread().getName());
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(LOCALE_BR);
        df.setPositivePrefix("");
        df.setNegativePrefix("-");
        return df;
    });

    /**
     * Formata um BigDecimal para exibição textual (ex: 1.234,56).
     * 
     * @param valor Valor monetário
     * @return String formatada ou "0,00" se nulo
     */
    public static String formatarParaExibicao(BigDecimal valor) {
        if (valor == null) {
            log.trace("{} [NEGOCIO] formatarParaExibicao: valor nulo recebido, retornando fallback.", LOG_PREFIX);
            return "0,00";
        }
        return CURRENCY_FORMATTER.get().format(valor).trim();
    }

    /**
     * Converte uma String formatada da UI para um BigDecimal compatível com o
     * banco de dados.
     * 
     * @param texto Valor vindo do campo de texto
     * @return BigDecimal saneado
     */
    public static BigDecimal extrairValorParaBanco(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Saneamento: Remove separadores de milhar e normaliza o separador
            // decimal
            String limpo = texto.replace(".", "").replace(",", ".");
            limpo = limpo.replaceAll("[^0-9\\.]", "");

            if (limpo.isEmpty())
                return BigDecimal.ZERO;

            BigDecimal valor = new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
            log.trace("{} [NEGOCIO] Valor extraído com sucesso: {}", LOG_PREFIX, valor);
            return valor;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao converter texto financeiro '{}': {}", LOG_PREFIX, texto, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Cria um TextFormatter configurado para campos de entrada de moeda.
     * 
     * @return TextFormatter<BigDecimal>
     */
    public static TextFormatter<BigDecimal> criarFormatadorMoeda() {
        log.debug("{} [SISTEMA] Criando novo TextFormatter de moeda.", LOG_PREFIX);

        StringConverter<BigDecimal> converter = new StringConverter<>() {
            @Override public String toString(BigDecimal valor) {
                return formatarParaExibicao(valor);
            }

            @Override public BigDecimal fromString(String string) {
                return extrairValorParaBanco(string);
            }
        };

        return new TextFormatter<>(converter, BigDecimal.ZERO, change -> {
            if (change.getControlNewText().matches("([\\d\\.,]*)")) {
                return change;
            }
            return null; // Rejeita caracteres não financeiros
        });
    }

    /**
     * Realiza o parse seguro de uma string para inteiro, removendo caracteres
     * não numéricos.
     * 
     * @param texto String de entrada
     * @return int valor convertido ou 0
     */
    public static int parseSafeInt(String texto) {
        if (texto == null || texto.isBlank())
            return 0;

        try {
            String apenasNumeros = texto.replaceAll("\\D", "");
            if (apenasNumeros.isEmpty())
                return 0;
            return Integer.parseInt(apenasNumeros);
        } catch (NumberFormatException e) {
            log.warn("{} [NEGOCIO] Falha ao converter '{}' para inteiro.", LOG_PREFIX, texto);
            return 0;
        }
    }

    /**
     * Cria um TextFormatter que permite apenas a entrada de dígitos numéricos.
     * 
     * @return TextFormatter<String>
     */
    public static TextFormatter<String> criarFormatadorInteiro() {
        log.debug("{} [SISTEMA] Criando novo TextFormatter de inteiros.", LOG_PREFIX);
        return new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        });
    }
}
