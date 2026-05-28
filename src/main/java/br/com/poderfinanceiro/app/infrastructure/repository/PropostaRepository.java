package br.com.poderfinanceiro.app.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropostaRepository extends JpaRepository<PropostaModel, Long> {

    @Query("""
            SELECT p FROM PropostaModel p
            JOIN FETCH p.proponente
            JOIN FETCH p.banco
            LEFT JOIN FETCH p.tabela
            WHERE p.usuario.id = :usuarioId
            """)
    List<PropostaModel> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // CORREÇÃO: Usar o Enum StatusProposta em vez de String
    List<PropostaModel> findByUsuarioIdAndStatus(Long usuarioId, StatusPropostaModel status);

    // 💉 A CURA: O JOIN FETCH força o Hibernate a trazer o Banco na mesma viagem,
    // evitando que o ComboBox do JavaFX encontre um proxy vazio ao chamar
    // getNome().
    // No PropostaRepository.java
    @Query("SELECT p FROM PropostaModel p JOIN FETCH p.proponente JOIN FETCH p.banco LEFT JOIN FETCH p.tabela WHERE p.proponente.id = :proponenteId ORDER BY p.id DESC")
    List<PropostaModel> findByProponenteId(@Param("proponenteId") Long proponenteId);

    // 🛡️ O DISTINCT garante que, se a proposta tiver 5 documentos,
    // ela apareça apenas UMA vez na lista do Java.
    @Query("SELECT DISTINCT p FROM PropostaModel p " +
            "JOIN FETCH p.proponente " +
            "JOIN FETCH p.banco " +
            "WHERE p.usuario.id = :usuarioId " +
            "ORDER BY p.id DESC")
    List<PropostaModel> buscarProdutividadeDoConsultor(@Param("usuarioId") Long usuarioId);
    
    @Query("""
                SELECT DISTINCT p FROM PropostaModel p
                JOIN FETCH p.proponente
                JOIN FETCH p.banco
                LEFT JOIN FETCH p.tabela
                ORDER BY p.dataSolicitacao DESC
            """)
    List<PropostaModel> findAllComDetalhes();

    @Query("""
                SELECT p FROM PropostaModel p
                JOIN FETCH p.proponente
                JOIN FETCH p.banco
                LEFT JOIN FETCH p.tabela
                WHERE p.id = :id
            """)
    Optional<PropostaModel> findByIdWithDetails(@Param("id") Long id);
}