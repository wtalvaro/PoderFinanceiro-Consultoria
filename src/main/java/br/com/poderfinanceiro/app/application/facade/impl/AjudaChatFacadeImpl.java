package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.facade.IAjudaChatFacade;
import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.domain.service.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Implementação da Facade do Chat de Ajuda.
 * Orquestra o contexto do sistema para alimentar a IA do Gemini.
 * Refatorada para injeção de dependência do ObjectMapper e logs rigorosos.
 */
@Service
public class AjudaChatFacadeImpl implements IAjudaChatFacade {

    private static final Logger log = LoggerFactory.getLogger(AjudaChatFacadeImpl.class);
    private static final String LOG_PREFIX = "[AjudaChatFacade]";

    private final GeminiService geminiService;
    private final AuthService authService;
    private final AtendimentoContextService contextoService;
    private final TabelaJurosService tabelaService;
    private final LinkUtilRepository linkRepository;
    private final SummaryGeneratorUtils summaryUtils;
    private final ObjectMapper objectMapper; // Injetado pelo Spring
    private final File pastaTrabalho;

    public AjudaChatFacadeImpl(
            GeminiService geminiService,
            AuthService authService,
            AtendimentoContextService contextoService,
            TabelaJurosService tabelaService,
            LinkUtilRepository linkRepository,
            SummaryGeneratorUtils summaryUtils,
            ObjectMapper objectMapper,
            @Value("${app.storage.chats:default}") String customPath) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.contextoService = contextoService;
        this.tabelaService = tabelaService;
        this.linkRepository = linkRepository;
        this.summaryUtils = summaryUtils;
        this.objectMapper = objectMapper;
        this.pastaTrabalho = inicializarDiretorio(customPath);
        log.info("{} [SISTEMA] Facade do Chat inicializada com ObjectMapper injetado.", LOG_PREFIX);
    }

    @Override
    public boolean isApiKeyConfigurada() {
        log.trace("{} [NEGOCIO] Verificando configuração de API Key.", LOG_PREFIX);
        return authService.estaLogado() && authService.getUsuarioLogado().getGeminiApiKey() != null;
    }

    @Override
    public String getApiKeyAtual() {
        log.trace("{} [TELEMETRIA] Recuperando API Key para operação de IA.", LOG_PREFIX);
        return isApiKeyConfigurada() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
    }

    @Override
    public void atualizarApiKey(String novaChave) {
        log.info("{} [AUDITORIA] Solicitando atualização de API Key do usuário.", LOG_PREFIX);
        authService.atualizarGeminiApiKey(novaChave);
    }

    @Override
    public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Consultando modelos multimodais disponíveis.", LOG_PREFIX);
        return geminiService.listarModelosMultimodais(getApiKeyAtual());
    }

    @Override
    public String enviarMensagemParaIA(String mensagem, File anexo, String modelo,
            List<GeminiRequest.Content> historico) {
        log.info("{} [TELEMETRIA] Orquestrando contexto e enviando mensagem para IA. Modelo: {}", LOG_PREFIX, modelo);

        String jsonFoco = obterContextoPrincipal();
        String jsonComissoes = obterContextoComissoes();
        String jsonTabelas = summaryUtils.gerarJsonTabelasJuros(tabelaService.listarAtivas());
        String jsonLinks = summaryUtils.gerarJsonLinksUteis(linkRepository.findAll());

        return geminiService.perguntarAoAssistente(mensagem, getApiKeyAtual(), modelo, anexo, jsonFoco, jsonTabelas,
                jsonLinks,
                jsonComissoes, historico);
    }

    @Override
    public void salvarSessao(String nomeArquivo, List<GeminiRequest.Content> historico) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando persistência da sessão: {}", LOG_PREFIX, nomeArquivo);
        File arquivo = new File(pastaTrabalho, nomeArquivo);

        // Uso do ObjectMapper injetado
        objectMapper.writeValue(arquivo, historico);

        log.info("{} [AUDITORIA] Sessão salva com sucesso no disco: {}", LOG_PREFIX, arquivo.getName());
    }

    @Override
    public List<GeminiRequest.Content> carregarSessao(File arquivo) throws Exception {
        log.info("{} [TELEMETRIA] Carregando histórico da sessão do arquivo: {}", LOG_PREFIX, arquivo.getName());

        // Uso do ObjectMapper injetado com TypeReference para manter integridade de
        // tipos
        List<GeminiRequest.Content> historico = objectMapper.readValue(arquivo, new TypeReference<>() {
        });

        log.debug("{} [TELEMETRIA] Sessão carregada. Total de mensagens: {}", LOG_PREFIX, historico.size());
        return historico;
    }

    @Override
    public List<SessaoChatDTO> listarSessoesRecentes() {
        log.trace("{} [TELEMETRIA] Listando arquivos de chat no diretório de trabalho.", LOG_PREFIX);
        File[] arquivos = pastaTrabalho.listFiles((dir, name) -> name.endsWith(".json"));

        if (arquivos == null || arquivos.length == 0) {
            log.debug("{} [NEGOCIO] Nenhuma sessão localizada.", LOG_PREFIX);
            return List.of();
        }

        return Arrays.stream(arquivos)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(file -> new SessaoChatDTO(file, extrairPrimeiraMensagem(file)))
                .toList();
    }

    // --- MÉTODOS PRIVADOS DE APOIO ---

    private File inicializarDiretorio(String path) {
        log.trace("{} [SISTEMA] Resolvendo diretório de armazenamento.", LOG_PREFIX);
        File pasta;
        if ("default".equals(path)) {
            String os = System.getProperty("os.name").toLowerCase();
            String home = System.getProperty("user.home");
            String base = os.contains("win") ? System.getenv("APPDATA") + File.separator + "PoderFinanceiro"
                    : os.contains("mac") ? home + "/Library/Application Support/PoderFinanceiro"
                            : home + "/.local/share/PoderFinanceiro";
            pasta = new File(base, "chats_gemini");
        } else {
            pasta = new File(path);
        }

        if (!pasta.exists()) {
            log.info("{} [SISTEMA] Criando diretório de chats: {}", LOG_PREFIX, pasta.getAbsolutePath());
            pasta.mkdirs();
        }
        return pasta;
    }

    private String obterContextoPrincipal() {
        log.trace("{} [NEGOCIO] Extraindo contexto da tela focada.", LOG_PREFIX);
        AtendimentoContextService.TipoTelaFocada telaAtual = contextoService.getTelaAtualFocada();

        if (telaAtual == AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS
                && contextoService.getPropostaAtiva() != null) {
            log.debug("{} [NEGOCIO] Foco em Proposta detectado.", LOG_PREFIX);
            return summaryUtils.gerarJsonPropostaParaIA(contextoService.getPropostaAtiva());
        }

        boolean isAbaCliente = (telaAtual == AtendimentoContextService.TipoTelaFocada.CADASTRO_CLIENTE);
        return contextoService.getLeadAtivo() != null
                ? summaryUtils.gerarJsonContextualParaIA(contextoService.getLeadAtivo(), isAbaCliente)
                : "{}";
    }

    private String obterContextoComissoes() {
        if (contextoService.getTelaAtualFocada() == AtendimentoContextService.TipoTelaFocada.GESTAO_COMISSOES) {
            log.debug("{} [NEGOCIO] Foco em Comissões detectado.", LOG_PREFIX);
            return summaryUtils.gerarJsonComissoes(contextoService.getComissoesAtivas());
        }
        return "[]";
    }

    private String extrairPrimeiraMensagem(File arquivo) {
        log.trace("{} [SISTEMA] Extraindo preview do arquivo: {}", LOG_PREFIX, arquivo.getName());
        try {
            List<GeminiRequest.Content> historico = carregarSessao(arquivo);
            for (GeminiRequest.Content content : historico) {
                if ("user".equals(content.role()) && content.parts() != null && !content.parts().isEmpty()) {
                    String texto = content.parts().get(0).text();
                    if (texto != null && !texto.isBlank())
                        return "💬 " + texto.trim();
                }
            }
        } catch (Exception e) {
            log.warn("{} [SISTEMA] Falha ao ler preview do arquivo {}: {}", LOG_PREFIX, arquivo.getName(),
                    e.getMessage());
        }
        return "💬 Chat Antigo";
    }
}
