package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.TabelaJuros;
import org.springframework.data.jpa.repository.JpaRepository;
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
}