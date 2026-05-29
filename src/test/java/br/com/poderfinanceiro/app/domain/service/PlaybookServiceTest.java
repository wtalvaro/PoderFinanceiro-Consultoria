package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.PlaybookItemDTO;
import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.strategy.DocumentStrategy;
import br.com.poderfinanceiro.app.infrastructure.config.PlaybookResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * <h1>PlaybookServiceTest</h1>
 * <p>
 * Testes de Unidade para o Playbook de Vendas.
 * Valida a persistência em JSON, o isolamento de itens dinâmicos (checklists)
 * e a integridade do cache em memória.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PlaybookServiceTest {

    private PlaybookService service;

    @Mock
    private PlaybookResolver playbookResolver;

    @Mock
    private DocumentStrategy mockStrategy;

    @TempDir
    Path tempDir; // Cria uma pasta temporária isolada para cada teste

    private File tempFile;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tempFile = tempDir.resolve("playbook_test.json").toFile();
        lenient().when(playbookResolver.obterCaminhoArquivo()).thenReturn(tempFile.toPath());

        // Inicializa o serviço com uma estratégia mockada
        service = new PlaybookService(List.of(mockStrategy), playbookResolver);
    }

    @Test
    @DisplayName("Deve carregar itens do disco para a memória durante a inicialização")
    void deveCarregarPlaybookDoDisco() throws Exception {
        // Cenário: Criar um arquivo JSON fake no diretório temporário
        List<PlaybookItemDTO> dadosIniciais = List.of(
                new PlaybookItemDTO("Geral", "Script Teste", "Conteúdo...", "Dica..."));
        mapper.writeValue(tempFile, dadosIniciais);

        // Ação: Dispara o PostConstruct manualmente
        service.init();

        // Validação
        List<PlaybookItemModel> resultado = service.listarTudoParaOPlaybook();
        assertThat(resultado).filteredOn(i -> i.getTitulo().equals("Script Teste")).isNotEmpty();
    }

    @Test
    @DisplayName("Deve combinar itens estáticos do JSON com checklists dinâmicas das Strategies")
    void deveCombinarItensEstaticosEDinamicos() {
        // Configura a estratégia para retornar uma checklist
        when(mockStrategy.supports(anyString())).thenReturn(true);
        when(mockStrategy.getChecklist()).thenReturn("RG, CPF, Comprovante");

        // Ação
        List<PlaybookItemModel> resultado = service.listarTudoParaOPlaybook();

        // Validação: Deve conter itens da categoria "Checklists de Documentos"
        assertThat(resultado).anyMatch(item -> item.getCategoria().contains("Checklists de Documentos"));
        assertThat(resultado).anyMatch(item -> item.getConteudo().contains("RG, CPF"));
    }

    @Test
    @DisplayName("Deve filtrar e NÃO salvar itens que pertencem às checklists dinâmicas")
    void deveFiltrarChecklistsAoSalvar() throws Exception {
        // Cenário: Uma lista misturando scripts reais e checklists
        PlaybookItemModel scriptReal = new PlaybookItemModel("Vendas", "Abordagem", "Olá...", "Dica");
        PlaybookItemModel checklistDinamica = new PlaybookItemModel("INSS / 4. Checklists de Documentos", "Doc", "...",
                "...");

        List<PlaybookItemModel> listaParaSalvar = List.of(scriptReal, checklistDinamica);

        // Ação
        service.salvarTodos(listaParaSalvar);

        // Validação: O arquivo JSON não deve conter a checklist
        List<PlaybookItemDTO> noDisco = mapper.readValue(tempFile,
                mapper.getTypeFactory().constructCollectionType(List.class, PlaybookItemDTO.class));

        assertThat(noDisco).hasSize(1);
        assertThat(noDisco.get(0).titulo()).isEqualTo("Abordagem");
        assertThat(noDisco.get(0).categoria()).isEqualTo("Vendas");
    }

    @Test
    @DisplayName("Deve garantir a integridade do arquivo criando-o a partir do template se ausente")
    void deveGarantirIntegridadeDoArquivo() throws Exception {
        // Cenário: Arquivo não existe
        if (tempFile.exists())
            tempFile.delete();
        assertThat(tempFile).doesNotExist();

        // Ação
        service.init();

        // Validação: O arquivo deve ter sido criado (clonado do classpath)
        assertThat(tempFile).exists();
    }
}
