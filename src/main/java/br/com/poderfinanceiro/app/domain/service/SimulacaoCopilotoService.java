package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.infrastructure.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

@Service
public class SimulacaoCopilotoService {

    private static final Logger log = LoggerFactory.getLogger(SimulacaoCopilotoService.class);

    private final TabelaJurosRepository tabelaJurosRepository;
    private final GeminiService geminiService;
    private final AuthService authService;

    public SimulacaoCopilotoService(TabelaJurosRepository tabelaJurosRepository,
            GeminiService geminiService,
            AuthService authService) {
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.geminiService = geminiService;
        this.authService = authService;
        log.debug("[SIMULACAO_COPILOTO] Construtor: Serviço instanciado");
    }

    @Transactional(readOnly = true)
    public List<ResultadoSimulacaoDTO> processarSimulacaoRapida(SimulacaoRascunhoDTO rascunho) {
        log.debug("[SIMULACAO_COPILOTO] processarSimulacaoRapida: Iniciando simulação para rascunho={}", rascunho);
        if (rascunho == null) {
            log.warn("[SIMULACAO_COPILOTO] Rascunho nulo, retornando lista vazia");
            return List.of();
        }

        TipoConvenioModel convenioEnum;
        try {
            convenioEnum = TipoConvenioModel.valueOf(rascunho.tipoConvenio());
            log.trace("[SIMULACAO_COPILOTO] Convênio identificado: {}", convenioEnum);
        } catch (IllegalArgumentException e) {
            log.warn("[SIMULACAO_COPILOTO] Tipo de convênio inválido: '{}', retornando lista vazia",
                    rascunho.tipoConvenio());
            return List.of();
        }

        List<TabelaJurosModel> tabelasValidas = tabelaJurosRepository.findTabelasElegiveis(
                convenioEnum,
                rascunho.idade(),
                rascunho.valorDesejado(),
                rascunho.prazoDesejado());
        log.debug("[SIMULACAO_COPILOTO] {} tabelas elegíveis encontradas", tabelasValidas.size());

        List<ResultadoSimulacaoDTO> resultados = tabelasValidas.stream().map(tabela -> {
            BigDecimal parcela = calcularParcelaEstimada(rascunho.valorDesejado(), tabela.getTaxaMensal(),
                    rascunho.prazoDesejado());
            BigDecimal comissao = calcularComissao(rascunho.valorDesejado(), tabela.getComissaoPercentual());
            log.trace("[SIMULACAO_COPILOTO] Tabela ID={}, parcela={}, comissão={}", tabela.getId(), parcela, comissao);
            return new ResultadoSimulacaoDTO(tabela, comissao, parcela);
        })
                .sorted(Comparator.comparing(ResultadoSimulacaoDTO::comissaoEstimada).reversed())
                .collect(Collectors.toList());

        log.info("[SIMULACAO_COPILOTO] Simulação finalizada com {} resultados ordenados por comissão",
                resultados.size());
        return resultados;
    }

    public String extrairMargemDocumento(File arquivo) {
        log.debug("[SIMULACAO_COPILOTO] extrairMargemDocumento: Iniciando extração para arquivo '{}'",
                arquivo != null ? arquivo.getName() : "null");
        if (arquivo == null || !arquivo.exists()) {
            log.error("[SIMULACAO_COPILOTO] Arquivo inválido ou inexistente");
            return "Erro: Arquivo não fornecido ou não encontrado.";
        }

        String prompt = """
                Você é um Analista de Crédito Consignado Sênior. Sua missão principal é extrair a Margem Consignável Livre (Disponível) do documento financeiro em anexo.

                Você tem total liberdade analítica para:
                - Identificar a natureza do documento (Holerite CLT, Extrato INSS, HISCON, etc.) e aplicar a legislação de margem correspondente (ex: 35% CLT pós-descontos, 35% INSS líquido).
                - Auditar o documento em busca de descontos ativos de empréstimos já vigentes e abatê-los da margem base.
                - Cruzar proventos e descontos para chegar ao valor real disponível.

                Como responder:
                Pense passo a passo. Descreva brevemente seu raciocínio lógico (o que achou, base de cálculo e deduções).
                Termine a sua análise obrigatoriamente cravando o valor financeiro exato neste formato final:
                RESULTADO FINAL: [valor numérico com vírgula para centavos]
                """;

        String apiKey = authService.getUsuarioLogado().getGeminiApiKey();
        log.info("[SIMULACAO_COPILOTO] Solicitando análise ao GeminiService para arquivo '{}'", arquivo.getName());
        String resposta = geminiService.perguntarAoAssistente(prompt, apiKey, "gemini-3.5-flash", arquivo, "{}", "[]",
                "[]", "[]",
                List.of());
        log.debug("[SIMULACAO_COPILOTO] Resposta recebida da IA (tamanho={})",
                resposta != null ? resposta.length() : 0);
        return resposta;
    }

    private BigDecimal calcularComissao(BigDecimal valorOperacao, BigDecimal percentualComissao) {
        if (valorOperacao == null || percentualComissao == null) {
            log.trace("[SIMULACAO_COPILOTO] calcularComissao: Parâmetros inválidos, retornando ZERO");
            return BigDecimal.ZERO;
        }
        BigDecimal comissao = valorOperacao.multiply(percentualComissao).divide(new BigDecimal("100"), 2,
                RoundingMode.HALF_UP);
        log.trace("[SIMULACAO_COPILOTO] Comissão calculada: {} (base={}, percentual={})", comissao, valorOperacao,
                percentualComissao);
        return comissao;
    }

    private BigDecimal calcularParcelaEstimada(BigDecimal valor, BigDecimal taxa, Integer prazo) {
        if (valor == null || taxa == null || prazo == null || prazo == 0) {
            log.trace("[SIMULACAO_COPILOTO] calcularParcelaEstimada: Parâmetros inválidos, retornando ZERO");
            return BigDecimal.ZERO;
        }
        BigDecimal taxaDecimal = taxa.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal parcela = valor.multiply(taxaDecimal).setScale(2, RoundingMode.HALF_UP);
        log.trace("[SIMULACAO_COPILOTO] Parcela estimada: {} (valor={}, taxa={}, prazo={})", parcela, valor, taxa,
                prazo);
        return parcela;
    }

    public String gerarRecomendacaoInteligenteIA(SimulacaoRascunhoDTO perfil, List<ResultadoSimulacaoDTO> ranking,
            String modeloEscolhido) {
        log.info("[SIMULACAO_COPILOTO] gerarRecomendacaoInteligenteIA: modelo='{}', ranking size={}", modeloEscolhido,
                ranking != null ? ranking.size() : 0);
        if (ranking == null || ranking.isEmpty()) {
            log.warn("[SIMULACAO_COPILOTO] Ranking vazio, retornando mensagem padrão");
            return "Nenhuma tabela encontrada para este perfil.";
        }

        String prompt = montarPromptEstrategico(perfil, ranking);
        String apiKey = authService.getUsuarioLogado().getGeminiApiKey();
        log.debug("[SIMULACAO_COPILOTO] Prompt montado (tamanho={}), chamando GeminiService", prompt.length());
        String resposta = geminiService.perguntarTexto(prompt, apiKey, modeloEscolhido);
        log.info("[SIMULACAO_COPILOTO] Recomendação gerada com sucesso (tamanho={})",
                resposta != null ? resposta.length() : 0);
        return resposta;
    }

    private String montarPromptEstrategico(SimulacaoRascunhoDTO perfil, List<ResultadoSimulacaoDTO> opcoes) {
        log.debug("[SIMULACAO_COPILOTO] montarPromptEstrategico: Construindo prompt estratégico com {} opções",
                opcoes.size());
        StringBuilder sb = new StringBuilder();

        sb.append(
                "Atue como um especialista sênior em crédito consignado e Copiloto de Vendas para um correspondente bancário no Brasil.\n");
        sb.append("Analise as opções de crédito abaixo e indique a melhor estratégia comercial para o consultor.\n\n");

        sb.append("Perfil do Cliente:\n");
        sb.append("- Convênio: ").append(perfil.tipoConvenio()).append("\n");
        if (perfil.idade() != null && perfil.idade() > 0) {
            sb.append("- Idade: ").append(perfil.idade()).append(" anos\n");
        }

        BigDecimal renda = perfil.rendaMensal();
        if (renda == null || renda.compareTo(BigDecimal.ZERO) <= 0) {
            sb.append(
                    "- Renda/Salário base: R$ 0,00 (ATENÇÃO: Como a renda não foi informada, ASSUMA AUTOMATICAMENTE que o cliente recebe um Salário Mínimo Nacional vigente no Brasil no ano de 2026 para todos os cálculos de margem e impacto).\n");
        } else {
            sb.append("- Renda/Salário base: R$ ").append(renda).append("\n");
        }
        sb.append("\n");

        sb.append("Opções de Crédito Disponíveis (já ordenadas pela maior remuneração):\n");
        for (int i = 0; i < opcoes.size(); i++) {
            ResultadoSimulacaoDTO op = opcoes.get(i);
            sb.append(i + 1).append(". Banco: ").append(op.tabela().getBanco().getNome())
                    .append(" | Tabela: ").append(op.tabela().getNomeTabela())
                    .append(" | Taxa de Juros Mensal: ").append(op.tabela().getTaxaMensal()).append("%")
                    .append(" | Comissão Percentual Base: ").append(op.tabela().getComissaoPercentual()).append("%")
                    .append(" | Parcela Estimada: R$ ").append(op.valorParcela())
                    .append("\n");
        }

        sb.append("\nInstruções Estratégicas OBRIGATÓRIAS:\n");
        sb.append(
                "1. INTELIGÊNCIA DE MERCADO E CÁLCULO: Busque ativamente na sua base de dados do Google informações sobre o mercado financeiro brasileiro. ");
        sb.append("Analise o *Nome da Tabela* (ex: Portabilidade, Refinanciamento, Cartão). ");
        sb.append(
                "Identifique a taxa de juros mensal associada e valide a comissão financeira do consultor com base no percentual definido para cada opção.\n\n");
        sb.append(
                "2. PROBABILIDADE DE APROVAÇÃO (BANCO): Avalie o risco de crédito. Baseie-se na relação entre a renda (real ou mínima assumida de 2026) e o comprometimento da margem (valor da parcela estimada). Leve em conta a burocracia de averbação do banco para o tipo da tabela.\n\n");
        sb.append(
                "3. PROBABILIDADE DE ACEITAÇÃO (CLIENTE): Avalie o esforço de vendas. Baseie-se no impacto do valor da parcela no orçamento mensal do cliente e no benefício imediato liberado.\n\n");
        sb.append(
                "4. FORMATO DA RESPOSTA: Você DEVE iniciar a sua resposta exata e unicamente com a tag [TOP: X, Y, Z], onde X, Y e Z representam os números das 3 melhores opções (em ordem de prioridade do 1º ao 3º lugar). Exemplo: se as melhores forem a 4, depois a 1 e depois a 2, escreva [TOP: 4, 1, 2]. Se houver menos opções disponíveis, liste as que existirem (ex: [TOP: 1, 2]).\n\n");
        sb.append(
                "5. JUSTIFICATIVA EXECUTIVA (MÁXIMA CONCISÃO): Logo após a tag [TOP], redija uma justificativa EXTREMAMENTE CURTA e direta. Use 'bullet points' (marcadores 🔹). Vá direto ao ponto: diga por que a opção 1 é a melhor (focando em aprovação/lucro) e por que as outras são boas alternativas secundárias. Não use parágrafos longos, faça uma introdução educadaa breve antes de apresentar as tabelas. O consultor está com pressa. Limite-se a no máximo 4 linhas de texto no total.");
        String prompt = sb.toString();
        log.trace("[SIMULACAO_COPILOTO] Prompt final (primeiros 200 caracteres): {}",
                prompt.substring(0, Math.min(200, prompt.length())));
        return prompt;
    }
}