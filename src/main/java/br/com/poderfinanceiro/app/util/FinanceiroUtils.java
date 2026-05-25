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

public class FinanceiroUtils {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroUtils.class);

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    // Formatador para exibição (com pontos e vírgulas)
    private static final DecimalFormat dfExibicao = (DecimalFormat) NumberFormat.getCurrencyInstance(LOCALE_BR);

    static {
        // Removemos o símbolo R$ para não poluir o campo de digitação
        dfExibicao.setPositivePrefix("");
        dfExibicao.setNegativePrefix("-");
        log.debug("[FINANCEIRO_UTILS] Classe utilitária carregada. Formatador de moeda configurado (sem símbolo R$)");
    }

    public static String formatarParaExibicao(BigDecimal valor) {
        log.debug("[FINANCEIRO_UTILS] formatarParaExibicao: valor={}", valor);
        if (valor == null) {
            log.warn("[FINANCEIRO_UTILS] formatarParaExibicao: valor nulo, retornando '0,00'");
            return "0,00";
        }
        String formatado = dfExibicao.format(valor).trim();
        log.trace("[FINANCEIRO_UTILS] formatarParaExibicao: resultado='{}'", formatado);
        return formatado;
    }

    public static BigDecimal extrairValorParaBanco(String texto) {
        log.debug("[FINANCEIRO_UTILS] extrairValorParaBanco: texto='{}'", texto);
        if (texto == null || texto.trim().isEmpty()) {
            log.warn("[FINANCEIRO_UTILS] extrairValorParaBanco: texto vazio ou nulo, retornando ZERO");
            return BigDecimal.ZERO;
        }

        try {
            // 1. Remove separadores de milhar (pontos)
            // 2. Troca a vírgula decimal por ponto (padrão Java/BigDecimal)
            String limpo = texto.replace(".", "").replace(",", ".");
            // Remove qualquer outro caractere que não seja número ou ponto
            limpo = limpo.replaceAll("[^0-9\\.]", "");
            log.trace("[FINANCEIRO_UTILS] extrairValorParaBanco: texto limpo='{}'", limpo);

            if (limpo.isEmpty()) {
                log.warn("[FINANCEIRO_UTILS] extrairValorParaBanco: após limpeza, string vazia, retornando ZERO");
                return BigDecimal.ZERO;
            }

            BigDecimal valor = new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
            log.info("[FINANCEIRO_UTILS] extrairValorParaBanco: valor extraído com sucesso: {}", valor);
            return valor;
        } catch (Exception e) {
            log.error("[FINANCEIRO_UTILS] extrairValorParaBanco: erro ao converter '{}' -> {}", texto, e.getMessage(),
                    e);
            return BigDecimal.ZERO;
        }
    }

    public static TextFormatter<BigDecimal> criarFormatadorMoeda() {
        log.debug("[FINANCEIRO_UTILS] criarFormatadorMoeda: criando formatador de moeda");
        StringConverter<BigDecimal> converter = new StringConverter<>() {
            @Override
            public String toString(BigDecimal valor) {
                String result = formatarParaExibicao(valor);
                log.trace("[FINANCEIRO_UTILS] converter.toString: {} -> '{}'", valor, result);
                return result;
            }

            @Override
            public BigDecimal fromString(String string) {
                BigDecimal result = extrairValorParaBanco(string);
                log.trace("[FINANCEIRO_UTILS] converter.fromString: '{}' -> {}", string, result);
                return result;
            }
        };

        TextFormatter<BigDecimal> formatter = new TextFormatter<>(converter, BigDecimal.ZERO, change -> {
            log.trace("[FINANCEIRO_UTILS] formatador moeda change: newText='{}'", change.getControlNewText());
            if (change.getControlNewText().matches("([\\d\\.,]*)")) {
                log.trace("[FINANCEIRO_UTILS] change aceito");
                return change;
            }
            log.trace("[FINANCEIRO_UTILS] change rejeitado (caractere inválido)");
            return null;
        });
        log.info("[FINANCEIRO_UTILS] formatador de moeda criado com sucesso");
        return formatter;
    }

    public static int parseSafeInt(String texto) {
        log.debug("[FINANCEIRO_UTILS] parseSafeInt: texto='{}'", texto);
        if (texto == null || texto.replaceAll("[^0-9]", "").isEmpty()) {
            log.warn("[FINANCEIRO_UTILS] parseSafeInt: texto vazio ou nulo, retornando 0");
            return 0;
        }
        try {
            String apenasNumeros = texto.replaceAll("[^0-9]", "");
            int valor = Integer.parseInt(apenasNumeros);
            log.info("[FINANCEIRO_UTILS] parseSafeInt: convertido '{}' -> {}", texto, valor);
            return valor;
        } catch (NumberFormatException e) {
            log.error("[FINANCEIRO_UTILS] parseSafeInt: erro ao converter '{}' -> {}", texto, e.getMessage(), e);
            return 0;
        }
    }

    public static TextFormatter<String> criarFormatadorInteiro() {
        log.debug("[FINANCEIRO_UTILS] criarFormatadorInteiro: criando formatador de inteiro");
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            log.trace("[FINANCEIRO_UTILS] formatador inteiro change: newText='{}'", change.getControlNewText());
            if (change.getControlNewText().matches("\\d*")) {
                log.trace("[FINANCEIRO_UTILS] change aceito");
                return change;
            }
            log.trace("[FINANCEIRO_UTILS] change rejeitado (apenas dígitos permitidos)");
            return null;
        });
        log.info("[FINANCEIRO_UTILS] formatador de inteiro criado com sucesso");
        return formatter;
    }
}