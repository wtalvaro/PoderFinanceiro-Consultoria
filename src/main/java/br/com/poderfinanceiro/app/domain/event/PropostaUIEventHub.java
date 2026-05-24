package br.com.poderfinanceiro.app.domain.event;

import javafx.application.Platform;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PropostaUIEventHub {

    // Guarda as funções de atualização das abas que estão ativas na tela
    private final Set<Runnable> listenersDeAtualizacao = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable atualizador) {
        listenersDeAtualizacao.add(atualizador);
    }

    public void desinscrever(Runnable atualizador) {
        listenersDeAtualizacao.remove(atualizador);
    }

    // 1. Escuta as transações do Banco de Dados (Backend)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPropostaCriada(PropostaCriadaEvent event) {
        dispararAtualizacao();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPropostaAtualizada(PropostaAtualizadaEvent event) {
        dispararAtualizacao();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPropostaExcluida(PropostaExcluidaEvent event) {
        dispararAtualizacao();
    }

    // 2. Repassa o aviso para a thread do JavaFX (UI)
    private void dispararAtualizacao() {
        listenersDeAtualizacao.forEach(listener -> Platform.runLater(listener));
    }
}