package br.com.poderfinanceiro.app.infrastructure.repository;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de Tabelas de Juros. Implementa consultas otimizadas com JOIN
 * FETCH para mitigar o problema de N+1 Select e garantir alta performance em
 * operações de leitura no PostgreSQL.
 */
@Repository
public interface TabelaJurosRepository extends JpaRepository<TabelaJurosModel, Long> {

        /**
         * Lista todas as tabelas de juros ativas de um banco específico com
         * carregamento antecipado.
         */
        @Query("""
                        SELECT t FROM TabelaJurosModel t
                        JOIN FETCH t.banco
                        WHERE t.banco.id = :bancoId
                        AND t.ativo = true
                        """) List<TabelaJurosModel> findByBancoIdAndAtivoTrue(@Param("bancoId") Long bancoId);

        /**
         * Lista tabelas ativas que atendam aos critérios de idade e prazo com
         * JOIN FETCH.
         */
        @Query("""
                        SELECT t FROM TabelaJurosModel t
                        JOIN FETCH t.banco
                        WHERE t.ativo = true
                        AND t.idadeMinima <= :idadeMin
                        AND t.idadeMaxima >= :idadeMax
                        """) List<TabelaJurosModel> findByAtivoTrueAndIdadeMinimaLessThanEqualAndIdadeMaximaGreaterThanEqual(
                        @Param("idadeMin") Integer idadeMin, @Param("idadeMax") Integer idadeMax);

        /**
         * Lista todas as tabelas de juros que estão ativas e não possuem data
         * de fim de vigência.
         */
        @Query("""
                        SELECT t FROM TabelaJurosModel t
                        JOIN FETCH t.banco
                        WHERE t.ativo = true
                        AND t.fimVigencia IS NULL
                        """) List<TabelaJurosModel> findByAtivoTrueAndFimVigenciaIsNull();

        /**
         * Listagem principal de tabelas ativas com banco (Suporta tabelas sem
         * fim e campanhas válidas). Otimizado para exibição em Grids e
         * ComboBoxes.
         */
        @Query("""
                        SELECT t FROM TabelaJurosModel t
                        JOIN FETCH t.banco
                        WHERE t.ativo = true
                        AND (t.fimVigencia IS NULL OR t.fimVigencia >= CURRENT_DATE)
                        """) List<TabelaJurosModel> findAllAtivasWithBanco();

        /**
         * Busca uma tabela específica por ID garantindo o carregamento do banco
         * associado.
         */
        @Query("SELECT t FROM TabelaJurosModel t JOIN FETCH t.banco WHERE t.id = :id") Optional<TabelaJurosModel> findByIdWithBanco(
                        @Param("id") Long id);

        /**
         * Busca uma tabela específica de um banco que esteja ativa para fins de
         * versionamento.
         */
        @Query("""
                        SELECT t FROM TabelaJurosModel t
                        JOIN FETCH t.banco
                        WHERE t.banco.id = :bancoId
                        AND t.nomeTabela = :nomeTabela
                        AND t.ativo = true
                        """) Optional<TabelaJurosModel> findByBancoIdAndNomeTabelaAndAtivoTrue(
                        @Param("bancoId") Long bancoId, @Param("nomeTabela") String nomeTabela);

        /**
         * Motor de busca de elegibilidade para o Copiloto de Vendas. Utiliza
         * JOIN FETCH para garantir que o ranking exiba os nomes dos bancos sem
         * novas consultas.
         */
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
                        """) List<TabelaJurosModel> findTabelasElegiveis(
                        @Param("convenio") TipoConvenioModel convenio, @Param("idade") Integer idade,
                        @Param("valor") BigDecimal valor, @Param("prazo") Integer prazo);
}
