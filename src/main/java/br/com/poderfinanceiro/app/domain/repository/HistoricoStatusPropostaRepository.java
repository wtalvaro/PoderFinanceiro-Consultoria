package br.com.poderfinanceiro.app.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.HistoricoStatusPropostaModel;

import java.util.List;

@Repository
public interface HistoricoStatusPropostaRepository extends JpaRepository<HistoricoStatusPropostaModel, Long> {

    /**
     * Busca toda a linha do tempo (histórico) de uma proposta específica,
     * ordenando da alteração mais recente (DESC) para a mais antiga.
     */
    List<HistoricoStatusPropostaModel> findByPropostaIdOrderByDataMudancaDesc(Long propostaId);

    /**
     * Busca todas as alterações feitas por um usuário específico.
     * Útil para relatórios de produtividade/auditoria do backoffice.
     */
    List<HistoricoStatusPropostaModel> findByUsuarioIdOrderByDataMudancaDesc(Long usuarioId);
}