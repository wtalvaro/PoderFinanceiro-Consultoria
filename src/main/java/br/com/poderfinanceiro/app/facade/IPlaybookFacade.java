package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface IPlaybookFacade {

    // --- Consultas e Persistência ---
    List<PlaybookItemModel> listarTodosOsScripts();

    void salvarTodosOsScripts(List<PlaybookItemModel> scripts);

    // --- Regras de Negócio e Filtros ---
    List<PlaybookItemModel> filtrarScripts(String termoBusca);

    String obterNomeConsultorLogado();

    // --- Inteligência Artificial ---
    List<String> listarModelosIADisponiveis();

    JsonNode estruturarTextoComIA(String textoBruto, String modeloEscolhido) throws Exception;
}
