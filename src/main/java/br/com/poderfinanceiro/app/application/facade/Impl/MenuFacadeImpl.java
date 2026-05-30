package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.application.facade.IMenuFacade;
import br.com.poderfinanceiro.app.domain.service.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação da Facade de Menu.
 * Atua como ponte entre a UI (MenuController) e o Domínio (UpdateService).
 */
@Component
public class MenuFacadeImpl implements IMenuFacade {

    private static final Logger log = LoggerFactory.getLogger(MenuFacadeImpl.class);
    private static final String LOG_PREFIX = "[MenuFacade]";

    private final UpdateService updateService;

    public MenuFacadeImpl(UpdateService updateService) {
        this.updateService = updateService;
        log.info("{} [SISTEMA] Facade de Menu inicializada com sucesso.", LOG_PREFIX);
    }

    @Override
    public String checarNovaVersao() throws Exception {
        log.trace("{} [TELEMETRIA] Iniciando orquestração de checagem de versão.", LOG_PREFIX);

        // Converte o Optional do domínio para a String esperada pela UI
        return updateService.checarNovaVersao()
                .map(GitHubReleaseDTO::tagName)
                .orElse(null);
    }

    @Override
    public void baixarEExecutarAtualizacao(String tag) throws Exception {
        log.info("{} [TELEMETRIA] Orquestrando download da versão: {}", LOG_PREFIX, tag);

        // Delega a lógica de download e execução para o serviço de domínio
        updateService.baixarEExecutarAtualizacaoPorTag(tag);

        log.info("{} [AUDITORIA] Orquestração de download concluída para a tag: {}", LOG_PREFIX, tag);
    }
}
