package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;

public interface IWorkspaceFacade {

    // --- Gestão de Contexto Global ---
    void atualizarContextoParaRota(RotaAba rota);

    void atualizarContextoParaAtendimento(ProponenteModel proponente);

    void resetarContextoParaDashboard();
}
