package br.com.poderfinanceiro.app.domain.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;

import java.util.List;

@Service
public class AtendimentoContextService {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoContextService.class);

    public enum TipoTelaFocada {
        DASHBOARD,
        LISTA_CLIENTES,
        CADASTRO_CLIENTE,
        ESTEIRA_PROPOSTAS,
        TABELAS_JUROS,
        LINKS_UTEIS,
        GESTAO_COMISSOES
    }

    private ProponenteModel leadAtivo;
    private PropostaModel propostaAtiva;
    private List<ComissaoModel> comissoesAtivas;
    private TipoTelaFocada telaAtualFocada = TipoTelaFocada.DASHBOARD;

    public AtendimentoContextService() {
        log.debug("[ATENDIMENTO_CONTEXT] Serviço de contexto instanciado");
    }

    // --- GETTERS E SETTERS ---

    public ProponenteModel getLeadAtivo() {
        log.trace("[ATENDIMENTO_CONTEXT] getLeadAtivo: {}", leadAtivo != null ? leadAtivo.getId() : "null");
        return leadAtivo;
    }

    public void setLeadAtivo(ProponenteModel leadAtivo) {
        Long id = leadAtivo != null ? leadAtivo.getId() : null;
        log.debug("[ATENDIMENTO_CONTEXT] setLeadAtivo: ID={}", id);
        this.leadAtivo = leadAtivo;
    }

    public PropostaModel getPropostaAtiva() {
        log.trace("[ATENDIMENTO_CONTEXT] getPropostaAtiva: {}", propostaAtiva != null ? propostaAtiva.getId() : "null");
        return propostaAtiva;
    }

    public void setPropostaAtiva(PropostaModel propostaAtiva) {
        Long id = propostaAtiva != null ? propostaAtiva.getId() : null;
        log.debug("[ATENDIMENTO_CONTEXT] setPropostaAtiva: ID={}", id);
        this.propostaAtiva = propostaAtiva;
    }

    public List<ComissaoModel> getComissoesAtivas() {
        log.trace("[ATENDIMENTO_CONTEXT] getComissoesAtivas: size={}",
                comissoesAtivas != null ? comissoesAtivas.size() : 0);
        return comissoesAtivas;
    }

    public void setComissoesAtivas(List<ComissaoModel> comissoesAtivas) {
        int size = comissoesAtivas != null ? comissoesAtivas.size() : 0;
        log.debug("[ATENDIMENTO_CONTEXT] setComissoesAtivas: {} comissão(ões) ativa(s)", size);
        this.comissoesAtivas = comissoesAtivas;
    }

    public TipoTelaFocada getTelaAtualFocada() {
        log.trace("[ATENDIMENTO_CONTEXT] getTelaAtualFocada: {}", telaAtualFocada);
        return telaAtualFocada;
    }

    public void setTelaAtualFocada(TipoTelaFocada telaAtualFocada) {
        log.debug("[ATENDIMENTO_CONTEXT] setTelaAtualFocada: de {} para {}", this.telaAtualFocada, telaAtualFocada);
        this.telaAtualFocada = telaAtualFocada;
    }

    // --- MÉTODOS DE APOIO ---

    public void atualizarFocoInterface(ProponenteModel lead, TipoTelaFocada tela) {
        Long leadId = lead != null ? lead.getId() : null;
        log.info("[ATENDIMENTO_CONTEXT] atualizarFocoInterface: leadId={}, tela={}", leadId, tela);
        this.leadAtivo = lead;
        this.telaAtualFocada = tela;
    }

    public boolean isAbaCadastroClienteAtiva() {
        boolean ativa = TipoTelaFocada.CADASTRO_CLIENTE.equals(this.telaAtualFocada);
        log.trace("[ATENDIMENTO_CONTEXT] isAbaCadastroClienteAtiva: {}", ativa);
        return ativa;
    }

    public void limparContexto() {
        log.info("[ATENDIMENTO_CONTEXT] limparContexto: resetando lead, proposta e tela para DASHBOARD");
        this.leadAtivo = null;
        this.propostaAtiva = null;
        this.telaAtualFocada = TipoTelaFocada.DASHBOARD;
    }
}