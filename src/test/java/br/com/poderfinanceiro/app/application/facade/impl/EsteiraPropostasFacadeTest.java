package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para EsteiraPropostasFacadeImpl.
 * Valida a gestão da esteira e o motor de busca avançado por valores.
 */
class EsteiraPropostasFacadeTest {

    private EsteiraPropostasFacadeImpl facade;
    private PropostaRepository propostaRepository;
    private PropostaService propostaService; // Necessário para o construtor
    private AuthService authService;
    private UsuarioModel usuarioLogado;

    @BeforeEach
    void setUp() {
        propostaRepository = mock(PropostaRepository.class);
        propostaService = mock(PropostaService.class);
        authService = mock(AuthService.class);

        usuarioLogado = new UsuarioModel();
        usuarioLogado.setId(1L);
        usuarioLogado.setNome("Consultor Teste");
        when(authService.getUsuarioLogado()).thenReturn(usuarioLogado);

        facade = new EsteiraPropostasFacadeImpl(propostaRepository, propostaService, authService);
    }

    @Test
    @DisplayName("Deve listar propostas filtrando pelo ID do usuário logado")
    void deveListarPropostasDoUsuario() {
        // GIVEN
        when(propostaRepository.findByUsuarioId(1L)).thenReturn(List.of(new PropostaModel()));

        // WHEN
        List<PropostaModel> resultado = facade.listarPropostasDoUsuario();

        // THEN
        assertEquals(1, resultado.size());
        verify(propostaRepository).findByUsuarioId(1L);
    }

    @Test
    @DisplayName("Deve criar uma nova proposta em memória com status inicial correto")
    void deveCriarPropostaEmBranco() {
        // WHEN
        PropostaModel nova = facade.criarNovaPropostaEmBranco();

        // THEN
        assertNotNull(nova);
        assertEquals(StatusPropostaModel.DIGITADA, nova.getStatus());
        assertEquals(usuarioLogado, nova.getUsuario());
        assertNull(nova.getId(), "A proposta não deve ser persistida na Facade.");
    }

    @Test
    @DisplayName("Deve filtrar propostas por nome do proponente e CPF")
    void deveFiltrarPorTexto() {
        // GIVEN
        ProponenteModel prop = new ProponenteModel();
        prop.setNomeCompleto("Ricardo Silva");
        prop.setCpf("12345678901");

        PropostaModel p = new PropostaModel();
        p.setProponente(prop);

        when(propostaRepository.findByUsuarioId(anyLong())).thenReturn(List.of(p));

        // WHEN & THEN
        assertEquals(1, facade.filtrarPropostas("ricardo").size());
        assertEquals(1, facade.filtrarPropostas("123456").size());
        assertEquals(0, facade.filtrarPropostas("inexistente").size());
    }

    @Test
    @DisplayName("Deve filtrar propostas por faixas de valor solicitado (>, <, -)")
    void deveFiltrarPorValor() {
        // GIVEN
        PropostaModel p1 = new PropostaModel();
        p1.setValorSolicitado(new BigDecimal("1500.00"));
        PropostaModel p2 = new PropostaModel();
        p2.setValorSolicitado(new BigDecimal("5000.00"));
        PropostaModel p3 = new PropostaModel();
        p3.setValorSolicitado(new BigDecimal("12000.00"));

        when(propostaRepository.findByUsuarioId(anyLong())).thenReturn(List.of(p1, p2, p3));

        // WHEN & THEN
        assertEquals(1, facade.filtrarPropostas("> 10000").size(), "Deve achar apenas p3");
        assertEquals(2, facade.filtrarPropostas("< 6000").size(), "Deve achar p1 e p2");
        assertEquals(1, facade.filtrarPropostas("4000 - 6000").size(), "Deve achar apenas p2");
    }

    @Test
    @DisplayName("Deve ser resiliente a erros de digitação no filtro de valor")
    void deveLidarComErroNoFiltroDeValor() {
        // GIVEN
        PropostaModel p = new PropostaModel();
        p.setValorSolicitado(new BigDecimal("1000"));
        when(propostaRepository.findByUsuarioId(anyLong())).thenReturn(List.of(p));

        // WHEN
        List<PropostaModel> resultado = facade.filtrarPropostas("> valor_invalido");

        // THEN
        // Não deve lançar exceção e deve retornar lista vazia (ou original dependendo
        // da lógica)
        // No código atual, o catch loga e retorna false no filter.
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve filtrar pelo label do status da proposta")
    void deveFiltrarPorStatus() {
        // GIVEN
        PropostaModel p = new PropostaModel();
        p.setStatus(StatusPropostaModel.PENDENTE); // Label: "Pendente"

        when(propostaRepository.findByUsuarioId(anyLong())).thenReturn(List.of(p));

        // WHEN
        List<PropostaModel> resultado = facade.filtrarPropostas("pendente");

        // THEN
        assertEquals(1, resultado.size());
    }
}
