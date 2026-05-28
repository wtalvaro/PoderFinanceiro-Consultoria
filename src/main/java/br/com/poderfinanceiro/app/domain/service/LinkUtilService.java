package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.*;
import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.infrastructure.repository.LinkUtilRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class LinkUtilService {
    private static final Logger log = LoggerFactory.getLogger(LinkUtilService.class);
    private final LinkUtilRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public LinkUtilService(LinkUtilRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        log.debug("[LINK_UTIL_SERVICE] Instanciado");
    }

    @Transactional
    public LinkUtilModel salvar(LinkUtilModel link) {
        boolean isNovo = link.getId() == null;
        LinkUtilModel salvo = repository.save(link);
        log.info("[LINK_UTIL_SERVICE] Link salvo: ID={}, titulo='{}', novo={}", salvo.getId(), salvo.getTitulo(),
                isNovo);
        if (isNovo) {
            eventPublisher.publishEvent(new LinkUtilCriadoEvent(salvo.getId()));
        } else {
            eventPublisher.publishEvent(new LinkUtilAtualizadoEvent(salvo.getId()));
        }
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        if (id == null)
            return;
        repository.deleteById(id);
        eventPublisher.publishEvent(new LinkUtilExcluidoEvent(id));
        log.info("[LINK_UTIL_SERVICE] Link excluído ID={}", id);
    }

    public List<LinkUtilModel> listarTodos() {
        return repository.findAllByOrderByCategoriaAscTituloAsc();
    }
}