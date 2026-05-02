package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProponenteRepository extends JpaRepository<Proponente, Long> {

    List<Proponente> findByUsuarioIdAndDeletadoEmIsNull(Long usuarioId);

    Optional<Proponente> findByCpfAndUsuarioIdAndDeletadoEmIsNull(String cpf, Long usuarioId);

    boolean existsByCpfAndUsuarioIdAndDeletadoEmIsNull(String cpf, Long usuarioId);

    // ADICIONE ESTE MÉTODO: Ele permite verificar duplicidade ignorando o registro
    // atual
    boolean existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(String cpf, Long usuarioId, Long id);

    @Query("SELECT p FROM Proponente p WHERE p.usuario.id = :usuarioId AND p.deletadoEm IS NULL AND " +
            "(LOWER(p.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%')) OR p.cpf LIKE CONCAT('%', :termo, '%'))")
    List<Proponente> buscarRapidaPorNomeOuCpf(@Param("termo") String termo, @Param("usuarioId") Long usuarioId);
}