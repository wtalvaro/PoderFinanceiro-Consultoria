package br.com.poderfinanceiro.app.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuração Central do Jackson para o ERP Poder Financeiro.
 * Refatorada para Auto-Descoberta de Módulos, garantindo compatibilidade com
 * Java 25.
 */
@Configuration
public class JacksonConfig {

    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);
    private static final String LOG_PREFIX = "[JacksonConfig]";

    /**
     * Provê o ObjectMapper configurado para o ecossistema Spring/JavaFX.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.trace("{} [SISTEMA] Inicializando motor JSON com auto-registro de módulos.", LOG_PREFIX);

        ObjectMapper mapper = new ObjectMapper();

        // 1. REGRA DE OURO: Registra automaticamente JavaTimeModule,
        // ParameterNamesModule, etc.
        // Isso elimina a necessidade de imports manuais que causam erros de compilação.
        mapper.findAndRegisterModules();

        // 2. Configurações de Resiliência (Essencial para integração com Gemini)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 3. Configurações de Formatação
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        log.info("{} [SISTEMA] ObjectMapper configurado com sucesso (Resiliência Ativa).", LOG_PREFIX);
        return mapper;
    }
}
