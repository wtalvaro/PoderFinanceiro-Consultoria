package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;

public interface IMainFacade {

    // --- Operações de Negócio Globais ---
    PropostaModel converterRascunhoParaProposta(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado, ProponenteModel cliente);

    // --- Sessão ---
    void realizarLogout();
}
