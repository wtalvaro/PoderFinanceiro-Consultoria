package br.com.poderfinanceiro.app.application.facade;

public interface IMenuFacade {

    // --- Atualizações do Sistema ---
    String checarNovaVersao() throws Exception;

    void baixarEExecutarAtualizacao(String tag) throws Exception;
}
