package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    /**
     * Busca todas as propostas atreladas a um consultor[cite: 1].
     */
    List<Proposta> findByUsuarioId(Long usuarioId);

    /**
     * Filtra propostas por status e consultor (ideal para o Dashboard)[cite: 1].
     */
    List<Proposta> findByUsuarioIdAndStatus(Long usuarioId, String status);

    /**
     * Lista propostas de um cliente específico[cite: 1].
     */
    List<Proposta> findByProponenteId(Long proponenteId);
}