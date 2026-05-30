package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para ImportadorTabelasFacadeImpl.
 * Corrigido para sincronizar com os campos reais do TabelaImportadaDTO.
 */
class ImportadorTabelasFacadeTest {

    private ImportadorTabelasFacadeImpl facade;

    private GeminiService geminiService;
    private AuthService authService;
    private TabelaJurosService tabelaJurosService;
    private BancoRepository bancoRepository;

    @BeforeEach
    void setUp() {
        geminiService = mock(GeminiService.class);
        authService = mock(AuthService.class);
        tabelaJurosService = mock(TabelaJurosService.class);
        bancoRepository = mock(BancoRepository.class);

        facade = new ImportadorTabelasFacadeImpl(
                geminiService, authService, tabelaJurosService, bancoRepository);
    }

    @Test
    @DisplayName("Deve extrair tabelas de uma imagem com sucesso via IA")
    void deveExtrairTabelasComSucesso() throws Exception {
        // GIVEN
        File arquivoMock = mock(File.class);
        when(arquivoMock.getName()).thenReturn("tabela_inss.png");

        UsuarioModel usuario = new UsuarioModel();
        usuario.setGeminiApiKey("AIzaSy_VALID_KEY");

        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        // CORREÇÃO: Usando "taxaMensal" em vez de "taxa" para coincidir com o DTO real
        String jsonIA = """
                [
                    {
                        "nomeTabela": "INSS NOVO",
                        "taxaMensal": 1.80,
                        "comissaoPercentual": 12.5,
                        "prazoMaximo": 84
                    },
                    {
                        "nomeTabela": "INSS PORT",
                        "taxaMensal": 1.75,
                        "comissaoPercentual": 10.0,
                        "prazoMaximo": 84
                    }
                ]
                """;

        when(geminiService.extrairTabelasEmLote(eq(arquivoMock), eq("AIzaSy_VALID_KEY"), anyString()))
                .thenReturn(jsonIA);

        // WHEN
        List<TabelaImportadaDTO> resultado = facade.extrairTabelasDeImagem(arquivoMock, "gemini-1.5-flash");

        // THEN
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("INSS NOVO", resultado.get(0).getNomeTabela());
        // Validando o campo que causou o erro anteriormente
        assertNotNull(resultado.get(0).getTaxaMensal());
        verify(geminiService, times(1)).extrairTabelasEmLote(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar extrair sem API Key configurada")
    void deveFalharSemApiKey() {
        // GIVEN
        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(new UsuarioModel());

        // WHEN & THEN
        assertThrows(IllegalStateException.class, () -> facade.extrairTabelasDeImagem(mock(File.class), "modelo"));
    }

    @Test
    @DisplayName("Deve listar apenas bancos ativos ordenados por nome")
    void deveListarBancosAtivos() {
        // GIVEN
        when(bancoRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(new BancoModel()));

        // WHEN
        List<BancoModel> bancos = facade.listarBancosAtivos();

        // THEN
        assertNotNull(bancos);
        assertFalse(bancos.isEmpty());
        verify(bancoRepository).findByAtivoTrueOrderByNomeAsc();
    }

    @Test
    @DisplayName("Deve delegar o salvamento em lote para o serviço de domínio")
    void deveSalvarLoteTabelas() {
        // GIVEN
        List<TabelaImportadaDTO> lote = List.of(new TabelaImportadaDTO());

        // WHEN
        facade.salvarLoteTabelas(lote);

        // THEN
        verify(tabelaJurosService, times(1)).salvarLoteTabelasImportadas(lote);
    }

    @Test
    @DisplayName("Deve retornar lista vazia de modelos se não houver token")
    void deveRetornarModelosVazioSemToken() {
        // GIVEN
        when(authService.estaLogado()).thenReturn(false);

        // WHEN
        List<String> modelos = facade.listarModelosIADisponiveis();

        // THEN
        assertTrue(modelos.isEmpty());
        verifyNoInteractions(geminiService);
    }
}
