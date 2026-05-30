package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.domain.service.ViaCepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para EnderecoFacadeImpl.
 * Corrigido para sincronizar com a estrutura de 7 campos do ViaCepResponse.
 */
class EnderecoFacadeTest {

    private EnderecoFacadeImpl facade;
    private ViaCepService viaCepService;

    @BeforeEach
    void setUp() {
        viaCepService = mock(ViaCepService.class);
        facade = new EnderecoFacadeImpl(viaCepService);
    }

    @Test
    @DisplayName("Deve buscar endereço com sucesso para um CEP válido e formatado")
    void deveBuscarEnderecoComSucesso() {
        // GIVEN
        String cepFormatado = "01001-000";
        String cepSanitizado = "01001000";

        // CORREÇÃO: Construtor de 7 campos conforme definido no Record ViaCepResponse
        ViaCepResponse mockResponse = new ViaCepResponse(
                cepSanitizado,
                "Praça da Sé",
                "lado ímpar",
                "Sé",
                "São Paulo",
                "SP",
                false);

        when(viaCepService.buscarEnderecoPorCep(cepSanitizado)).thenReturn(mockResponse);

        // WHEN
        ViaCepResponse resultado = facade.buscarEnderecoPorCep(cepFormatado);

        // THEN
        assertNotNull(resultado);
        assertEquals("São Paulo", resultado.localidade());
        assertEquals("SP", resultado.uf());
        assertFalse(resultado.erro());
        verify(viaCepService, times(1)).buscarEnderecoPorCep(cepSanitizado);
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP com formato inválido")
    void deveFalharParaCepIncompleto() {
        // GIVEN
        String cepInvalido = "12345-67"; // 7 dígitos

        // WHEN & THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> facade.buscarEnderecoPorCep(cepInvalido));

        assertEquals("O CEP deve conter exatamente 8 números.", ex.getMessage());
        verifyNoInteractions(viaCepService);
    }

    @Test
    @DisplayName("Deve retornar null quando o CEP não for encontrado")
    void deveRetornarNullParaCepInexistente() {
        // GIVEN
        String cepInexistente = "99999999";
        when(viaCepService.buscarEnderecoPorCep(cepInexistente)).thenReturn(null);

        // WHEN
        ViaCepResponse resultado = facade.buscarEnderecoPorCep(cepInexistente);

        // THEN
        assertNull(resultado);
        verify(viaCepService, times(1)).buscarEnderecoPorCep(cepInexistente);
    }

    @Test
    @DisplayName("Deve sanitizar CEP contendo caracteres especiais")
    void deveSanitizarEntradasRuidosas() {
        // GIVEN
        String cepRuidoso = "CEP: 01.001-000!";
        String cepSanitizado = "01001000";

        when(viaCepService.buscarEnderecoPorCep(cepSanitizado)).thenReturn(mock(ViaCepResponse.class));

        // WHEN
        facade.buscarEnderecoPorCep(cepRuidoso);

        // THEN
        verify(viaCepService).buscarEnderecoPorCep(cepSanitizado);
    }
}
