package br.com.poderfinanceiro.app.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PlaybookResolverTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve carregar o conteúdo real de um arquivo quando o caminho é injetado")
    void deveCarregarConteudoReal() throws IOException {
        // GIVEN
        String jsonEsperado = "{\"versao\": \"1.0\", \"scripts\": []}";
        Files.writeString(tempDir.resolve("playbook_scripts.json"), jsonEsperado);

        // Injetamos o diretório temporário no construtor
        PlaybookResolver resolver = new PlaybookResolver(tempDir.toString());

        // WHEN
        String resultado = resolver.carregarPlaybook();

        // THEN
        assertEquals(jsonEsperado, resultado, "O conteúdo lido deve ser idêntico ao escrito no arquivo temporário.");
    }

    @Test
    @DisplayName("Deve manter a compatibilidade com a resolução automática de SO")
    void deveManterResolucaoAutomatica() {
        // GIVEN
        PlaybookResolver resolver = new PlaybookResolver("default");
        String os = System.getProperty("os.name").toLowerCase();

        // WHEN
        Path caminho = resolver.obterCaminhoArquivo();

        // THEN
        if (os.contains("linux")) {
            assertTrue(caminho.toString().contains(".local/share"), "No Linux deve usar .local/share");
        }
        assertTrue(caminho.toString().endsWith("playbook_scripts.json"));
    }

    @Test
    @DisplayName("Deve retornar JSON vazio se o arquivo configurado não existir")
    void deveRetornarVazioSeArquivoNaoExistir() {
        // GIVEN - Apontamos para uma pasta vazia
        PlaybookResolver resolver = new PlaybookResolver(tempDir.toString());

        // WHEN
        String resultado = resolver.carregarPlaybook();

        // THEN
        assertEquals("{}", resultado);
    }
}
