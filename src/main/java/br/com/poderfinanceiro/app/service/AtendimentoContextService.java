package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import org.springframework.stereotype.Service;

@Service
public class AtendimentoContextService {

    public enum TipoTelaFocada {
        DASHBOARD,
        LISTA_CLIENTES,
        CADASTRO_CLIENTE,
        TABELAS_JUROS,
        LINKS_UTEIS
    }

    private ProponenteModel leadAtivo;
    private TipoTelaFocada telaAtualFocada = TipoTelaFocada.DASHBOARD;

    public ProponenteModel getLeadAtivo() {
        return leadAtivo;
    }

    public void setLeadAtivo(ProponenteModel leadAtivo) {
        this.leadAtivo = leadAtivo;
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