package br.com.poderfinanceiro.app.facade;

public interface IMenuFacade {

    // --- Atualizações do Sistema ---
    String checarNovaVersao() throws Exception;

    void baixarEExecutarAtualizacao(String tag) throws Exception;
}
