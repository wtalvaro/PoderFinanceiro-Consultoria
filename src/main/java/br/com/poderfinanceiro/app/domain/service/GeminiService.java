package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.dto.GeminiRequest;
import br.com.poderfinanceiro.app.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestClient restClient;

    @Value("${gemini.api.url}")
    private String baseUrl;

    public GeminiService() {
        this.restClient = RestClient.builder().build();
        log.debug("[GEMINI_SERVICE] Construtor: Serviço inicializado com URL base: {}", baseUrl);
    }

    // 🚀 SOBRECARGA DE COMPATIBILIDADE: Para PropostaController (sem histórico)
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, File arquivoAnexo,
            String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis, String jsonComissoes) {
        log.debug(
                "[GEMINI_SERVICE] perguntarAoAssistente (overload): chamando assinatura principal com modelo default");
        return perguntarAoAssistente(perguntaUsuario, apiKeyDoConsultor, "gemini-2.5-flash", arquivoAnexo,
                jsonClienteAtivo, jsonTabelasJuros, jsonLinksUteis, jsonComissoes, List.of());
    }

    // 🚀 ASSINATURA PRINCIPAL: Com Modelo Dinâmico, Memória e Exponential Backoff
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, String modeloEscolhido,
            File arquivoAnexo, String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis,
            String jsonComissoes, List<GeminiRequest.Content> historico) {

        log.info("[GEMINI_SERVICE] perguntarAoAssistente: modelo='{}', anexo={}, pergunta='{}'",
                modeloEscolhido, arquivoAnexo != null ? arquivoAnexo.getName() : "null", perguntaUsuario);

        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
            log.warn("[GEMINI_SERVICE] perguntarAoAssistente: Chave de API ausente ou vazia");
            return "⚠️ Acesso Negado: A sua chave de API do Gemini não está configurada.";
        }

        try {
            String playbookJson = carregarPlaybookLocal();
            String clienteSeguro = (jsonClienteAtivo == null || jsonClienteAtivo.isBlank()) ? "{}" : jsonClienteAtivo;
            String tabelasSeguro = (jsonTabelasJuros == null || jsonTabelasJuros.isBlank()) ? "[]" : jsonTabelasJuros;
            String linksSeguro = (jsonLinksUteis == null || jsonLinksUteis.isBlank()) ? "[]" : jsonLinksUteis;
            String comissoesSeguro = (jsonComissoes == null || jsonComissoes.isBlank()) ? "[]" : jsonComissoes;

            String instrucaoSistema = """
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
                    .formatted(playbookJson, clienteSeguro, tabelasSeguro, linksSeguro, comissoesSeguro);

            GeminiRequest.Content systemContent = new GeminiRequest.Content(
                    List.of(GeminiRequest.Part.ofText(instrucaoSistema)));

            List<GeminiRequest.Part> partsUsuario = new ArrayList<>();
            partsUsuario.add(GeminiRequest.Part.ofText(perguntaUsuario));

            if (arquivoAnexo != null && arquivoAnexo.exists()) {
                byte[] fileBytes = Files.readAllBytes(arquivoAnexo.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                String fileName = arquivoAnexo.getName().toLowerCase();
                String mimeType = fileName.endsWith(".pdf") ? "application/pdf"
                        : fileName.endsWith(".png") ? "image/png"
                                : (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) ? "image/jpeg"
                                        : "application/octet-stream";
                partsUsuario.add(GeminiRequest.Part.ofFile(mimeType, base64Data));
                log.trace("[GEMINI_SERVICE] Arquivo anexado: {} (mime={}, size={})", fileName, mimeType,
                        fileBytes.length);
            }

            List<GeminiRequest.Content> contents = new ArrayList<>(historico);
            contents.add(new GeminiRequest.Content("user", partsUsuario));

            GeminiRequest payload = new GeminiRequest(systemContent, contents);
            String urlCompleta = baseUrl + "/" + modeloEscolhido + ":generateContent?key=" + apiKeyDoConsultor;
            log.debug("[GEMINI_SERVICE] URL de requisição montada (chave omitida) para modelo {}", modeloEscolhido);

            int maxTentativas = 6;
            int tempoEspera = 2000;

            for (int tentativaAtual = 1; tentativaAtual <= maxTentativas; tentativaAtual++) {
                try {
                    log.trace("[GEMINI_SERVICE] Tentativa {}/{} de chamada à IA", tentativaAtual, maxTentativas);
                    GeminiResponse response = restClient.post()
                            .uri(urlCompleta)
                            .body(payload)
                            .retrieve()
                            .body(GeminiResponse.class);

                    if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                        String texto = response.candidates().get(0).content().parts().get(0).text();
                        log.info("[GEMINI_SERVICE] Resposta recebida com sucesso (tamanho={})",
                                texto != null ? texto.length() : 0);
                        return texto;
                    }
                    log.warn("[GEMINI_SERVICE] Resposta vazia ou sem candidatos");
                    return "🤖 Sem resposta dos servidores cognitivos.";

                } catch (RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    log.warn("[GEMINI_SERVICE] Erro HTTP {} na tentativa {}/{}", status, tentativaAtual, maxTentativas);
                    if (status == 429 || status >= 500) {
                        if (tentativaAtual == maxTentativas) {
                            log.error("[GEMINI_SERVICE] Esgotadas {} tentativas para erro {}.", maxTentativas, status);
                            return "⚠️ Os servidores da Google estão sobrecarregados no momento (Erro " + status
                                    + "). Tentamos " + maxTentativas
                                    + " vezes. Por favor, aguarde um minuto ou selecione outro modelo na engrenagem de configuração.";
                        }
                        log.info("⚠️ Erro " + status + " na IA. Aguardando " + (tempoEspera / 1000)
                                + "s para nova tentativa (" + tentativaAtual + "/" + maxTentativas + ")...");
                        Thread.sleep(tempoEspera);
                        tempoEspera *= 2;
                    } else {
                        log.error("[GEMINI_SERVICE] Erro não recuperável: status={}", status);
                        return "❌ Erro na consulta (Código " + status
                                + "). Verifique sua chave de API ou o anexo enviado.";
                    }
                }
            }

        } catch (InterruptedException ie) {
            log.warn("[GEMINI_SERVICE] Thread interrompida durante backoff");
            Thread.currentThread().interrupt();
            return "⚠️ Consulta cancelada.";
        } catch (Exception e) {
            log.error("[GEMINI_SERVICE] Erro inesperado: {}", e.getMessage(), e);
            return "⚠️ Falha crítica ao montar a requisição: " + e.getMessage();
        }

        log.warn("[GEMINI_SERVICE] Falha de comunicação após todas as tentativas");
        return "🤖 Houve um problema de comunicação com os servidores.";
    }

    // 🚀 MÉTODO ATUALIZADO: Recebe o modelo dinamicamente e inclui motor de
    // resiliência
    public String perguntarTexto(String prompt, String apiKey, String modeloEscolhido) {
        log.debug("[GEMINI_SERVICE] perguntarTexto: modelo='{}', prompt length={}", modeloEscolhido,
                prompt != null ? prompt.length() : 0);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GEMINI_SERVICE] perguntarTexto: API key ausente");
            return "⚠️ API Key não configurada.";
        }

        try {
            List<GeminiRequest.Part> parts = List.of(GeminiRequest.Part.ofText(prompt));
            GeminiRequest payload = new GeminiRequest(List.of(new GeminiRequest.Content(parts)));

            String urlCompleta = baseUrl + "/" + modeloEscolhido + ":generateContent?key=" + apiKey;
            log.trace("[GEMINI_SERVICE] URL montada para modelo {}", modeloEscolhido);

            int maxTentativas = 6;
            int tempoEspera = 2000;

            for (int tentativaAtual = 1; tentativaAtual <= maxTentativas; tentativaAtual++) {
                try {
                    log.trace("[GEMINI_SERVICE] Tentativa {}/{}", tentativaAtual, maxTentativas);
                    GeminiResponse response = restClient.post()
                            .uri(urlCompleta)
                            .body(payload)
                            .retrieve()
                            .body(GeminiResponse.class);

                    if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                        String texto = response.candidates().get(0).content().parts().get(0).text();
                        log.info("[GEMINI_SERVICE] Texto gerado com sucesso (tamanho={})",
                                texto != null ? texto.length() : 0);
                        return texto;
                    }
                    log.warn("[GEMINI_SERVICE] Resposta sem candidatos");
                    return "🤖 Sem resposta da IA.";

                } catch (RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    log.warn("[GEMINI_SERVICE] Erro HTTP {} na tentativa {}/{}", status, tentativaAtual, maxTentativas);
                    if (status == 429 || status >= 500) {
                        if (tentativaAtual == maxTentativas) {
                            log.error("[GEMINI_SERVICE] Esgotadas tentativas para erro {}", status);
                            return "⚠️ Os servidores estão sobrecarregados (Erro " + status
                                    + "). Tente novamente mais tarde ou mude o modelo.";
                        }
                        log.info("⚠️ Erro " + status + " na geração de texto. Aguardando "
                                + (tempoEspera / 1000) + "s...");
                        Thread.sleep(tempoEspera);
                        tempoEspera *= 2;
                    } else {
                        log.error("[GEMINI_SERVICE] Erro não recuperável: status={}", status);
                        return "❌ Erro na consulta de texto (Código " + status + ").";
                    }
                }
            }
        } catch (InterruptedException ie) {
            log.warn("[GEMINI_SERVICE] Thread interrompida");
            Thread.currentThread().interrupt();
            return "⚠️ Consulta cancelada.";
        } catch (Exception e) {
            log.error("[GEMINI_SERVICE] Erro crítico em perguntarTexto", e);
            return "⚠️ Erro crítico: " + e.getMessage();
        }

        log.warn("[GEMINI_SERVICE] Falha de comunicação após todas as tentativas");
        return "🤖 Falha de comunicação.";
    }

    // 🚀 NOVO MÉTODO: Extração em Lote de Tabelas com Resiliência (Exponential
    // Backoff)
    public String extrairTabelasEmLote(java.io.File arquivoAnexo, String apiKey, String modeloEscolhido) {
        log.info("[GEMINI_SERVICE] extrairTabelasEmLote: modelo='{}', arquivo='{}'", modeloEscolhido,
                arquivoAnexo.getName());
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[GEMINI_SERVICE] extrairTabelasEmLote: API key ausente");
            throw new IllegalArgumentException("API Key não configurada. Verifique seu login.");
        }

        String promptRigido = """
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

        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(arquivoAnexo.toPath());
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);

            String fileName = arquivoAnexo.getName().toLowerCase();
            String mimeType = fileName.endsWith(".pdf") ? "application/pdf"
                    : fileName.endsWith(".png") ? "image/png"
                            : (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) ? "image/jpeg"
                                    : "application/octet-stream";
            log.trace("[GEMINI_SERVICE] Arquivo para OCR: mime={}, size={} bytes", mimeType, fileBytes.length);

            java.util.List<br.com.poderfinanceiro.app.dto.GeminiRequest.Part> parts = java.util.List.of(
                    br.com.poderfinanceiro.app.dto.GeminiRequest.Part.ofText(promptRigido),
                    br.com.poderfinanceiro.app.dto.GeminiRequest.Part.ofFile(mimeType, base64Data));

            br.com.poderfinanceiro.app.dto.GeminiRequest payload = new br.com.poderfinanceiro.app.dto.GeminiRequest(
                    java.util.List.of(new br.com.poderfinanceiro.app.dto.GeminiRequest.Content(parts)));

            String urlCompleta = baseUrl + "/" + modeloEscolhido + ":generateContent?key=" + apiKey;

            int maxTentativas = 6;
            int tempoEspera = 2000;

            for (int tentativaAtual = 1; tentativaAtual <= maxTentativas; tentativaAtual++) {
                try {
                    log.trace("[GEMINI_SERVICE] Chamada OCR tentativa {}/{}", tentativaAtual, maxTentativas);
                    String respostaJson = restClient.post()
                            .uri(urlCompleta)
                            .body(payload)
                            .retrieve()
                            .body(String.class);

                    if (respostaJson != null && !respostaJson.isBlank()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(respostaJson);

                        if (rootNode.has("candidates") && !rootNode.get("candidates").isEmpty()) {
                            String conteudoExtraido = rootNode.get("candidates").get(0).get("content").get("parts")
                                    .get(0).get("text").asText();
                            String jsonLimpo = conteudoExtraido.replaceAll("```json", "").replaceAll("```", "").trim();
                            log.info("[GEMINI_SERVICE] OCR extraído com sucesso (tamanho={})", jsonLimpo.length());
                            return jsonLimpo;
                        }
                    }
                    throw new RuntimeException("Resposta da IA vazia ou estruturalmente inválida.");

                } catch (org.springframework.web.client.RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    log.warn("[GEMINI_SERVICE] Erro HTTP {} na tentativa {}/{} de OCR", status, tentativaAtual,
                            maxTentativas);
                    if (status == 429 || status >= 500) {
                        if (tentativaAtual == maxTentativas) {
                            log.error("[GEMINI_SERVICE] Esgotadas tentativas para OCR com erro {}", status);
                            throw new RuntimeException("Servidores do Google sobrecarregados (Erro " + status + ").");
                        }
                        log.info("⚠️ API IA Ocupada (Erro " + status + "). Tentativa " + tentativaAtual
                                + ". Aguardando...");
                        Thread.sleep(tempoEspera);
                        tempoEspera *= 2;
                    } else {
                        log.error("[GEMINI_SERVICE] Erro não recuperável no OCR: status={}", status);
                        throw new RuntimeException("Erro HTTP na comunicação com IA: " + status);
                    }
                }
            }
        } catch (InterruptedException ie) {
            log.warn("[GEMINI_SERVICE] Thread interrompida durante OCR");
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante análise.");
        } catch (Exception e) {
            log.error("[GEMINI_SERVICE] Erro fatal no OCR: {}", e.getMessage(), e);
            throw new RuntimeException("Erro fatal ao analisar imagem: " + e.getMessage(), e);
        }
        log.warn("[GEMINI_SERVICE] Retorno padrão vazio para OCR");
        return "[]";
    }

    // 🚀 MÉTODO ATUALIZADO: Listar apenas modelos estáveis da geração 2.5 ou
    // superior
    public List<String> listarModelosMultimodais(String apiKey) {
        log.debug("[GEMINI_SERVICE] listarModelosMultimodais: solicitando modelos (apiKey present? {})",
                apiKey != null && !apiKey.isBlank());
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GEMINI_SERVICE] API key ausente, usando fallback estático");
            return List.of(
                    "gemini-3.5-flash",
                    "gemini-3.1-pro",
                    "gemini-3.1-flash-lite",
                    "gemini-2.5-pro",
                    "gemini-2.5-flash");
        }

        try {
            String url = baseUrl + "?key=" + apiKey;
            log.trace("[GEMINI_SERVICE] Chamando endpoint de listagem de modelos");

            String respostaJson = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            List<String> modelos = new ArrayList<>();

            if (respostaJson != null && !respostaJson.isBlank()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(respostaJson);

                if (rootNode.has("models")) {
                    for (com.fasterxml.jackson.databind.JsonNode node : rootNode.get("models")) {
                        String name = node.get("name").asText();
                        String cleanName = name.replace("models/", "");

                        if (cleanName.startsWith("gemini-") &&
                                !cleanName.contains("embedding") &&
                                !cleanName.contains("imagen") &&
                                !cleanName.contains("veo") &&
                                !cleanName.contains("aqa") &&
                                !cleanName.contains("preview") &&
                                !cleanName.contains("exp") &&
                                !cleanName.contains("tts") &&
                                !cleanName.contains("latest")) {

                            String[] parts = cleanName.split("-");
                            if (parts.length > 1) {
                                try {
                                    double versao = Double.parseDouble(parts[1]);
                                    if (versao < 2.5) {
                                        log.trace("[GEMINI_SERVICE] Descartando modelo versão antiga: {}", cleanName);
                                        continue;
                                    }
                                } catch (NumberFormatException e) {
                                    log.trace("[GEMINI_SERVICE] Descartando modelo com formato inesperado: {}",
                                            cleanName);
                                    continue;
                                }
                            }

                            modelos.add(cleanName);
                            log.trace("[GEMINI_SERVICE] Modelo aceito: {}", cleanName);
                        }
                    }
                }
            }

            modelos.sort(java.util.Comparator.reverseOrder());
            log.info("[GEMINI_SERVICE] {} modelos multimodais carregados da API", modelos.size());
            return modelos;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("⚠️ API do Google recusou a requisição de modelos (Erro " + e.getStatusCode()
                    + "). Usando Fallback.");
        } catch (Exception e) {
            log.error("⚠️ Erro interno ao processar a lista de modelos: {}", e.getMessage(), e);
        }

        log.warn("[GEMINI_SERVICE] Retornando fallback estático de modelos devido a erro");
        return List.of(
                "gemini-3.5-flash",
                "gemini-3.1-pro",
                "gemini-3.1-flash-lite",
                "gemini-2.5-pro",
                "gemini-2.5-flash");
    }

    private String carregarPlaybookLocal() {
        log.trace("[GEMINI_SERVICE] carregarPlaybookLocal: tentando carregar playbook do sistema de arquivos");
        try {
            Path caminho = obterCaminhoPlaybookCrossPlatform();
            if (Files.exists(caminho)) {
                String conteudo = Files.readString(caminho);
                log.debug("[GEMINI_SERVICE] Playbook carregado com sucesso (tamanho={})", conteudo.length());
                return conteudo;
            } else {
                log.warn("[GEMINI_SERVICE] Playbook não encontrado no caminho: {}", caminho);
            }
        } catch (IOException e) {
            log.error("[GEMINI_SERVICE] Falha ao ler o playbook: {}", e.getMessage(), e);
            return "{ 'aviso': 'Falha ao ler o playbook.' }";
        }
        return "{ 'aviso': 'Playbook interno vazio.' }";
    }

    private Path obterCaminhoPlaybookCrossPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String homeUser = System.getProperty("user.home");
        Path pastaBase;
        if (os.contains("win")) {
            String appDataRoaming = System.getenv("APPDATA");
            pastaBase = (appDataRoaming != null) ? Paths.get(appDataRoaming, "PoderFinanceiro")
                    : Paths.get(homeUser, "AppData", "Roaming", "PoderFinanceiro");
        } else if (os.contains("mac")) {
            pastaBase = Paths.get(homeUser, "Library", "Application Support", "PoderFinanceiro");
        } else {
            pastaBase = Paths.get(homeUser, ".local", "share", "PoderFinanceiro");
        }
        Path caminho = pastaBase.resolve("playbook_scripts.json");
        log.trace("[GEMINI_SERVICE] Caminho do playbook: {}", caminho);
        return caminho;
    }
}