package br.com.poderfinanceiro.app.domain.service;

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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h1>AssistenteDocumentalServiceTest</h1>
 * <p>
 * Testes de Unidade para o Assistente Documental via IA.
 * Valida a seleção de estratégias (Prompts) e a orquestração da análise
 * cognitiva.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AssistenteDocumentalServiceTest {

    @InjectMocks
    private AssistenteDocumentalService service;

    @Mock
    private GeminiService geminiService;

    @Mock
    private AuthService authService;

    @Mock
    private GeminiPromptFactory promptFactory;

    @TempDir
    Path tempDir;

    private ProponenteModel proponenteMock;
    private UsuarioModel usuarioMock;

    @BeforeEach
    void setUp() {
        // Instanciação de objetos de suporte
        proponenteMock = new ProponenteModel();
        proponenteMock.setId(1L);
        proponenteMock.setNomeCompleto("WAGNER ALVARO");

        usuarioMock = new UsuarioModel();
        usuarioMock.setGeminiApiKey("api_key_teste");
    }

    @Test
    @DisplayName("Deve selecionar a estratégia de Identificação para documentos como RG ou CNH")
    void deveDeterminarConfigIdentificacao() {
        // Ação
        var configRG = service.determinarConfiguracaoIA("RG_FRENTE");
        var configCNH = service.determinarConfiguracaoIA("CNH_VERSO");

        // Validação
        assertThat(configRG.titulo()).isEqualTo("Triagem Visual de Identificação");
        assertThat(configCNH.icone()).isEqualTo("🔍");
        verify(promptFactory, atLeastOnce()).getIdentificacaoDocumentalPrompt();
    }

    @Test
    @DisplayName("Deve selecionar a estratégia Financeira para Holerites ou Extratos")
    void deveDeterminarConfigFinanceira() {
        // Ação
        var config = service.determinarConfiguracaoIA("HOLERITE_MARCO");

        // Validação
        assertThat(config.titulo()).isEqualTo("Auditoria de Margem Consignável");
        assertThat(config.icone()).isEqualTo("📊");
        verify(promptFactory).getFinanceiroDocumentalPrompt();
    }

    @Test
    @DisplayName("Deve selecionar a estratégia Geral para tipos de documentos desconhecidos")
    void deveDeterminarConfigGeral() {
        // Ação
        var config = service.determinarConfiguracaoIA("CONTRATO_SOCIAL");

        // Validação
        assertThat(config.titulo()).isEqualTo("Análise Documental Geral");
        assertThat(config.icone()).isEqualTo("🤖");
        verify(promptFactory).getGeralDocumentalPrompt();
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar analisar um documento cujo arquivo físico não existe")
    void deveFalharSeArquivoNaoExiste() {
        // Cenário
        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setArquivoPath(tempDir.resolve("inexistente.pdf").toString());

        // Validação
        assertThatThrownBy(() -> service.analisarDocumento(doc, proponenteMock, "gemini-pro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo físico não encontrado");
    }

    @Test
    @DisplayName("Deve orquestrar a análise chamando o GeminiService com os parâmetros corretos")
    void deveAnalisarDocumentoComSucesso() throws Exception {
        // 1. Preparar arquivo físico real no diretório temporário
        Path arquivoPath = tempDir.resolve("rg_teste.jpg");
        Files.createFile(arquivoPath);
        File arquivoFisico = arquivoPath.toFile();

        // 2. Configurar o modelo de documento
        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setId(100L);
        doc.setTipoDocumento("RG");
        doc.setArquivoPath(arquivoFisico.getAbsolutePath());

        // 3. Simular dependências (Stubbings movidos para cá para evitar
        // UnnecessaryStubbingException)
        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(promptFactory.getIdentificacaoDocumentalPrompt()).thenReturn("Prompt de Identificação");

        when(geminiService.perguntarAoAssistente(anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                anyString(), anyString(), anyList()))
                .thenReturn("Análise da IA: Documento OK");

        // 4. Executar
        String resultado = service.analisarDocumento(doc, proponenteMock, "gemini-2.5-flash");

        // 5. Validar
        assertThat(resultado).isEqualTo("Análise da IA: Documento OK");

        verify(geminiService).perguntarAoAssistente(
                eq("Prompt de Identificação"),
                eq("api_key_teste"),
                eq("gemini-2.5-flash"),
                eq(arquivoFisico),
                anyString(), anyString(), anyString(), anyString(), anyList());
    }
}
