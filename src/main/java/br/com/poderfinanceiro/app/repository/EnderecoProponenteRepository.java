package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.EnderecoProponente;
import br.com.poderfinanceiro.app.model.Proponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoProponenteRepository extends JpaRepository<EnderecoProponente, Long> {

    /**
     * Busca o endereço atual vinculado a um proponente específico.
     * Útil para carregar a ficha do lead no JavaFX.
     */
    Optional<EnderecoProponente> findByProponenteId(Long proponenteId);

    /**
     * Caso precise de uma lista de endereços por proponente
     * (útil se decidir manter histórico de mudanças).
     */
    List<EnderecoProponente> findAllByProponenteOrderByCriadoEmDesc(Proponente proponente);

    /**
     * Verifica se já existe um endereço cadastrado para aquele proponente.
     */
    boolean existsByProponenteId(Long proponenteId);
}