package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UpdateServiceTest {

    private UpdateService updateService;
    private UpdateClient updateClient;
    private final String VERSAO_ATUAL = "1.0.0";

    @BeforeEach
    void setUp() {
        updateClient = Mockito.mock(UpdateClient.class);
        updateService = new UpdateService(updateClient, VERSAO_ATUAL);
    }

    @Test
    @DisplayName("Deve validar corretamente se uma versão é nova")
    void deveValidarComparacaoDeVersao() {
        // Atende ao erro: "The method isVersaoNova(String, String) is undefined"
        assertTrue(updateService.isVersaoNova("1.0.1", "1.0.0"));
        assertTrue(updateService.isVersaoNova("2.0.0", "1.9.9"));
        assertFalse(updateService.isVersaoNova("1.0.0", "1.0.0"));
        assertFalse(updateService.isVersaoNova("0.9.9", "1.0.0"));
    }

    @Test
    @DisplayName("Deve retornar Optional com release quando houver nova versão")
    void deveDetectarNovaVersao() {
        // Atende ao erro: "The constructor GitHubReleaseDTO(String, List<Object>) is
        // undefined"
        // O Record exige: tagName, name, body, htmlUrl, assets
        GitHubReleaseDTO mockRelease = new GitHubReleaseDTO(
                "v1.1.0", "Release 1.1.0", "Notas", "http://url", List.of());

        when(updateClient.buscarUltimaRelease()).thenReturn(mockRelease);

        // Atende ao erro: "Type mismatch: cannot convert from
        // Optional<GitHubReleaseDTO> to String"
        Optional<GitHubReleaseDTO> resultado = updateService.checarNovaVersao();

        assertTrue(resultado.isPresent());
        assertEquals("v1.1.0", resultado.get().tagName());
    }

    @Test
    @DisplayName("Deve retornar vazio quando a versão for igual ou inferior")
    void deveIgnorarVersaoAntiga() {
        GitHubReleaseDTO mockRelease = new GitHubReleaseDTO(
                "v1.0.0", "Release 1.0.0", "Notas", "http://url", List.of());

        when(updateClient.buscarUltimaRelease()).thenReturn(mockRelease);

        Optional<GitHubReleaseDTO> resultado = updateService.checarNovaVersao();

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve lidar com falhas na API do GitHub retornando vazio")
    void deveLidarComErroNaApi() {
        when(updateClient.buscarUltimaRelease()).thenThrow(new RuntimeException("API Down"));

        Optional<GitHubReleaseDTO> resultado = updateService.checarNovaVersao();

        assertTrue(resultado.isEmpty());
    }
}
