package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.ILinkUtilFacade;
import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.service.LinkUtilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LinkUtilFacadeImpl implements ILinkUtilFacade {

    private static final Logger log = LoggerFactory.getLogger(LinkUtilFacadeImpl.class);
    private static final String LOG_PREFIX = "[LinkUtilFacade]";

    private final LinkUtilService linkUtilService;

    public LinkUtilFacadeImpl(LinkUtilService linkUtilService) {
        this.linkUtilService = linkUtilService;
        log.debug("{} [SISTEMA] Facade de Links Úteis instanciada.", LOG_PREFIX);
    }

    @Override public List<LinkUtilModel> listarTodosOsLinks() {
        log.trace("{} [TELEMETRIA] Solicitando listagem completa de links.", LOG_PREFIX);
        return linkUtilService.listarTodos();
    }

    @Override @Transactional public LinkUtilModel salvarLink(LinkUtilModel link) {
        log.info("{} [TELEMETRIA] Iniciando salvamento do link. ID: {}", LOG_PREFIX, link.getId() != null ? link.getId() : "NOVO");

        if (link.getTitulo() == null || link.getTitulo().isBlank() || link.getUrl() == null || link.getUrl().isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de salvar link com dados obrigatórios ausentes.", LOG_PREFIX);
            throw new IllegalArgumentException("Título e URL são obrigatórios.");
        }

        LinkUtilModel salvo = linkUtilService.salvar(link);
        log.info("{} [AUDITORIA] Link salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
        return salvo;
    }

    @Override @Transactional public void excluirLink(Long id) {
        log.warn("{} [AUDITORIA] Solicitando exclusão do link ID: {}", LOG_PREFIX, id);
        linkUtilService.excluir(id);
        log.info("{} [AUDITORIA] Link ID: {} excluído com sucesso.", LOG_PREFIX, id);
    }

    @Override public List<LinkUtilModel> filtrarLinks(String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca: '{}'", LOG_PREFIX, termoBusca);
        List<LinkUtilModel> todos = listarTodosOsLinks();

        if (termoBusca == null || termoBusca.isBlank()) {
            return todos;
        }

        String termoLower = termoBusca.toLowerCase().trim();
        return todos.stream().filter(link -> atendeCriterioDeBusca(link, termoLower)).toList();
    }

    private boolean atendeCriterioDeBusca(LinkUtilModel link, String termo) {
        return contemTermo(link.getTitulo(), termo) || contemTermo(link.getDescricao(), termo)
                || contemTermo(link.getCategoria() != null ? link.getCategoria().getLabel() : null, termo)
                || contemTermo(link.getTags(), termo);
    }

    private boolean contemTermo(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }
}
