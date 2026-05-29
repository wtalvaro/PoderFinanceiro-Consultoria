package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.facade.IMenuFacade;
import br.com.poderfinanceiro.app.domain.service.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MenuFacadeImpl implements IMenuFacade {

    private static final Logger log = LoggerFactory.getLogger(MenuFacadeImpl.class);
    private static final String LOG_PREFIX = "[MenuFacade]";

    private final UpdateService updateService;

    public MenuFacadeImpl(UpdateService updateService) {
        this.updateService = updateService;
        log.debug("{} [SISTEMA] Facade do Menu instanciada.", LOG_PREFIX);
    }

    @Override public String checarNovaVersao() throws Exception {
        log.info("{} [TELEMETRIA] Solicitando verificação de nova versão no servidor.", LOG_PREFIX);
        String novaTag = updateService.checarNovaVersao();

        if (novaTag != null) {
            log.info("{} [SISTEMA] Nova versão encontrada: {}", LOG_PREFIX, novaTag);
        } else {
            log.trace("{} [SISTEMA] Sistema já está na versão mais recente.", LOG_PREFIX);
        }

        return novaTag;
    }

    @Override public void baixarEExecutarAtualizacao(String tag) throws Exception {
        log.warn("{} [AUDITORIA] Iniciando download e aplicação da atualização: {}", LOG_PREFIX, tag);
        updateService.baixarEExecutarAtualizacao(tag);
    }
}
