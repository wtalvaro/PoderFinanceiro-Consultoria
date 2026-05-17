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

    // 🚀 NOVA ASSINATURA: Recebe o ficheiro (File)
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor, File arquivoAnexo) {
        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
            return "⚠️ Acesso Negado: A sua chave de API do Gemini não está configurada. Aceda às configurações no topo para configurá-la.";
        }

        try {
            String playbookJson = carregarPlaybookLocal();

            String instrucaoSistema = """
                    Você é um Analista Bancário Sênior e Especialista em Crédito.
                    Você atua como assistente interno (Copilot) do sistema 'Poder Financeiro'.

                    Se o consultor enviar um documento (holerite, HISCON, extrato), faça a leitura OCR com extrema precisão, cruze com as regras do Playbook e forneça a análise solicitada.

                    --- INÍCIO DO PLAYBOOK INTERNO (JSON) ---
                    """
                    + playbookJson + """
                            \n--- FIM DO PLAYBOOK INTERNO ---

                            Responda em formato limpo e estruturado.
                            """;

            String promptFinal = instrucaoSistema + "\n\n[CONTEXTO] Pergunta do Consultor: " + perguntaUsuario;

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

    // 🚀 NOVO MÉTODO: Detecta o OS e devolve a rota exata da pasta oculta
    private Path obterCaminhoPlaybookCrossPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String homeUser = System.getProperty("user.home");
        Path pastaBase;

        if (os.contains("win")) {
            // Padrão Windows: Tenta pegar o AppData/Local da variável de ambiente
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null) {
                appData = System.getenv("APPDATA"); // Fallback para Roaming
            }
            if (appData != null) {
                pastaBase = Paths.get(appData, "PoderFinanceiro");
            } else {
                // Hard-fallback caso variáveis de ambiente falhem
                pastaBase = Paths.get(homeUser, "AppData", "Local", "PoderFinanceiro");
            }
        } else if (os.contains("mac")) {
            // Padrão macOS
            pastaBase = Paths.get(homeUser, "Library", "Application Support", "PoderFinanceiro");
        } else {
            // Padrão Linux/Fedora que você já usava
            pastaBase = Paths.get(homeUser, ".local", "share", "PoderFinanceiro");
        }

        return pastaBase.resolve("playbook_scripts.json");
    }
    
}