package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Long> {

    /**
     * Retorna todos os bancos que estão ativos, ideal para popular comboboxes no
     * FXML.
     */
    List<Banco> findByAtivoTrue();

    /**
     * Retorna todos os bancos ativos, ordenados por nome para facilitar a busca do
     * usuário.
     */
    List<Banco> findByAtivoTrueOrderByNomeBancoAsc();

    /**
     * Busca um banco específico pelo nome (ignorando maiúsculas e minúsculas).
     */
    Optional<Banco> findByNomeBancoIgnoreCase(String nomeBanco);
}