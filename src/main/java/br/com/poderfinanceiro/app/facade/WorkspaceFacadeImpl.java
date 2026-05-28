package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.RotaAba;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
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

    @Override public void atualizarContextoParaRota(RotaAba rota) {
        if (rota == null || rota.getTipoTelaFocada() == null) {
            log.trace("{} [NEGOCIO] Rota nula ou sem tipo de tela focado. Nenhuma ação tomada.", LOG_PREFIX);
            return;
        }

        log.trace("{} [SISTEMA] Atualizando contexto global para a rota: {}", LOG_PREFIX, rota);

        if (rota == RotaAba.PROPOSTAS) {
            // A esteira de propostas não limpa o proponente ativo
            contextoService.setTelaAtualFocada(rota.getTipoTelaFocada());
        } else {
            // Outras telas limpam o proponente ativo
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
