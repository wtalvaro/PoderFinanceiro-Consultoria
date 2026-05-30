package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serviço de Gestão de Contexto de Atendimento. Centraliza o estado da
 * aplicação e notifica interessados via eventos Spring. Otimizado para Project
 * Loom com ReentrantLock.
 */
@Service
public class AtendimentoContextService {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoContextService.class);
    private static final String LOG_PREFIX = "[AtendimentoContextService]";

    public enum TipoTelaFocada {
        DASHBOARD,
        LISTA_CLIENTES,
        CADASTRO_CLIENTE,
        ESTEIRA_PROPOSTAS,
        TABELAS_JUROS,
        LINKS_UTEIS,
        GESTAO_COMISSOES,
        GESTAO_BANCOS,
        IMPORTADOR_IA,
        COPILOTO_SIMULACAO,
        PLAYBOOK_VENDAS
    }

    /**
     * Record interno para representar a mudança de contexto.
     */
    public record ContextoAlteradoEvent(Object source, ProponenteModel lead, TipoTelaFocada tela) {
    }

    private final ApplicationEventPublisher eventPublisher;
    private final ReentrantLock lock = new ReentrantLock();

    private ProponenteModel leadAtivo;
    private PropostaModel propostaAtiva;
    private List<ComissaoModel> comissoesAtivas = new ArrayList<>();
    private TipoTelaFocada telaAtualFocada = TipoTelaFocada.DASHBOARD;

    public AtendimentoContextService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        log.info("{} [SISTEMA] Serviço de contexto reativo inicializado.", LOG_PREFIX);
    }

    // --- GETTERS (Thread-Safe) ---

    public ProponenteModel getLeadAtivo() {
        lock.lock();
        try {
            return leadAtivo;
        } finally {
            lock.unlock();
        }
    }

    public PropostaModel getPropostaAtiva() {
        lock.lock();
        try {
            return propostaAtiva;
        } finally {
            lock.unlock();
        }
    }

    public List<ComissaoModel> getComissoesAtivas() {
        lock.lock();
        try {
            return comissoesAtivas != null ? Collections.unmodifiableList(comissoesAtivas) : List.of();
        } finally {
            lock.unlock();
        }
    }

    public TipoTelaFocada getTelaAtualFocada() {
        lock.lock();
        try {
            return telaAtualFocada;
        } finally {
            lock.unlock();
        }
    }

    // --- SETTERS COM DISPARO DE EVENTOS ---

    public void setLeadAtivo(ProponenteModel leadAtivo) {
        lock.lock();
        try {
            log.debug("{} [NEGOCIO] Alterando Lead Ativo. ID: {}", LOG_PREFIX,
                    leadAtivo != null ? leadAtivo.getId() : "NULL");
            this.leadAtivo = leadAtivo;
            notificarMudanca();
        } finally {
            lock.unlock();
        }
    }

    public void setPropostaAtiva(PropostaModel propostaAtiva) {
        lock.lock();
        try {
            log.debug("{} [NEGOCIO] Alterando Proposta Ativa. ID: {}", LOG_PREFIX,
                    propostaAtiva != null ? propostaAtiva.getId() : "NULL");
            this.propostaAtiva = propostaAtiva;
            notificarMudanca();
        } finally {
            lock.unlock();
        }
    }

    public void setTelaAtualFocada(TipoTelaFocada novaTela) {
        lock.lock();
        try {
            if (this.telaAtualFocada != novaTela) {
                log.info("{} [NEGOCIO] Transição de tela: {} -> {}", LOG_PREFIX, this.telaAtualFocada, novaTela);
                this.telaAtualFocada = novaTela;
                notificarMudanca();
            }
        } finally {
            lock.unlock();
        }
    }

    public void setComissoesAtivas(List<ComissaoModel> comissoesAtivas) {
        lock.lock();
        try {
            this.comissoesAtivas = comissoesAtivas != null ? new ArrayList<>(comissoesAtivas) : new ArrayList<>();
            notificarMudanca();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atualiza o foco da interface de forma atômica e notifica uma única vez.
     */
    public void atualizarFocoInterface(ProponenteModel lead, TipoTelaFocada tela) {
        lock.lock();
        try {
            log.info("{} [NEGOCIO] Atualização atômica de contexto: Lead={}, Tela={}", LOG_PREFIX,
                    lead != null ? lead.getId() : "NULL", tela);
            this.leadAtivo = lead;
            this.telaAtualFocada = tela;
            notificarMudanca();
        } finally {
            lock.unlock();
        }
    }

    public void limparContexto() {
        lock.lock();
        try {
            log.info("{} [AUDITORIA] Resetando contexto de atendimento.", LOG_PREFIX);
            this.leadAtivo = null;
            this.propostaAtiva = null;
            this.comissoesAtivas = new ArrayList<>();
            this.telaAtualFocada = TipoTelaFocada.DASHBOARD;
            notificarMudanca();
        } finally {
            lock.unlock();
        }
    }

    public boolean isAbaCadastroClienteAtiva() {
        lock.lock();
        try {
            return TipoTelaFocada.CADASTRO_CLIENTE.equals(this.telaAtualFocada);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Método privado para centralizar a publicação do evento. Nota: O Spring
     * Events é síncrono por padrão. Se o listener for UI, ele deve usar
     * Platform.runLater() ou AsyncUtils.
     */
    private void notificarMudanca() {
        log.trace("{} [TELEMETRIA] Publicando evento de alteração de contexto.", LOG_PREFIX);
        eventPublisher.publishEvent(new ContextoAlteradoEvent(this, this.leadAtivo, this.telaAtualFocada));
    }
}
