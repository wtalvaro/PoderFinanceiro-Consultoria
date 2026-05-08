package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.DocumentoProponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoProponenteRepository extends JpaRepository<DocumentoProponente, Long> {

    /**
     * Busca todos os documentos de um proponente mantendo a ordem de inserção.
     * O OrderByIdAsc garante que a linha não pule para o final ao ser atualizada.
     */
    List<DocumentoProponente> findByProponenteIdOrderByIdAsc(Long proponenteId);

    /**
     * Busca por tipo mantendo a ordenação estável.
     */
    List<DocumentoProponente> findByProponenteIdAndTipoDocumentoOrderByIdAsc(Long proponenteId, String tipoDocumento);

    /**
     * Busca um documento pelo seu Hash SHA-256.
     * (Não precisa de OrderBy pois o Hash é único, retorna apenas 0 ou 1 registro).
     */
    Optional<DocumentoProponente> findByHashSha256(String hashSha256);

    /**
     * Lista documentos pendentes de verificação.
     * Aqui usamos OrderByIdAsc para o auditor ver os documentos mais antigos
     * primeiro.
     */
    List<DocumentoProponente> findByVerificadoFalseOrderByIdAsc();
}