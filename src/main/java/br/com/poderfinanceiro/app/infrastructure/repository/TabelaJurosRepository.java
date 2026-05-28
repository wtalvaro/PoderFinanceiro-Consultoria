package br.com.poderfinanceiro.app.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TabelaJurosRepository extends JpaRepository<TabelaJurosModel, Long> {

    /**
     * Lista todas as tabelas de juros ativas de um banco específico.
     * Útil para filtrar as opções no ComboBox "Tabela Vigente" do simulador.
     */
    List<TabelaJurosModel> findByBancoIdAndAtivoTrue(Long bancoId);

    /**
     * Lista tabelas ativas que atendam aos critérios de idade e prazo.
     * Ideal para o "Match Automático" de taxas mencionado no seu simulator.fxml.
     */
    List<TabelaJurosModel> findByAtivoTrueAndIdadeMinimaLessThanEqualAndIdadeMaximaGreaterThanEqual(
            Integer idadeMin, Integer idadeMax);

    /**
     * Lista todas as tabelas de juros que estão ativas e não possuem data de fim de
     * vigência.
     * Esta consulta é essencial para garantir que o simulador utilize apenas as
     * taxas atualmente vigentes.
     */
    List<TabelaJurosModel> findByAtivoTrueAndFimVigenciaIsNull();

    // Listagem ativa com banco (Suporta tabelas sem fim e campanhas válidas)
    @Query("""
                SELECT t FROM TabelaJurosModel t
                JOIN FETCH t.banco
                WHERE t.ativo = true
                AND (t.fimVigencia IS NULL OR t.fimVigencia >= CURRENT_DATE)
            """)
    List<TabelaJurosModel> findAllAtivasWithBanco();

    // Busca por ID com banco
    @Query("SELECT t FROM TabelaJurosModel t JOIN FETCH t.banco WHERE t.id = :id")
    Optional<TabelaJurosModel> findByIdWithBanco(@Param("id") Long id);

    // 🚀 NOVO MÉTODO: Busca uma tabela específica de um banco que esteja ativa
    // (Usado para o Soft Delete de atualização)
    Optional<TabelaJurosModel> findByBancoIdAndNomeTabelaAndAtivoTrue(Long bancoId, String nomeTabela);

    @Query("""
            SELECT t FROM TabelaJurosModel t
            JOIN FETCH t.banco b
            WHERE t.ativo = true
            AND t.tipoConvenio = :convenio
            AND (:idade IS NULL OR t.idadeMinima IS NULL OR t.idadeMinima = 0 OR t.idadeMinima <= :idade)
            AND (:idade IS NULL OR t.idadeMaxima IS NULL OR t.idadeMaxima = 0 OR t.idadeMaxima >= :idade)
            AND (:valor IS NULL OR t.valorMinimoEmprestimo IS NULL OR t.valorMinimoEmprestimo = 0 OR t.valorMinimoEmprestimo <= :valor)
            AND (:valor IS NULL OR t.valorMaximoEmprestimo IS NULL OR t.valorMaximoEmprestimo = 0 OR t.valorMaximoEmprestimo >= :valor)
            AND (:prazo IS NULL OR t.prazoMinimo IS NULL OR t.prazoMinimo = 0 OR t.prazoMinimo <= :prazo)
            AND (:prazo IS NULL OR t.prazoMaximo IS NULL OR t.prazoMaximo = 0 OR t.prazoMaximo >= :prazo)
            AND (t.fimVigencia IS NULL OR t.fimVigencia >= CURRENT_DATE)
            """)
    List<TabelaJurosModel> findTabelasElegiveis(
            @Param("convenio") TipoConvenioModel convenio,
            @Param("idade") Integer idade,
            @Param("valor") BigDecimal valor,
            @Param("prazo") Integer prazo);

}