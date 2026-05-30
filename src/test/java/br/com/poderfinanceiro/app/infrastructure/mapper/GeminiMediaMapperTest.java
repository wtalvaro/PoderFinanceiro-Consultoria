package br.com.poderfinanceiro.app.infrastructure.mapper;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste Unitário para o GeminiMediaMapper.
 * Valida a conversão de arquivos físicos para o formato aceito pela API Gemini.
 */
class GeminiMediaMapperTest {

    private GeminiMediaMapper mapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mapper = new GeminiMediaMapper();
    }

    @Test
    @DisplayName("Deve converter um arquivo PDF para Part com Base64 correto")
    void deveConverterPdfComSucesso() throws IOException {
        // GIVEN
        Path pdfPath = tempDir.resolve("documento.pdf");
        byte[] conteudoOriginal = "Conteúdo PDF Simulado".getBytes();
        Files.write(pdfPath, conteudoOriginal);
        File arquivo = pdfPath.toFile();

        // WHEN
        GeminiRequest.Part resultado = mapper.toPart(arquivo);

        // THEN
        assertNotNull(resultado);
        assertEquals("application/pdf", resultado.inlineData().mimeType());
        assertEquals(Base64.getEncoder().encodeToString(conteudoOriginal), resultado.inlineData().data());
    }

    @Test
    @DisplayName("Deve detectar corretamente MIME type de imagens PNG e JPG")
    void deveDetectarMimeImagens() throws IOException {
        // GIVEN
        Path pngPath = tempDir.resolve("foto.png");
        Path jpgPath = tempDir.resolve("foto.jpg");
        Files.write(pngPath, new byte[] { 0 });
        Files.write(jpgPath, new byte[] { 0 });

        // WHEN
        GeminiRequest.Part partPng = mapper.toPart(pngPath.toFile());
        GeminiRequest.Part partJpg = mapper.toPart(jpgPath.toFile());

        // THEN
        assertEquals("image/png", partPng.inlineData().mimeType());
        assertEquals("image/jpeg", partJpg.inlineData().mimeType());
    }

    @Test
    @DisplayName("Deve usar application/octet-stream para extensões desconhecidas")
    void deveUsarMimePadraoParaDesconhecido() throws IOException {
        // GIVEN
        Path path = tempDir.resolve("arquivo.xyz");
        Files.write(path, new byte[] { 1, 2, 3 });

        // WHEN
        GeminiRequest.Part resultado = mapper.toPart(path.toFile());

        // THEN
        assertEquals("application/octet-stream", resultado.inlineData().mimeType());
    }

    @Test
    @DisplayName("Deve lançar IOException quando o arquivo não existir")
    void deveFalharParaArquivoInexistente() {
        // GIVEN
        File arquivoInexistente = new File(tempDir.toFile(), "fantasma.pdf");

        // WHEN & THEN
        IOException ex = assertThrows(IOException.class, () -> mapper.toPart(arquivoInexistente));
        assertTrue(ex.getMessage().contains("não encontrado"));
    }

    @Test
    @DisplayName("Deve lançar IOException para entrada nula")
    void deveFalharParaNull() {
        assertThrows(IOException.class, () -> mapper.toPart(null));
    }
}
