package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.UsuarioModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioModel, Long> {
    // Usado para o Login
    Optional<UsuarioModel> findByUsernameAndAtivoTrue(String username);

    // Usado para checagem de cadastro
    Optional<UsuarioModel> findByUsername(String username);

    Optional<UsuarioModel> findByEmail(String email);
}