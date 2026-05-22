package br.com.poderfinanceiro.app.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.InteracaoContatoModel;

import java.util.List;

@Repository
public interface InteracaoContatoRepository extends JpaRepository<InteracaoContatoModel, Long> {

    /**
     * Busca todas as interações de um cliente específico.
     * Ordenado da mais recente para a mais antiga (DESC) para exibir o chat
     * corretamente[cite: 1].
     */
    List<InteracaoContatoModel> findByProponenteIdOrderByDataInteracaoDesc(Long proponenteId);

    /**
     * Busca interações de um cliente filtradas por canal (ex: apenas WhatsApp).
     */
    List<InteracaoContatoModel> findByProponenteIdAndCanalOrderByDataInteracaoDesc(Long proponenteId, String canal);

    /**
     * Lista as últimas interações realizadas por um consultor específico.
     * Útil para relatórios de produtividade diária.
     */
    List<InteracaoContatoModel> findByUsuarioIdOrderByDataInteracaoDesc(Long usuarioId);
}