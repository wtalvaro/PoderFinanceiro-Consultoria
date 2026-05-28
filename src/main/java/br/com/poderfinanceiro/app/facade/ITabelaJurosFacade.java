package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;

import java.util.List;

public interface ITabelaJurosFacade {

    // --- Consultas ---
    List<TabelaJurosModel> listarTabelasAtivas();

    List<BancoModel> listarBancosAtivos();

    // --- Operações de Banco de Dados ---
    TabelaJurosModel salvarTabela(TabelaJurosModel tabela);

    void arquivarTabela(TabelaJurosModel tabela);

    // --- Regras de Negócio e Filtros ---
    List<TabelaJurosModel> filtrarTabelas(String termoBusca);
}
