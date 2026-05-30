package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.IProponenteFacade;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProponenteFacadeImpl implements IProponenteFacade {

    private static final Logger log = LoggerFactory.getLogger(ProponenteFacadeImpl.class);
    private static final String LOG_PREFIX = "[ProponenteFacade]";

    private final ProponenteService proponenteService;

    public ProponenteFacadeImpl(ProponenteService proponenteService) {
        this.proponenteService = proponenteService;
        log.debug("{} [SISTEMA] Facade de Proponentes instanciada.", LOG_PREFIX);
    }

    @Override public List<ProponenteModel> listarClientesCarteira() {
        log.trace("{} [TELEMETRIA] Solicitando listagem completa da carteira de clientes.", LOG_PREFIX);
        return proponenteService.listarMinhaCarteira();
    }

    @Override public List<ProponenteModel> buscarClientes(String termoBusca) {
        log.trace("{} [NEGOCIO] Executando busca rápida de clientes. Termo: '{}'", LOG_PREFIX, termoBusca);
        if (termoBusca == null || termoBusca.isBlank()) {
            return listarClientesCarteira();
        }
        return proponenteService.buscaRapida(termoBusca.trim());
    }

    @Override public void excluirCliente(Long id) {
        log.warn("{} [AUDITORIA] Solicitando exclusão do cliente ID: {}", LOG_PREFIX, id);
        proponenteService.excluirProponente(id);
    }
}
