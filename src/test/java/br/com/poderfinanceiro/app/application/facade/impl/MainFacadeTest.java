package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para MainFacadeImpl.
 * Valida a conversão de rascunhos em propostas e gestão de sessão global.
 */
class MainFacadeTest {

    private MainFacadeImpl facade;
    private PropostaService propostaService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        propostaService = mock(PropostaService.class);
        authService = mock(AuthService.class);
        facade = new MainFacadeImpl(propostaService, authService);
    }

    @Test
    @DisplayName("Deve converter rascunho para proposta com sucesso")
    void deveConverterParaPropostaComSucesso() {
        // GIVEN
        SimulacaoRascunhoDTO rascunho = new SimulacaoRascunhoDTO(45, BigDecimal.valueOf(5000), "INSS",
                BigDecimal.valueOf(10000), 84, BigDecimal.ZERO);
        TabelaJurosModel tabela = mock(TabelaJurosModel.class);
        ResultadoSimulacaoDTO resultado = new ResultadoSimulacaoDTO(tabela, BigDecimal.valueOf(1.80),
                BigDecimal.valueOf(250));
        ProponenteModel cliente = new ProponenteModel();
        cliente.setId(10L);

        PropostaModel mockSalva = new PropostaModel();
        mockSalva.setId(100L);

        when(propostaService.converterRascunhoParaProposta(eq(rascunho), eq(resultado), eq(cliente)))
                .thenReturn(mockSalva);

        // WHEN
        PropostaModel resultadoFinal = facade.converterRascunhoParaProposta(rascunho, resultado, cliente);

        // THEN
        assertNotNull(resultadoFinal);
        assertEquals(100L, resultadoFinal.getId());
        verify(propostaService, times(1)).converterRascunhoParaProposta(rascunho, resultado, cliente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar converter com dados nulos")
    void deveFalharAoConverterComDadosNulos() {
        // Cenário 1: Cliente nulo
        assertThrows(IllegalArgumentException.class,
                () -> facade.converterRascunhoParaProposta(null, mock(ResultadoSimulacaoDTO.class), null));

        // Cenário 2: Resultado nulo
        assertThrows(IllegalArgumentException.class,
                () -> facade.converterRascunhoParaProposta(null, null, new ProponenteModel()));

        // Cenário 3: Tabela dentro do resultado nula
        ResultadoSimulacaoDTO resultadoSemTabela = new ResultadoSimulacaoDTO(null, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> facade.converterRascunhoParaProposta(null, resultadoSemTabela, new ProponenteModel()));
    }

    @Test
    @DisplayName("Deve realizar logout delegando para o AuthService")
    void deveRealizarLogout() {
        // WHEN
        facade.realizarLogout();

        // THEN
        verify(authService, times(1)).logout();
    }
}
