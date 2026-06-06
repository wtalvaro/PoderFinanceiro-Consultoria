package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.enums.AppRoute;

public interface IWorkspaceFacade {

    // --- Gestão de Contexto Global ---
    void atualizarContextoParaRota(AppRoute rota);

    void atualizarContextoParaAtendimento(ProponenteModel proponente);

    void resetarContextoParaDashboard();
}
