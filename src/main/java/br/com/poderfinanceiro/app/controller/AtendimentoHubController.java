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

    /**
     * Permite que a MainController acesse os dados do Lead para o Binding da Aba.
     */
    public LeadController getLeadController() {
        return abaLeadController;
    }

    public boolean temAlteracoesNaoSalvas() {
        // 3. Usa o método de verificação que você já escreveu
        return abaLeadController.getViewModel().temAlteracoesPendentes();
    }

    /**
     * Pede ao LeadController para mostrar o modal de confirmação.
     * Se o usuário confirmar a saída, o 'acaoFecharAba' será executado.
     */
    public void solicitarFechamento(Runnable acaoFecharAba) {
        abaLeadController.tentarNavegar(acaoFecharAba);
    }

    /**
     * Repassa a ordem de limpeza de memória para as abas filhas.
     */
    public void limparRecursos() {
        abaLeadController.liberarRecursos();
        // Se no futuro você criar timers no SimulatorController, chame-os aqui também!
    }
}