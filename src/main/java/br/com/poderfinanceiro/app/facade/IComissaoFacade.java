package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import java.math.BigDecimal;
import java.util.List;

public interface IComissaoFacade {

    // --- Consultas e Persistência ---
    List<ComissaoModel> listarComissoes();

    ComissaoModel buscarComissaoPorId(Long id);

    ComissaoModel salvarConciliacao(ComissaoModel comissao);

    // --- Contexto Global ---
    void atualizarContextoComissoes(List<ComissaoModel> comissoes);

    // --- Regras de Negócio e Cálculos ---
    BigDecimal calcularTotalPendente(List<ComissaoModel> comissoes);

    BigDecimal calcularTotalRecebido(List<ComissaoModel> comissoes);

    String resolverNomeCiclo(ComissaoModel comissao);

    boolean isTravaQuintaFeiraAtiva();
}
