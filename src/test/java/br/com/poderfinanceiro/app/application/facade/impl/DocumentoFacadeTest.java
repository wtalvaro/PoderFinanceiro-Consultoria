package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.DocumentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para DocumentoFacadeImpl.
 * Valida a orquestração de documentos e integridade de arquivos físicos.
 */
class DocumentoFacadeTest {

    private DocumentoFacadeImpl facade;
    private DocumentoService documentoService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        documentoService = mock(DocumentoService.class);
        facade = new DocumentoFacadeImpl(documentoService);
    }

    @Test
    @DisplayName("Deve listar documentos de um proponente delegando ao serviço")
    void deveListarDocumentos() {
        // GIVEN
        Long proponenteId = 1L;
        when(documentoService.listarDoProponente(proponenteId)).thenReturn(List.of(new DocumentoProponenteModel()));

        // WHEN
        List<DocumentoProponenteModel> docs = facade.listarDocumentosDoProponente(proponenteId);

        // THEN
        assertNotNull(docs);
        assertEquals(1, docs.size());
        verify(documentoService, times(1)).listarDoProponente(proponenteId);
    }

    @Test
    @DisplayName("Deve salvar novo documento com sucesso quando o arquivo físico existe")
    void deveSalvarNovoDocumentoComSucesso() throws Exception {
        // GIVEN
        Path arquivoPath = tempDir.resolve("rg.pdf");
        Files.writeString(arquivoPath, "Conteudo Simulado");
        File arquivo = arquivoPath.toFile();

        ProponenteModel proponente = new ProponenteModel();
        proponente.setId(10L);

        DocumentoProponenteModel mockSalvo = new DocumentoProponenteModel();
        mockSalvo.setId(100L);

        when(documentoService.processarUpload(eq(arquivo), eq("RG"), eq(proponente), any())).thenReturn(mockSalvo);

        // WHEN
        DocumentoProponenteModel resultado = facade.salvarNovoDocumento(arquivo, "RG", proponente);

        // THEN
        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        verify(documentoService, times(1)).processarUpload(arquivo, "RG", proponente, null);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar arquivo inexistente")
    void deveFalharAoSalvarArquivoInexistente() {
        // GIVEN
        File arquivoFantasma = new File(tempDir.toFile(), "nao_existe.pdf");
        ProponenteModel proponente = new ProponenteModel();

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> facade.salvarNovoDocumento(arquivoFantasma, "CPF", proponente));
        verifyNoInteractions(documentoService);
    }

    @Test
    @DisplayName("Deve atualizar o tipo do documento delegando ao serviço")
    void deveAtualizarTipoDocumento() throws Exception {
        // GIVEN
        Long docId = 50L;
        String novoTipo = "CNH";
        DocumentoProponenteModel mockAtualizado = new DocumentoProponenteModel();
        when(documentoService.atualizarTipoDocumento(docId, novoTipo)).thenReturn(mockAtualizado);

        // WHEN
        DocumentoProponenteModel resultado = facade.atualizarTipoDocumento(docId, novoTipo);

        // THEN
        assertNotNull(resultado);
        verify(documentoService, times(1)).atualizarTipoDocumento(docId, novoTipo);
    }

    @Test
    @DisplayName("Deve alternar status de verificação delegando ao serviço")
    void deveAlternarVerificacao() throws Exception {
        // GIVEN
        Long docId = 50L;
        DocumentoProponenteModel mockDoc = new DocumentoProponenteModel();
        when(documentoService.alternarVerificacao(docId)).thenReturn(mockDoc);

        // WHEN
        DocumentoProponenteModel resultado = facade.alternarStatusVerificacao(docId);

        // THEN
        assertNotNull(resultado);
        verify(documentoService, times(1)).alternarVerificacao(docId);
    }

    @Test
    @DisplayName("Deve validar corretamente a existência física do arquivo no disco")
    void deveValidarExistenciaFisica() throws Exception {
        // GIVEN
        Path pathReal = tempDir.resolve("contrato.pdf");
        Files.writeString(pathReal, "Dados");

        DocumentoProponenteModel docExistente = new DocumentoProponenteModel();
        docExistente.setArquivoPath(pathReal.toString());

        DocumentoProponenteModel docInexistente = new DocumentoProponenteModel();
        docInexistente.setArquivoPath(tempDir.resolve("fantasma.pdf").toString());

        // WHEN & THEN
        assertTrue(facade.validarExistenciaArquivoFisico(docExistente));
        assertFalse(facade.validarExistenciaArquivoFisico(docInexistente));
        assertFalse(facade.validarExistenciaArquivoFisico(null));
    }

    @Test
    @DisplayName("Deve delegar exclusão para o serviço")
    void deveExcluirDocumento() {
        // WHEN
        facade.excluirDocumento(1L);

        // THEN
        verify(documentoService, times(1)).excluirDocumento(1L);
    }
}
