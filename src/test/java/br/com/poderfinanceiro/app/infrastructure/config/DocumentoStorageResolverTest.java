package br.com.poderfinanceiro.app.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoStorageResolverTest {

    @TempDir
    Path tempDir;

    private DocumentoStorageResolver storageResolver;

    @BeforeEach
    void setUp() {
        storageResolver = new DocumentoStorageResolver(tempDir.toString());
    }

    @Test
    @DisplayName("Deve salvar um arquivo e retornar metadados corretos com Hash SHA-256")
    void deveSalvarArquivoComSucesso() throws Exception {
        // GIVEN
        String conteudo = "Conteúdo de teste para o ERP Poder Financeiro";
        String nomeOriginal = "contrato_teste.pdf";
        byte[] bytesConteudo = conteudo.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytesConteudo);

        // Cálculo manual do hash esperado para validação real
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hashEsperado = HexFormat.of().formatHex(digest.digest(bytesConteudo));

        // WHEN
        DocumentoStorageResolver.StorageResult resultado = storageResolver.salvar(inputStream, nomeOriginal);

        // THEN
        assertNotNull(resultado);
        assertNotNull(resultado.hash());

        // RESOLUÇÃO DO WARNING: Utilizando a variável para validar a integridade
        assertEquals(hashEsperado, resultado.hash(),
                "O hash gerado pelo storage deve ser idêntico ao hash do conteúdo original");

        assertTrue(resultado.pathRelativo().endsWith(".pdf"));

        Path arquivoSalvo = tempDir.resolve(resultado.pathRelativo());
        assertTrue(Files.exists(arquivoSalvo), "O arquivo físico deve existir no diretório temporário");
        assertEquals(bytesConteudo.length, resultado.tamanho());
    }

    @Test
    @DisplayName("Deve criar estrutura de pastas baseada na data atual")
    void deveCriarEstruturaDePastasPorData() {
        String conteudo = "Teste de Pastas";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8));

        DocumentoStorageResolver.StorageResult resultado = storageResolver.salvar(inputStream, "teste.txt");

        Path arquivoSalvo = tempDir.resolve(resultado.pathRelativo());
        String anoAtual = String.valueOf(java.time.LocalDate.now().getYear());

        assertTrue(arquivoSalvo.toString().contains(anoAtual), "O caminho deve conter o ano atual para organização");
    }

    @Test
    @DisplayName("Deve resolver pasta do cliente com nome sanitizado")
    void deveResolverPastaClienteSanitizada() {
        // GIVEN
        Long idCliente = 123L;
        String nomeSujo = "João Ações & Negócios Ltda.";

        // WHEN
        Path pathCliente = storageResolver.resolverPastaCliente(idCliente, nomeSujo);

        // THEN
        String nomePasta = pathCliente.getFileName().toString();
        assertTrue(nomePasta.startsWith("123_"));
        assertFalse(nomePasta.contains("&"), "Caracteres especiais devem ser removidos");
        assertFalse(nomePasta.contains(" "), "Espaços devem ser substituídos por underscores");
        assertEquals(nomePasta, nomePasta.toUpperCase(), "O nome da pasta deve ser em caixa alta");
    }
}
