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
public class TabelaJurosUIEventHub {
    private static final Logger log = LoggerFactory.getLogger(TabelaJurosUIEventHub.class);
    private final Set<Runnable> listeners = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable listener) {
        log.debug("[TABELA_JUROS_UI] inscrever: total agora {}", listeners.size() + 1);
        listeners.add(listener);
    }

    public void desinscrever(Runnable listener) {
        boolean removed = listeners.remove(listener);
        if (removed)
            log.debug("[TABELA_JUROS_UI] desinscrever: removido, restam {}", listeners.size());
        else
            log.warn("[TABELA_JUROS_UI] desinscrever: listener não encontrado");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTabelaCriada(TabelaJurosCriadoEvent event) {
        log.info("[TABELA_JUROS_UI] Evento: TabelaJurosCriadoEvent (ID={})", event.idTabela());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTabelaAtualizada(TabelaJurosAtualizadoEvent event) {
        log.info("[TABELA_JUROS_UI] Evento: TabelaJurosAtualizadoEvent (ID={})", event.idTabela());
        disparar();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTabelaArquivada(TabelaJurosArquivadoEvent event) {
        log.info("[TABELA_JUROS_UI] Evento: TabelaJurosArquivadoEvent (ID={})", event.idTabela());
        disparar();
    }

    private void disparar() {
        listeners.forEach(l -> Platform.runLater(l));
    }
}