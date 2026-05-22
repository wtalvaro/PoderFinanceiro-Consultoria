package br.com.poderfinanceiro.app.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioModel, Long> {
    // Usado para o Login
    Optional<UsuarioModel> findByUsernameAndAtivoTrue(String username);

    // Usado para checagem de cadastro
    Optional<UsuarioModel> findByUsername(String username);

    Optional<UsuarioModel> findByEmail(String email);
}