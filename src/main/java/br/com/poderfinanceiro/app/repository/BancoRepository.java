package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Long> {

    /**
     * Retorna todos os bancos que estão marcados como ativos.
     * Útil para exibir no formulário da Solange.
     */
    List<Banco> findByAtivoTrueOrderByNomeBancoAsc();

    /**
     * Busca um banco pelo nome exato (Ex: 'Real Grandeza').
     */
    Banco findByNomeBancoIgnoreCase(String nomeBanco);
}