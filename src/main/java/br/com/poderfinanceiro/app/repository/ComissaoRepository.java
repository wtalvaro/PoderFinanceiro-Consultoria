package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Comissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComissaoRepository extends JpaRepository<Comissao, Long> {

    /**
     * Busca todo o histórico de comissões de um consultor específico.
     * Útil para popular a tabela de "Controle de Comissões (RV)".
     */
    List<Comissao> findByUsuarioId(Long usuarioId);

    /**
     * Busca comissões de um consultor filtrando pelo status (ex: "Pendente" ou
     * "Pago").
     * Útil para os cards de resumo financeiro do Dashboard.
     */
    List<Comissao> findByUsuarioIdAndStatusPagamento(Long usuarioId, String statusPagamento);

    /**
     * Busca as comissões atreladas a uma proposta específica.
     */
    List<Comissao> findByPropostaId(Long propostaId);

    /**
     * Busca as comissões e já carrega (FETCH) a proposta, o proponente e o banco juntos.
     * Evita o erro de LazyInitializationException no JavaFX.
     */
    @Query("SELECT c FROM Comissao c JOIN FETCH c.proposta p JOIN FETCH p.proponente JOIN FETCH p.banco")
    List<Comissao> findAllComDetalhes();
}