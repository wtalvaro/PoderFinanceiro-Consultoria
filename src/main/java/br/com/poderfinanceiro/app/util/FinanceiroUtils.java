package br.com.poderfinanceiro.app.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceiroUtils {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    // Formatador para exibição (com pontos e vírgulas)
    private static final DecimalFormat dfExibicao = (DecimalFormat) NumberFormat.getCurrencyInstance(LOCALE_BR);

    static {
        // Removemos o símbolo R$ para não poluir o campo de digitação
        dfExibicao.setPositivePrefix("");
        dfExibicao.setNegativePrefix("-");
    }

    public static String formatarParaExibicao(BigDecimal valor) {
        if (valor == null)
            return "0,00";
        return dfExibicao.format(valor).trim();
    }

    public static BigDecimal extrairValorParaBanco(String texto) {
        if (texto == null || texto.trim().isEmpty())
            return BigDecimal.ZERO;

        try {
            // 1. Remove separadores de milhar (pontos)
            // 2. Troca a vírgula decimal por ponto (padrão Java/BigDecimal)
            String limpo = texto.replace(".", "").replace(",", ".");

            // Remove qualquer outro caractere que não seja número ou ponto
            limpo = limpo.replaceAll("[^0-9\\.]", "");

            if (limpo.isEmpty())
                return BigDecimal.ZERO;

            return new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static TextFormatter<BigDecimal> criarFormatadorMoeda() {
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

        return new TextFormatter<>(converter, BigDecimal.ZERO, change -> {
            // Permite apenas dígitos, pontos e vírgulas durante a digitação
            if (change.getControlNewText().matches("([\\d\\.,]*)")) {
                return change;
            }
            return null;
        });
    }
}