package br.com.poderfinanceiro.app.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste Unitário Otimizado para SecurityConfig.
 * Utiliza ciclo de vida por classe para evitar logs redundantes de
 * infraestrutura.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityConfigTest {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfigTest.class);
    private static final String LOG_PREFIX = "[SecurityConfigTest]";

    private PasswordEncoder passwordEncoder;

    @BeforeAll
    void setupAll() {
        log.info("{} [TELEMETRIA] Inicializando motor de criptografia para bateria de testes.", LOG_PREFIX);
        // Instanciação única para todos os testes da classe
        SecurityConfig config = new SecurityConfig();
        this.passwordEncoder = config.passwordEncoder();
    }

    @Test
    @DisplayName("Deve instanciar o PasswordEncoder corretamente")
    void deveInstanciarPasswordEncoder() {
        assertNotNull(passwordEncoder, "O PasswordEncoder deve ser um Singleton no contexto de teste.");
    }

    @Test
    @DisplayName("Deve codificar uma senha e gerar um hash BCrypt válido")
    void deveCodificarSenhaComSucesso() {
        String senhaPura = "Poder@2025";
        String senhaCodificada = passwordEncoder.encode(senhaPura);

        assertNotNull(senhaCodificada);
        assertTrue(senhaCodificada.startsWith("$2a$") || senhaCodificada.startsWith("$2b$"),
                "O hash deve seguir o padrão BCrypt.");
    }

    @Test
    @DisplayName("Deve validar corretamente uma senha pura contra seu hash")
    void deveValidarSenhaComSucesso() {
        String senhaPura = "SenhaForte#123";
        String hash = passwordEncoder.encode(senhaPura);

        assertTrue(passwordEncoder.matches(senhaPura, hash));
    }

    @Test
    @DisplayName("Deve falhar ao validar senha incorreta")
    void deveFalharComSenhaIncorreta() {
        String hash = passwordEncoder.encode("SenhaCorreta");
        assertFalse(passwordEncoder.matches("SenhaErrada", hash));
    }

    @Test
    @DisplayName("Deve garantir que o Salting do BCrypt gere hashes diferentes para a mesma senha")
    void deveGerarHashesDiferentesParaMesmaSenha() {
        String senha = "MesmaSenha";
        String hash1 = passwordEncoder.encode(senha);
        String hash2 = passwordEncoder.encode(senha);

        assertNotEquals(hash1, hash2, "BCrypt deve aplicar salts aleatórios em cada operação.");
    }
}
