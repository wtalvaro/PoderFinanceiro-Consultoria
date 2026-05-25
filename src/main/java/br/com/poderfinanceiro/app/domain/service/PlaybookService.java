package br.com.poderfinanceiro.app.domain.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.poderfinanceiro.app.domain.model.PlaybookItemModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.strategy.DocumentStrategy;
import br.com.poderfinanceiro.app.dto.PlaybookItemDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PlaybookService {

    private static final Logger log = LoggerFactory.getLogger(PlaybookService.class);

    private final List<DocumentStrategy> documentStrategies;
    private final ObjectMapper objectMapper;

    private final List<PlaybookItemModel> itensEstaticos = new ArrayList<>();
    private String caminhoArquivoFinal;

    public PlaybookService(List<DocumentStrategy> documentStrategies) {
        this.documentStrategies = documentStrategies;
        this.objectMapper = new ObjectMapper();
        log.debug("[PLAYBOOK_SERVICE] Construtor: Serviço instanciado com {} estratégias de documento",
                documentStrategies != null ? documentStrategies.size() : 0);
    }

    @PostConstruct
    public void init() {
        log.debug("[PLAYBOOK_SERVICE] init: Inicializando playbook");
        this.caminhoArquivoFinal = obterCaminhoDoProntuario();
        garantirArquivoExiste();
        carregarPlaybook();
        log.info("[PLAYBOOK_SERVICE] init: Playbook inicializado com sucesso - arquivo em '{}'", caminhoArquivoFinal);
    }

    private String obterCaminhoDoProntuario() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        String caminhoBase;

        if (os.contains("win")) {
            caminhoBase = System.getenv("APPDATA") + File.separator + "PoderFinanceiro";
            log.trace("[PLAYBOOK_SERVICE] obterCaminhoDoProntuario: SO Windows, APPDATA={}", System.getenv("APPDATA"));
        } else if (os.contains("mac")) {
            caminhoBase = home + "/Library/Application Support/PoderFinanceiro";
            log.trace("[PLAYBOOK_SERVICE] obterCaminhoDoProntuario: SO macOS, home={}", home);
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            caminhoBase = (xdgData != null && !xdgData.isEmpty())
                    ? xdgData + File.separator + "PoderFinanceiro"
                    : home + File.separator + ".local" + File.separator + "share" + File.separator + "PoderFinanceiro";
            log.trace("[PLAYBOOK_SERVICE] obterCaminhoDoProntuario: SO Linux/Unix, XDG_DATA_HOME={}", xdgData);
        }

        File pasta = new File(caminhoBase);
        if (!pasta.exists()) {
            boolean criada = pasta.mkdirs();
            if (criada) {
                log.debug("[PLAYBOOK_SERVICE] obterCaminhoDoProntuario: Pasta base criada: {}", caminhoBase);
            } else {
                log.warn("[PLAYBOOK_SERVICE] obterCaminhoDoProntuario: Não foi possível criar a pasta base: {}",
                        caminhoBase);
            }
        }

        String caminhoCompleto = caminhoBase + File.separator + "playbook_scripts.json";
        log.trace("[PLAYBOOK_SERVICE] obterCaminhoDoProntuario: caminho final = {}", caminhoCompleto);
        return caminhoCompleto;
    }

    private void garantirArquivoExiste() {
        File arquivo = new File(caminhoArquivoFinal);
        log.debug("[PLAYBOOK_SERVICE] garantirArquivoExiste: Verificando existência em '{}'", caminhoArquivoFinal);

        if (!arquivo.exists()) {
            log.info("[PLAYBOOK_SERVICE] garantirArquivoExiste: Arquivo não encontrado. Copiando template interno.");
            try (InputStream is = new ClassPathResource("playbooks/playbook_scripts.json").getInputStream()) {
                Files.copy(is, arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("[PLAYBOOK_SERVICE] garantirArquivoExiste: Protocolo inicial clonado para: {}",
                        caminhoArquivoFinal);
            } catch (IOException e) {
                log.error("[PLAYBOOK_SERVICE] garantirArquivoExiste: Erro crítico ao criar o prontuário inicial.", e);
            }
        } else {
            log.trace("[PLAYBOOK_SERVICE] garantirArquivoExiste: Arquivo já existe.");
        }
    }

    private void carregarPlaybook() {
        log.debug("[PLAYBOOK_SERVICE] carregarPlaybook: Carregando conteúdo do arquivo '{}'", caminhoArquivoFinal);
        try {
            File arquivo = new File(caminhoArquivoFinal);
            List<PlaybookItemDTO> dtos = objectMapper.readValue(arquivo, new TypeReference<List<PlaybookItemDTO>>() {
            });

            itensEstaticos.clear();
            for (PlaybookItemDTO dto : dtos) {
                itensEstaticos.add(new PlaybookItemModel(
                        dto.categoria(),
                        dto.titulo(),
                        dto.conteudo(),
                        dto.dica()));
            }

            log.info("[PLAYBOOK_SERVICE] carregarPlaybook: {} itens carregados com sucesso de: {}",
                    itensEstaticos.size(), caminhoArquivoFinal);
        } catch (IOException e) {
            log.error("[PLAYBOOK_SERVICE] carregarPlaybook: Erro ao ler o prontuário: {}", e.getMessage(), e);
        }
    }

    public List<PlaybookItemModel> listarTudoParaOPlaybook() {
        log.debug("[PLAYBOOK_SERVICE] listarTudoParaOPlaybook: Montando lista completa (estáticos + checklists)");
        List<PlaybookItemModel> itens = new ArrayList<>(itensEstaticos);
        int checklistsSize = gerarChecklistsDeDocumentos().size();
        itens.addAll(gerarChecklistsDeDocumentos());
        log.info(
                "[PLAYBOOK_SERVICE] listarTudoParaOPlaybook: Total de {} itens retornados ({} estáticos + {} checklists)",
                itens.size(), itensEstaticos.size(), checklistsSize);
        return itens;
    }

    public void salvarTodos(List<PlaybookItemModel> todosOsItens) {
        log.debug("[PLAYBOOK_SERVICE] salvarTodos: Iniciando persistência de {} itens", todosOsItens.size());
        List<PlaybookItemDTO> dtosParaSalvar = todosOsItens.stream()
                .filter(item -> item.getCategoria() != null
                        && !item.getCategoria().contains("Checklists de Documentos"))
                .map(item -> new PlaybookItemDTO(
                        item.getCategoria(),
                        item.getTitulo(),
                        item.getConteudo(),
                        item.getDica()))
                .collect(Collectors.toList());
        log.trace("[PLAYBOOK_SERVICE] salvarTodos: {} itens serão persistidos (excluindo checklists dinâmicos)",
                dtosParaSalvar.size());

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(caminhoArquivoFinal), dtosParaSalvar);

            itensEstaticos.clear();
            todosOsItens.stream()
                    .filter(item -> item.getCategoria() != null
                            && !item.getCategoria().contains("Checklists de Documentos"))
                    .forEach(itensEstaticos::add);

            log.info(
                    "[PLAYBOOK_SERVICE] salvarTodos: Prontuário atualizado e persistido com segurança no disco ({} itens salvos).",
                    itensEstaticos.size());
        } catch (IOException e) {
            log.error("[PLAYBOOK_SERVICE] salvarTodos: Erro na 'cirurgia' de salvamento do playbook: {}",
                    e.getMessage(), e);
        }
    }

    private List<PlaybookItemModel> gerarChecklistsDeDocumentos() {
        log.trace("[PLAYBOOK_SERVICE] gerarChecklistsDeDocumentos: Gerando checklists dinâmicas para convênios");
        List<PlaybookItemModel> checklists = new ArrayList<>();

        for (TipoConvenioModel convenio : TipoConvenioModel.values()) {
            documentStrategies.stream()
                    .filter(s -> s.supports(convenio.name()))
                    .findFirst()
                    .ifPresent(strategy -> {
                        String categoria = convenio.getLabel() + " / 4. Checklists de Documentos";
                        checklists.add(new PlaybookItemModel(
                                categoria,
                                "Documentação Exigida",
                                strategy.getChecklist(),
                                "Fotos nítidas, sem cortes e sem reflexos para o convênio " + convenio.getLabel()
                                        + "."));
                        log.trace(
                                "[PLAYBOOK_SERVICE] gerarChecklistsDeDocumentos: Checklist adicionada para convênio '{}'",
                                convenio.getLabel());
                    });
        }
        log.debug("[PLAYBOOK_SERVICE] gerarChecklistsDeDocumentos: {} checklists geradas", checklists.size());
        return checklists;
    }
}