package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class AssistenteDocumentalService {

    private static final Logger log = LoggerFactory.getLogger(AssistenteDocumentalService.class);

    private static final String PROMPT_IDENTIFICACAO = """
            Você é um Inspetor de Compliance e Segurança Documental Bancária do Poder Financeiro.
            Analise estritamente os aspectos visuais, de nitidez e enquadramento deste documento de identificação em anexo.

            Critérios obrigatórios para aceitação na esteira de crédito do banco:
            1. Enquadramento e Corte: O documento está inteiro na foto ou faltam bordas, textos ou assinaturas?
            2. Nitidez e Legibilidade: Há algum desfoque, trepidação ou pixelização que impeça ler os dados?
            3. Reflexos: Há clarões de flash ou luz artificial em cima de dados críticos (CPF, nome, foto ou data de emissão)?
            4. Tempo de Emissão: Se for RG, avalie visualmente se aparenta ter mais de 10 anos de emissão, alertando sobre risco de recusa.

            Retorne um relatório scannável em Bootstrap 5/HTML (envie direto as tags HTML como <p>, <br>, <strong> e tabelas, sem envolver o bloco em blocos de código com crases). Comece com uma badge elegante de status: <span class='badge bg-success'>RECOMENDADO</span> ou <span class='badge bg-warning'>RASCUNHO COM RESTRIÇÕES</span> ou <span class='badge bg-danger'>REPROVADO NA CONFERÊNCIA</span>. Seja direto, prático e profissional.
            """;

    private static final String PROMPT_FINANCEIRO = """
            Você é um Analista de Crédito Consignado Sênior do Poder Financeiro.
            Sua missão é extrair a verdade financeira deste holerite / contracheque em anexo.

            Execute os seguintes passos e monte o relatório matemático:
            1. Identifique a Renda Bruta e os Descontos Obrigatórios (Previdência, Imposto de Renda).
            2. Localize empréstimos ativos já descontados diretamente em folha de pagamento.
            3. Calcule ou localize a MARGEM CONSIGNÁVEL disponível para novos empréstimos (normalmente 30% a 35% da base regulamentar líquida).
            4. Alerte sobre rasuras, competência antiga (meses atrás) ou anotações suspeitas.

            Retorne um resumo executivo formatado com tabelas do Bootstrap 5 (sem incluir blocos de código com crases) detalhando: Renda Bruta, Descontos, Margem Utilizada e Margem Livre Estimada para novos contratos. Use <strong> para destacar todos os valores monetários.
            """;

    private static final String PROMPT_GERAL = """
            Você é um Assistente Analítico do Poder Financeiro.
            Analise o documento em anexo, identifique o seu propósito principal (ex: se for comprovante de residência, verifique a data de emissão recente e se está no nome do cliente ativo) e valide se a foto está nítida e elegível para ser submetida a uma esteira de crédito bancário tradicional.
            """;

    public record ConfigIA(String icone, String titulo, String prompt) {
    }

    private final GeminiService geminiService;
    private final AuthService authService;

    public AssistenteDocumentalService(GeminiService geminiService, AuthService authService) {
        this.geminiService = geminiService;
        this.authService = authService;
    }

    public List<String> listarModelosDisponiveis() {
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        return geminiService.listarModelosMultimodais(token);
    }

    /**
     * Executa a análise de forma síncrona. O Controller deve envelopar isso em uma
     * Task assíncrona.
     */
    public String analisarDocumento(DocumentoProponenteModel doc, ProponenteModel proponente,
            String modeloSelecionado) {
        log.info("[ASSISTENTE_IA] Iniciando análise do documento ID={}, tipo={}", doc.getId(), doc.getTipoDocumento());

        File arquivoFisico = new File(doc.getArquivoPath());
        if (!arquivoFisico.exists()) {
            throw new IllegalArgumentException("Arquivo físico não encontrado no servidor.");
        }

        ConfigIA config = determinarConfiguracaoIA(doc.getTipoDocumento());
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        String jsonCliente = SummaryGeneratorUtils.gerarJsonContextualParaIA(proponente, true);

        return geminiService.perguntarAoAssistente(
                config.prompt(), token, modeloSelecionado, arquivoFisico, jsonCliente, "[]", "[]", "[]", List.of());
    }

    public ConfigIA determinarConfiguracaoIA(String tipo) {
        String upper = tipo != null ? tipo.toUpperCase() : "OUTROS";
        if (upper.contains("RG") || upper.contains("CPF") || upper.contains("CNH")) {
            return new ConfigIA("🔍", "Triagem Visual de Identificação", PROMPT_IDENTIFICACAO);
        } else if (upper.contains("CONTRACHEQUE") || upper.contains("EXTRATO BANCARIO") || upper.contains("HOLERITE")
                || upper.contains("HISCON")) {
            return new ConfigIA("📊", "Auditoria de Margem Consignável", PROMPT_FINANCEIRO);
        }
        return new ConfigIA("🤖", "Análise Documental Geral", PROMPT_GERAL);
    }
}
