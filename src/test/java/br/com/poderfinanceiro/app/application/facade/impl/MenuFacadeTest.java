package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.domain.service.UpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para MenuFacadeImpl.
 * Valida a orquestração de atualizações e ponte entre UI e Domínio.
 */
class MenuFacadeTest {

    private MenuFacadeImpl facade;
    private UpdateService updateService;

    @BeforeEach
    void setUp() {
        updateService = mock(UpdateService.class);
        facade = new MenuFacadeImpl(updateService);
    }

    @Test
    @DisplayName("Deve retornar a tag da nova versão quando houver atualização disponível")
    void deveRetornarTagQuandoHouverAtualizacao() throws Exception {
        // GIVEN
        GitHubReleaseDTO mockRelease = new GitHubReleaseDTO(
                "v2.1.4",
                "Release v2.1.4",
                "Notas da release",
                "https://github.com/release",
                List.of());

        when(updateService.checarNovaVersao()).thenReturn(Optional.of(mockRelease));

        // WHEN
        String tagEncontrada = facade.checarNovaVersao();

        // THEN
        assertNotNull(tagEncontrada);
        assertEquals("v2.1.4", tagEncontrada);
        verify(updateService, times(1)).checarNovaVersao();
    }

    @Test
    @DisplayName("Deve retornar null quando o sistema já estiver atualizado")
    void deveRetornarNullQuandoNaoHouverAtualizacao() throws Exception {
        // GIVEN
        when(updateService.checarNovaVersao()).thenReturn(Optional.empty());

        // WHEN
        String tagEncontrada = facade.checarNovaVersao();

        // THEN
        assertNull(tagEncontrada);
        verify(updateService, times(1)).checarNovaVersao();
    }

    @Test
    @DisplayName("Deve delegar o download da atualização para o serviço de domínio")
    void deveDelegarDownload() throws Exception {
        // GIVEN
        String tagParaBaixar = "v2.1.4";

        // WHEN
        facade.baixarEExecutarAtualizacao(tagParaBaixar);

        // THEN
        verify(updateService, times(1)).baixarEExecutarAtualizacaoPorTag(tagParaBaixar);
    }

    @Test
    @DisplayName("Deve propagar exceções do serviço de domínio")
    void devePropagarExcecoes() throws Exception {
        // GIVEN
        when(updateService.checarNovaVersao()).thenThrow(new RuntimeException("Erro de Rede"));

        // WHEN & THEN
        assertThrows(Exception.class, () -> facade.checarNovaVersao());
    }
}
