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
public class PropostaUIEventHub {

    private static final Logger log = LoggerFactory.getLogger(PropostaUIEventHub.class);

    // Guarda as funções de atualização das abas que estão ativas na tela
    private final Set<Runnable> listenersDeAtualizacao = new CopyOnWriteArraySet<>();

    public void inscrever(Runnable atualizador) {
        log.debug("[PROPOSTA_UI_EVENT_HUB] inscrever: Adicionando listener (total atual: {})",
                listenersDeAtualizacao.size() + 1);
        listenersDeAtualizacao.add(atualizador);
    }

    public void desinscrever(Runnable atualizador) {
        boolean removed = listenersDeAtualizacao.remove(atualizador);
        if (removed) {
            log.debug("[PROPOSTA_UI_EVENT_HUB] desinscrever: Listener removido (total restante: {})",
                    listenersDeAtualizacao.size());
        } else {
            log.warn("[PROPOSTA_UI_EVENT_HUB] desinscrever: Tentativa de remover listener não encontrado");
        }
    }

    // 1. Escuta as transações do Banco de Dados (Backend)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPropostaCriada(PropostaCriadaEvent event) {
        log.info("[PROPOSTA_UI_EVENT_HUB] Evento recebido: PropostaCriadaEvent (ID={})", event.idProposta());
        dispararAtualizacao();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPropostaAtualizada(PropostaAtualizadaEvent event) {
        log.info("[PROPOSTA_UI_EVENT_HUB] Evento recebido: PropostaAtualizadaEvent (ID={})", event.idProposta());
        dispararAtualizacao();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPropostaExcluida(PropostaExcluidaEvent event) {
        log.info("[PROPOSTA_UI_EVENT_HUB] Evento recebido: PropostaExcluidaEvent (ID={})", event.idProposta());
        dispararAtualizacao();
    }

    // 2. Repassa o aviso para a thread do JavaFX (UI)
    private void dispararAtualizacao() {
        log.debug("[PROPOSTA_UI_EVENT_HUB] dispararAtualizacao: Notificando {} listener(s) na thread UI",
                listenersDeAtualizacao.size());
        listenersDeAtualizacao.forEach(listener -> Platform.runLater(listener));
    }
}