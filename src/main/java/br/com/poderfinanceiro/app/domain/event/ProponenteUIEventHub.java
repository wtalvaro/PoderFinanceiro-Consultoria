package br.com.poderfinanceiro.app.domain.event;

import javafx.application.Platform;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ProponenteUIEventHub {

    private final Set<Runnable> listenersDeAtualizacao = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable atualizador) {
        listenersDeAtualizacao.add(atualizador);
    }

    public void desinscrever(Runnable atualizador) {
        listenersDeAtualizacao.remove(atualizador);
    }

    // Escuta as transações concluídas no banco de dados
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProponenteCriado(ProponenteCriadoEvent event) {
        dispararAtualizacao();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProponenteAtualizado(ProponenteAtualizadoEvent event) {
        dispararAtualizacao();
    }

    // Repassa o aviso para a thread do JavaFX (UI)
    private void dispararAtualizacao() {
        listenersDeAtualizacao.forEach(listener -> Platform.runLater(listener));
    }
}