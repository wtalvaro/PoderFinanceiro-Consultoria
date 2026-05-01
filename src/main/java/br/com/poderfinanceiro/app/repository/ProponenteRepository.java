package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProponenteRepository extends JpaRepository<Proponente, Long> {

    /**
     * Lista todos os clientes ativos de um consultor específico.
     */
    List<Proponente> findByUsuarioIdAndDeletadoEmIsNull(Long usuarioId);

    /**
     * Busca um cliente pelo CPF dentro da carteira de um consultor.
     */
    Optional<Proponente> findByCpfAndUsuarioIdAndDeletadoEmIsNull(String cpf, Long usuarioId);

    /**
     * Busca rápida por nome ou CPF para a busca da Sidebar (panel.fxml)[cite: 1].
     */
    List<Proponente> findByNomeCompletoContainingIgnoreCaseOrCpfContainingAndUsuarioIdAndDeletadoEmIsNull(
            String nome, String cpf, Long usuarioId);

    /**
     * Verifica se o consultor já possui este CPF cadastrado[cite: 1].
     */
    boolean existsByCpfAndUsuarioIdAndDeletadoEmIsNull(String cpf, Long usuarioId);
}