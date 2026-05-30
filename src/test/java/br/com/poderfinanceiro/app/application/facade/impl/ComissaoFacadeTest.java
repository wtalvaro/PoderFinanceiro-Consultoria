package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ComissaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para ComissaoFacadeImpl.
 * Valida cálculos financeiros, regras de ciclo e travas de segurança.
 */
class ComissaoFacadeTest {

    private ComissaoFacadeImpl facade;
    private ComissaoService comissaoService;
    private AtendimentoContextService contextoService;

    @BeforeEach
    void setUp() {
        comissaoService = mock(ComissaoService.class);
        contextoService = mock(AtendimentoContextService.class);
        facade = new ComissaoFacadeImpl(comissaoService, contextoService);
    }

    @Test
    @DisplayName("Deve calcular corretamente os totais pendentes e recebidos")
    void deveCalcularTotaisFinanceiros() {
        // GIVEN
        ComissaoModel c1 = new ComissaoModel();
        c1.setStatusPagamento("Pendente");
        c1.setValorBrutoComissao(new BigDecimal("1000.00"));

        ComissaoModel c2 = new ComissaoModel();
        c2.setStatusPagamento("Pago");
        c2.setValorPagoPelaPoder(new BigDecimal("850.00"));

        List<ComissaoModel> lista = List.of(c1, c2);

        // WHEN
        BigDecimal totalPendente = facade.calcularTotalPendente(lista);
        BigDecimal totalRecebido = facade.calcularTotalRecebido(lista);

        // THEN
        assertEquals(0, new BigDecimal("1000.00").compareTo(totalPendente));
        assertEquals(0, new BigDecimal("850.00").compareTo(totalRecebido));
    }

    @Test
    @DisplayName("Deve resolver o nome do ciclo usando fallback para data de recebimento")
    void deveResolverNomeCiclo() {
        // GIVEN
        ComissaoModel comissao = new ComissaoModel();
        comissao.setDataRecebimentoBanco(LocalDateTime.of(2026, 5, 27, 10, 0)); // Uma Quarta-feira

        // WHEN
        String ciclo = facade.resolverNomeCiclo(comissao);

        // THEN
        assertNotNull(ciclo);
        assertNotEquals("Legado", ciclo);
        assertNotEquals("N/A", ciclo);
    }

    @Test
    @DisplayName("Deve ativar a trava de fechamento na Quinta-feira após as 15h")
    void deveValidarTravaQuintaFeira() {
        // Simula Quinta-feira, 16:00h
        LocalDateTime quintaAposAs15 = LocalDateTime.of(2026, 5, 28, 16, 0);

        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class)) {
            mockedTime.when(LocalDateTime::now).thenReturn(quintaAposAs15);
            assertTrue(facade.isTravaQuintaFeiraAtiva(), "A trava deve estar ativa após as 15h de quinta.");
        }

        // Simula Quarta-feira, 10:00h
        LocalDateTime quartaFeira = LocalDateTime.of(2026, 5, 27, 10, 0);
        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class)) {
            mockedTime.when(LocalDateTime::now).thenReturn(quartaFeira);
            assertFalse(facade.isTravaQuintaFeiraAtiva(), "A trava não deve estar ativa na quarta-feira.");
        }
    }

    @Test
    @DisplayName("Deve buscar comissão por ID e tratar retorno vazio")
    void deveBuscarComissaoPorId() {
        // GIVEN
        ComissaoModel mockComissao = new ComissaoModel();
        mockComissao.setId(1L);
        when(comissaoService.buscarPorId(1L)).thenReturn(Optional.of(mockComissao));
        when(comissaoService.buscarPorId(99L)).thenReturn(Optional.empty());

        // WHEN
        ComissaoModel encontrada = facade.buscarComissaoPorId(1L);
        ComissaoModel naoEncontrada = facade.buscarComissaoPorId(99L);

        // THEN
        assertNotNull(encontrada);
        assertNull(naoEncontrada);
    }

    @Test
    @DisplayName("Deve salvar conciliação delegando para o serviço de domínio")
    void deveSalvarConciliacao() {
        // GIVEN
        ComissaoModel comissao = new ComissaoModel();
        comissao.setId(50L);
        when(comissaoService.salvarConciliacao(any())).thenReturn(comissao);

        // WHEN
        ComissaoModel resultado = facade.salvarConciliacao(comissao);

        // THEN
        assertNotNull(resultado);
        verify(comissaoService, times(1)).salvarConciliacao(comissao);
    }

    @Test
    @DisplayName("Deve atualizar o contexto global de comissões")
    void deveAtualizarContexto() {
        // GIVEN
        List<ComissaoModel> lista = List.of(new ComissaoModel());

        // WHEN
        facade.atualizarContextoComissoes(lista);

        // THEN
        verify(contextoService, times(1)).setComissoesAtivas(lista);
    }
}
