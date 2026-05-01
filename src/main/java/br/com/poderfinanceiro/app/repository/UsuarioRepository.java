package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca um usuário pelo e-mail exato.
     * Retorna um Optional para evitar NullPointerException caso o usuário não
     * exista.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca um usuário pelo e-mail, mas APENAS se ele estiver com a conta ativa.
     * Perfeito para a validação de login na tela inicial.
     */
    Optional<Usuario> findByEmailAndAtivoTrue(String email);
}