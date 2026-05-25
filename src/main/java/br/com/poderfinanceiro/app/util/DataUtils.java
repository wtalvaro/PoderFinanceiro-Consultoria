package br.com.poderfinanceiro.app.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DataUtils {

    private static final Logger log = LoggerFactory.getLogger(DataUtils.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static {
        log.debug("[DATA_UTILS] Inicializado com formatador dd/MM/yyyy");
    }

    public static TextFormatter<LocalDate> criarFormatadorData() {
        log.debug("[DATA_UTILS] criarFormatadorData: Criando formatador de data");
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                String result = (date != null) ? DATE_FORMATTER.format(date) : "";
                log.trace("[DATA_UTILS] toString: date={} -> '{}'", date, result);
                return result;
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) {
                    log.trace("[DATA_UTILS] fromString: string vazia ou nula, retornando null");
                    return null;
                }
                try {
                    LocalDate date = LocalDate.parse(string, DATE_FORMATTER);
                    log.trace("[DATA_UTILS] fromString: '{}' -> {}", string, date);
                    return date;
                } catch (Exception e) {
                    log.warn("[DATA_UTILS] fromString: Falha ao converter '{}' para LocalDate: {}", string,
                            e.getMessage());
                    return null;
                }
            }
        };

        TextFormatter<LocalDate> formatter = new TextFormatter<>(converter, null, change -> {
            String newText = change.getControlNewText();
            log.trace("[DATA_UTILS] Formatter change: newText='{}', added={}, deleted={}",
                    newText, change.isAdded(), change.isDeleted());

            if (!newText.matches("[\\d/]*")) {
                log.trace("[DATA_UTILS] Change rejeitado: contém caractere inválido (não dígito ou '/')");
                return null;
            }

            if (change.isAdded()) {
                if (newText.length() > 10) {
                    log.trace("[DATA_UTILS] Change rejeitado: comprimento excede 10");
                    return null;
                }

                int start = change.getRangeStart();
                if (start == 2 || start == 5) {
                    if (!change.getText().equals("/")) {
                        log.trace("[DATA_UTILS] Inserindo barra automaticamente na posição {}", start);
                        change.setText("/" + change.getText());
                        int newCaretPos = change.getControlNewText().length();
                        change.setCaretPosition(newCaretPos);
                        change.setAnchor(newCaretPos);
                    }
                }
            }
            log.trace("[DATA_UTILS] Change aceito: newText='{}'", change.getControlNewText());
            return change;
        });

        log.info("[DATA_UTILS] Formatador de data criado com sucesso");
        return formatter;
    }
}