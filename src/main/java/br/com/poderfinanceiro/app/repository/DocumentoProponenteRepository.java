package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoProponenteRepository extends JpaRepository<DocumentoProponenteModel, Long> {

    List<DocumentoProponenteModel> findByProponenteIdAndPropostaIdIsNull(Long proponenteId);
    /**
     * Busca todos os documentos de um proponente mantendo a ordem de inserção.
     * O OrderByIdAsc garante que a linha não pule para o final ao ser atualizada.
     */
    List<DocumentoProponenteModel> findByProponenteIdOrderByIdAsc(Long proponenteId);

    /**
     * Busca por tipo mantendo a ordenação estável.
     */
    List<DocumentoProponenteModel> findByProponenteIdAndTipoDocumentoOrderByIdAsc(Long proponenteId, String tipoDocumento);

    /**
     * Busca um documento pelo seu Hash SHA-256.
     * (Não precisa de OrderBy pois o Hash é único, retorna apenas 0 ou 1 registro).
     */
    Optional<DocumentoProponenteModel> findByHashSha256(String hashSha256);

    /**
     * Lista documentos pendentes de verificação.
     * Aqui usamos OrderByIdAsc para o auditor ver os documentos mais antigos
     * primeiro.
     */
    List<DocumentoProponenteModel> findByVerificadoFalseOrderByIdAsc();

    // Busca exames gerais do paciente (Lead)
    List<DocumentoProponenteModel> findByProponenteIdAndPropostaIsNull(Long proponenteId);

    // Busca exames específicos de uma internação (Proposta)
    List<DocumentoProponenteModel> findByPropostaId(Long propostaId);
}