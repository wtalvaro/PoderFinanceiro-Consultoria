package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;

public interface IAuthFacade {

    // --- Operações de Autenticação e Cadastro ---
    boolean realizarLogin(String username, String senha);

    UsuarioModel cadastrarUsuario(String nome, String username, String email, String senha, String geminiApiKey);

    void realizarLogout();

    // --- Consultas de Sessão ---
    boolean isUsuarioLogado();

    UsuarioModel getUsuarioLogado();
}
