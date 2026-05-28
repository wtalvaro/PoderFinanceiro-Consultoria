package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import java.util.List;

public interface ILinkUtilFacade {

    // --- Consultas ---
    List<LinkUtilModel> listarTodosOsLinks();

    // --- Operações de Banco de Dados ---
    LinkUtilModel salvarLink(LinkUtilModel link);

    void excluirLink(Long id);

    // --- Regras de Negócio e Filtros ---
    List<LinkUtilModel> filtrarLinks(String termoBusca);
}
