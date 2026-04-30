package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.model.StatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    // Busca todas as propostas de um cliente específico
    List<Proposta> findByProponenteIdOrderByDataSolicitacaoDesc(Long proponenteId);

    // Filtra propostas por status (Útil para a Tabela Principal)
    List<Proposta> findByStatusOrderByUltimaAtualizacaoDesc(StatusProposta status);

    // Conta quantas propostas estão em um status específico (Útil para os Cards do
    // Dashboard)
    long countByStatus(StatusProposta status);

    // Busca customizada para trazer as últimas propostas com fetch nos
    // relacionamentos
    // Isso evita o problema de "N+1 queries" deixando a tabela do JavaFX super
    // rápida
    @Query("""
                SELECT p FROM Proposta p
                JOIN FETCH p.proponente
                JOIN FETCH p.banco
                ORDER BY p.dataSolicitacao DESC
            """)
    List<Proposta> findAllWithDetails();
}