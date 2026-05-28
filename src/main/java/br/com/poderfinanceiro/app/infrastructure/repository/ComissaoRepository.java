package br.com.poderfinanceiro.app.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;

import java.util.List;

@Repository
public interface ComissaoRepository extends JpaRepository<ComissaoModel, Long> {

    /**
     * Busca todo o histórico de comissões de um consultor específico.
     * Útil para popular a tabela de "Controle de Comissões (RV)".
     */
    List<ComissaoModel> findByUsuarioId(Long usuarioId);

    /**
     * Busca comissões de um consultor filtrando pelo status (ex: "Pendente" ou
     * "Pago").
     * Útil para os cards de resumo financeiro do Dashboard.
     */
    List<ComissaoModel> findByUsuarioIdAndStatusPagamento(Long usuarioId, String statusPagamento);

    /**
     * Busca as comissões atreladas a uma proposta específica.
     */
    List<ComissaoModel> findByPropostaId(Long propostaId);

    /**
     * Busca as comissões e já carrega (FETCH) a proposta, o proponente e o banco juntos.
     * Evita o erro de LazyInitializationException no JavaFX.
     */
    @Query("SELECT c FROM ComissaoModel c JOIN FETCH c.proposta p JOIN FETCH p.proponente JOIN FETCH p.banco")
    List<ComissaoModel> findAllComDetalhes();
}