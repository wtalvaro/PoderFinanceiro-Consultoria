package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.service.AssistenteDocumentalService;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

public interface IPropostaFacade {
    // --- Dados e Listas ---
    List<TabelaJurosModel> listarTabelasAtivas();

    List<BancoModel> listarBancosDasTabelasAtivas();

    List<ProponenteModel> listarClientesCarteira();

    PropostaModel carregarPropostaCompleta(Long id);

    // --- Operações de Proposta ---
    PropostaModel salvarProposta(PropostaModel model);

    void excluirProposta(Long id);

    boolean isStatusTerminal(StatusPropostaModel status, Long id);

    BigDecimal calcularComissao(BigDecimal valorBase, Long tabelaId);

    // --- Gestão de Documentos ---
    List<DocumentoProponenteModel> buscarDocumentosDaProposta(Long propostaId);

    DocumentoProponenteModel salvarDocumento(File arquivo, String tipo, ProponenteModel proponente,
            PropostaModel proposta) throws Exception;

    DocumentoProponenteModel atualizarTipoDocumento(Long docId, String novoTipo) throws Exception;

    void excluirDocumento(Long docId);

    // --- Contexto de UI ---
    void atualizarContextoAtendimento(PropostaModel proposta);

    // --- Inteligência Artificial ---
    List<String> listarModelosIADisponiveis();

    AssistenteDocumentalService.ConfigIA obterConfiguracaoIADocumento(String tipoDocumento);

    String analisarDocumentoComIA(DocumentoProponenteModel doc, ProponenteModel proponente, String modelo);

    // --- Regras de Negócio (Substituindo as antigas) ---
    BigDecimal calcularComissao(PropostaModel proposta); // Passamos a proposta inteira

    boolean isBloqueadaPeloCopiloto(PropostaModel proposta);
}
