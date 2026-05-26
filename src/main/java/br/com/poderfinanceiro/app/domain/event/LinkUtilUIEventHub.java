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
public class LinkUtilUIEventHub {
    private static final Logger log = LoggerFactory.getLogger(LinkUtilUIEventHub.class);
    private final Set<Runnable> listeners = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable listener) {
        log.debug("[LINK_UTIL_UI] inscrever: total agora {}", listeners.size() + 1);
        listeners.add(listener);
    }

    public void desinscrever(Runnable listener) {
        boolean removed = listeners.remove(listener);
        if (removed)
            log.debug("[LINK_UTIL_UI] desinscrever: removido, restam {}", listeners.size());
        else
            log.warn("[LINK_UTIL_UI] desinscrever: listener não encontrado");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLinkCriado(LinkUtilCriadoEvent event) {
        log.info("[LINK_UTIL_UI] Evento: LinkUtilCriadoEvent (ID={})", event.idLink());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLinkAtualizado(LinkUtilAtualizadoEvent event) {
        log.info("[LINK_UTIL_UI] Evento: LinkUtilAtualizadoEvent (ID={})", event.idLink());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLinkExcluido(LinkUtilExcluidoEvent event) {
        log.info("[LINK_UTIL_UI] Evento: LinkUtilExcluidoEvent (ID={})", event.idLink());
        disparar();
    }

    private void disparar() {
        listeners.forEach(l -> Platform.runLater(l));
    }
}