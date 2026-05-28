package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;

public interface IAtendimentoFacade {

    // --- Persistência ---
    ProponenteModel salvarAtendimentoCompleto(ProponenteModel lead,
            br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel endereco);

    // --- Utilitários de Negócio ---
    String gerarResumoParaCopia(ProponenteModel lead, String rendaFormatada);

    String formatarLinkWhatsApp(String telefone);

    // --- Contexto ---
    void limparContextoAtendimento();

    void definirLeadAtivo(ProponenteModel lead);
}
