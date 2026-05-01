package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.InteracaoContato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteracaoContatoRepository extends JpaRepository<InteracaoContato, Long> {

    /**
     * Busca todas as interações de um cliente específico.
     * Ordenado da mais recente para a mais antiga (DESC) para exibir o chat
     * corretamente[cite: 1].
     */
    List<InteracaoContato> findByProponenteIdOrderByDataInteracaoDesc(Long proponenteId);

    /**
     * Busca interações de um cliente filtradas por canal (ex: apenas WhatsApp).
     */
    List<InteracaoContato> findByProponenteIdAndCanalOrderByDataInteracaoDesc(Long proponenteId, String canal);

    /**
     * Lista as últimas interações realizadas por um consultor específico.
     * Útil para relatórios de produtividade diária.
     */
    List<InteracaoContato> findByUsuarioIdOrderByDataInteracaoDesc(Long usuarioId);
}