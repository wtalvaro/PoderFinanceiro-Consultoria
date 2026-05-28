package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.PlaybookItemDTO;
import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.strategy.DocumentStrategy;
import br.com.poderfinanceiro.app.infrastructure.config.PlaybookResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Serviço de Domínio para gestão do Playbook de Vendas. Otimizado para Project
 * Loom utilizando ReentrantLock para evitar Thread Pinning.
 */
@Service
public class PlaybookService {

    private static final Logger log = LoggerFactory.getLogger(PlaybookService.class);
    private static final String LOG_PREFIX = "[PlaybookService]";
    private static final String CATEGORIA_CHECKLIST = "Checklists de Documentos";

    private final List<DocumentStrategy> documentStrategies;
    private final PlaybookResolver playbookResolver;
    private final ObjectMapper objectMapper;

    // Cache em memória protegido por Lock amigável a Virtual Threads
    private final List<PlaybookItemModel> itensEstaticos = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public PlaybookService(List<DocumentStrategy> documentStrategies, PlaybookResolver playbookResolver) {
        this.documentStrategies = documentStrategies;
        this.playbookResolver = playbookResolver;
        this.objectMapper = new ObjectMapper();
        log.info("{} [SISTEMA] Serviço inicializado com suporte a Project Loom.", LOG_PREFIX);
    }

    @PostConstruct public void init() {
        log.info("{} [SISTEMA] Iniciando ciclo de vida do Playbook.", LOG_PREFIX);
        try {
            garantirIntegridadeDoArquivo();
            carregarPlaybookDoDisco();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica na inicialização: {}", LOG_PREFIX, e.getMessage());
        }
    }

    /**
     * Retorna a lista completa. O uso de Lock garante consistência durante a
     * leitura caso uma gravação esteja ocorrendo simultaneamente em outra
     * Virtual Thread.
     */
    public List<PlaybookItemModel> listarTudoParaOPlaybook() {
        log.trace("{} [TELEMETRIA] Solicitada listagem completa do Playbook.", LOG_PREFIX);

        lock.lock();
        try {
            List<PlaybookItemModel> listaCompleta = new ArrayList<>(itensEstaticos);
            List<PlaybookItemModel> checklists = gerarChecklistsDeDocumentos();
            listaCompleta.addAll(checklists);

            log.debug("{} [NEGOCIO] Playbook montado: {} itens.", LOG_PREFIX, listaCompleta.size());
            return listaCompleta;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Persiste os itens no disco. O ReentrantLock permite que o Loom desmonte a
     * thread durante a escrita no arquivo (I/O), mantendo a escalabilidade.
     */
    public void salvarTodos(List<PlaybookItemModel> todosOsItens) {
        log.info("{} [TELEMETRIA] Iniciando persistência do Playbook via Virtual Thread.", LOG_PREFIX);

        if (todosOsItens == null)
            return;

        List<PlaybookItemDTO> dtosParaSalvar = todosOsItens.stream()
                .filter(item -> item.getCategoria() != null && !item.getCategoria().contains(CATEGORIA_CHECKLIST))
                .map(item -> new PlaybookItemDTO(item.getCategoria(), item.getTitulo(), item.getConteudo(),
                        item.getDica()))
                .collect(Collectors.toList());

        lock.lock();
        try {
            Path caminho = playbookResolver.obterCaminhoArquivo();
            // Operação de I/O: Com ReentrantLock, o Loom libera a Carrier
            // Thread aqui
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(caminho.toFile(), dtosParaSalvar);

            itensEstaticos.clear();
            todosOsItens.stream()
                    .filter(item -> item.getCategoria() != null && !item.getCategoria().contains(CATEGORIA_CHECKLIST))
                    .forEach(itensEstaticos::add);

            log.info("{} [AUDITORIA] Playbook persistido com sucesso. Total: {} itens.", LOG_PREFIX,
                    itensEstaticos.size());
        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro ao gravar arquivo de playbook: {}", LOG_PREFIX, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private List<PlaybookItemModel> gerarChecklistsDeDocumentos() {
        List<PlaybookItemModel> checklists = new ArrayList<>();
        for (TipoConvenioModel convenio : TipoConvenioModel.values()) {
            documentStrategies.stream().filter(s -> s.supports(convenio.name())).findFirst().ifPresent(strategy -> {
                String categoria = convenio.getLabel() + " / 4. " + CATEGORIA_CHECKLIST;
                checklists.add(new PlaybookItemModel(categoria, "Documentação Exigida", strategy.getChecklist(),
                        "Fotos nítidas para o convênio " + convenio.getLabel() + "."));
            });
        }
        return checklists;
    }

    private void carregarPlaybookDoDisco() {
        lock.lock();
        try {
            Path caminho = playbookResolver.obterCaminhoArquivo();
            if (Files.exists(caminho)) {
                List<PlaybookItemDTO> dtos = objectMapper.readValue(caminho.toFile(),
                        new TypeReference<List<PlaybookItemDTO>>() {
                        });
                itensEstaticos.clear();
                for (PlaybookItemDTO dto : dtos) {
                    itensEstaticos
                            .add(new PlaybookItemModel(dto.categoria(), dto.titulo(), dto.conteudo(), dto.dica()));
                }
                log.info("{} [SISTEMA] {} itens carregados do disco.", LOG_PREFIX, itensEstaticos.size());
            }
        } catch (IOException e) {
            log.error("{} [SISTEMA] Falha ao ler arquivo: {}", LOG_PREFIX, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void garantirIntegridadeDoArquivo() throws IOException {
        Path caminhoArquivo = playbookResolver.obterCaminhoArquivo();
        if (!Files.exists(caminhoArquivo)) {
            log.info("{} [SISTEMA] Criando estrutura inicial do Playbook.", LOG_PREFIX);
            Files.createDirectories(caminhoArquivo.getParent());
            try (InputStream is = new ClassPathResource("playbooks/playbook_scripts.json").getInputStream()) {
                Files.copy(is, caminhoArquivo, StandardCopyOption.REPLACE_EXISTING);
                log.info("{} [AUDITORIA] Template clonado com sucesso.", LOG_PREFIX);
            }
        }
    }
}
