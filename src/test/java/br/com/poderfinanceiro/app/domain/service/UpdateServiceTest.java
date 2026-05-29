package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * <h1>UpdateServiceTest</h1>
 * <p>
 * Testes de Unidade para o motor de atualizações.
 * Localizado no mesmo pacote do serviço para testar métodos package-private.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class UpdateServiceTest {

    private UpdateService service;

    @Mock
    private UpdateClient updateClient;

    private final String VERSAO_LOCAL = "v2.1.0";

    @BeforeEach
    void setUp() {
        // Injeção manual passando a versão local para o construtor
        this.service = new UpdateService(updateClient, VERSAO_LOCAL);
    }

    @ParameterizedTest
    @DisplayName("Deve identificar corretamente se a versão remota é mais nova")
    @CsvSource({
            "v1.0.0, v1.0.1, true",
            "v2.1.0, v2.1.1, true",
            "v2.1.0, v3.0.0, true",
            "v2.1.0, v2.1.0, false",
            "v2.1.0, v2.0.9, false"
    })
    void deveValidarComparacaoDeVersao(String atual, String remota, boolean esperado) {
        // Acesso permitido pois o método é package-private e o teste está no mesmo
        // pacote
        assertThat(service.isVersaoNova(atual, remota)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Deve retornar a tag da nova versão quando disponível no GitHub")
    void deveDetectarNovaVersaoRemota() {
        // CORREÇÃO: GitHubReleaseDTO exige tag_name e List<Asset>
        GitHubReleaseDTO release = new GitHubReleaseDTO("v2.1.1", List.of());
        when(updateClient.buscarUltimaRelease()).thenReturn(release);

        String resultado = service.checarNovaVersao();

        assertThat(resultado).isEqualTo("v2.1.1");
        verify(updateClient).buscarUltimaRelease();
    }

    @Test
    @DisplayName("Deve retornar null quando o sistema já estiver atualizado")
    void deveRetornarNullSeJaAtualizado() {
        // CORREÇÃO: GitHubReleaseDTO exige tag_name e List<Asset>
        GitHubReleaseDTO release = new GitHubReleaseDTO("v2.1.0", List.of());
        when(updateClient.buscarUltimaRelease()).thenReturn(release);

        String resultado = service.checarNovaVersao();

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("Deve tratar falhas de rede no cliente de atualização graciosamente")
    void deveTratarErroDeRede() {
        when(updateClient.buscarUltimaRelease()).thenThrow(new RuntimeException("GitHub Offline"));

        String resultado = service.checarNovaVersao();

        assertThat(resultado).isNull();
    }
}
