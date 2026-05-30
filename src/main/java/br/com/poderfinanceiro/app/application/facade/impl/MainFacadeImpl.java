package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.IMainFacade;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.PropostaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MainFacadeImpl implements IMainFacade {

    private static final Logger log = LoggerFactory.getLogger(MainFacadeImpl.class);
    private static final String LOG_PREFIX = "[MainFacade]";

    private final PropostaService propostaService;
    private final AuthService authService;

    public MainFacadeImpl(PropostaService propostaService, AuthService authService) {
        this.propostaService = propostaService;
        this.authService = authService;
        log.debug("{} [SISTEMA] Facade Principal (Main) instanciada.", LOG_PREFIX);
    }

    @Override public PropostaModel converterRascunhoParaProposta(SimulacaoRascunhoDTO rascunho,
            ResultadoSimulacaoDTO resultado, ProponenteModel cliente) {
        log.info("{} [TELEMETRIA] Iniciando conversão de rascunho do Copiloto para Proposta. Cliente ID: {}", LOG_PREFIX,
                cliente != null ? cliente.getId() : "N/A");

        if (cliente == null || resultado == null || resultado.tabela() == null) {
            log.warn("{} [NEGOCIO] Conversão bloqueada: Dados insuficientes (Cliente ou Resultado nulos).", LOG_PREFIX);
            throw new IllegalArgumentException("Dados insuficientes para gerar a proposta.");
        }

        PropostaModel salva = propostaService.converterRascunhoParaProposta(rascunho, resultado, cliente);
        log.info("{} [AUDITORIA] Proposta gerada com sucesso a partir do Copiloto. ID: {}", LOG_PREFIX, salva.getId());

        return salva;
    }

    @Override public void realizarLogout() {
        log.info("{} [TELEMETRIA] Solicitando encerramento de sessão (Logout).", LOG_PREFIX);
        authService.logout();
    }
}
