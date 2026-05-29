package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("[Domínio] Teste de Unidade - SimulacaoCopilotoService")
class SimulacaoCopilotoServiceTest {

    @Mock
    private TabelaJurosRepository tabelaJurosRepository;
    @Mock
    private GeminiService geminiService;
    @Mock
    private AuthService authService;
    @Mock
    private GeminiPromptFactory promptFactory;

    @InjectMocks
    private SimulacaoCopilotoService simulacaoCopilotoService;

    private UsuarioModel consultorMock;
    private SimulacaoRascunhoDTO rascunhoMock;

    @BeforeEach
    void setUp() {
        consultorMock = new UsuarioModel();
        consultorMock.setId(1L);
        consultorMock.setGeminiApiKey("api-key-teste-123");

        rascunhoMock = new SimulacaoRascunhoDTO(
                65, // idade
                new BigDecimal("3500.00"), // rendaMensal
                "INSS_CONSIGNADO", // tipoConvenio
                new BigDecimal("10000.00"), // valorDesejado
                84, // prazoDesejado
                new BigDecimal("450.00") // margemDisponivel
        );
    }

    @Test
    @DisplayName("Deve processar simulação rápida e ordenar por maior comissão")
    void deveProcessarSimulacaoRapidaComOrdenacao() {
        // GIVEN
        TabelaJurosModel tabelaA = new TabelaJurosModel();
        tabelaA.setNomeTabela("Tabela A - 5%");
        tabelaA.setTaxaMensal(new BigDecimal("1.80"));
        tabelaA.setComissaoPercentual(new BigDecimal("5.00"));

        TabelaJurosModel tabelaB = new TabelaJurosModel();
        tabelaB.setNomeTabela("Tabela B - 7%");
        tabelaB.setTaxaMensal(new BigDecimal("1.95"));
        tabelaB.setComissaoPercentual(new BigDecimal("7.00"));

        when(tabelaJurosRepository.findTabelasElegiveis(any(), anyInt(), any(), anyInt()))
                .thenReturn(List.of(tabelaA, tabelaB));

        // WHEN
        List<ResultadoSimulacaoDTO> resultados = simulacaoCopilotoService.processarSimulacaoRapida(rascunhoMock);

        // THEN
        assertThat(resultados).hasSize(2);
        // Tabela B deve vir primeiro (7% > 5%)
        assertThat(resultados.get(0).tabela().getComissaoPercentual()).isEqualByComparingTo("7.00");
        assertThat(resultados.get(0).comissaoEstimada()).isEqualByComparingTo("700.00"); // 10000 * 0.07
        assertThat(resultados.get(1).comissaoEstimada()).isEqualByComparingTo("500.00"); // 10000 * 0.05

        // Validação do cálculo de parcela (estimativa linear: valor * taxa)
        // 10000 * 0.0180 = 180.00
        assertThat(resultados.get(1).valorParcela()).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("Deve retornar lista vazia para convênio inválido")
    void deveRetornarVazioParaConvenioInvalido() {
        // GIVEN
        SimulacaoRascunhoDTO rascunhoInvalido = new SimulacaoRascunhoDTO(
                30, new BigDecimal("2000"), "CONVENIO_INEXISTENTE",
                new BigDecimal("1000"), 12, new BigDecimal("100"));

        // WHEN
        List<ResultadoSimulacaoDTO> resultados = simulacaoCopilotoService.processarSimulacaoRapida(rascunhoInvalido);

        // THEN
        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("Deve extrair margem de documento via IA com sucesso")
    void deveExtrairMargemViaIA() {
        // GIVEN
        File arquivoFake = mock(File.class);
        when(arquivoFake.exists()).thenReturn(true);
        when(arquivoFake.getName()).thenReturn("contracheque.pdf");

        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(promptFactory.getMargemDocumentoPrompt()).thenReturn("Prompt de Margem");
        when(geminiService.perguntarAoAssistente(anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                anyString(), anyString(), anyList()))
                .thenReturn("Margem extraída: R$ 450,00");

        // WHEN
        String resultado = simulacaoCopilotoService.extrairMargemDocumento(arquivoFake);

        // THEN
        assertThat(resultado).contains("R$ 450,00");
    }

    @Test
    @DisplayName("Deve tratar erro de arquivo inexistente na extração de margem")
    void deveTratarArquivoInexistente() {
        // GIVEN
        File arquivoInexistente = new File("nao_existe.pdf");

        // WHEN
        String resultado = simulacaoCopilotoService.extrairMargemDocumento(arquivoInexistente);

        // THEN
        assertThat(resultado).contains("Erro: Arquivo não fornecido");
    }

    @Test
    @DisplayName("Deve gerar recomendação estratégica via IA")
    void deveGerarRecomendacaoEstrategica() {
        // GIVEN
        ResultadoSimulacaoDTO res = new ResultadoSimulacaoDTO(new TabelaJurosModel(), BigDecimal.TEN, BigDecimal.ONE);
        List<ResultadoSimulacaoDTO> ranking = List.of(res);

        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(promptFactory.getRecomendacaoEstrategicaPrompt(any(), anyList())).thenReturn("Prompt Estratégico");
        when(geminiService.perguntarTexto(anyString(), anyString(), anyString()))
                .thenReturn("Recomendação: Use a Tabela B para maior lucro.");

        // WHEN
        String recomendacao = simulacaoCopilotoService.gerarRecomendacaoInteligenteIA(rascunhoMock, ranking,
                "gemini-2.0-flash");

        // THEN
        assertThat(recomendacao).contains("Tabela B");
    }

    @Test
    @DisplayName("Deve abortar recomendação se o ranking estiver vazio")
    void deveAbortarRecomendacaoSeRankingVazio() {
        // WHEN
        String resultado = simulacaoCopilotoService.gerarRecomendacaoInteligenteIA(rascunhoMock, List.of(), "modelo");

        // THEN
        assertThat(resultado).isEqualTo("Nenhuma tabela encontrada para este perfil.");
    }
}
