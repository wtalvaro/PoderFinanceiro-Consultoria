package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para ProponenteFacadeImpl.
 * Valida a gestão de carteira e lógica de busca de clientes.
 */
class ProponenteFacadeTest {

    private ProponenteFacadeImpl facade;
    private ProponenteService proponenteService;

    @BeforeEach
    void setUp() {
        proponenteService = mock(ProponenteService.class);
        facade = new ProponenteFacadeImpl(proponenteService);
    }

    @Test
    @DisplayName("Deve listar clientes da carteira delegando ao serviço de domínio")
    void deveListarClientesCarteira() {
        // GIVEN
        List<ProponenteModel> mockLista = List.of(new ProponenteModel());
        when(proponenteService.listarMinhaCarteira()).thenReturn(mockLista);

        // WHEN
        List<ProponenteModel> resultado = facade.listarClientesCarteira();

        // THEN
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(proponenteService, times(1)).listarMinhaCarteira();
    }

    @Test
    @DisplayName("Deve retornar listagem completa quando o termo de busca for vazio")
    void deveRetornarTudoQuandoBuscaVazia() {
        // GIVEN
        List<ProponenteModel> mockLista = List.of(new ProponenteModel(), new ProponenteModel());
        when(proponenteService.listarMinhaCarteira()).thenReturn(mockLista);

        // WHEN
        List<ProponenteModel> resultadoNulo = facade.buscarClientes(null);
        List<ProponenteModel> resultadoVazio = facade.buscarClientes("   ");

        // THEN
        assertEquals(2, resultadoNulo.size());
        assertEquals(2, resultadoVazio.size());
        verify(proponenteService, times(2)).listarMinhaCarteira();
        verify(proponenteService, never()).buscaRapida(anyString());
    }

    @Test
    @DisplayName("Deve executar busca rápida sanitizando o termo de pesquisa")
    void deveExecutarBuscaRapida() {
        // GIVEN
        String termo = "  João Silva  ";
        String termoSanitizado = "João Silva";
        when(proponenteService.buscaRapida(termoSanitizado)).thenReturn(List.of(new ProponenteModel()));

        // WHEN
        List<ProponenteModel> resultado = facade.buscarClientes(termo);

        // THEN
        assertEquals(1, resultado.size());
        verify(proponenteService, times(1)).buscaRapida(termoSanitizado);
    }

    @Test
    @DisplayName("Deve delegar a exclusão do proponente para o serviço")
    void deveExcluirCliente() {
        // GIVEN
        Long idExclusao = 500L;

        // WHEN
        facade.excluirCliente(idExclusao);

        // THEN
        verify(proponenteService, times(1)).excluirProponente(idExclusao);
    }
}
