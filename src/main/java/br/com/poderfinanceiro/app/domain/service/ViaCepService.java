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
        // Inicializa o cliente HTTP apontando para o servidor do ViaCEP
        this.restClient = RestClient.builder().baseUrl("https://viacep.com.br/ws/").build();
    }

    public ViaCepResponse buscarEnderecoPorCep(String cep) {
        // Limpa a string (tira hífen, pontos, espaços) para garantir
        String cepLimpo = cep.replaceAll("\\D", "");

        if (cepLimpo.length() != 8) {
            return null; // Ignora se não tiver o tamanho exato de um CEP
        }

        try {
            ViaCepResponse response = restClient.get()
                    .uri(cepLimpo + "/json/")
                    .retrieve()
                    .body(ViaCepResponse.class);

            // Se o ViaCEP não encontrar a rua, ele devolve um JSON com { "erro": true }
            if (response != null && Boolean.TRUE.equals(response.erro())) {
                return null;
            }

            return response;

        } catch (Exception e) {
            log.error(("⚠️ Falha ao comunicar com o ViaCEP: " + e.getMessage()));
            return null;
        }
    }
}