package br.com.poderfinanceiro.app.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/**
 * Utilitário de Validação de Formatos e Integridade de Dados.
 * Centraliza regras de sanidade para E-mails, Usernames, Senhas e URLs.
 */
public final class ValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtils.class);
    private static final String LOG_PREFIX = "[ValidationUtils]";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    // Regex para validação de URL (HTTP/HTTPS)
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");

    private ValidationUtils() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada.");
    }

    static {
        log.info("{} [SISTEMA] Utilitário de validação inicializado.", LOG_PREFIX);
    }

    public static boolean isEmailValido(String email) {
        if (email == null || email.isBlank())
            return false;
        boolean valido = EMAIL_PATTERN.matcher(email).matches();
        log.trace("{} [NEGOCIO] Validação de e-mail '{}': {}", LOG_PREFIX, email, valido);
        return valido;
    }

    public static boolean isUsernameValido(String username) {
        if (username == null || username.isBlank())
            return false;
        boolean valido = !username.contains(" ") && username.length() >= 3;
        log.trace("{} [NEGOCIO] Validação de username '{}': {}", LOG_PREFIX, username, valido);
        return valido;
    }

    public static boolean isSenhaForte(String senha) {
        if (senha == null)
            return false;
        boolean forte = senha.length() >= 8;
        log.trace("{} [NEGOCIO] Verificação de complexidade de senha: {}", LOG_PREFIX, forte);
        return forte;
    }

    /**
     * Valida se uma URL de portal ou site é válida.
     */
    public static boolean isUrlValida(String url) {
        if (url == null || url.isBlank())
            return true; // Opcional em alguns contextos
        boolean valida = URL_PATTERN.matcher(url).matches();
        log.trace("{} [NEGOCIO] Validação de URL '{}': {}", LOG_PREFIX, url, valida);
        return valida;
    }

    /**
     * Verifica se o texto não é nulo ou vazio.
     */
    public static boolean isPreenchido(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }
}
