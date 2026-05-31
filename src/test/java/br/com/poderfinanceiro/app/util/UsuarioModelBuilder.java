package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;

/**
 * Object Mother para UsuarioModel.
 * Garante estados consistentes para testes de autenticação e segurança.
 */
public class UsuarioModelBuilder {

    private final UsuarioModel instance;

    private UsuarioModelBuilder() {
        instance = new UsuarioModel();
        instance.setNome("Wagner Consultor");
        instance.setUsername("wagner_pf");
        instance.setEmail("wagner@poderfinanceiro.com.br");
        instance.setSenhaHash("$2a$10$fakehash");
        instance.setAtivo(true);
        instance.setPapel("CONSULTOR");
    }

    public static UsuarioModelBuilder umUsuario() {
        return new UsuarioModelBuilder();
    }

    public UsuarioModelBuilder comUsername(String username) {
        instance.setUsername(username);
        // Email único derivado do username para evitar violação de UNIQUE
        instance.setEmail(username + "@poderfinanceiro.com.br");
        return this;
    }

    public UsuarioModelBuilder comEmail(String email) {
        instance.setEmail(email);
        return this;
    }

    public UsuarioModelBuilder inativo() {
        instance.setAtivo(false);
        return this;
    }

    public UsuarioModel build() {
        return instance;
    }
}
