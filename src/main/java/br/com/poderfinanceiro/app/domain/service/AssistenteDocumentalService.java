package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.infrastructure.factory.GeminiPromptFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Serviço de Assistência Documental via IA. Orquestra a triagem visual e
 * financeira de documentos anexados à esteira de crédito.
 */
@Service
public class AssistenteDocumentalService {

    private static final Logger log = LoggerFactory.getLogger(AssistenteDocumentalService.class);
    private static final String LOG_PREFIX = "[AssistenteDocumentalService]";

    public record ConfigIA(String icone, String titulo, String prompt) {
    }

    private final GeminiService geminiService;
    private final AuthService authService;
    private final GeminiPromptFactory promptFactory;

    public AssistenteDocumentalService(GeminiService geminiService, AuthService authService,
            GeminiPromptFactory promptFactory) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.promptFactory = promptFactory;
        log.info("{} [SISTEMA] Serviço de assistência documental inicializado.", LOG_PREFIX);
    }

    /**
     * Recupera a lista de modelos multimodais disponíveis para análise.
     */
    public List<String> listarModelosDisponiveis() {
        log.trace("{} [TELEMETRIA] Solicitando modelos multimodais para análise documental.", LOG_PREFIX);
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        return geminiService.listarModelosMultimodais(token);
    }

    /**
     * Executa a análise cognitiva do documento. Nota: Este método é síncrono e
     * deve ser orquestrado via AsyncUtils no Controller/Facade.
     */
    public String analisarDocumento(DocumentoProponenteModel doc, ProponenteModel proponente,
            String modeloSelecionado) {
        log.info("{} [TELEMETRIA] Iniciando análise cognitiva. Doc ID: {}, Tipo: {}", LOG_PREFIX, doc.getId(),
                doc.getTipoDocumento());

        File arquivoFisico = new File(doc.getArquivoPath());
        if (!arquivoFisico.exists()) {
            log.error("{} [SISTEMA] Falha na análise: Arquivo físico não localizado em {}", LOG_PREFIX,
                    doc.getArquivoPath());
            throw new IllegalArgumentException("Arquivo físico não encontrado no servidor.");
        }

        // 1. Determinar Configuração e Prompt via Factory
        ConfigIA config = determinarConfiguracaoIA(doc.getTipoDocumento());

        // 2. Preparar Contexto
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        String jsonCliente = SummaryGeneratorUtils.gerarJsonContextualParaIA(proponente, true);

        log.debug("{} [NEGOCIO] Aplicando estratégia: {}", LOG_PREFIX, config.titulo());

        // 3. Chamar Motor de IA
        String resposta = geminiService.perguntarAoAssistente(config.prompt(), token, modeloSelecionado, arquivoFisico,
                jsonCliente, "[]", "[]", "[]", List.of());

        log.info("{} [AUDITORIA] Análise documental concluída com sucesso para o documento ID: {}", LOG_PREFIX,
                doc.getId());
        return resposta;
    }

    /**
     * Define a estratégia de análise (Prompt e Ícone) baseada no tipo do
     * documento.
     */
    public ConfigIA determinarConfiguracaoIA(String tipo) {
        String upper = tipo != null ? tipo.toUpperCase() : "OUTROS";

        if (upper.contains("RG") || upper.contains("CPF") || upper.contains("CNH")) {
            log.trace("{} [NEGOCIO] Identificado documento de identificação.", LOG_PREFIX);
            return new ConfigIA("🔍", "Triagem Visual de Identificação",
                    promptFactory.getIdentificacaoDocumentalPrompt());
        }

        if (upper.contains("CONTRACHEQUE") || upper.contains("EXTRATO BANCARIO") || upper.contains("HOLERITE")
                || upper.contains("HISCON")) {
            log.trace("{} [NEGOCIO] Identificado documento financeiro/margem.", LOG_PREFIX);
            return new ConfigIA("📊", "Auditoria de Margem Consignável", promptFactory.getFinanceiroDocumentalPrompt());
        }

        log.trace("{} [NEGOCIO] Aplicando análise documental geral.", LOG_PREFIX);
        return new ConfigIA("🤖", "Análise Documental Geral", promptFactory.getGeralDocumentalPrompt());
    }
}
