package br.com.poderfinanceiro.app.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuração de Segurança da Infraestrutura.
 * Define os beans de criptografia e proteção de dados sensíveis.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String LOG_PREFIX = "[SecurityConfig]";

    /**
     * Define o algoritmo de hashing de senhas para o sistema.
     * Utiliza BCrypt com força de processamento padrão (10 rounds).
     * 
     * @return Instância de PasswordEncoder para injeção de dependência.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.trace("{} [SISTEMA] Solicitando criação do bean PasswordEncoder.", LOG_PREFIX);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        log.info("{} [SISTEMA] PasswordEncoder (BCrypt) configurado e pronto para uso.", LOG_PREFIX);
        return encoder;
    }
}
