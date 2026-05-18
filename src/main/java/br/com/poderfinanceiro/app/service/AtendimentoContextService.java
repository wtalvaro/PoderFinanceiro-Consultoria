package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.ComissaoModel; // 🚀 Importado
import org.springframework.stereotype.Service;
import java.util.List; // 🚀 Importado

@Service
public class AtendimentoContextService {

    public enum TipoTelaFocada {
        DASHBOARD,
        LISTA_CLIENTES,
        CADASTRO_CLIENTE,
        TABELAS_JUROS,
        LINKS_UTEIS,
        GESTAO_COMISSOES // 🎯 NOVO MARCO: Mapeia o foco na tela de fluxo de caixa
    }

    private ProponenteModel leadAtivo;
    private List<ComissaoModel> comissoesAtivas; // 🎯 CACHE DE CONTEXTO: Guarda o que a tabela está exibindo
    private TipoTelaFocada telaAtualFocada = TipoTelaFocada.DASHBOARD;
   
    public ProponenteModel getLeadAtivo() {
        return leadAtivo;
    }

    public void setLeadAtivo(ProponenteModel leadAtivo) {
        this.leadAtivo = leadAtivo;
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

    public void atualizarFocoInterface(ProponenteModel lead, TipoTelaFocada tela) {
        this.leadAtivo = lead;
        this.telaAtualFocada = tela;
    }

    public boolean isAbaCadastroClienteAtiva() {
        return TipoTelaFocada.CADASTRO_CLIENTE.equals(this.telaAtualFocada);
    }

    // 🚀 VALVULA DE ESCAPE: Corrige o erro da linha 309 do AtendimentoHubController
    public void limparContexto() {
        this.leadAtivo = null;
        this.telaAtualFocada = TipoTelaFocada.DASHBOARD;
    }
}