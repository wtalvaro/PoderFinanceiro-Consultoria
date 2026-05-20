package br.com.poderfinanceiro.app.service;

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

@Service
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api.url}")
    private String baseUrl;

    public GeminiService() {
        this.restClient = RestClient.builder().build();
    }

    // 🚀 SOBRECARGA: Mantém a compatibilidade com o PropostaController (Usa o 3.5
    // Flash como padrão)
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, File arquivoAnexo,
            String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis, String jsonComissoes) {
        return perguntarAoAssistente(perguntaUsuario, apiKeyDoConsultor, "gemini-3.5-flash", arquivoAnexo,
                jsonClienteAtivo, jsonTabelasJuros, jsonLinksUteis, jsonComissoes);
    }

    // 🚀 ASSINATURA PRINCIPAL: Com Modelo Dinâmico e Exponential Backoff
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, String modeloEscolhido,
            File arquivoAnexo, String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis,
            String jsonComissoes) {

        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
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

                    [CONTEXTO FORNECIDO PARA CONSULTA]
                    - REGRAS DE NEGÓCIO: %s
                    - CLIENTE EM ATENDIMENTO: %s
                    - TABELAS DE JUROS: %s
                    - LINKS ÚTEIS: %s
                    - COMISSÕES: %s

                    [DIRETRIZES DE FORMATAÇÃO VISUAL (BOOTSTRAP 5)]
                    O chat do sistema renderiza HTML. É OBRIGATÓRIO formatar todas as respostas para serem visualmente atraentes:
                    - Use <strong> ou <b> para destacar valores financeiros e conceitos-chave.
                    - Tabelas de simulação devem usar <table class="table table-sm table-striped table-hover bg-white my-2">.
                    - Listas devem ser <ul class="list-unstyled"> ou <li> com emojis.
                    """
                    .formatted(playbookJson, clienteSeguro, tabelasSeguro, linksSeguro, comissoesSeguro);

            String promptFinal = instrucaoSistema + "\n\n[DÚVIDA ATUAL DO OPERADOR]: " + perguntaUsuario;
            List<GeminiRequest.Part> parts = new ArrayList<>();
            parts.add(GeminiRequest.Part.ofText(promptFinal));

            if (arquivoAnexo != null && arquivoAnexo.exists()) {
                byte[] fileBytes = Files.readAllBytes(arquivoAnexo.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                String fileName = arquivoAnexo.getName().toLowerCase();
                String mimeType = fileName.endsWith(".pdf") ? "application/pdf"
                        : fileName.endsWith(".png") ? "image/png"
                                : (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) ? "image/jpeg"
                                        : "application/octet-stream";
                parts.add(GeminiRequest.Part.ofFile(mimeType, base64Data));
            }

            GeminiRequest payload = new GeminiRequest(List.of(new GeminiRequest.Content(parts)));
            String urlCompleta = baseUrl + "/" + modeloEscolhido + ":generateContent?key=" + apiKeyDoConsultor;

            // 🛡️ MOTOR DE RESILIÊNCIA (EXPONENTIAL BACKOFF)
            int maxTentativas = 6;
            int tempoEspera = 2000; // Começa aguardando 2 segundos

            for (int tentativaAtual = 1; tentativaAtual <= maxTentativas; tentativaAtual++) {
                try {
                    GeminiResponse response = restClient.post()
                            .uri(urlCompleta)
                            .body(payload)
                            .retrieve()
                            .body(GeminiResponse.class);

                    if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                        return response.candidates().get(0).content().parts().get(0).text();
                    }
                    return "🤖 Sem resposta dos servidores cognitivos.";

                } catch (RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    // Falhas de Sobrecarga: 429 (Rate Limit), 500, 502, 503, 504
                    if (status == 429 || status >= 500) {
                        if (tentativaAtual == maxTentativas) {
                            return "⚠️ Os servidores da Google estão sobrecarregados no momento (Erro " + status
                                    + "). Tentamos " + maxTentativas
                                    + " vezes. Por favor, aguarde um minuto ou selecione outro modelo na engrenagem de configuração.";
                        }
                        System.out.println("⚠️ Erro " + status + " na IA. Aguardando " + (tempoEspera / 1000)
                                + "s para nova tentativa (" + tentativaAtual + "/" + maxTentativas + ")...");
                        Thread.sleep(tempoEspera);
                        tempoEspera *= 2; // Dobra o tempo (2s, 4s, 8s, 16s, 32s)
                    } else {
                        // Erros 400 (Bad Request), 403 (Auth) não adiantam tentar de novo
                        return "❌ Erro na consulta (Código " + status
                                + "). Verifique sua chave de API ou o anexo enviado.";
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "⚠️ Consulta cancelada.";
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Falha crítica ao montar a requisição: " + e.getMessage();
        }

        return "🤖 Houve um problema de comunicação com os servidores.";
    }

    // 🚀 MÉTODO ATUALIZADO: Recebe o modelo dinamicamente e inclui motor de
    // resiliência
    public String perguntarTexto(String prompt, String apiKey, String modeloEscolhido) {
        if (apiKey == null || apiKey.isBlank())
            return "⚠️ API Key não configurada.";

        try {
            List<GeminiRequest.Part> parts = List.of(GeminiRequest.Part.ofText(prompt));
            GeminiRequest payload = new GeminiRequest(List.of(new GeminiRequest.Content(parts)));

            // URL 100% dinâmica baseada na escolha do consultor
            String urlCompleta = baseUrl + "/" + modeloEscolhido + ":generateContent?key=" + apiKey;

            // 🛡️ MOTOR DE RESILIÊNCIA (EXPONENTIAL BACKOFF) PARA TEXTO
            int maxTentativas = 6;
            int tempoEspera = 2000;

            for (int tentativaAtual = 1; tentativaAtual <= maxTentativas; tentativaAtual++) {
                try {
                    GeminiResponse response = restClient.post()
                            .uri(urlCompleta)
                            .body(payload)
                            .retrieve()
                            .body(GeminiResponse.class);

                    return (response != null && response.candidates() != null && !response.candidates().isEmpty())
                            ? response.candidates().get(0).content().parts().get(0).text()
                            : "🤖 Sem resposta da IA.";

                } catch (org.springframework.web.client.RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    if (status == 429 || status >= 500) {
                        if (tentativaAtual == maxTentativas) {
                            return "⚠️ Os servidores estão sobrecarregados (Erro " + status
                                    + "). Tente novamente mais tarde ou mude o modelo.";
                        }
                        System.out.println("⚠️ Erro " + status + " na geração de texto. Aguardando "
                                + (tempoEspera / 1000) + "s...");
                        Thread.sleep(tempoEspera);
                        tempoEspera *= 2;
                    } else {
                        return "❌ Erro na consulta de texto (Código " + status + ").";
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "⚠️ Consulta cancelada.";
        } catch (Exception e) {
            return "⚠️ Erro crítico: " + e.getMessage();
        }

        return "🤖 Falha de comunicação.";
    }

    // 🚀 NOVO MÉTODO: Extração em Lote de Tabelas com Resiliência (Exponential
    // Backoff)
    public String extrairTabelasEmLote(java.io.File arquivoAnexo, String apiKey, String modeloEscolhido) {
        if (apiKey == null || apiKey.isBlank()) {
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
                  "tipoConvenio": "INSS_CONSIGNADO, CLT_CONSIGNADO, BOLSA_FAMILIA, SIAPE, EXERCITO, AERONAUTICA, MARINHA",
                  "valorMinimo": 0.0, "valorMaximo": 0.0,
                  "prazoMinimo": 0, "prazoMaximo": 0,
                  "idadeMinima": 0, "idadeMaxima": 0,
                  "taxaMensal": 0.0, "comissaoPercentual": 0.0,
                  "fimVigenciaCalculado": "ISO_DATE_TIME (ex: 2026-12-31T23:59:00) ou nulo se não houver na imagem"
                }
                Se um dado numérico não existir na imagem, preencha com 0 ou 0.0.
                """;

        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(arquivoAnexo.toPath());
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);

            String fileName = arquivoAnexo.getName().toLowerCase();
            String mimeType = fileName.endsWith(".pdf") ? "application/pdf"
                    : fileName.endsWith(".png") ? "image/png"
                            : (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) ? "image/jpeg"
                                    : "application/octet-stream";

            java.util.List<br.com.poderfinanceiro.app.dto.GeminiRequest.Part> parts = java.util.List.of(
                    br.com.poderfinanceiro.app.dto.GeminiRequest.Part.ofText(promptRigido),
                    br.com.poderfinanceiro.app.dto.GeminiRequest.Part.ofFile(mimeType, base64Data));

            br.com.poderfinanceiro.app.dto.GeminiRequest payload = new br.com.poderfinanceiro.app.dto.GeminiRequest(
                    java.util.List.of(new br.com.poderfinanceiro.app.dto.GeminiRequest.Content(parts)));

            String urlCompleta = baseUrl + "/" + modeloEscolhido + ":generateContent?key=" + apiKey;

            // 🛡️ MOTOR DE RESILIÊNCIA (EXPONENTIAL BACKOFF)
            int maxTentativas = 6;
            int tempoEspera = 2000;

            for (int tentativaAtual = 1; tentativaAtual <= maxTentativas; tentativaAtual++) {
                try {
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
                            // Blindagem extra contra marcação Markdown do Gemini
                            return conteudoExtraido.replaceAll("```json", "").replaceAll("```", "").trim();
                        }
                    }
                    throw new RuntimeException("Resposta da IA vazia ou estruturalmente inválida.");

                } catch (org.springframework.web.client.RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    if (status == 429 || status >= 500) {
                        if (tentativaAtual == maxTentativas) {
                            throw new RuntimeException("Servidores do Google sobrecarregados (Erro " + status + ").");
                        }
                        System.out.println("⚠️ API IA Ocupada (Erro " + status + "). Tentativa " + tentativaAtual
                                + ". Aguardando...");
                        Thread.sleep(tempoEspera);
                        tempoEspera *= 2;
                    } else {
                        throw new RuntimeException("Erro HTTP na comunicação com IA: " + status);
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante análise.");
        } catch (Exception e) {
            throw new RuntimeException("Erro fatal ao analisar imagem: " + e.getMessage(), e);
        }
        return "[]";
    }

    // 🚀 MÉTODO ATUALIZADO: Listar apenas modelos estáveis da geração 2.5 ou
    // superior
    public List<String> listarModelosMultimodais(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of(
                    "gemini-3.5-flash",
                    "gemini-3.1-pro",
                    "gemini-3.1-flash-lite",
                    "gemini-2.5-pro",
                    "gemini-2.5-flash");
        }

        try {
            String url = baseUrl + "?key=" + apiKey;

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

                        // 1. FILTRO DE LIMPEZA (Remove betas, previews e ferramentas não textuais)
                        if (cleanName.startsWith("gemini-") &&
                                !cleanName.contains("embedding") &&
                                !cleanName.contains("imagen") &&
                                !cleanName.contains("veo") &&
                                !cleanName.contains("aqa") &&
                                !cleanName.contains("preview") &&
                                !cleanName.contains("exp") &&
                                !cleanName.contains("tts") &&
                                !cleanName.contains("latest")) {

                            // 2. 🚀 FILTRO DINÂMICO DE VERSÃO (Apenas 2.5 para cima)
                            String[] parts = cleanName.split("-");
                            if (parts.length > 1) {
                                try {
                                    double versao = Double.parseDouble(parts[1]);
                                    if (versao < 2.5) {
                                        continue; // Descarta 1.5, 2.0, etc.
                                    }
                                } catch (NumberFormatException e) {
                                    continue; // Descarta padrões fora do escopo comercial (ex: robotics)
                                }
                            }

                            modelos.add(cleanName);
                        }
                    }
                }
            }

            // Ordena os modelos dinâmicos da API de forma decrescente para manter os mais
            // novos no topo
            modelos.sort(java.util.Comparator.reverseOrder());
            return modelos;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            System.err.println("⚠️ API do Google recusou a requisição de modelos (Erro " + e.getStatusCode()
                    + "). Usando Fallback.");
        } catch (Exception e) {
            System.err.println("⚠️ Erro interno ao processar a lista de modelos: " + e.getMessage());
        }

        // 🛡️ FALLBACK BLINDADO ORDENADO (Apenas 2.5+ se a API falhar)
        return List.of(
                "gemini-3.5-flash",
                "gemini-3.1-pro",
                "gemini-3.1-flash-lite",
                "gemini-2.5-pro",
                "gemini-2.5-flash");
    }

    private String carregarPlaybookLocal() {
        try {
            Path caminho = obterCaminhoPlaybookCrossPlatform();
            if (Files.exists(caminho))
                return Files.readString(caminho);
        } catch (IOException e) {
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
        return pastaBase.resolve("playbook_scripts.json");
    }
}