package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import br.com.poderfinanceiro.app.application.facade.IAjudaChatFacade;
import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import br.com.poderfinanceiro.app.domain.service.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class AjudaChatFacadeImpl implements IAjudaChatFacade {

    private static final Logger log = LoggerFactory.getLogger(AjudaChatFacadeImpl.class);
    private static final String LOG_PREFIX = "[AjudaChatFacade]";

    private final GeminiService geminiService;
    private final AuthService authService;
    private final AtendimentoContextService contextoService;
    private final TabelaJurosService tabelaService;
    private final LinkUtilRepository linkRepository;

    public AjudaChatFacadeImpl(GeminiService geminiService, AuthService authService, AtendimentoContextService contextoService,
            TabelaJurosService tabelaService, LinkUtilRepository linkRepository) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.contextoService = contextoService;
        this.tabelaService = tabelaService;
        this.linkRepository = linkRepository;
        log.debug("{} [SISTEMA] Facade do Chat instanciada.", LOG_PREFIX);
    }

    @Override public boolean isApiKeyConfigurada() {
        return authService.estaLogado() && authService.getUsuarioLogado().getGeminiApiKey() != null;
    }

    @Override public String getApiKeyAtual() {
        return isApiKeyConfigurada() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
    }

    @Override public void atualizarApiKey(String novaChave) {
        log.info("{} [AUDITORIA] Atualizando API Key do usuário.", LOG_PREFIX);
        authService.atualizarGeminiApiKey(novaChave);
    }

    @Override public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Solicitando modelos de IA disponíveis.", LOG_PREFIX);
        return geminiService.listarModelosMultimodais(getApiKeyAtual());
    }

    @Override public String enviarMensagemParaIA(String mensagem, File anexo, String modelo, List<GeminiRequest.Content> historico) {
        log.info("{} [TELEMETRIA] Montando contexto e enviando mensagem para IA. Modelo: {}", LOG_PREFIX, modelo);

        String jsonFoco = obterContextoPrincipal();
        String jsonComissoes = obterContextoComissoes();
        String jsonTabelas = SummaryGeneratorUtils.gerarJsonTabelasJuros(tabelaService.listarAtivas());
        String jsonLinks = SummaryGeneratorUtils.gerarJsonLinksUteis(linkRepository.findAll());

        return geminiService.perguntarAoAssistente(mensagem, getApiKeyAtual(), modelo, anexo, jsonFoco, jsonTabelas, jsonLinks,
                jsonComissoes, historico);
    }

    @Override public void salvarSessao(String nomeArquivo, List<GeminiRequest.Content> historico) throws Exception {
        File arquivo = new File(obterDiretorioChats(), nomeArquivo);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(arquivo, historico);
        log.trace("{} [SISTEMA] Sessão salva no disco: {}", LOG_PREFIX, arquivo.getName());
    }

    @Override public List<GeminiRequest.Content> carregarSessao(File arquivo) throws Exception {
        log.info("{} [TELEMETRIA] Carregando histórico da sessão: {}", LOG_PREFIX, arquivo.getName());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(arquivo, new TypeReference<>() {
        });
    }

    @Override public List<SessaoChatDTO> listarSessoesRecentes() {
        File diretorio = obterDiretorioChats();
        File[] arquivos = diretorio.listFiles((dir, name) -> name.endsWith(".json"));
        if (arquivos == null || arquivos.length == 0)
            return List.of();

        return Arrays.stream(arquivos).sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(file -> new SessaoChatDTO(file, extrairPrimeiraMensagem(file))).toList();
    }

    // --- MÉTODOS PRIVADOS DE APOIO ---

    private String obterContextoPrincipal() {
        AtendimentoContextService.TipoTelaFocada telaAtual = contextoService.getTelaAtualFocada();
        if (telaAtual == AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS && contextoService.getPropostaAtiva() != null) {
            return SummaryGeneratorUtils.gerarJsonPropostaParaIA(contextoService.getPropostaAtiva());
        }
        boolean isAbaCliente = (telaAtual == AtendimentoContextService.TipoTelaFocada.CADASTRO_CLIENTE);
        return contextoService.getLeadAtivo() != null
                ? SummaryGeneratorUtils.gerarJsonContextualParaIA(contextoService.getLeadAtivo(), isAbaCliente)
                : "{}";
    }

    private String obterContextoComissoes() {
        if (contextoService.getTelaAtualFocada() == AtendimentoContextService.TipoTelaFocada.GESTAO_COMISSOES) {
            return SummaryGeneratorUtils.gerarJsonComissoes(contextoService.getComissoesAtivas());
        }
        return "[]";
    }

    private File obterDiretorioChats() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        String caminhoBase = os.contains("win") ? System.getenv("APPDATA") + File.separator + "PoderFinanceiro"
                : os.contains("mac")
                        ? home + File.separator + "Library" + File.separator + "Application Support" + File.separator + "PoderFinanceiro"
                        : home + File.separator + ".local" + File.separator + "share" + File.separator + "PoderFinanceiro";

        File pastaChats = new File(caminhoBase, "chats_gemini");
        if (!pastaChats.exists())
            pastaChats.mkdirs();
        return pastaChats;
    }

    private String extrairPrimeiraMensagem(File arquivo) {
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
            log.warn("{} [SISTEMA] Falha ao extrair preview do arquivo {}: {}", LOG_PREFIX, arquivo.getName(), e.getMessage());
        }
        return formatarNomeArquivo(arquivo.getName());
    }

    private String formatarNomeArquivo(String nome) {
        try {
            String parteData = nome.substring(5, 13);
            return "💬 Chat " + parteData.substring(6, 8) + "/" + parteData.substring(4, 6) + "/" + parteData.substring(0, 4);
        } catch (Exception e) {
            return "💬 " + nome.replace(".json", "");
        }
    }
}
