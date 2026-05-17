package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import org.springframework.stereotype.Service;

@Service
public class AtendimentoContextService {

    private LeadViewModel leadAtivo;

    public void setLeadAtivo(LeadViewModel lead) {
        this.leadAtivo = lead;
    }

    public LeadViewModel getLeadAtivo() {
        return this.leadAtivo;
    }

    public void limparContexto() {
        this.leadAtivo = null;
    }
}