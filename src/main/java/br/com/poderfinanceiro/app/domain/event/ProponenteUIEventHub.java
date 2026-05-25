package br.com.poderfinanceiro.app.domain.event;

import javafx.application.Platform;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ProponenteUIEventHub {

    private static final Logger log = LoggerFactory.getLogger(ProponenteUIEventHub.class);

    private final Set<Runnable> listenersDeAtualizacao = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable atualizador) {
        log.debug("[PROPONENTE_UI_EVENT_HUB] inscrever: Adicionando listener (total atual: {})",
                listenersDeAtualizacao.size() + 1);
        listenersDeAtualizacao.add(atualizador);
    }

    public void desinscrever(Runnable atualizador) {
        boolean removed = listenersDeAtualizacao.remove(atualizador);
        if (removed) {
            log.debug("[PROPONENTE_UI_EVENT_HUB] desinscrever: Listener removido (total restante: {})",
                    listenersDeAtualizacao.size());
        } else {
            log.warn("[PROPONENTE_UI_EVENT_HUB] desinscrever: Tentativa de remover listener não encontrado");
        }
    }

    // Escuta as transações concluídas no banco de dados
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProponenteCriado(ProponenteCriadoEvent event) {
        log.info("[PROPONENTE_UI_EVENT_HUB] Evento recebido: ProponenteCriadoEvent (ID={})", event.idProponente());
        dispararAtualizacao();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProponenteAtualizado(ProponenteAtualizadoEvent event) {
        log.info("[PROPONENTE_UI_EVENT_HUB] Evento recebido: ProponenteAtualizadoEvent (ID={})", event.idProponente());
        dispararAtualizacao();
    }

    // Repassa o aviso para a thread do JavaFX (UI)
    private void dispararAtualizacao() {
        log.debug("[PROPONENTE_UI_EVENT_HUB] dispararAtualizacao: Notificando {} listener(s) na thread UI",
                listenersDeAtualizacao.size());
        listenersDeAtualizacao.forEach(listener -> Platform.runLater(listener));
    }
}