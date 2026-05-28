package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.infrastructure.client.ViaCepClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço de Domínio para gestão de endereços via CEP. Orquestra a limpeza de
 * dados e a integração com o provedor externo.
 */
@Service
public class ViaCepService {

    private static final Logger log = LoggerFactory.getLogger(ViaCepService.class);
    private static final String LOG_PREFIX = "[ViaCepService]";

    private final ViaCepClient viaCepClient;

    public ViaCepService(ViaCepClient viaCepClient) {
        this.viaCepClient = viaCepClient;
        log.info("{} [SISTEMA] Serviço de CEP instanciado com sucesso.", LOG_PREFIX);
    }

    /**
     * Busca um endereço completo a partir de um CEP. Realiza o saneamento do
     * input e validação de formato antes da chamada externa.
     * 
     * @param cep String contendo o CEP (com ou sem máscara)
     * @return ViaCepResponse ou null se não encontrado/inválido
     */
    public ViaCepResponse buscarEnderecoPorCep(String cep) {
        log.info("{} [TELEMETRIA] Iniciando orquestração de busca de endereço para CEP: {}", LOG_PREFIX, cep);

        if (cep == null || cep.isBlank()) {
            log.warn("{} [NEGOCIO] Busca abortada: CEP nulo ou vazio.", LOG_PREFIX);
            return null;
        }

        // 1. Saneamento de Dados (Sanitization)
        String cepLimpo = cep.replaceAll("\\D", "");
        log.trace("{} [NEGOCIO] CEP saneado: {} -> {}", LOG_PREFIX, cep, cepLimpo);

        // 2. Validação de Regra de Negócio
        if (cepLimpo.length() != 8) {
            log.warn("{} [NEGOCIO] Formato de CEP inválido após limpeza: {}", LOG_PREFIX, cepLimpo);
            return null;
        }

        try {
            // 3. Delegação para o Cliente de Infraestrutura
            ViaCepResponse response = viaCepClient.getEndereco(cepLimpo);

            // 4. Tratamento de Resposta de Negócio da API (ViaCEP retorna 200
            // OK com flag 'erro' para CEPs inexistentes)
            if (response != null && Boolean.TRUE.equals(response.erro())) {
                log.warn("{} [AUDITORIA] CEP {} não localizado na base de dados nacional.", LOG_PREFIX, cepLimpo);
                return null;
            }

            if (response != null) {
                log.info("{} [AUDITORIA] Endereço localizado com sucesso: {}, {} - {}", LOG_PREFIX,
                        response.logradouro(), response.localidade(), response.uf());
                return response;
            }

            log.warn("{} [AUDITORIA] Resposta nula recebida do provedor para o CEP: {}", LOG_PREFIX, cepLimpo);
            return null;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica na orquestração da busca de CEP: {}", LOG_PREFIX, e.getMessage());
            return null;
        }
    }
}
