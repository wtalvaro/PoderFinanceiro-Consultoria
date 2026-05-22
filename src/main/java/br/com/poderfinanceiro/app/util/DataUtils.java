package br.com.poderfinanceiro.app.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DataUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                    return LocalDate.parse(string, DATE_FORMATTER);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        return new TextFormatter<>(converter, null, change -> {
            String newText = change.getControlNewText();

            if (!newText.matches("[\\d/]*")) {
                return null;
            }

            if (change.isAdded()) {
                if (newText.length() > 10)
                    return null;

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
}