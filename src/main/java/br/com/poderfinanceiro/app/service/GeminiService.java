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

    // 🚀 NOVA ASSINATURA: Agora o método exige obrigatoriamente a chave do
    // consultor logado
    public String perguntarAoAssistente(String perguntaUsuario, String apiKeyDoConsultor) {

        // 🛑 BARREIRA DE SEGURANÇA: Sabota a execução se o consultor não possuir a
        // chave cadastrada
        if (apiKeyDoConsultor == null || apiKeyDoConsultor.isBlank()) {
            return "⚠️ Acesso Negado: Sua chave de API do Gemini não está cadastrada. Vá no menu do seu perfil para configurá-la.";
        }

        try {
            String playbookJson = carregarPlaybookLocal();

            // Configuração da Persona rica do Especialista
            String instrucaoSistema = """
                    Você é um Analista Bancário Sênior e Especialista em Crédito Consignado, Pessoal e Financiamentos no Brasil.
                    Você atua como assistente interno (Copilot) do sistema 'Poder Financeiro'.
                    Seu objetivo é ajudar os consultores a estruturar operações, calcular margens, entender normativas (INSS, SIAPE, FGTS) e fechar negócios.

                    SUA PERSONA E CONHECIMENTO:
                    - Você domina cálculo de Tabela Price, SAC, Custo Efetivo Total (CET), IOF, portabilidade, refinanciamento e troco.
                    - Você conhece as regras gerais do Banco Central do Brasil e do INSS para consignados.
                    - Seja direto, profissional, encorajador e use jargões do mercado financeiro quando apropriado.

                    A REGRA DE OURO (HIERARQUIA DE INFORMAÇÃO):
                    Abaixo está o 'Playbook Operacional' da empresa. As regras contidas nele TÊM PRIORIDADE MÁXIMA.
                    Se o seu conhecimento geral de mercado entrar em conflito com o Playbook, o Playbook sempre vence.
                    Se a pergunta for sobre processos internos da empresa, use exclusivamente o Playbook.

                    --- INÍCIO DO PLAYBOOK INTERNO (JSON) ---
                    """
                    + playbookJson
                    + """
                            \n--- FIM DO PLAYBOOK INTERNO ---

                            Responda em formato limpo, usando marcadores para facilitar a leitura rápida na tela do sistema.
                            """;

            // 🎯 Consolidação da injeção direto no corpo do texto principal
            String promptFinal = instrucaoSistema + "\n\n[CONTEXTO OPERACIONAL PROCESSADO] Pergunta do Consultor: "
                    + perguntaUsuario;

            GeminiRequest payload = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(promptFinal)))));

            // 🎯 O AJUSTE: O RestClient consome dinamicamente os créditos da chave do
            // usuário que fez a pergunta
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
            String homeUser = System.getProperty("user.home");
            Path caminho = Paths.get(homeUser, ".local", "share", "PoderFinanceiro", "playbook_scripts.json");

            if (Files.exists(caminho)) {
                return Files.readString(caminho);
            }
        } catch (IOException e) {
            return "{ 'aviso': 'Falha ao ler o arquivo local de playbook.' }";
        }
        return "{ 'aviso': 'Playbook interno vazio.' }";
    }
}