package br.com.poderfinanceiro.app.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContatoUtils {

    private static final Logger log = LoggerFactory.getLogger(ContatoUtils.class);

    public static String formatarTelefone(String tel) {
        log.debug("[CONTATO_UTILS] formatarTelefone: tel={}", tel);
        if (tel == null) {
            log.trace("[CONTATO_UTILS] telefone nulo, retornando string vazia");
            return "";
        }
        String limpo = tel.replaceAll("[^0-9]", "");
        log.trace("[CONTATO_UTILS] telefone limpo: {}", limpo);

        if (limpo.length() == 11) {
            String formatado = String.format("(%s) %s-%s",
                    limpo.substring(0, 2), limpo.substring(2, 7), limpo.substring(7));
            log.info("[CONTATO_UTILS] telefone formatado (11 dígitos): {}", formatado);
            return formatado;
        } else if (limpo.length() == 10) {
            String formatado = String.format("(%s) %s-%s",
                    limpo.substring(0, 2), limpo.substring(2, 6), limpo.substring(6));
            log.info("[CONTATO_UTILS] telefone formatado (10 dígitos): {}", formatado);
            return formatado;
        }
        log.warn("[CONTATO_UTILS] telefone com formato inesperado ({} dígitos), retornando original: {}",
                limpo.length(), tel);
        return tel;
    }

    public static TextFormatter<String> criarFormatadorTelefone() {
        log.debug("[CONTATO_UTILS] criarFormatadorTelefone: criando formatador");
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

        TextFormatter<String> formatter = new TextFormatter<>(converter, "", change -> {
            if (change.getControlNewText().matches("([\\d\\s\\(\\)-]*)")) {
                String apenasNumeros = change.getControlNewText().replaceAll("[^0-9]", "");
                if (apenasNumeros.length() <= 11) {
                    log.trace("[CONTATO_UTILS] alteração aceita: nova string='{}'", change.getControlNewText());
                    return change;
                } else {
                    log.trace("[CONTATO_UTILS] alteração rejeitada (máx 11 dígitos): nova string='{}'",
                            change.getControlNewText());
                }
            } else {
                log.trace("[CONTATO_UTILS] alteração rejeitada (caractere inválido): nova string='{}'",
                        change.getControlNewText());
            }
            return null;
        });

        log.info("[CONTATO_UTILS] formatador de telefone criado com sucesso");
        return formatter;
    }
}