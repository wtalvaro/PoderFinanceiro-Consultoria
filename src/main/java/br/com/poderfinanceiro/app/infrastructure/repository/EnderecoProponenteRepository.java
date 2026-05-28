package br.com.poderfinanceiro.app.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoProponenteRepository extends JpaRepository<EnderecoProponenteModel, Long> {

    /**
     * Busca o endereço atual vinculado a um proponente específico.
     * Útil para carregar a ficha do lead no JavaFX.
     */
    Optional<EnderecoProponenteModel> findByProponenteId(Long proponenteId);

    /**
     * Caso precise de uma lista de endereços por proponente
     * (útil se decidir manter histórico de mudanças).
     */
    List<EnderecoProponenteModel> findAllByProponenteOrderByCriadoEmDesc(ProponenteModel proponente);

    /**
     * Verifica se já existe um endereço cadastrado para aquele proponente.
     */
    boolean existsByProponenteId(Long proponenteId);
}