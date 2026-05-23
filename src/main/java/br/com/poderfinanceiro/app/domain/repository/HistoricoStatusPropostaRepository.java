package br.com.poderfinanceiro.app.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import br.com.poderfinanceiro.app.domain.model.HistoricoStatusPropostaModel;
import java.util.List;

@Repository
public interface HistoricoStatusPropostaRepository extends JpaRepository<HistoricoStatusPropostaModel, Long> {

        List<HistoricoStatusPropostaModel> findByPropostaIdOrderByDataMudancaDesc(Long propostaId);

        List<HistoricoStatusPropostaModel> findByUsuarioIdOrderByDataMudancaDesc(Long usuarioId);
}