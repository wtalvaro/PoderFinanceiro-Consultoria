package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.application.facade.IEnderecoFacade;
import br.com.poderfinanceiro.app.domain.service.ViaCepService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EnderecoFacadeImpl implements IEnderecoFacade {

    private static final Logger log = LoggerFactory.getLogger(EnderecoFacadeImpl.class);
    private static final String LOG_PREFIX = "[EnderecoFacade]";

    private final ViaCepService viaCepService;

    public EnderecoFacadeImpl(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
        log.debug("{} [SISTEMA] Facade de Endereço instanciada.", LOG_PREFIX);
    }

    @Override public ViaCepResponse buscarEnderecoPorCep(String cep) {
        log.info("{} [TELEMETRIA] Solicitando busca de CEP na API externa. CEP: {}", LOG_PREFIX, cep);

        if (cep == null || cep.replaceAll("\\D", "").length() != 8) {
            log.warn("{} [NEGOCIO] Busca bloqueada: CEP inválido ou incompleto.", LOG_PREFIX);
            throw new IllegalArgumentException("O CEP deve conter exatamente 8 números.");
        }

        ViaCepResponse response = viaCepService.buscarEnderecoPorCep(cep.replaceAll("\\D", ""));

        if (response != null) {
            log.info("{} [AUDITORIA] Endereço encontrado com sucesso para o CEP: {}", LOG_PREFIX, cep);
        } else {
            log.warn("{} [NEGOCIO] CEP não encontrado na base dos Correios: {}", LOG_PREFIX, cep);
        }

        return response;
    }
}
