package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.model.StatusProposta; // Importe o Enum
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    List<Proposta> findByUsuarioId(Long usuarioId);

    // CORREÇÃO: Usar o Enum StatusProposta em vez de String
    List<Proposta> findByUsuarioIdAndStatus(Long usuarioId, StatusProposta status);

    List<Proposta> findByProponenteId(Long proponenteId);
}