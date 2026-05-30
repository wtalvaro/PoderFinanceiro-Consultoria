package br.com.poderfinanceiro.app.common.util;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitário para saneamento, formatação e validação de documentos (CPF).
 * Provê suporte a máscaras dinâmicas para JavaFX e normalização para
 * persistência.
 */
public class DocumentoUtils {

    private static final Logger log = LoggerFactory.getLogger(DocumentoUtils.class);
    private static final String LOG_PREFIX = "[DocumentoUtils]";

    static {
        log.info("{} [SISTEMA] Utilitário de documentos inicializado.", LOG_PREFIX);
    }

    /**
     * Formata uma string numérica para o padrão CPF {@code 000.000.000-00}.
     * 
     * @param cpf String contendo o CPF (formatado ou não).
     * @return String formatada ou apenas os números se o tamanho for inválido.
     */
    public static String formatarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return "";
        }

        String limpo = cpf.replaceAll("[^0-9]", "");

        if (limpo.length() == 11) {
            log.trace("{} [TELEMETRIA] Formatando CPF: {}", LOG_PREFIX, limpo);
            return String.format("%s.%s.%s-%s",
                    limpo.substring(0, 3),
                    limpo.substring(3, 6),
                    limpo.substring(6, 9),
                    limpo.substring(9, 11));
        }

        log.debug("{} [NEGOCIO] CPF com tamanho inválido para formatação: {} dígitos", LOG_PREFIX, limpo.length());
        return limpo;
    }

    /**
     * Remove formatação de documentos, mantendo apenas dígitos.
     * 
     * @param dado String formatada.
     * @return String contendo apenas números.
     */
    public static String desformatar(String dado) {
        if (dado == null) {
            return "";
        }
        return dado.replaceAll("[^0-9]", "");
    }

    /**
     * Cria um TextFormatter para campos de CPF no JavaFX.
     * Implementa filtro de entrada rigoroso e limite de 11 dígitos.
     * 
     * @return TextFormatter configurado para CPF.
     */
    public static TextFormatter<String> criarFormatadorCpf() {
        log.debug("{} [SISTEMA] Criando formatador de CPF para UI.", LOG_PREFIX);

        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String string) {
                return formatarCpf(string);
            }

            @Override
            public String fromString(String string) {
                return desformatar(string);
            }
        };

        return new TextFormatter<>(converter, "", change -> {
            String novoTexto = change.getControlNewText();

            // Permite apenas dígitos, pontos e traços na visualização
            if (novoTexto.matches("([\\d\\.-]*)")) {
                String apenasNumeros = desformatar(novoTexto);

                // Limita a carga útil a 11 dígitos
                if (apenasNumeros.length() <= 11) {
                    return change;
                }
            }

            log.trace("{} [UI] Entrada de CPF inválida bloqueada: {}", LOG_PREFIX, novoTexto);
            return null;
        });
    }

    /**
     * Validação rigorosa de CPF utilizando o algoritmo de dígitos verificadores.
     * 
     * @param cpf String contendo o CPF a ser validado.
     * @return true se o CPF for matematicamente válido.
     */
    public static boolean isCpfValido(String cpf) {
        String limpo = desformatar(cpf);

        // Elimina CPFs com tamanhos errados ou sequências conhecidas inválidas
        if (limpo.length() != 11 || limpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int d1 = 0, d2 = 0;
            int digit1, digit2, rest;
            int nDig;

            for (int nCount = 1; nCount < limpo.length() - 1; nCount++) {
                nDig = Integer.parseInt(limpo.substring(nCount - 1, nCount));
                d1 = d1 + (11 - nCount) * nDig;
                d2 = d2 + (12 - nCount) * nDig;
            }

            rest = (d1 % 11);
            digit1 = (rest < 2) ? 0 : 11 - rest;

            d2 = d2 + 2 * digit1;
            rest = (d2 % 11);
            digit2 = (rest < 2) ? 0 : 11 - rest;

            String nDigVerific = limpo.substring(limpo.length() - 2);
            String nDigResult = String.valueOf(digit1) + String.valueOf(digit2);

            return nDigVerific.equals(nDigResult);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao validar algoritmo de CPF: {}", LOG_PREFIX, e.getMessage());
            return false;
        }
    }
}
