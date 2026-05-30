package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para AssistenteDocumentalService.
 * Corrigido para incluir injeção de SummaryGeneratorUtils e logs rigorosos.
 */
@ExtendWith(MockitoExtension.class)
class AssistenteDocumentalServiceTest {

    private static final Logger log = LoggerFactory.getLogger(AssistenteDocumentalServiceTest.class);
    private static final String LOG_PREFIX = "[AssistenteDocumentalServiceTest]";

    @InjectMocks
    private AssistenteDocumentalService service;

    @Mock
    private GeminiService geminiService;
    @Mock
    private AuthService authService;
    @Mock
    private GeminiPromptFactory promptFactory;
    @Mock
    private SummaryGeneratorUtils summaryUtils; // CORREÇÃO: Mock adicionado para evitar NPE

    @TempDir
    Path tempDir;

    private ProponenteModel proponenteMock;
    private UsuarioModel usuarioMock;

    @BeforeEach
    void setUp() {
        proponenteMock = new ProponenteModel();
        proponenteMock.setId(1L);
        proponenteMock.setNomeCompleto("WAGNER ALVARO");

        usuarioMock = new UsuarioModel();
        usuarioMock.setGeminiApiKey("api_key_teste");
    }

    @Test
    @DisplayName("Deve selecionar a estratégia de Identificação para documentos como RG ou CNH")
    void deveDeterminarConfigIdentificacao() {
        log.info("{} [TELEMETRIA] Testando determinação de config para Identificação.", LOG_PREFIX);

        var configRG = service.determinarConfiguracaoIA("RG_FRENTE");
        assertThat(configRG.titulo()).isEqualTo("Triagem Visual de Identificação");

        verify(promptFactory, atLeastOnce()).getIdentificacaoDocumentalPrompt();
        log.info("{} [AUDITORIA] Teste de Identificação concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve selecionar a estratégia Financeira para Holerites ou Extratos")
    void deveDeterminarConfigFinanceira() {
        log.info("{} [TELEMETRIA] Testando determinação de config Financeira.", LOG_PREFIX);

        var config = service.determinarConfiguracaoIA("HOLERITE_MARCO");
        assertThat(config.titulo()).isEqualTo("Auditoria de Margem Consignável");

        verify(promptFactory).getFinanceiroDocumentalPrompt();
        log.info("{} [AUDITORIA] Teste Financeiro concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve selecionar a estratégia Geral para tipos de documentos desconhecidos")
    void deveDeterminarConfigGeral() {
        log.info("{} [TELEMETRIA] Testando determinação de config Geral.", LOG_PREFIX);

        var config = service.determinarConfiguracaoIA("CONTRATO_SOCIAL");
        assertThat(config.titulo()).isEqualTo("Análise Documental Geral");

        verify(promptFactory).getGeralDocumentalPrompt();
        log.info("{} [AUDITORIA] Teste Geral concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar analisar um documento cujo arquivo físico não existe")
    void deveFalharSeArquivoNaoExiste() {
        log.info("{} [TELEMETRIA] Testando falha por arquivo inexistente.", LOG_PREFIX);

        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setArquivoPath(tempDir.resolve("inexistente.pdf").toString());

        assertThatThrownBy(() -> service.analisarDocumento(doc, proponenteMock, "gemini-pro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo físico não encontrado");

        log.info("{} [AUDITORIA] Teste de falha de arquivo concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve orquestrar a análise chamando o GeminiService com os parâmetros corretos")
    void deveAnalisarDocumentoComSucesso() throws Exception {
        log.info("{} [TELEMETRIA] Testando orquestração de análise com sucesso.", LOG_PREFIX);

        Path arquivoPath = tempDir.resolve("rg_teste.jpg");
        Files.createFile(arquivoPath);
        File arquivoFisico = arquivoPath.toFile();

        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setId(100L);
        doc.setTipoDocumento("RG");
        doc.setArquivoPath(arquivoFisico.getAbsolutePath());

        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(promptFactory.getIdentificacaoDocumentalPrompt()).thenReturn("Prompt ID");
        when(summaryUtils.gerarJsonContextualParaIA(any(), anyBoolean())).thenReturn("{}"); // CORREÇÃO: Stub do
                                                                                            // utilitário

        when(geminiService.perguntarAoAssistente(anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                anyString(), anyString(), anyList())).thenReturn("OK");

        String resultado = service.analisarDocumento(doc, proponenteMock, "gemini-flash");

        assertThat(resultado).isEqualTo("OK");
        verify(geminiService).perguntarAoAssistente(eq("Prompt ID"), eq("api_key_teste"), eq("gemini-flash"),
                eq(arquivoFisico), anyString(), anyString(), anyString(), anyString(), anyList());

        log.info("{} [AUDITORIA] Teste de orquestração concluído com sucesso.", LOG_PREFIX);
    }
}
