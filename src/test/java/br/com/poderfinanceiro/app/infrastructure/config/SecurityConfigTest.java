package br.com.poderfinanceiro.app.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste Unitário para a Configuração de Segurança.
 * Valida a integridade do algoritmo de criptografia BCrypt.
 */
class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        this.securityConfig = new SecurityConfig();
        this.passwordEncoder = securityConfig.passwordEncoder();
    }

    @Test
    @DisplayName("Deve instanciar o PasswordEncoder corretamente")
    void deveInstanciarPasswordEncoder() {
        assertNotNull(passwordEncoder, "O PasswordEncoder não deve ser nulo.");
    }

    @Test
    @DisplayName("Deve codificar uma senha e garantir que o hash seja diferente do texto original")
    void deveCodificarSenhaComSucesso() {
        // GIVEN
        String senhaPura = "Poder@2025";

        // WHEN
        String senhaCodificada = passwordEncoder.encode(senhaPura);

        // THEN
        assertNotNull(senhaCodificada);
        assertNotEquals(senhaPura, senhaCodificada, "A senha codificada não deve ser igual à senha pura.");
        assertTrue(senhaCodificada.startsWith("$2a$") || senhaCodificada.startsWith("$2b$"),
                "O hash deve seguir o padrão BCrypt.");
    }

    @Test
    @DisplayName("Deve validar corretamente uma senha pura contra seu hash")
    void deveValidarSenhaComSucesso() {
        // GIVEN
        String senhaPura = "SenhaForte#123";
        String hash = passwordEncoder.encode(senhaPura);

        // WHEN
        boolean matches = passwordEncoder.matches(senhaPura, hash);

        // THEN
        assertTrue(matches, "O PasswordEncoder deve validar a senha corretamente.");
    }

    @Test
    @DisplayName("Deve falhar ao validar senha incorreta")
    void deveFalharComSenhaIncorreta() {
        // GIVEN
        String senhaCorreta = "SenhaCorreta";
        String senhaErrada = "SenhaErrada";
        String hash = passwordEncoder.encode(senhaCorreta);

        // WHEN
        boolean matches = passwordEncoder.matches(senhaErrada, hash);

        // THEN
        assertFalse(matches, "O PasswordEncoder não deve validar uma senha incorreta.");
    }

    @Test
    @DisplayName("Deve gerar hashes diferentes para a mesma senha (Salting)")
    void deveGerarHashesDiferentesParaMesmaSenha() {
        // GIVEN
        String senha = "MesmaSenha";

        // WHEN
        String hash1 = passwordEncoder.encode(senha);
        String hash2 = passwordEncoder.encode(senha);

        // THEN
        assertNotEquals(hash1, hash2, "BCrypt deve usar salts diferentes para cada codificação.");
    }
}
