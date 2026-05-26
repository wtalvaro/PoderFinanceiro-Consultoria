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
public class ComissaoUIEventHub {
    private static final Logger log = LoggerFactory.getLogger(ComissaoUIEventHub.class);
    private final Set<Runnable> listeners = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable listener) {
        log.debug("[COMISSAO_UI] inscrever: total agora {}", listeners.size() + 1);
        listeners.add(listener);
    }

    public void desinscrever(Runnable listener) {
        boolean removed = listeners.remove(listener);
        if (removed)
            log.debug("[COMISSAO_UI] desinscrever: removido, restam {}", listeners.size());
        else
            log.warn("[COMISSAO_UI] desinscrever: listener não encontrado");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onComissaoCriada(ComissaoCriadaEvent event) {
        log.info("[COMISSAO_UI] Evento: ComissaoCriadaEvent (ID={})", event.idComissao());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onComissaoAtualizada(ComissaoAtualizadaEvent event) {
        log.info("[COMISSAO_UI] Evento: ComissaoAtualizadaEvent (ID={})", event.idComissao());
        disparar();
    }

    private void disparar() {
        listeners.forEach(l -> Platform.runLater(l));
    }
}