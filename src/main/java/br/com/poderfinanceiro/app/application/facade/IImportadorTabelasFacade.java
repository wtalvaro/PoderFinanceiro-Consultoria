package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.domain.model.BancoModel;

import java.io.File;
import java.util.List;

public interface IImportadorTabelasFacade {

    // --- Consultas ---
    List<String> listarModelosIADisponiveis();

    List<BancoModel> listarBancosAtivos();

    // --- Inteligência Artificial ---
    List<TabelaImportadaDTO> extrairTabelasDeImagem(File arquivo, String modeloEscolhido) throws Exception;

    // --- Operações de Banco de Dados ---
    void salvarLoteTabelas(List<TabelaImportadaDTO> lote);
}
