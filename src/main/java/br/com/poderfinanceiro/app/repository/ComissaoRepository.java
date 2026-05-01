package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Comissao;
import org.springframework.data.jpa.repository.JpaRepository;
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
}