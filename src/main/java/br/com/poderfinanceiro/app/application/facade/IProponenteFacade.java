package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import java.util.List;

public interface IProponenteFacade {

    // --- Consultas e Listagens ---
    List<ProponenteModel> listarClientesCarteira();

    List<ProponenteModel> buscarClientes(String termoBusca);

    // --- Operações (Caso precise no futuro) ---
    void excluirCliente(Long id);
}
