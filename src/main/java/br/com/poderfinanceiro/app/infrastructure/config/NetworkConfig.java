package br.com.poderfinanceiro.app.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.Executors;

/**
 * Configuração Central de Rede e Infraestrutura de Comunicação.
 * Resolve a ausência de autoconfiguração do RestClient em ambiente Desktop.
 */
@Configuration
public class NetworkConfig {

    private static final Logger log = LoggerFactory.getLogger(NetworkConfig.class);
    private static final String LOG_PREFIX = "[NetworkConfig]";

    /**
     * Provê o Builder do RestClient para injeção nos Clientes de Infraestrutura.
     * Marcado como @Primary para garantir prioridade na resolução de dependências.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        log.trace("{} [SISTEMA] Criando bean RestClient.Builder para suporte a Fluent API.", LOG_PREFIX);
        return RestClient.builder();
    }

    /**
     * Provê o HttpClient nativo do Java 25 configurado com Virtual Threads.
     * Essencial para que operações de rede (como downloads no UpdateClient)
     * não consumam threads pesadas do SO no Fedora.
     */
    @Bean
    public HttpClient httpClient() {
        log.info("{} [SISTEMA] Inicializando HttpClient nativo com Virtual Thread Executor (Project Loom).",
                LOG_PREFIX);

        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Executors.newVirtualThreadPerTaskExecutor()) // Orquestração via Project Loom
                .build();
    }
}
