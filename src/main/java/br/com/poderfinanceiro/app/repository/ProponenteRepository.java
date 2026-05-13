package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProponenteRepository extends JpaRepository<ProponenteModel, Long> {

    List<ProponenteModel> findByUsuarioIdAndDeletadoEmIsNull(Long usuarioId);

    Optional<ProponenteModel> findByCpfAndUsuarioIdAndDeletadoEmIsNull(String cpf, Long usuarioId);

    boolean existsByCpfAndUsuarioIdAndDeletadoEmIsNull(String cpf, Long usuarioId);

    // ADICIONE ESTE MÉTODO: Ele permite verificar duplicidade ignorando o registro
    // atual
    boolean existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(String cpf, Long usuarioId, Long id);

    // ✅ CORREÇÃO 1: Adicionado o "Model" no nome da Entidade
    @Query("SELECT p FROM ProponenteModel p WHERE p.usuario.id = :usuarioId AND p.deletadoEm IS NULL AND " +
            "(LOWER(p.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%')) OR p.cpf LIKE CONCAT('%', :termo, '%'))")
    List<ProponenteModel> buscarRapidaPorNomeOuCpf(@Param("termo") String termo, @Param("usuarioId") Long usuarioId);

    // ✅ CORREÇÃO 2: Adicionado o "Model" na busca por ID com endereços
    @Query("SELECT p FROM ProponenteModel p LEFT JOIN FETCH p.enderecos WHERE p.id = :id")
    Optional<ProponenteModel> findByIdWithEnderecos(@Param("id") Long id);

    // Busca todas as propostas de um consultor específico, ordenadas pela mais
    // recente
    List<PropostaModel> findByUsuarioId(Long usuarioId);
}