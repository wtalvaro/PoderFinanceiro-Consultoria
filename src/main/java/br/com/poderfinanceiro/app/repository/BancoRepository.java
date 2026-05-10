package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Long> {

    /**
     * Retorna todos os bancos que estão ativos (para preencher os cards).
     */
    List<Banco> findByAtivoTrue();

    /**
     * Retorna todos os bancos ativos, ordenados por nome.
     */
    List<Banco> findByAtivoTrueOrderByNomeAsc();

    /**
     * Busca um banco específico pelo nome (ignorando maiúsculas e minúsculas).
     */
    Optional<Banco> findByNomeIgnoreCase(String nome);

    /**
     * Busca um banco pelo código (Ex: "623", "341"). Útil para integrações futuras.
     */
    Optional<Banco> findByCodigo(String codigo);
}