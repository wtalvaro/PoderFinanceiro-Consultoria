package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.*;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class BancoService {
    private static final Logger log = LoggerFactory.getLogger(BancoService.class);
    private final BancoRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public BancoService(BancoRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        log.debug("[BANCO_SERVICE] Instanciado");
    }

    @Transactional
    public BancoModel salvar(BancoModel banco) {
        boolean isNovo = banco.getId() == null;
        BancoModel salvo = repository.save(banco);
        log.info("[BANCO_SERVICE] Banco salvo: ID={}, nome={}, novo={}", salvo.getId(), salvo.getNome(), isNovo);
        if (isNovo)
            eventPublisher.publishEvent(new BancoCriadoEvent(salvo.getId()));
        else
            eventPublisher.publishEvent(new BancoAtualizadoEvent(salvo.getId()));
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        if (id == null)
            return;
        repository.deleteById(id);
        eventPublisher.publishEvent(new BancoExcluidoEvent(id));
        log.info("[BANCO_SERVICE] Banco excluído ID={}", id);
    }

    public List<BancoModel> listarTodos() {
        return repository.findAll();
    }
    
}
