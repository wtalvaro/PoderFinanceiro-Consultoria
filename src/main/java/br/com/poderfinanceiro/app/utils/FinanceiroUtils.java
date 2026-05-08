package br.com.poderfinanceiro.app.utils;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceiroUtils {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    public static String formatarParaExibicao(BigDecimal valor) {
        if (valor == null)
            return "";
        NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
        return limparCaracteresInvalidos(nf.format(valor));
    }

    public static BigDecimal extrairValorParaBanco(String texto) {
        if (texto == null || texto.replaceAll("[^\\d]", "").isEmpty()) {
            return null;
        }
        try {
            String apenasNumeros = texto.replaceAll("[^\\d]", "");
            return new BigDecimal(apenasNumeros)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
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
            if (change.getControlNewText().matches("([\\d\\.,]*)")) {
                return change;
            }
            return null;
        });
    }

    private static String limparCaracteresInvalidos(String texto) {
        return texto.replaceAll("[^\\d\\.,]", "");
    }
}