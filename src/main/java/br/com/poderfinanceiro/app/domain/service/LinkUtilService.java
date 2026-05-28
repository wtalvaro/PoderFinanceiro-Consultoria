package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.LinkUtilAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.LinkUtilCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.LinkUtilExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de Domínio para gestão de Links Úteis. Orquestra a persistência,
 * validação e notificação de eventos do ciclo de vida dos links.
 */
@Service
@Transactional(readOnly = true)
public class LinkUtilService {

    private static final Logger log = LoggerFactory.getLogger(LinkUtilService.class);
    private static final String LOG_PREFIX = "[LinkUtilService]";

    private final LinkUtilRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public LinkUtilService(LinkUtilRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        log.info("{} [SISTEMA] Serviço de Links Úteis inicializado.", LOG_PREFIX);
    }

    /**
     * Salva ou atualiza um link útil no repositório. Dispara eventos
     * específicos para criação ou atualização.
     * 
     * @param link Objeto de modelo a ser persistido
     * @return LinkUtilModel persistido
     */
    @Transactional public LinkUtilModel salvar(LinkUtilModel link) {
        log.info("{} [TELEMETRIA] Iniciando processo de salvamento de link: {}", LOG_PREFIX,
                link != null ? link.getTitulo() : "NULL");

        if (link == null) {
            log.warn("{} [NEGOCIO] Tentativa de salvar um objeto nulo.", LOG_PREFIX);
            throw new IllegalArgumentException("O link útil não pode ser nulo.");
        }

        try {
            boolean isNovo = link.getId() == null;
            LinkUtilModel salvo = repository.save(link);

            log.info("{} [AUDITORIA] Link {} com sucesso. ID: {}, Título: {}", LOG_PREFIX,
                    isNovo ? "CRIADO" : "ATUALIZADO", salvo.getId(), salvo.getTitulo());

            // Publicação de eventos para sincronização de UI ou Cache
            if (isNovo) {
                eventPublisher.publishEvent(new LinkUtilCriadoEvent(salvo.getId()));
            } else {
                eventPublisher.publishEvent(new LinkUtilAtualizadoEvent(salvo.getId()));
            }

            return salvo;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao persistir link útil: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    /**
     * Exclui um link útil pelo seu identificador único. Verifica a existência
     * antes da tentativa de exclusão.
     * 
     * @param id Identificador do link
     */
    @Transactional public void excluir(Long id) {
        log.info("{} [TELEMETRIA] Solicitada exclusão do link ID: {}", LOG_PREFIX, id);

        if (id == null) {
            log.warn("{} [NEGOCIO] Abortando exclusão: ID fornecido é nulo.", LOG_PREFIX);
            return;
        }

        if (!repository.existsById(id)) {
            log.warn("{} [NEGOCIO] Tentativa de excluir link inexistente. ID: {}", LOG_PREFIX, id);
            return;
        }

        try {
            repository.deleteById(id);
            log.info("{} [AUDITORIA] Link ID {} excluído do banco de dados.", LOG_PREFIX, id);

            eventPublisher.publishEvent(new LinkUtilExcluidoEvent(id));

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao excluir link ID {}: {}", LOG_PREFIX, id, e.getMessage());
            throw e;
        }
    }

    /**
     * Lista todos os links úteis ordenados por categoria e título. Operação
     * otimizada para somente leitura.
     * 
     * @return List de LinkUtilModel
     */
    public List<LinkUtilModel> listarTodos() {
        log.trace("{} [TELEMETRIA] Recuperando lista completa de links úteis.", LOG_PREFIX);
        try {
            return repository.findAllByOrderByCategoriaAscTituloAsc();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao listar links: {}", LOG_PREFIX, e.getMessage());
            return List.of();
        }
    }
}
