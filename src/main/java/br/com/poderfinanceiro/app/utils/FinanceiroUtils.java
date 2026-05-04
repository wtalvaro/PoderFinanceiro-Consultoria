package br.com.poderfinanceiro.app.utils;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FinanceiroUtils {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

    /**
     * Cria um TextFormatter para datas (DD/MM/AAAA).
     * Insere as barras automaticamente e converte para LocalDate.
     */
    public static TextFormatter<LocalDate> criarFormatadorData() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? DATE_FORMATTER.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty())
                    return null;
                try {
                    // Tenta converter a string formatada de volta para LocalDate
                    return LocalDate.parse(string, DATE_FORMATTER);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        return new TextFormatter<>(converter, null, change -> {
            String newText = change.getControlNewText();

            // 1. Permite apenas números e barras
            if (!newText.matches("[\\d/]*")) {
                return null;
            }

            // 2. Lógica de inserção automática de barras
            if (change.isAdded()) {
                if (newText.length() > 10)
                    return null; // Limite de caracteres

                int start = change.getRangeStart();
                if (start == 2 || start == 5) {
                    if (!change.getText().equals("/")) {
                        change.setText("/" + change.getText());
                        int newCaretPos = change.getControlNewText().length();
                        change.setCaretPosition(newCaretPos);
                        change.setAnchor(newCaretPos);
                    }
                }
            }
            return change;
        });
    }

    // ========================================================================
    // UTILITÁRIOS INTERNOS
    // ========================================================================

    private static String limparCaracteresInvalidos(String texto) {
        return texto.replaceAll("[^\\d\\.,]", "");
    }
}