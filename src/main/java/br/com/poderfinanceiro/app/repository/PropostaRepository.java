package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.model.enums.StatusProposta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    List<Proposta> findByUsuarioId(Long usuarioId);

    // CORREÇÃO: Usar o Enum StatusProposta em vez de String
    List<Proposta> findByUsuarioIdAndStatus(Long usuarioId, StatusProposta status);

    List<Proposta> findByProponenteId(Long proponenteId);

    /**
     * Busca as propostas e já traz os dados do Proponente e do Banco juntos.
     * Isso evita o erro de LazyInitializationException no JavaFX (Tabela em branco).
     */
    @Query("SELECT p FROM Proposta p JOIN FETCH p.proponente JOIN FETCH p.banco")
    List<Proposta> findAllComDetalhes();
}