package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.TipoTelaFocada;
import br.com.poderfinanceiro.app.presentation.ui.navigation.enums.AppRoute;
import br.com.poderfinanceiro.app.util.ProponenteModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para WorkspaceFacadeImpl.
 * Valida a orquestração de rotas e sincronização de contexto global.
 */
class WorkspaceFacadeTest {

    private WorkspaceFacadeImpl facade;
    private AtendimentoContextService contextoService;

    @BeforeEach
    void setUp() {
        contextoService = mock(AtendimentoContextService.class);
        facade = new WorkspaceFacadeImpl(contextoService);
    }

    @Test
    @DisplayName("Deve atualizar apenas o foco da tela ao navegar para Propostas (Preservando cliente)")
    void deveAtualizarContextoParaPropostas() {
        // GIVEN
        // AppRoute.PROPOSTAS deve retornar TipoTelaFocada.ESTEIRA_PROPOSTAS
        AppRoute rota = AppRoute.ESTEIRA_PROPOSTAS;

        // WHEN
        facade.atualizarContextoParaRota(rota);

        // THEN
        verify(contextoService, times(1)).setTelaAtualFocada(rota.getTipoTelaFocada());
        verify(contextoService, never()).atualizarFocoInterface(any(), any());
    }

    @Test
    @DisplayName("Deve limpar o proponente ativo ao navegar para rotas que não sejam Propostas")
    void deveLimparProponenteEmOutrasRotas() {
        // GIVEN
        AppRoute rota = AppRoute.BANCOS_CONVENIOS;
        var tipoTelaEsperado = rota.getTipoTelaFocada(); // Agora não é mais null

        // WHEN
        facade.atualizarContextoParaRota(rota);

        // THEN
        // O teste agora prova que o sistema limpa o cliente ao entrar em Bancos
        verify(contextoService, times(1)).atualizarFocoInterface(null, tipoTelaEsperado);
        assertNotNull(tipoTelaEsperado, "A rota BANCOS_CONVENIOS deve possuir um foco de tela definido.");
    }

    @Test
    @DisplayName("Deve configurar contexto de atendimento para um proponente específico")
    void deveAtualizarContextoParaAtendimento() {
        // GIVEN
        ProponenteModel proponente = ProponenteModelBuilder.umProponente().comId(10L).build();

        // WHEN
        facade.atualizarContextoParaAtendimento(proponente);

        // THEN
        verify(contextoService, times(1)).atualizarFocoInterface(proponente, TipoTelaFocada.CADASTRO_CLIENTE);
    }

    @Test
    @DisplayName("Deve resetar o contexto global ao voltar para o Dashboard")
    void deveResetarContextoParaDashboard() {
        // WHEN
        facade.resetarContextoParaDashboard();

        // THEN
        verify(contextoService, times(1)).atualizarFocoInterface(null, TipoTelaFocada.DASHBOARD);
    }

    @Test
    @DisplayName("Deve ignorar atualização se a rota for nula (Resiliência)")
    void deveIgnorarRotaNula() {
        // WHEN
        facade.atualizarContextoParaRota(null);

        // THEN
        verifyNoInteractions(contextoService);
    }
}
