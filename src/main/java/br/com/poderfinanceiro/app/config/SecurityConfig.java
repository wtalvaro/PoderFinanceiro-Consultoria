package br.com.poderfinanceiro.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("[SECURITY_CONFIG] Criando bean PasswordEncoder (BCryptPasswordEncoder)");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        log.info("[SECURITY_CONFIG] PasswordEncoder configurado com sucesso");
        return encoder;
    }
}