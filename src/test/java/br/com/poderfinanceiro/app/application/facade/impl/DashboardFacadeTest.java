package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.IDashboardFacade.MetricasDashboardDTO;
import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para DashboardFacadeImpl.
 * Valida a consolidação de métricas financeiras e filtros de busca.
 */
class DashboardFacadeTest {

    private DashboardFacadeImpl facade;
    private PropostaRepository propostaRepository;
    private ComissaoRepository comissaoRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        propostaRepository = mock(PropostaRepository.class);
        comissaoRepository = mock(ComissaoRepository.class);
        authService = mock(AuthService.class);
        facade = new DashboardFacadeImpl(propostaRepository, comissaoRepository, authService);
    }

    @Test
    @DisplayName("Deve retornar o nome do consultor logado ou fallback")
    void deveObterNomeConsultor() {
        // Cenário 1: Logado
        UsuarioModel usuario = new UsuarioModel();
        usuario.setNome("Consultor Ouro");
        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);
        assertEquals("Consultor Ouro", facade.obterNomeConsultorLogado());

        // Cenário 2: Offline
        when(authService.estaLogado()).thenReturn(false);
        assertEquals("Consultor Offline", facade.obterNomeConsultorLogado());
    }

    @Test
    @DisplayName("Deve calcular métricas gerais consolidando propostas e comissões")
    void deveCalcularMetricasGerais() {
        // GIVEN - Propostas
        PropostaModel p1 = new PropostaModel();
        p1.setStatus(StatusPropostaModel.DIGITADA);
        PropostaModel p2 = new PropostaModel();
        p2.setStatus(StatusPropostaModel.PAGO);
        p2.setValorFinalCliente(new BigDecimal("50000.00"));

        when(propostaRepository.findAllComDetalhes()).thenReturn(List.of(p1, p2));

        // GIVEN - Comissões
        ComissaoModel c1 = new ComissaoModel();
        c1.setStatusPagamento("Pendente");
        c1.setValorBrutoComissao(new BigDecimal("1500.00"));

        ComissaoModel c2 = new ComissaoModel();
        c2.setStatusPagamento("Pago");
        c2.setValorPagoPelaPoder(new BigDecimal("1200.00"));

        when(comissaoRepository.findAll()).thenReturn(List.of(c1, c2));

        // WHEN
        MetricasDashboardDTO metricas = facade.calcularMetricasGerais();

        // THEN
        // CORREÇÃO: Acessando o campo correto do Record (qtdAguardando)
        assertEquals(1, metricas.qtdAguardando(), "Deveria haver 1 proposta aguardando (DIGITADA).");
        assertEquals(0, new BigDecimal("50000.00").compareTo(metricas.volumeAprovado()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(metricas.comissaoPendente()));
        assertEquals(0, new BigDecimal("1200.00").compareTo(metricas.comissaoPaga()));
    }

    @Test
    @DisplayName("Deve filtrar propostas por nome, CPF ou banco")
    void deveFiltrarPropostas() {
        // GIVEN
        ProponenteModel prop1 = new ProponenteModel();
        prop1.setNomeCompleto("João Silva");
        prop1.setCpf("123.456.789-00");
        BancoModel banco1 = new BancoModel();
        banco1.setNome("Banco Itaú");
        PropostaModel p1 = new PropostaModel();
        p1.setProponente(prop1);
        p1.setBanco(banco1);

        ProponenteModel prop2 = new ProponenteModel();
        prop2.setNomeCompleto("Maria Oliveira");
        prop2.setCpf("98765432100");
        BancoModel banco2 = new BancoModel();
        banco2.setNome("Bradesco");
        PropostaModel p2 = new PropostaModel();
        p2.setProponente(prop2);
        p2.setBanco(banco2);

        List<PropostaModel> lista = List.of(p1, p2);

        // WHEN & THEN
        assertEquals(1, facade.filtrarPropostas(lista, "joão").size());
        assertEquals(1, facade.filtrarPropostas(lista, "12345678900").size());
        assertEquals(1, facade.filtrarPropostas(lista, "bradesco").size());
        assertEquals(2, facade.filtrarPropostas(lista, "").size());
    }
}
