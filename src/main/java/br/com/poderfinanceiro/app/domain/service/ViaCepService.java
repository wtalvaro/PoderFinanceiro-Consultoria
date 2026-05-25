package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.ViaCepResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ViaCepService {

    private static final Logger log = LoggerFactory.getLogger(ViaCepService.class);

    private final RestClient restClient;

    public ViaCepService() {
        log.debug("[VIA_CEP] Construtor: Inicializando cliente RestClient com base URL https://viacep.com.br/ws/");
        this.restClient = RestClient.builder().baseUrl("https://viacep.com.br/ws/").build();
        log.info("[VIA_CEP] Construtor: ViaCepService instanciado");
    }

    public ViaCepResponse buscarEnderecoPorCep(String cep) {
        log.debug("[VIA_CEP] buscarEnderecoPorCep: Iniciando busca para CEP='{}'", cep);

        String cepLimpo = cep.replaceAll("\\D", "");
        log.trace("[VIA_CEP] CEP limpo: '{}' -> '{}'", cep, cepLimpo);

        if (cepLimpo.length() != 8) {
            log.warn("[VIA_CEP] buscarEnderecoPorCep: CEP '{}' possui tamanho inválido ({}), retornando null", cep,
                    cepLimpo.length());
            return null;
        }

        try {
            log.debug("[VIA_CEP] Chamando API ViaCEP para CEP={}", cepLimpo);
            ViaCepResponse response = restClient.get()
                    .uri(cepLimpo + "/json/")
                    .retrieve()
                    .body(ViaCepResponse.class);

            if (response != null && Boolean.TRUE.equals(response.erro())) {
                log.warn("[VIA_CEP] ViaCEP retornou erro para CEP={}, endereço não encontrado", cepLimpo);
                return null;
            }

            if (response != null) {
                log.info("[VIA_CEP] Endereço encontrado para CEP={}: logradouro='{}', localidade='{}', uf='{}'",
                        cepLimpo, response.logradouro(), response.localidade(), response.uf());
            } else {
                log.warn("[VIA_CEP] Resposta nula do ViaCEP para CEP={}", cepLimpo);
            }

            return response;

        } catch (Exception e) {
            log.error("[VIA_CEP] Falha ao comunicar com o ViaCEP para CEP={}: {}", cepLimpo, e.getMessage(), e);
            return null;
        }
    }
}