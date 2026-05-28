package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import java.util.List;

public interface IBancoFacade {

    // --- Operações de Banco de Dados ---
    List<BancoModel> listarTodos();

    BancoModel salvarBanco(BancoModel banco);

    void excluirBanco(Long id);

    // --- Regras de Negócio e Filtros ---
    List<BancoModel> filtrarBancos(String termoBusca);

    boolean isExclusaoBloqueada(BancoModel banco);

    // --- Utilitários de Formatação ---
    String formatarUrlPortal(String urlOriginal);

    String formatarLinkWhatsApp(String telefone);
}
