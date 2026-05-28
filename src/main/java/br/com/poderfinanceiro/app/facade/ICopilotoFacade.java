package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;

import java.io.File;
import java.util.List;

public interface ICopilotoFacade {

    // --- Consultas ---
    List<ProponenteModel> listarClientesCarteira();

    List<String> listarModelosIADisponiveis();

    // --- Simulação e IA ---
    List<ResultadoSimulacaoDTO> processarSimulacaoRapida(SimulacaoRascunhoDTO rascunho);

    String extrairMargemDocumento(File arquivo);

    // --- Orquestração Complexa ---
    record ConselhoIADTO(String textoResposta, List<ResultadoSimulacaoDTO> rankingReordenado, List<Integer> indicesRecomendados) {
    }

    ConselhoIADTO gerarConselhoEReordenarRanking(SimulacaoRascunhoDTO rascunho, List<ResultadoSimulacaoDTO> rankingAtual,
            String modeloEscolhido);

    // --- Utilitários de Negócio ---
    int calcularIdade(java.time.LocalDate dataNascimento);
}
