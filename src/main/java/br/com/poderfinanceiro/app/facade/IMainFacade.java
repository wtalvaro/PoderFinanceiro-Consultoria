package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;

public interface IMainFacade {

    // --- Operações de Negócio Globais ---
    PropostaModel converterRascunhoParaProposta(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado, ProponenteModel cliente);

    // --- Sessão ---
    void realizarLogout();
}
