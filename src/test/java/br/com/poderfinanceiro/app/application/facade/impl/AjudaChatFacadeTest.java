package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.facade.IAjudaChatFacade.SessaoChatDTO;
import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.domain.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Gold Standard para AjudaChatFacadeImpl.
 * Sincronizado com a refatoração de injeção de dependência total (v2.1.4).
 */
class AjudaChatFacadeTest {

    private AjudaChatFacadeImpl facade;

    // Mocks de dependências
    private GeminiService geminiService;
    private AuthService authService;
    private AtendimentoContextService contextoService;
    private TabelaJurosService tabelaService;
    private LinkUtilRepository linkRepository;
    private ObjectMapper objectMapper;
    private SummaryGeneratorUtils summaryUtils; // Novo mock necessário

    private Properties originalProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        originalProperties = (Properties) System.getProperties().clone();
        System.setProperty("os.name", "Linux");

        // Inicialização dos Mocks
        geminiService = mock(GeminiService.class);
        authService = mock(AuthService.class);
        contextoService = mock(AtendimentoContextService.class);
        tabelaService = mock(TabelaJurosService.class);
        linkRepository = mock(LinkUtilRepository.class);
        summaryUtils = mock(SummaryGeneratorUtils.class);

        // Configuração do motor JSON real para os testes de persistência
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // CORREÇÃO: Instanciação com os 8 parâmetros exigidos pela Facade Gold Standard
        facade = new AjudaChatFacadeImpl(
                geminiService,
                authService,
                contextoService,
                tabelaService,
                linkRepository,
                summaryUtils, // Injetando o mock do utilitário
                objectMapper,
                tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperties(originalProperties);
    }

    @Test
    @DisplayName("Deve validar se a API Key está configurada no UsuarioModel logado")
    void deveValidarApiKeyConfigurada() {
        UsuarioModel usuario = new UsuarioModel();
        usuario.setGeminiApiKey("AIzaSy_TESTE_KEY");

        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        assertTrue(facade.isApiKeyConfigurada());
        assertEquals("AIzaSy_TESTE_KEY", facade.getApiKeyAtual());
    }

    @Test
    @DisplayName("Deve enviar mensagem para IA orquestrando contexto via SummaryUtils")
    void deveEnviarMensagemComContexto() {
        // GIVEN
        String mensagem = "Analise esta proposta";
        UsuarioModel usuario = new UsuarioModel();
        usuario.setGeminiApiKey("key-123");

        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);
        when(contextoService.getTelaAtualFocada())
                .thenReturn(AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS);
        when(contextoService.getPropostaAtiva()).thenReturn(new PropostaModel());

        // Simulando o retorno do SummaryUtils injetado
        when(summaryUtils.gerarJsonTabelasJuros(any())).thenReturn("[]");
        when(summaryUtils.gerarJsonLinksUteis(any())).thenReturn("[]");
        when(summaryUtils.gerarJsonPropostaParaIA(any())).thenReturn("{\"id\": 1}");

        // WHEN
        facade.enviarMensagemParaIA(mensagem, null, "gemini-1.5-pro", new ArrayList<>());

        // THEN
        verify(geminiService).perguntarAoAssistente(
                eq(mensagem), eq("key-123"), eq("gemini-1.5-pro"), any(),
                anyString(), anyString(), anyString(), anyString(), anyList());
        // Verifica se a Facade usou a instância injetada do utilitário
        verify(summaryUtils, atLeastOnce()).gerarJsonTabelasJuros(any());
    }

    @Test
    @DisplayName("Deve salvar e carregar uma sessão de chat no sistema de arquivos temporário")
    void devePersistirSessaoNoDisco() throws Exception {
        String nomeArquivo = "sessao_teste.json";
        List<GeminiRequest.Content> historico = List.of(
                new GeminiRequest.Content("user", List.of(GeminiRequest.Part.ofText("Olá Gemini"))));

        facade.salvarSessao(nomeArquivo, historico);

        File arquivoGerado = new File(tempDir.toFile(), nomeArquivo);
        assertTrue(arquivoGerado.exists(), "O arquivo JSON deveria ter sido criado fisicamente.");

        List<GeminiRequest.Content> carregado = facade.carregarSessao(arquivoGerado);
        assertNotNull(carregado);
        assertEquals(1, carregado.size());
        assertEquals("Olá Gemini", carregado.get(0).parts().get(0).text());
    }

    @Test
    @DisplayName("Deve listar sessões recentes ordenadas por data de modificação")
    void deveListarSessoesRecentes() throws Exception {
        facade.salvarSessao("chat_antigo.json", new ArrayList<>());
        Thread.sleep(150);
        facade.salvarSessao("chat_recente.json", new ArrayList<>());

        List<SessaoChatDTO> sessoes = facade.listarSessoesRecentes();

        assertFalse(sessoes.isEmpty());
        assertEquals("chat_recente.json", sessoes.get(0).arquivo().getName());
    }
}
