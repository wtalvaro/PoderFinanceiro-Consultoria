package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.dto.GeminiRequest;
import java.io.File;
import java.util.List;

public interface IAjudaChatFacade {

    // DTO para transferir dados da sessão sem expor a lógica de JSON para a UI
    record SessaoChatDTO(File arquivo, String tituloPreview) {
    }

    // --- Configurações e Modelos ---
    boolean isApiKeyConfigurada();

    String getApiKeyAtual();

    void atualizarApiKey(String novaChave);

    List<String> listarModelosIADisponiveis();

    // --- Orquestração de IA ---
    String enviarMensagemParaIA(String mensagem, File anexo, String modelo, List<GeminiRequest.Content> historico);

    // --- Gestão de Histórico (I/O) ---
    void salvarSessao(String nomeArquivo, List<GeminiRequest.Content> historico) throws Exception;

    List<GeminiRequest.Content> carregarSessao(File arquivo) throws Exception;

    List<SessaoChatDTO> listarSessoesRecentes();
}
