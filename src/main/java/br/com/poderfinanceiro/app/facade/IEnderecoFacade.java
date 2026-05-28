package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.dto.ViaCepResponse;

public interface IEnderecoFacade {

    // --- Integrações Externas ---
    ViaCepResponse buscarEnderecoPorCep(String cep);
}
