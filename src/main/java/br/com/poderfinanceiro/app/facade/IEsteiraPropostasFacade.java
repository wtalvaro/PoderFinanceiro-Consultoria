package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import java.util.List;

public interface IEsteiraPropostasFacade {

    // --- Consultas ---
    List<PropostaModel> listarPropostasDoUsuario();

    // --- Operações de Banco de Dados ---
    PropostaModel criarNovaPropostaEmBranco();

    // --- Regras de Negócio e Filtros ---
    List<PropostaModel> filtrarPropostas(String termoBusca);
}
