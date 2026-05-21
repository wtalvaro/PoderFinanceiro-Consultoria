package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // Listagem ativa com banco
    @Query("""
                SELECT t FROM TabelaJurosModel t
                JOIN FETCH t.banco
                WHERE t.ativo = true AND t.fimVigencia IS NULL
            """)
    List<TabelaJurosModel> findAllAtivasWithBanco();

    // Busca por ID com banco
    @Query("SELECT t FROM TabelaJurosModel t JOIN FETCH t.banco WHERE t.id = :id")
    Optional<TabelaJurosModel> findByIdWithBanco(@Param("id") Long id);

    // 🚀 NOVO MÉTODO: Busca uma tabela específica de um banco que esteja ativa
    // (Usado para o Soft Delete de atualização)
    Optional<TabelaJurosModel> findByBancoIdAndNomeTabelaAndAtivoTrue(Long bancoId, String nomeTabela);

}