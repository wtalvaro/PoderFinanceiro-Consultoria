package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.application.facade.ICopilotoFacade.ConselhoIADTO;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.domain.service.SimulacaoCopilotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para CopilotoFacadeImpl.
 * Corrigido para suportar Records e estrutura real de modelos.
 */
class CopilotoFacadeTest {

    private CopilotoFacadeImpl facade;
    private SimulacaoCopilotoService copilotoService;
    private ProponenteService proponenteService;
    private GeminiService geminiService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        copilotoService = mock(SimulacaoCopilotoService.class);
        proponenteService = mock(ProponenteService.class);
        geminiService = mock(GeminiService.class);
        authService = mock(AuthService.class);
        facade = new CopilotoFacadeImpl(copilotoService, proponenteService, geminiService, authService);
    }

    @Test
    @DisplayName("Deve reordenar o ranking de simulações com base no conselho da IA")
    void deveGerarConselhoEReordenarRanking() {
        // GIVEN
        SimulacaoRascunhoDTO rascunho = new SimulacaoRascunhoDTO(45, BigDecimal.valueOf(5000), "INSS",
                BigDecimal.valueOf(10000), 84, BigDecimal.ZERO);

        // Criando Mocks para Tabela e Banco para compor o ResultadoSimulacaoDTO
        TabelaJurosModel t1 = mock(TabelaJurosModel.class);
        BancoModel b1 = new BancoModel();
        b1.setNome("Banco A");
        when(t1.getBanco()).thenReturn(b1);

        TabelaJurosModel t2 = mock(TabelaJurosModel.class);
        BancoModel b2 = new BancoModel();
        b2.setNome("Banco B");
        when(t2.getBanco()).thenReturn(b2);

        TabelaJurosModel t3 = mock(TabelaJurosModel.class);
        BancoModel b3 = new BancoModel();
        b3.setNome("Banco C");
        when(t3.getBanco()).thenReturn(b3);

        ResultadoSimulacaoDTO res1 = new ResultadoSimulacaoDTO(t1, BigDecimal.TEN, BigDecimal.valueOf(200));
        ResultadoSimulacaoDTO res2 = new ResultadoSimulacaoDTO(t2, BigDecimal.TEN, BigDecimal.valueOf(210));
        ResultadoSimulacaoDTO res3 = new ResultadoSimulacaoDTO(t3, BigDecimal.TEN, BigDecimal.valueOf(190));

        List<ResultadoSimulacaoDTO> rankingAtual = List.of(res1, res2, res3);
        String respostaIA = "Recomendo: [TOP: 3, 1].";

        when(copilotoService.gerarRecomendacaoInteligenteIA(any(), anyList(), anyString())).thenReturn(respostaIA);

        // WHEN
        ConselhoIADTO resultado = facade.gerarConselhoEReordenarRanking(rascunho, rankingAtual, "gemini-pro");

        // THEN
        assertNotNull(resultado);
        // Acessando campos do Record ConselhoIADTO corretamente
        assertEquals(3, resultado.rankingReordenado().size());
        assertEquals("Banco C", resultado.rankingReordenado().get(0).tabela().getBanco().getNome());
        assertEquals("Banco A", resultado.rankingReordenado().get(1).tabela().getBanco().getNome());
        assertFalse(resultado.textoResposta().contains("[TOP:"));
    }

    @Test
    @DisplayName("Deve extrair margem limpa do documento")
    void deveExtrairMargemDocumento() {
        File mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("margem.pdf");
        when(copilotoService.extrairMargemDocumento(any())).thenReturn("RESULTADO FINAL: 1.500,00");

        String margem = facade.extrairMargemDocumento(mockFile);

        assertEquals("1500,00", margem);
    }

    @Test
    @DisplayName("Deve calcular idade corretamente")
    void deveCalcularIdade() {
        LocalDate nascimento = LocalDate.now().minusYears(25);
        assertEquals(25, facade.calcularIdade(nascimento));
    }
}
