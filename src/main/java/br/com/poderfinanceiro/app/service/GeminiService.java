package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.dto.GeminiRequest;
import br.com.poderfinanceiro.app.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import java.util.Base64;
import java.util.ArrayList;
import java.io.File;

@Service
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api.url}")
    private String apiUrl;

    // ❌ REMOVIDO COMPLETAMENTE: A chave master do application.properties foi
    // eliminada.

    public GeminiService() {
        this.restClient = RestClient.builder().build();
    }

    // 🚀 ASSINATURA EXPANDIDA PARA RECEBER O ECOSSISTEMA COMPLETO
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, File arquivoAnexo,
            String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis, String jsonComissoes) {
        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
            return "⚠️ Acesso Negado: A sua chave de API do Gemini não está configurada.";
        }

        try {
            String playbookJson = carregarPlaybookLocal();

            String clienteSeguro = (jsonClienteAtivo == null || jsonClienteAtivo.isBlank()) ? "{}" : jsonClienteAtivo;
            String tabelasSeguro = (jsonTabelasJuros == null || jsonTabelasJuros.isBlank()) ? "[]" : jsonTabelasJuros;
            String linksSeguro = (jsonLinksUteis == null || jsonLinksUteis.isBlank()) ? "[]" : jsonLinksUteis;
            String comissoesSeguro = (jsonComissoes == null || jsonComissoes.isBlank()) ? "[]" : jsonComissoes;

            // 🎯 ENGENHARIA DE PROMPT HÍBRIDA (CONTEXTO + CONHECIMENTO GERAL)
            String instrucaoSistema = """
                    Você é um Analista de Crédito Sênior especializado e altamente persuasivo do Poder Financeiro.
                    Você possui dois níveis de conhecimento:
                    1. CONTEXTO INTERNO (Prioridade Máxima): Dados do cliente, tabelas de juros, regras de negócio e comissões que eu forneço.
                    2. CONHECIMENTO GERAL (Rede de Segurança): Todo o seu treinamento como modelo de linguagem avançado sobre finanças, economia e mercado.

                    [SUAS DIRETRIZES DE COMPORTAMENTO]
                    1. Raciocínio Holístico: Ao responder, sempre busque primeiro nos dados internos. Se a informação (ex: salário mínimo, regras do INSS, conceitos de mercado) não estiver disponível no contexto interno, use seu conhecimento geral para responder de forma precisa.
                    2. Postura de Mentor: Não importa se a resposta vem do contexto interno ou do seu conhecimento geral, mantenha sempre a postura de consultor financeiro do Poder Financeiro. Seja prestativo, persuasivo e estratégico.
                    3. Postura Consultiva: Atue como um mentor. Destaque vantagens, contorne objeções e seja um consultor ativo.
                    4. Proibição de Termos Técnicos: Nunca diga 'o JSON não contém essa info' ou 'os dados que você me passou não dizem isso'. Simplesmente responda ao consultor com a autoridade de quem domina o assunto.

                    [CONTEXTO FORNECIDO PARA CONSULTA]
                    - REGRAS DE NEGÓCIO (PLAYBOOK BANCÁRIO): %s
                    - CLIENTE EM ATENDIMENTO NA TELA: %s
                    - TABELAS DE JUROS E BANCOS EM TEMPO REAL: %s
                    - BASE DE CONHECIMENTO (SISTEMAS E LINKS ÚTEIS): %s
                    - REPASSES E FLUXO DE CAIXA (COMISSÕES): %s

                    [DIRETRIZES DE FORMATAÇÃO VISUAL (BOOTSTRAP 5)]
                    O chat do sistema renderiza HTML. É OBRIGATÓRIO formatar todas as respostas para serem visualmente atraentes:
                    - Use <strong> ou <b> para destacar valores financeiros, bancos e conceitos-chave.
                    - Tabelas de simulação ou dados devem usar <table class="table table-sm table-striped table-hover bg-white my-2">.
                    - Listas devem ser <ul class="list-unstyled"> ou <li> com emojis.
                    - Se citar fontes ou leis, crie links sutis. Nunca revele que você está acessando bancos de dados internos.
                    """
                    .formatted(playbookJson, clienteSeguro, tabelasSeguro, linksSeguro, comissoesSeguro);
            // Se a sua API/Serviço aceitar a System Instruction separada (como configuração
            // do modelo),
            // passe a 'instrucaoSistema' nela. Caso contrário, junte de forma clara como
            // abaixo:
            String promptFinal = instrucaoSistema + "\n\n[DÚVIDA ATUAL DO OPERADOR]: " + perguntaUsuario;

            // 🎯 CONSTRUÇÃO DINÂMICA DAS PARTS (Texto + Ficheiro)
            List<GeminiRequest.Part> parts = new ArrayList<>();

            // 1. Adiciona a pergunta em texto
            parts.add(GeminiRequest.Part.ofText(promptFinal));

            // 2. Se houver ficheiro, converte para Base64 e adiciona como InlineData
            if (arquivoAnexo != null && arquivoAnexo.exists()) {
                byte[] fileBytes = Files.readAllBytes(arquivoAnexo.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);

                // Determina o MimeType
                String fileName = arquivoAnexo.getName().toLowerCase();
                String mimeType = "application/pdf"; // Padrão
                if (fileName.endsWith(".png"))
                    mimeType = "image/png";
                else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                    mimeType = "image/jpeg";

                parts.add(GeminiRequest.Part.ofFile(mimeType, base64Data));
            }

            GeminiRequest payload = new GeminiRequest(
                    List.of(new GeminiRequest.Content(parts)));

            GeminiResponse response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKeyDoConsultor)
                    .body(payload)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                return response.candidates().get(0).content().parts().get(0).text();
            }

            return "🤖 Houve um problema de comunicação com os servidores do banco de dados analítico.";

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Sistema de IA indisponível no momento. Detalhe técnico: " + e.getMessage();
        }
    }

    // NOVO MÉTODO: Focado apenas em texto
    public String perguntarTexto(String prompt, String apiKey) {
        if (apiKey == null || apiKey.isBlank())
            return "⚠️ API Key não configurada.";

        try {
            List<GeminiRequest.Part> parts = List.of(GeminiRequest.Part.ofText(prompt));
            GeminiRequest payload = new GeminiRequest(List.of(new GeminiRequest.Content(parts)));

            GeminiResponse response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(GeminiResponse.class);

            return (response != null && !response.candidates().isEmpty())
                    ? response.candidates().get(0).content().parts().get(0).text()
                    : "🤖 Sem resposta da IA.";
        } catch (Exception e) {
            return "⚠️ Erro: " + e.getMessage();
        }
    }

    private String carregarPlaybookLocal() {
        try {
            Path caminho = obterCaminhoPlaybookCrossPlatform();

            if (Files.exists(caminho)) {
                return Files.readString(caminho);
            } else {
                System.out.println("⚠️ Arquivo de Playbook não encontrado no caminho: " + caminho.toAbsolutePath());
            }
        } catch (IOException e) {
            return "{ 'aviso': 'Falha ao ler o arquivo local de playbook.' }";
        }
        return "{ 'aviso': 'Playbook interno vazio.' }";
    }

    // 🚀 MÉTODO ATUALIZADO: Alinhado para buscar no AppData\Roaming do Windows
    private Path obterCaminhoPlaybookCrossPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String homeUser = System.getProperty("user.home");
        Path pastaBase;

        if (os.contains("win")) {
            // 🎯 A CURA: Captura a variável %APPDATA% que aponta direto para
            // AppData\Roaming
            String appDataRoaming = System.getenv("APPDATA");

            if (appDataRoaming != null) {
                pastaBase = Paths.get(appDataRoaming, "PoderFinanceiro");
            } else {
                // Fallback manual seguro para Roaming caso as variáveis globais sumam
                pastaBase = Paths.get(homeUser, "AppData", "Roaming", "PoderFinanceiro");
            }
        } else if (os.contains("mac")) {
            // Padrão macOS
            pastaBase = Paths.get(homeUser, "Library", "Application Support", "PoderFinanceiro");
        } else {
            // Padrão Linux/Fedora do contêiner
            pastaBase = Paths.get(homeUser, ".local", "share", "PoderFinanceiro");
        }

        return pastaBase.resolve("playbook_scripts.json");
    }

}