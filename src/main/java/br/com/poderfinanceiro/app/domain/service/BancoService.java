package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.BancoAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.BancoCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.BancoExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de Domínio para gestão de Instituições Bancárias. Responsável por
 * garantir a consistência dos dados mestres de bancos e notificar o sistema
 * sobre mudanças estruturais.
 */
@Service
@Transactional(readOnly = true)
public class BancoService {

    private static final Logger log = LoggerFactory.getLogger(BancoService.class);
    private static final String LOG_PREFIX = "[BancoService]";

    private final BancoRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public BancoService(BancoRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        log.info("{} [SISTEMA] Serviço de Instituições Bancárias inicializado.", LOG_PREFIX);
    }

    /**
     * Persiste ou atualiza uma instituição bancária.
     * 
     * @param banco Objeto contendo os dados do banco
     * @return BancoModel persistido
     */
    @Transactional public BancoModel salvar(BancoModel banco) {
        log.info("{} [TELEMETRIA] Iniciando persistência do banco: {}", LOG_PREFIX,
                banco != null ? banco.getNome() : "NULL");

        if (banco == null) {
            log.warn("{} [NEGOCIO] Falha ao salvar: Objeto banco está nulo.", LOG_PREFIX);
            throw new IllegalArgumentException("O modelo do banco não pode ser nulo.");
        }

        try {
            boolean isNovo = banco.getId() == null;
            BancoModel salvo = repository.save(banco);

            log.info("{} [AUDITORIA] Banco {} com sucesso. ID: {}, Nome: {}", LOG_PREFIX,
                    isNovo ? "CRIADO" : "ATUALIZADO", salvo.getId(), salvo.getNome());

            // Disparo de eventos para atualização de componentes de UI
            // (ComboBoxes, Grids)
            if (isNovo) {
                eventPublisher.publishEvent(new BancoCriadoEvent(salvo.getId()));
            } else {
                eventPublisher.publishEvent(new BancoAtualizadoEvent(salvo.getId()));
            }

            return salvo;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro crítico ao salvar banco: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    /**
     * Remove uma instituição bancária do sistema.
     * 
     * @param id Identificador único do banco
     */
    @Transactional public void excluir(Long id) {
        log.info("{} [TELEMETRIA] Solicitada exclusão do banco ID: {}", LOG_PREFIX, id);

        if (id == null) {
            log.warn("{} [NEGOCIO] Exclusão abortada: ID nulo.", LOG_PREFIX);
            return;
        }

        // Verificação defensiva para evitar exceções de infraestrutura
        if (!repository.existsById(id)) {
            log.warn("{} [NEGOCIO] Tentativa de excluir banco inexistente. ID: {}", LOG_PREFIX, id);
            return;
        }

        try {
            repository.deleteById(id);
            log.info("{} [AUDITORIA] Banco ID {} removido permanentemente.", LOG_PREFIX, id);

            eventPublisher.publishEvent(new BancoExcluidoEvent(id));

        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao excluir banco ID {}: {}", LOG_PREFIX, id, e.getMessage());
            throw e;
        }
    }

    /**
     * Recupera todos os bancos cadastrados. Operação otimizada para leitura
     * (read-only).
     * 
     * @return List de BancoModel
     */
    public List<BancoModel> listarTodos() {
        log.trace("{} [TELEMETRIA] Listagem de bancos solicitada.", LOG_PREFIX);
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao listar bancos: {}", LOG_PREFIX, e.getMessage());
            return List.of();
        }
    }
}
