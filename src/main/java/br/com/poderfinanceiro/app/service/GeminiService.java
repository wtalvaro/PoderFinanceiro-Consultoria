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

            // 🎯 ENGENHARIA DE PROMPT COGNITIVA E INTEGRADA COM BOOTSTRAP 5
            String instrucaoSistema = """
                    Você é um Analista de Crédito Sênior especializado e altamente persuasivo do Poder Financeiro.
                    Abaixo, você receberá dados do cliente ativo, tabelas de juros, links internos e regras de negócio. Use esses dados para embasar as suas decisões, mas use toda a sua inteligência de mercado, técnicas de contorno de objeções e conhecimento financeiro para formular respostas consultivas, humanas e estratégicas.

                    Seu objetivo NÃO É apenas buscar dados isolados, mas sim CRUZAR as informações de forma inteligente para gerar uma consultoria comercial de alto nível para o consultor de vendas.

                    [CONTEXTO DISPONÍVEL]
                    - REGRAS DE NEGÓCIO (PLAYBOOK BANCÁRIO): %s
                    - CLIENTE EM ATENDIMENTO NA TELA: %s
                    - TABELAS DE JUROS E BANCOS EM TEMPO REAL: %s
                    - BASE DE CONHECIMENTO (SISTEMAS E LINKS ÚTEIS): %s
                    - REPASSES E FLUXO DE CAIXA (COMISSÕES): %s

                    [DIRETRIZES DE INTELIGÊNCIA E COMPORTAMENTO]
                    1. Raciocínio Holístico: Diante de qualquer dúvida, cruze os dados. Se o usuário perguntar sobre o cliente, não cuspa apenas os dados dele; analise quais bancos da tabela de juros se encaixam na idade, renda e regras do playbook para aquele perfil.
                    2. Postura Consultiva: Atue como um mentor para o consultor. Sugira estratégias de contorno de objeções baseadas no Playbook, destaque vantagens competitivas de taxas mais baixas e aponte os caminhos dos links úteis sempre que o consultor precisar acessar um sistema externo.
                    3. Proibição de Termos Técnicos: Nunca diga 'com base no JSON fornecido', 'de acordo com o módulo X' ou 'o sistema me enviou os dados'. Aja como se você estivesse enxergando a tela do sistema de forma nativa.

                    [DIRETRIZES DE FORMATAÇÃO VISUAL (BOOTSTRAP 5)]
                    O chat do sistema agora renderiza HTML moderno com Bootstrap 5. É OBRIGATÓRIO formatar suas respostas para ficarem visualmente atraentes:
                    - Use <strong> ou <b> para destacar valores financeiros, nomes de bancos e taxas.
                    - Quando listar opções de bancos, simulações ou propostas, organize os dados in tabelas do Bootstrap usando a classe: <table class="table table-sm table-striped table-hover bg-white my-2">.
                    - Utilize listas estruturadas (<ul class="list-unstyled"> ou <li>) com emojis para deixar a leitura rápida e scannável para o operador.
                    - Sempre que indicar uma URL do contexto, crie um link HTML real de forma sutil.
                    
                    [REGRAS DE FORMATAÇÃO E NAVEGAÇÃO]
                    - Proibido enviar qualquer tipo de link interno, protocolo de navegação (como 'app://') ou referência a telas específicas.
                    - Se precisar indicar um caminho ou funcionalidade, descreva-o apenas com texto.
                    - Continue usando HTML/Bootstrap para formatar tabelas e texto, mas não crie links clicáveis que apontem para dentro do sistema.
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