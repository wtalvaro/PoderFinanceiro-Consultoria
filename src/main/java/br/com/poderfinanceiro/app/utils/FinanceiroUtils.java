package br.com.poderfinanceiro.app.utils;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceiroUtils {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    // ========================================================================
    // LÓGICA DE MOEDA (SALÁRIO / RENDA)
    // ========================================================================

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

    // ========================================================================
    // LÓGICA DE CPF
    // ========================================================================

    public static String formatarCpf(String cpf) {
        if (cpf == null || cpf.replaceAll("[^0-9]", "").length() != 11) {
            return cpf;
        }
        String limpo = cpf.replaceAll("[^0-9]", "");
        return String.format("%s.%s.%s-%s",
                limpo.substring(0, 3),
                limpo.substring(3, 6),
                limpo.substring(6, 9),
                limpo.substring(9, 11));
    }

    public static TextFormatter<String> criarFormatadorCpf() {
        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String string) {
                return formatarCpf(string);
            }

            @Override
            public String fromString(String string) {
                if (string == null)
                    return "";
                return string.replaceAll("[^0-9]", "");
            }
        };

        return new TextFormatter<>(converter, "", change -> {
            if (change.getControlNewText().matches("([\\d\\.-]*)")) {
                String apenasNumeros = change.getControlNewText().replaceAll("[^0-9]", "");
                if (apenasNumeros.length() <= 11) {
                    return change;
                }
            }
            return null;
        });
    }

    // ========================================================================
    // LÓGICA DE TELEFONE
    // ========================================================================

    public static String formatarTelefone(String tel) {
        if (tel == null)
            return "";
        String limpo = tel.replaceAll("[^0-9]", "");

        if (limpo.length() == 11) {
            return String.format("(%s) %s-%s",
                    limpo.substring(0, 2), limpo.substring(2, 7), limpo.substring(7));
        } else if (limpo.length() == 10) {
            return String.format("(%s) %s-%s",
                    limpo.substring(0, 2), limpo.substring(2, 6), limpo.substring(6));
        }
        return tel;
    }

    public static TextFormatter<String> criarFormatadorTelefone() {
        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String string) {
                return formatarTelefone(string);
            }

            @Override
            public String fromString(String string) {
                if (string == null)
                    return "";
                return string.replaceAll("[^0-9]", "");
            }
        };

        return new TextFormatter<>(converter, "", change -> {
            if (change.getControlNewText().matches("([\\d\\s\\(\\)-]*)")) {
                String apenasNumeros = change.getControlNewText().replaceAll("[^0-9]", "");
                if (apenasNumeros.length() <= 11) {
                    return change;
                }
            }
            return null;
        });
    }

    // ========================================================================
    // UTILITÁRIOS INTERNOS
    // ========================================================================

    private static String limparCaracteresInvalidos(String texto) {
        return texto.replaceAll("[^\\d\\.,]", "");
    }
}