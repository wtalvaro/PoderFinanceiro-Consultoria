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
            String jsonClienteAtivo, String jsonTabelasJuros, String jsonLinksUteis) {
        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
            return "⚠️ Acesso Negado: A sua chave de API do Gemini não está configurada.";
        }

        try {
            String playbookJson = carregarPlaybookLocal();

            String clienteSeguro = (jsonClienteAtivo == null || jsonClienteAtivo.isBlank()) ? "{}" : jsonClienteAtivo;
            String tabelasSeguro = (jsonTabelasJuros == null || jsonTabelasJuros.isBlank()) ? "[]" : jsonTabelasJuros;
            String linksSeguro = (jsonLinksUteis == null || jsonLinksUteis.isBlank()) ? "[]" : jsonLinksUteis;

            // 🎯 A ENGENHARIA DE PROMPT MODULAR
            String instrucaoSistema = """
                    Você é um Analista Bancário Sênior e assistente (Copilot) do 'Poder Financeiro ERP'.
                    Responda às dúvidas do consultor cruzando as informações dos módulos abaixo.

                    [MÓDULO 1 - REGRAS DE NEGÓCIO (PLAYBOOK)]
                    %s

                    [MÓDULO 2 - DADOS DO CLIENTE EM ATENDIMENTO NA TELA]
                    %s

                    [MÓDULO 3 - TABELAS DE JUROS E BANCOS DISPONÍVEIS (TEMPO REAL)]
                    %s

                    [MÓDULO 4 - BASE DE CONHECIMENTO E LINKS ÚTEIS]
                    %s

                    Diretrizes:
                    - Se perguntarem sobre margem ou cliente, use o Módulo 2.
                    - Se perguntarem qual a taxa do banco X, busque no Módulo 3.
                    - Se perguntarem onde achar um sistema ou manual, indique o título do link no Módulo 4.
                    """.formatted(playbookJson, clienteSeguro, tabelasSeguro, linksSeguro);

            String promptFinal = instrucaoSistema + "\n\n[PERGUNTA DO CONSULTOR]: " + perguntaUsuario;
            
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