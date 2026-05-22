package br.com.poderfinanceiro.app.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

public class DocumentoUtils {

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
}