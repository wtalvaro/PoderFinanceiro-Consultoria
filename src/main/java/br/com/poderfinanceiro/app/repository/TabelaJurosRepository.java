package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.TabelaJuros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TabelaJurosRepository extends JpaRepository<TabelaJuros, Long> {

    /**
     * Busca tabelas compatíveis com o perfil do cliente e o banco selecionado.
     * Otimizado para os índices criados no banco.
     */
    @Query("""
                SELECT t FROM TabelaJuros t
                WHERE t.banco.id = :bancoId
                AND :idade BETWEEN t.idadeMinima AND t.idadeMaxima
                AND :renda >= t.rendaMinima
                AND t.ativo = true
                ORDER BY t.taxaMensal ASC
            """)
    List<TabelaJuros> findTabelasCompativeis(
            @Param("bancoId") Long bancoId,
            @Param("idade") Integer idade,
            @Param("renda") BigDecimal renda);
}