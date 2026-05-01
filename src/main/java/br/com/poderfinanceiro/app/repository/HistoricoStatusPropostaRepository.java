package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.HistoricoStatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoStatusPropostaRepository extends JpaRepository<HistoricoStatusProposta, Long> {

    /**
     * Busca toda a linha do tempo (histórico) de uma proposta específica,
     * ordenando da alteração mais recente (DESC) para a mais antiga.
     */
    List<HistoricoStatusProposta> findByPropostaIdOrderByDataMudancaDesc(Long propostaId);

    /**
     * Busca todas as alterações feitas por um usuário específico.
     * Útil para relatórios de produtividade/auditoria do backoffice.
     */
    List<HistoricoStatusProposta> findByUsuarioIdOrderByDataMudancaDesc(Long usuarioId);
}