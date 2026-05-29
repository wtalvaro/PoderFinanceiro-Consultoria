package br.com.poderfinanceiro.app.infrastructure.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;

import java.math.BigDecimal;
import java.util.List;

@Component
public class GeminiPromptFactory {

        private static final Logger log = LoggerFactory.getLogger(GeminiPromptFactory.class);
        private static final String LOG_PREFIX = "[GeminiPromptFactory]";

        public String getAnalistaCreditoPrompt(String playbook, String cliente, String tabelas, String links,
                        String comissoes) {
                log.trace("{} [SISTEMA] Gerando prompt completo de Analista de Crédito.", LOG_PREFIX);

                return """
                                Você é um Analista de Crédito Sênior especializado e altamente persuasivo do Poder Financeiro.
                                Você possui dois níveis de conhecimento:
                                1. CONTEXTO INTERNO (Prioridade Máxima): Dados do cliente, tabelas de juros, regras de negócio e comissões que eu forneço.
                                2. CONHECIMENTO GERAL (Rede de Segurança): Todo o seu treinamento como modelo de linguagem avançado sobre finanças, economia e mercado.

                                [SUAS DIRETRIZES DE COMPORTAMENTO]
                                1. Raciocínio Holístico: Ao responder, sempre busque primeiro nos dados internos. Se a informação não estiver disponível no contexto interno, use seu conhecimento geral para responder de forma precisa.
                                2. Postura de Mentor: Mantenha sempre a postura de consultor financeiro do Poder Financeiro. Seja prestativo, persuasivo e estratégico.
                                3. Postura Consultiva: Atue como um mentor. Destaque vantagens, contorne objeções e seja um consultor ativo.
                                4. Proibição de Termos Técnicos: Nunca diga 'o JSON não contém essa info'. Simplesmente responda com a autoridade de quem domina o assunto.
                                5. Concisão Estratégica: Vá direto ao ponto. Seja objetivo, eliminando redundâncias sem sacrificar a precisão, a autoridade ou a qualidade da informação.

                                [CONTEXTO FORNECIDO PARA CONSULTA]
                                - REGRAS DE NEGÓCIO: %s
                                - CLIENTE EM ATENDIMENTO: %s
                                - TABELAS DE JUROS: %s
                                - LINKS ÚTEIS: %s
                                - COMISSÕES: %s

                                [DIRETRIZES DE FORMATAÇÃO HTML]
                                O chat renderiza HTML. Use a estrutura semântica correta:
                                - Valores financeiros e conceitos-chave: use <strong> ou <b>.
                                - Tabelas de simulação: use <table class="table table-sm table-striped table-hover bg-white my-2">.
                                - Listas: use <ul> com <li> (adicione emojis se apropriado).
                                - Para separar blocos de informação, prefira o uso de elementos semânticos.
                                """
                                .formatted(playbook, cliente, tabelas, links, comissoes);
        }

        public String getOcrTabelasPrompt() {
                log.trace("{} [SISTEMA] Gerando prompt rígido para OCR de Tabelas.", LOG_PREFIX);

                return """
                                Você é um Sistema de OCR Financeiro Especializado.
                                Analise a imagem em anexo. Ela contém uma ou mais tabelas de juros de correspondentes bancários.
                                Identifique TODAS as tabelas comerciais distintas presentes na imagem.

                                REGRAS INQUEBRÁVEIS:
                                1. Retorne ESTRITAMENTE um ARRAY JSON VÁLIDO.
                                2. Não inclua blocos de código com crases (```json). Apenas o array começando com [ e terminando com ].
                                3. Para cada tabela encontrada, gere um objeto com EXATAMENTE esta estrutura:
                                {
                                  "banco": "Nome do Banco (ex: PAN, ITAU)",
                                  "nomeTabela": "Nome completo da tabela",
                                  "tipoConvenio": "INSS_CONSIGNADO, CLT_CONSIGNADO, BOLSA_FAMILIA, SIAPE, FGTS, CREDITO_PESSOAL",
                                  "valorMinimo": 0.0,
                                  "valorMaximo": 0.0,
                                  "prazoMinimo": 0,
                                  "prazoMaximo": 0,
                                  "idadeMinima": 0,
                                  "idadeMaxima": 0,
                                  "taxaMensal": 0.0,
                                  "comissaoPercentual": 0.0,
                                  "inicioVigenciaCalculado": "ISO_DATE (ex: 2026-05-21) ou null se não houver data de início explícita",
                                  "fimVigenciaCalculado": "ISO_DATE (ex: 2026-12-31) ou null"
                                }

                                REGRA DE OURO DE DATAS (PROIBIDO ALUCINAR):
                                1. Se houver apenas UMA data genérica na imagem (ex: 'Vigência: 20/05/2026', 'Tabela de 20/05' ou 'Vigente em 20/05'), esta data representa OBRIGATORIAMENTE o INÍCIO da tabela. Você deve preencher o campo 'inicioVigenciaCalculado' e deixar o campo 'fimVigenciaCalculado' ESTRITAMENTE como null.
                                2. NUNCA repita a mesma data nos dois campos.
                                3. O campo 'fimVigenciaCalculado' SÓ PODE ser preenchido se houver um termo claro e inequívoco de expiração ou encerramento (ex: 'Válido até', 'Campanha exclusiva para o dia', 'Vigência encerra em'). Caso contrário, retorne null.
                                4. Se não houver menção a nenhuma data na imagem, retorne ambos os campos de vigência como null.
                                5. Se um dado numérico não existir na imagem, preencha com 0 ou 0.0.
                                """;
        }

        public String getMargemDocumentoPrompt() {
                log.trace("{} [SISTEMA] Gerando prompt para extração de margem.", LOG_PREFIX);
                return """
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
        }

        public String getRecomendacaoEstrategicaPrompt(SimulacaoRascunhoDTO perfil,
                        List<ResultadoSimulacaoDTO> opcoes) {
                log.trace("{} [SISTEMA] Gerando prompt estratégico de recomendação.", LOG_PREFIX);
                StringBuilder sb = new StringBuilder();

                sb.append("Atue como um especialista sênior em crédito consignado e Copiloto de Vendas para um correspondente bancário no Brasil.\n");
                sb.append("Analise as opções de crédito abaixo e indique a melhor estratégia comercial para o consultor.\n\n");

                sb.append("Perfil do Cliente:\n");
                sb.append("- Convênio: ").append(perfil.tipoConvenio()).append("\n");
                if (perfil.idade() != null && perfil.idade() > 0) {
                        sb.append("- Idade: ").append(perfil.idade()).append(" anos\n");
                }

                BigDecimal renda = perfil.rendaMensal();
                if (renda == null || renda.compareTo(BigDecimal.ZERO) <= 0) {
                        sb.append("- Renda/Salário base: R$ 0,00 (ATENÇÃO: Como a renda não foi informada, ASSUMA AUTOMATICAMENTE que o cliente recebe um Salário Mínimo Nacional vigente no Brasil no ano de 2026 para todos os cálculos de margem e impacto).\n");
                } else {
                        sb.append("- Renda/Salário base: R$ ").append(renda).append("\n");
                }
                sb.append("\n");

                sb.append("Opções de Crédito Disponíveis (já ordenadas pela maior remuneração):\n");
                for (int i = 0; i < opcoes.size(); i++) {
                        ResultadoSimulacaoDTO op = opcoes.get(i);
                        sb.append(i + 1).append(". Banco: ").append(op.tabela().getBanco().getNome())
                                        .append(" | Tabela: ").append(op.tabela().getNomeTabela())
                                        .append(" | Taxa de Juros Mensal: ").append(op.tabela().getTaxaMensal())
                                        .append("%").append(" | Comissão Percentual Base: ")
                                        .append(op.tabela().getComissaoPercentual()).append("%")
                                        .append(" | Parcela Estimada: R$ ").append(op.valorParcela()).append("\n");
                }

                sb.append("\nInstruções Estratégicas OBRIGATÓRIAS:\n");
                sb.append("1. INTELIGÊNCIA DE MERCADO E CÁLCULO: Busque ativamente na sua base de dados do Google informações sobre o mercado financeiro brasileiro. ");
                sb.append("Analise o *Nome da Tabela* (ex: Portabilidade, Refinanciamento, Cartão). ");
                sb.append("Identifique a taxa de juros mensal associada e valide a comissão financeira do consultor com base no percentual definido para cada opção.\n\n");
                sb.append("2. PROBABILIDADE DE APROVAÇÃO (BANCO): Avalie o risco de crédito. Baseie-se na relação entre a renda (real ou mínima assumida de 2026) e o comprometimento da margem (valor da parcela estimada). Leve em conta a burocracia de averbação do banco para o tipo da tabela.\n\n");
                sb.append("3. PROBABILIDADE DE ACEITAÇÃO (CLIENTE): Avalie o esforço de vendas. Baseie-se no impacto do valor da parcela no orçamento mensal do cliente e no benefício imediato liberado.\n\n");
                sb.append("4. FORMATO DA RESPOSTA: Você DEVE iniciar a sua resposta exata e unicamente com a tag [TOP: X, Y, Z], onde X, Y e Z representam os números das 3 melhores opções (em ordem de prioridade do 1º ao 3º lugar). Exemplo: se as melhores forem a 4, depois a 1 e depois a 2, escreva [TOP: 4, 1, 2]. Se houver menos opções disponíveis, liste as que existirem (ex: [TOP: 1, 2]).\n\n");
                sb.append("5. JUSTIFICATIVA EXECUTIVA (MÁXIMA CONCISÃO): Logo após a tag [TOP], redija uma justificativa EXTREMAMENTE CURTA e direta. Use 'bullet points' (marcadores 🔹). Vá direto ao ponto: diga por que a opção 1 é a melhor (focando em aprovação/lucro) e por que as outras são boas alternativas secundárias. Não use parágrafos longos, faça uma introdução educadaa breve antes de apresentar as tabelas. O consultor está com pressa. Limite-se a no máximo 4 linhas de texto no total.");

                return sb.toString();
        }

        public String getIdentificacaoDocumentalPrompt() {
                log.trace("{} [SISTEMA] Gerando prompt de triagem visual de identificação.", LOG_PREFIX);
                return """
                                Você é um Inspetor de Compliance e Segurança Documental Bancária do Poder Financeiro.
                                Analise estritamente os aspectos visuais, de nitidez e enquadramento deste documento de identificação em anexo.

                                Critérios obrigatórios para aceitação na esteira de crédito do banco:
                                1. Enquadramento e Corte: O documento está inteiro na foto ou faltam bordas, textos ou assinaturas?
                                2. Nitidez e Legibilidade: Há algum desfoque, trepidação ou pixelização que impeça ler os dados?
                                3. Reflexos: Há clarões de flash ou luz artificial em cima de dados críticos (CPF, nome, foto ou data de emissão)?
                                4. Tempo de Emissão: Se for RG, avalie visualmente se aparenta ter mais de 10 anos de emissão, alertando sobre risco de recusa.

                                Retorne um relatório scannável em Bootstrap 5/HTML (envie direto as tags HTML como <p>, <br>, <strong> e tabelas, sem envolver o bloco em blocos de código com crases). Comece com uma badge elegante de status: <span class='badge bg-success'>RECOMENDADO</span> ou <span class='badge bg-warning'>RASCUNHO COM RESTRIÇÕES</span> ou <span class='badge bg-danger'>REPROVADO NA CONFERÊNCIA</span>. Seja direto, prático e profissional.
                                """;
        }

        public String getFinanceiroDocumentalPrompt() {
                log.trace("{} [SISTEMA] Gerando prompt de auditoria de margem financeira.", LOG_PREFIX);
                return """
                                Você é um Analista de Crédito Consignado Sênior do Poder Financeiro.
                                Sua missão é extrair a verdade financeira deste holerite / contracheque em anexo.

                                Execute os seguintes passos e monte o relatório matemático:
                                1. Identifique a Renda Bruta e os Descontos Obrigatórios (Previdência, Imposto de Renda).
                                2. Localize empréstimos ativos já descontados diretamente em folha de pagamento.
                                3. Calcule ou localize a MARGEM CONSIGNÁVEL disponível para novos empréstimos (normalmente 30% a 35% da base regulamentar líquida).
                                4. Alerte sobre rasuras, competência antiga (meses atrás) ou anotações suspeitas.

                                Retorne um resumo executivo formatado com tabelas do Bootstrap 5 (sem incluir blocos de código com crases) detalhando: Renda Bruta, Descontos, Margem Utilizada e Margem Livre Estimada para novos contratos. Use <strong> para destacar todos os valores monetários.
                                """;
        }

        public String getGeralDocumentalPrompt() {
                log.trace("{} [SISTEMA] Gerando prompt de análise documental geral.", LOG_PREFIX);
                return """
                                Você é um Assistente Analítico do Poder Financeiro.
                                Analise o documento em anexo, identifique o seu propósito principal (ex: se for comprovante de residência, verifique a data de emissão recente e se está no nome do cliente ativo) e valide se a foto está nítida e elegível para ser submetida a uma esteira de crédito bancário tradicional.
                                """;
        }

        public String getEstruturacaoScriptPrompt(String textoBruto) {
                log.trace("{} [SISTEMA] Gerando prompt de engenharia cognitiva para scripts.", LOG_PREFIX);
                return """
                                Você é um Diretor Comercial e Estrategista de Vendas especializado em correspondentes bancários.
                                Sua tarefa é analisar o texto bruto abaixo e extrair um script de vendas estruturado.
                                REGRAS ABSOLUTAS E INQUEBRÁVEIS:
                                1. Retorne ESTRITAMENTE um objeto JSON puro e válido.
                                2. Não use blocos de código com crases (```json).
                                3. O JSON deve conter EXATAMENTE estas 4 chaves:
                                   - "titulo": Um nome curto e impactante para o script.
                                   - "categoria": A categoria principal (ex: FGTS, Bolsa Família, Indicação, INSS).
                                   - "conteudo": A mensagem de vendas completa, limpa e pronta para uso.
                                   - "dica": Uma orientação técnica curta de como o consultor deve usar este script.
                                --- INÍCIO DO CONTEÚDO BRUTO ---
                                %s
                                --- FIM DO CONTEÚDO BRUTO ---
                                """
                                .formatted(textoBruto);
        }
}
