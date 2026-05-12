package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.TabelaJuros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TabelaJurosRepository extends JpaRepository<TabelaJuros, Long> {

    /**
     * Lista todas as tabelas de juros ativas de um banco específico.
     * Útil para filtrar as opções no ComboBox "Tabela Vigente" do simulador.
     */
    List<TabelaJuros> findByBancoIdAndAtivoTrue(Long bancoId);

    /**
     * Lista tabelas ativas que atendam aos critérios de idade e prazo.
     * Ideal para o "Match Automático" de taxas mencionado no seu simulator.fxml.
     */
    List<TabelaJuros> findByAtivoTrueAndIdadeMinimaLessThanEqualAndIdadeMaximaGreaterThanEqual(
            Integer idadeMin, Integer idadeMax);

    /**
     * Lista todas as tabelas de juros que estão ativas e não possuem data de fim de
     * vigência.
     * Esta consulta é essencial para garantir que o simulador utilize apenas as
     * taxas atualmente vigentes.
     */
    List<TabelaJuros> findByAtivoTrueAndFimVigenciaIsNull();

    // O "JOIN FETCH t.banco" é o bisturi mágico. Ele obriga o Hibernate a 
    // trazer o objeto Banco real junto com a Tabela em uma única viagem!
    @Query("SELECT t FROM TabelaJuros t JOIN FETCH t.banco WHERE t.ativo = true AND t.fimVigencia IS NULL")
    List<TabelaJuros> buscarTodasAtivasComBanco();

}