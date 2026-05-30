package br.com.poderfinanceiro.app.common.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitário para saneamento e formatação de dados de contato.
 * Provê máscaras dinâmicas para componentes JavaFX e normalização de strings.
 */
public class ContatoUtils {

    private static final Logger log = LoggerFactory.getLogger(ContatoUtils.class);
    private static final String LOG_PREFIX = "[ContatoUtils]";

    /**
     * Formata uma string numérica para o padrão de telefone brasileiro.
     * Suporta 10 dígitos (Fixo) e 11 dígitos (Celular).
     */
    public static String formatarTelefone(String tel) {
        if (tel == null || tel.isBlank()) {
            return "";
        }

        log.trace("{} [TELEMETRIA] Formatando telefone: {}", LOG_PREFIX, tel);
        String limpo = tel.replaceAll("[^0-9]", "");

        if (limpo.length() == 11) {
            return String.format("(%s) %s-%s",
                    limpo.substring(0, 2),
                    limpo.substring(2, 7),
                    limpo.substring(7));
        } else if (limpo.length() == 10) {
            return String.format("(%s) %s-%s",
                    limpo.substring(0, 2),
                    limpo.substring(2, 6),
                    limpo.substring(6));
        }

        log.debug("{} [NEGOCIO] Telefone com formato não padrão ({} dígitos): {}", LOG_PREFIX, limpo.length(), tel);
        return limpo; // Retorna apenas os números se não encaixar nos padrões
    }

    /**
     * Cria um TextFormatter para campos de telefone no JavaFX.
     * Implementa filtro de entrada (apenas números) e limite de 11 caracteres.
     */
    public static TextFormatter<String> criarFormatadorTelefone() {
        log.debug("{} [SISTEMA] Criando formatador de telefone para UI.", LOG_PREFIX);

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
            String novoTexto = change.getControlNewText();

            // 1. Permite apenas números e caracteres de formatação na visualização
            if (novoTexto.matches("([\\d\\s\\(\\)-]*)")) {
                String apenasNumeros = novoTexto.replaceAll("[^0-9]", "");

                // 2. Limita a carga útil a 11 dígitos (padrão celular BR)
                if (apenasNumeros.length() <= 11) {
                    return change;
                }
            }

            log.trace("{} [UI] Tentativa de entrada inválida bloqueada: {}", LOG_PREFIX, novoTexto);
            return null; // Rejeita a alteração
        });
    }

    /**
     * Remove qualquer caractere não numérico de uma string.
     */
    public static String desformatar(String dado) {
        if (dado == null)
            return "";
        return dado.replaceAll("[^0-9]", "");
    }
}
