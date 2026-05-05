package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
import javafx.fxml.FXML;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype") // Essencial para isolar cada cliente
public class AtendimentoHubController {

    @FXML
    private LeadController abaLeadController;
    @FXML
    private SimulatorController abaSimulatorController;

    public void inicializarAtendimento(Proponente proponente) {
        // Usamos o método existente que você já criou!
        abaLeadController.getViewModel().loadFromModel(proponente);

        // Vincula ao simulador
        abaSimulatorController.vincularProponente(proponente);
    }

    public boolean temAlteracoesNaoSalvas() {
        // 3. Usa o método de verificação que você já escreveu
        return abaLeadController.getViewModel().temAlteracoesPendentes();
    }
}