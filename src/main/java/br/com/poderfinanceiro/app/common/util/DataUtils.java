package br.com.poderfinanceiro.app.common.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utilitário para manipulação e formatação de datas (LocalDate).
 * Provê suporte a máscaras dinâmicas para JavaFX e conversões seguras.
 */
public class DataUtils {

    private static final Logger log = LoggerFactory.getLogger(DataUtils.class);
    private static final String LOG_PREFIX = "[DataUtils]";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static {
        log.info("{} [SISTEMA] Inicializado com padrão dd/MM/yyyy.", LOG_PREFIX);
    }

    /**
     * Formata um LocalDate para String no padrão dd/MM/yyyy.
     */
    public static String formatar(LocalDate data) {
        if (data == null)
            return "";
        return DATE_FORMATTER.format(data);
    }

    /**
     * Converte uma String dd/MM/yyyy para LocalDate de forma segura.
     */
    public static LocalDate parse(String dataStr) {
        if (dataStr == null || dataStr.isBlank())
            return null;
        try {
            return LocalDate.parse(dataStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("{} [NEGOCIO] Falha ao parsear data: '{}'. Erro: {}", LOG_PREFIX, dataStr, e.getMessage());
            return null;
        }
    }

    /**
     * Cria um TextFormatter para campos de data no JavaFX.
     * Implementa auto-inserção de barras e bloqueio de caracteres não numéricos.
     */
    public static TextFormatter<LocalDate> criarFormatadorData() {
        log.debug("{} [SISTEMA] Criando formatador de data para UI.", LOG_PREFIX);

        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return formatar(date);
            }

            @Override
            public LocalDate fromString(String string) {
                return parse(string);
            }
        };

        return new TextFormatter<>(converter, null, change -> {
            String novoTexto = change.getControlNewText();

            // 1. Bloqueia qualquer caractere que não seja dígito ou barra
            if (!novoTexto.matches("[\\d/]*")) {
                return null;
            }

            if (change.isAdded()) {
                // 2. Limita a 10 caracteres (dd/mm/yyyy)
                if (novoTexto.length() > 10) {
                    return null;
                }

                // 3. Auto-inserção de barras nas posições 2 e 5
                int start = change.getRangeStart();
                if ((start == 2 || start == 5) && !change.getText().equals("/")) {
                    change.setText("/" + change.getText());
                    int novaPosicao = change.getControlNewText().length();
                    change.setCaretPosition(novaPosicao);
                    change.setAnchor(novaPosicao);
                }
            }

            log.trace("{} [UI] Alteração de data aceita: {}", LOG_PREFIX, novoTexto);
            return change;
        });
    }
}
