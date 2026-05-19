package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.ComissaoModel;
import br.com.poderfinanceiro.app.model.PropostaModel; // 🚀 Import necessário
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AtendimentoContextService {

    public enum TipoTelaFocada {
        DASHBOARD,
        LISTA_CLIENTES,
        CADASTRO_CLIENTE,
        ESTEIRA_PROPOSTAS, // 🚀 NOVO: Foco na esteira de crédito
        TABELAS_JUROS,
        LINKS_UTEIS,
        GESTAO_COMISSOES
    }

    private ProponenteModel leadAtivo;
    private PropostaModel propostaAtiva; // 🚀 NOVO: Contexto da proposta em edição
    private List<ComissaoModel> comissoesAtivas;
    private TipoTelaFocada telaAtualFocada = TipoTelaFocada.DASHBOARD;

    // --- GETTERS E SETTERS ---

    public ProponenteModel getLeadAtivo() {
        return leadAtivo;
    }

    public void setLeadAtivo(ProponenteModel leadAtivo) {
        this.leadAtivo = leadAtivo;
    }

    // 🚀 Novos métodos para a proposta
    public PropostaModel getPropostaAtiva() {
        return propostaAtiva;
    }

    public void setPropostaAtiva(PropostaModel propostaAtiva) {
        this.propostaAtiva = propostaAtiva;
    }

    public List<ComissaoModel> getComissoesAtivas() {
        return comissoesAtivas;
    }

    public void setComissoesAtivas(List<ComissaoModel> comissoesAtivas) {
        this.comissoesAtivas = comissoesAtivas;
    }

    public TipoTelaFocada getTelaAtualFocada() {
        return telaAtualFocada;
    }

    public void setTelaAtualFocada(TipoTelaFocada telaAtualFocada) {
        this.telaAtualFocada = telaAtualFocada;
    }

    // --- MÉTODOS DE APOIO ---

    public void atualizarFocoInterface(ProponenteModel lead, TipoTelaFocada tela) {
        this.leadAtivo = lead;
        this.telaAtualFocada = tela;
    }

    public boolean isAbaCadastroClienteAtiva() {
        return TipoTelaFocada.CADASTRO_CLIENTE.equals(this.telaAtualFocada);
    }

    public void limparContexto() {
        this.leadAtivo = null;
        this.propostaAtiva = null; // 🚀 Limpeza também da proposta
        this.telaAtualFocada = TipoTelaFocada.DASHBOARD;
    }
}