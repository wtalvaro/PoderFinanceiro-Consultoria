package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.IWorkspaceFacade;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.presentation.ui.navigation.AppRoute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceFacadeImpl implements IWorkspaceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFacadeImpl.class);
    private static final String LOG_PREFIX = "[WorkspaceFacade]";

    private final AtendimentoContextService contextoService;

    public WorkspaceFacadeImpl(AtendimentoContextService contextoService) {
        this.contextoService = contextoService;
        log.debug("{} [SISTEMA] Facade do Workspace instanciada.", LOG_PREFIX);
    }

    @Override
    public void atualizarContextoParaRota(AppRoute rota) {
        if (rota == null)
            return;

        if (rota.getTipoTelaFocada() == null) {
            // Em vez de apenas retornar, logamos um aviso crítico para o desenvolvedor
            log.warn("{} [SISTEMA] A rota {} não possui um TipoTelaFocada mapeado! O contexto ficará dessincronizado.",
                    LOG_PREFIX, rota);
            return;
        }

        log.trace("{} [SISTEMA] Atualizando contexto global para a rota: {}", LOG_PREFIX, rota);

        if (rota == AppRoute.ESTEIRA_PROPOSTAS) {
            contextoService.setTelaAtualFocada(rota.getTipoTelaFocada());
        } else {
            contextoService.atualizarFocoInterface(null, rota.getTipoTelaFocada());
        }
    }

    @Override public void atualizarContextoParaAtendimento(ProponenteModel proponente) {
        log.trace("{} [SISTEMA] Atualizando contexto global para Atendimento. Proponente ID: {}", LOG_PREFIX,
                proponente != null ? proponente.getId() : "NOVO");
        contextoService.atualizarFocoInterface(proponente, AtendimentoContextService.TipoTelaFocada.CADASTRO_CLIENTE);
    }

    @Override public void resetarContextoParaDashboard() {
        log.trace("{} [SISTEMA] Resetando contexto global para o Dashboard.", LOG_PREFIX);
        contextoService.atualizarFocoInterface(null, AtendimentoContextService.TipoTelaFocada.DASHBOARD);
    }
}
