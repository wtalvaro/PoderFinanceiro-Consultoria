package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;

public interface IEnderecoFacade {

    // --- Integrações Externas ---
    ViaCepResponse buscarEnderecoPorCep(String cep);
}
