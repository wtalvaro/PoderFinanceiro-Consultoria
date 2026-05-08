package br.com.poderfinanceiro.app.utils;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

public class ContatoUtils {

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
}