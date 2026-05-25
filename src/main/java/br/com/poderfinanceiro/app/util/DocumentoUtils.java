package br.com.poderfinanceiro.app.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentoUtils {

    private static final Logger log = LoggerFactory.getLogger(DocumentoUtils.class);

    static {
        log.debug("[DOCUMENTO_UTILS] Classe utilitária inicializada");
    }

    public static String formatarCpf(String cpf) {
        log.debug("[DOCUMENTO_UTILS] formatarCpf: CPF original='{}'", cpf);
        if (cpf == null || cpf.replaceAll("[^0-9]", "").length() != 11) {
            log.warn("[DOCUMENTO_UTILS] formatarCpf: CPF inválido ou nulo (tamanho != 11 após limpeza)");
            return cpf;
        }
        String limpo = cpf.replaceAll("[^0-9]", "");
        String formatado = String.format("%s.%s.%s-%s",
                limpo.substring(0, 3),
                limpo.substring(3, 6),
                limpo.substring(6, 9),
                limpo.substring(9, 11));
        log.info("[DOCUMENTO_UTILS] formatarCpf: CPF formatado='{}'", formatado);
        return formatado;
    }

    public static TextFormatter<String> criarFormatadorCpf() {
        log.debug("[DOCUMENTO_UTILS] criarFormatadorCpf: Criando formatador de CPF");
        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String string) {
                return formatarCpf(string);
            }

            @Override
            public String fromString(String string) {
                if (string == null) {
                    log.trace("[DOCUMENTO_UTILS] fromString: string nula, retornando vazio");
                    return "";
                }
                String numeros = string.replaceAll("[^0-9]", "");
                log.trace("[DOCUMENTO_UTILS] fromString: '{}' -> '{}'", string, numeros);
                return numeros;
            }
        };

        TextFormatter<String> formatter = new TextFormatter<>(converter, "", change -> {
            String newText = change.getControlNewText();
            log.trace("[DOCUMENTO_UTILS] TextFormatter change: newText='{}', added={}", newText, change.isAdded());

            if (newText.matches("([\\d\\.-]*)")) {
                String apenasNumeros = newText.replaceAll("[^0-9]", "");
                if (apenasNumeros.length() <= 11) {
                    log.trace("[DOCUMENTO_UTILS] Change aceito: apenasNumeros='{}'", apenasNumeros);
                    return change;
                } else {
                    log.trace("[DOCUMENTO_UTILS] Change rejeitado: excede 11 dígitos ({} dígitos)",
                            apenasNumeros.length());
                }
            } else {
                log.trace("[DOCUMENTO_UTILS] Change rejeitado: contém caracteres inválidos");
            }
            return null;
        });

        log.info("[DOCUMENTO_UTILS] Formatador de CPF criado com sucesso");
        return formatter;
    }
}