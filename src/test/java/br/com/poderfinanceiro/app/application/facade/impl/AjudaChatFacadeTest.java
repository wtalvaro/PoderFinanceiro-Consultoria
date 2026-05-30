package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.facade.IAjudaChatFacade.SessaoChatDTO;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.domain.service.*;
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
 * Valida a orquestração de IA e persistência de histórico.
 */
class AjudaChatFacadeTest {

    private AjudaChatFacadeImpl facade;

    // Mocks de dependências de domínio e infraestrutura
    private GeminiService geminiService;
    private AuthService authService;
    private AtendimentoContextService contextoService;
    private TabelaJurosService tabelaService;
    private LinkUtilRepository linkRepository;

    private Properties originalProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Preservar ambiente do desenvolvedor
        originalProperties = (Properties) System.getProperties().clone();

        // Redireciona o diretório de trabalho para a pasta temporária do JUnit
        System.setProperty("user.home", tempDir.toString());
        System.setProperty("os.name", "Linux");

        geminiService = mock(GeminiService.class);
        authService = mock(AuthService.class);
        contextoService = mock(AtendimentoContextService.class);
        tabelaService = mock(TabelaJurosService.class);
        linkRepository = mock(LinkUtilRepository.class);

        facade = new AjudaChatFacadeImpl(
                geminiService, authService, contextoService, tabelaService, linkRepository);
    }

    @AfterEach
    void tearDown() {
        // Restaura as propriedades originais do Fedora
        System.setProperties(originalProperties);
    }

    @Test
    @DisplayName("Deve validar se a API Key está configurada no UsuarioModel logado")
    void deveValidarApiKeyConfigurada() {
        // GIVEN
        UsuarioModel usuario = new UsuarioModel();
        usuario.setGeminiApiKey("AIzaSy_TESTE_KEY");

        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        // WHEN & THEN
        assertTrue(facade.isApiKeyConfigurada());
        assertEquals("AIzaSy_TESTE_KEY", facade.getApiKeyAtual());
    }

    @Test
    @DisplayName("Deve enviar mensagem para IA com contexto de PropostaModel quando focada")
    void deveEnviarMensagemComContextoDeProposta() {
        // GIVEN
        String mensagem = "Analise esta proposta";
        UsuarioModel usuario = new UsuarioModel();
        usuario.setGeminiApiKey("key-123");

        when(authService.estaLogado()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuario);
        when(contextoService.getTelaAtualFocada())
                .thenReturn(AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS);
        when(contextoService.getPropostaAtiva()).thenReturn(new PropostaModel());
        when(tabelaService.listarAtivas()).thenReturn(new ArrayList<>());
        when(linkRepository.findAll()).thenReturn(new ArrayList<>());

        // WHEN
        facade.enviarMensagemParaIA(mensagem, null, "gemini-1.5-pro", new ArrayList<>());

        // THEN
        verify(geminiService).perguntarAoAssistente(
                eq(mensagem),
                eq("key-123"),
                eq("gemini-1.5-pro"),
                any(),
                anyString(), // jsonFoco (Deve conter dados da proposta)
                anyString(), // jsonTabelas
                anyString(), // jsonLinks
                anyString(), // jsonComissoes
                anyList() // historico
        );
    }

    @Test
    @DisplayName("Deve salvar e carregar uma sessão de chat no sistema de arquivos")
    void devePersistirSessaoNoDisco() throws Exception {
        // GIVEN
        String nomeArquivo = "sessao_teste.json";
        List<GeminiRequest.Content> historico = List.of(
                new GeminiRequest.Content("user", List.of(GeminiRequest.Part.ofText("Olá Gemini"))));

        // WHEN
        facade.salvarSessao(nomeArquivo, historico);

        // THEN
        // O caminho esperado no Linux simulado é
        // ~/.local/share/PoderFinanceiro/chats_gemini/
        File pastaEsperada = new File(tempDir.toFile(), ".local/share/PoderFinanceiro/chats_gemini");
        File arquivoGerado = new File(pastaEsperada, nomeArquivo);

        assertTrue(arquivoGerado.exists(), "O arquivo JSON deveria ter sido criado fisicamente.");

        List<GeminiRequest.Content> carregado = facade.carregarSessao(arquivoGerado);
        assertNotNull(carregado);
        assertEquals(1, carregado.size());
        assertEquals("Olá Gemini", carregado.get(0).parts().get(0).text());
    }

    @Test
    @DisplayName("Deve listar sessões recentes ordenadas por data de modificação")
    void deveListarSessoesRecentes() throws Exception {
        // GIVEN
        facade.salvarSessao("chat_antigo.json", new ArrayList<>());
        Thread.sleep(150); // Garante diferença no timestamp de modificação
        facade.salvarSessao("chat_recente.json", new ArrayList<>());

        // WHEN
        List<SessaoChatDTO> sessoes = facade.listarSessoesRecentes();

        // THEN
        assertFalse(sessoes.isEmpty());
        assertEquals("chat_recente.json", sessoes.get(0).arquivo().getName(),
                "O arquivo mais recente deve ser o primeiro.");
    }
}
