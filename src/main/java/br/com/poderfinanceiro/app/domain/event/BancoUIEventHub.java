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
public class BancoUIEventHub {
    private static final Logger log = LoggerFactory.getLogger(BancoUIEventHub.class);
    private final Set<Runnable> listeners = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable listener) {
        log.debug("[BANCO_UI] inscrever: total agora {}", listeners.size() + 1);
        listeners.add(listener);
    }

    public void desinscrever(Runnable listener) {
        boolean removed = listeners.remove(listener);
        if (removed)
            log.debug("[BANCO_UI] desinscrever: removido, restam {}", listeners.size());
        else
            log.warn("[BANCO_UI] desinscrever: listener não encontrado");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBancoCriado(BancoCriadoEvent event) {
        log.info("[BANCO_UI] Evento: BancoCriadoEvent (ID={})", event.idBanco());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBancoAtualizado(BancoAtualizadoEvent event) {
        log.info("[BANCO_UI] Evento: BancoAtualizadoEvent (ID={})", event.idBanco());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBancoExcluido(BancoExcluidoEvent event) {
        log.info("[BANCO_UI] Evento: BancoExcluidoEvent (ID={})", event.idBanco());
        disparar();
    }

    private void disparar() {
        listeners.forEach(l -> Platform.runLater(l));
    }
}