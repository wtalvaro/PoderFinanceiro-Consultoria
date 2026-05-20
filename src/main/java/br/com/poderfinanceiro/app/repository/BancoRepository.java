package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.BancoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BancoRepository extends JpaRepository<BancoModel, Long> {

    /**
     * Retorna todos os bancos que estão ativos (para preencher os cards).
     */
    List<BancoModel> findByAtivoTrue();

    /**
     * Retorna todos os bancos ativos, ordenados por nome.
     */
    List<BancoModel> findByAtivoTrueOrderByNomeAsc();

    /**
     * Busca um banco específico pelo nome (ignorando maiúsculas e minúsculas).
     */
    Optional<BancoModel> findByNomeIgnoreCase(String nome);

    /**
     * Busca um banco pelo código (Ex: "623", "341"). Útil para integrações futuras.
     */
    Optional<BancoModel> findByCodigo(String codigo);

    /**
     * Busca o primeiro banco que contenha parte do nome (Match Aproximado da IA).
     */
    Optional<BancoModel> findFirstByNomeContainingIgnoreCase(String nomeAproximado);
}