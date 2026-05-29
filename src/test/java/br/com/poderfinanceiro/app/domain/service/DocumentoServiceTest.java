package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.infrastructure.config.DocumentoStorageResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h1>DocumentoServiceTest</h1>
 * <p>
 * Testes de Unidade para a gestão de Documentos Digitais.
 * Valida a persistência física, integridade via Hash SHA-256 e regras de
 * duplicidade.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @InjectMocks
    private DocumentoService service;

    @Mock
    private DocumentoProponenteRepository repository;

    @Mock
    private AuthService authService;

    @Mock
    private DocumentoStorageResolver storageResolver;

    @TempDir
    Path tempDir; // Diretório temporário isolado para simular o storage

    private ProponenteModel proponenteMock;
    private UsuarioModel consultorMock;

    @BeforeEach
    void setUp() {
        consultorMock = new UsuarioModel();
        consultorMock.setId(1L);
        consultorMock.setNome("Consultor Teste");

        proponenteMock = new ProponenteModel();
        proponenteMock.setId(100L);
        proponenteMock.setNomeCompleto("WAGNER ALVARO");

        // Mock padrão de segurança
        lenient().when(authService.getUsuarioLogado()).thenReturn(consultorMock);
    }

    @Test
    @DisplayName("Deve processar upload com sucesso, calculando hash e movendo arquivo para a pasta do cliente")
    void deveProcessarUploadComSucesso() throws Exception {
        // 1. Criar um arquivo de origem fake no diretório temporário
        Path origemPath = tempDir.resolve("rg_original.pdf");
        Files.writeString(origemPath, "conteudo_secreto_do_documento");
        File arquivoOriginal = origemPath.toFile();

        // 2. Configurar o destino fake via StorageResolver
        Path pastaCliente = tempDir.resolve("CLIENTE_100_WAGNER_ALVARO");
        Files.createDirectories(pastaCliente);
        when(storageResolver.resolverPastaCliente(100L, "WAGNER ALVARO")).thenReturn(pastaCliente);

        // 3. Simular que o hash não existe no banco (arquivo inédito)
        when(repository.findByHashSha256(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(DocumentoProponenteModel.class))).thenAnswer(i -> {
            DocumentoProponenteModel doc = i.getArgument(0);
            doc.setId(1L);
            return doc;
        });

        // 4. Executar
        DocumentoProponenteModel resultado = service.processarUpload(arquivoOriginal, "RG", proponenteMock, null);

        // 5. Validações
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipoDocumento()).isEqualTo("RG");
        assertThat(resultado.getHashSha256()).isNotNull();

        // Verifica se o arquivo físico foi realmente persistido no destino correto
        File arquivoFinal = new File(resultado.getArquivoPath());
        assertThat(arquivoFinal).exists();
        assertThat(arquivoFinal.getParentFile().getName()).isEqualTo("CLIENTE_100_WAGNER_ALVARO");

        verify(repository).save(any(DocumentoProponenteModel.class));
    }

    @Test
    @DisplayName("Deve impedir upload de arquivo duplicado baseado no Hash SHA-256")
    void deveImpedirUploadDuplicado() throws Exception {
        // Cenário: Arquivo com conteúdo que já existe no banco
        Path origemPath = tempDir.resolve("documento_repetido.pdf");
        Files.writeString(origemPath, "mesmo_conteudo");
        File arquivo = origemPath.toFile();

        when(repository.findByHashSha256(anyString())).thenReturn(Optional.of(new DocumentoProponenteModel()));

        // Validação da exceção de negócio
        assertThatThrownBy(() -> service.processarUpload(arquivo, "CPF", proponenteMock, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já foi anexado anteriormente");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve renomear o arquivo físico ao atualizar o tipo do documento")
    void deveRenomearArquivoAoAtualizarTipo() throws Exception {
        // 1. Criar arquivo físico inicial
        Path pasta = tempDir.resolve("DOCS");
        Files.createDirectories(pasta);
        Path arquivoAntigo = pasta.resolve("RG_WAGNER_123.pdf");
        Files.createFile(arquivoAntigo);

        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setId(1L);
        doc.setTipoDocumento("RG");
        doc.setArquivoPath(arquivoAntigo.toString());
        doc.setProponente(proponenteMock);

        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(repository.save(any(DocumentoProponenteModel.class))).thenReturn(doc);

        // 2. Executar atualização para CNH
        service.atualizarTipoDocumento(1L, "CNH");

        // 3. Validar
        assertThat(doc.getTipoDocumento()).isEqualTo("CNH");
        assertThat(doc.getArquivoPath()).contains("CNH_WAGNER");
        assertThat(new File(doc.getArquivoPath())).exists();
        assertThat(Files.exists(arquivoAntigo)).isFalse(); // O antigo deve ter sido movido/deletado
    }

    @Test
    @DisplayName("Deve excluir o registro do banco e o arquivo físico do disco")
    void deveExcluirDocumentoEArquivoFisico() throws Exception {
        // 1. Criar arquivo físico para ser deletado
        Path arquivoPath = tempDir.resolve("arquivo_para_morte.pdf");
        Files.createFile(arquivoPath);

        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setId(500L);
        doc.setArquivoPath(arquivoPath.toString());

        when(repository.findById(500L)).thenReturn(Optional.of(doc));

        // 2. Executar
        service.excluirDocumento(500L);

        // 3. Validar
        assertThat(Files.exists(arquivoPath)).isFalse(); // Arquivo sumiu do disco
        verify(repository).delete(doc); // Registro sumiu do banco
    }

    @Test
    @DisplayName("Deve lançar exceção se tentar upload sem consultor logado")
    void deveFalharUploadSemConsultor() throws Exception {
        when(authService.getUsuarioLogado()).thenReturn(null);
        File f = new File("qualquer.pdf");

        assertThatThrownBy(() -> service.processarUpload(f, "RG", proponenteMock, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sessão inválida");
    }
}
