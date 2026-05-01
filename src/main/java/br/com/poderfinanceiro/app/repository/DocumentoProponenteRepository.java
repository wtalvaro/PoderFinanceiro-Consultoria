package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.DocumentoProponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoProponenteRepository extends JpaRepository<DocumentoProponente, Long> {

    /**
     * Busca todos os documentos atrelados a um proponente (cliente) específico.
     * Útil para carregar a galeria de documentos na tela de detalhes do cliente.
     */
    List<DocumentoProponente> findByProponenteId(Long proponenteId);

    /**
     * Busca todos os documentos de um cliente filtrando pelo tipo (ex: "RG",
     * "Comprovante de Endereço").
     */
    List<DocumentoProponente> findByProponenteIdAndTipoDocumento(Long proponenteId, String tipoDocumento);

    /**
     * Busca um documento pelo seu Hash SHA-256.
     * Excelente prática de segurança e economia de armazenamento para evitar
     * que o consultor faça o upload do mesmo arquivo PDF/imagem duas vezes.
     */
    Optional<DocumentoProponente> findByHashSha256(String hashSha256);

    /**
     * Lista documentos que ainda não foram verificados (útil para
     * auditoria/backoffice).
     */
    List<DocumentoProponente> findByVerificadoFalse();
}