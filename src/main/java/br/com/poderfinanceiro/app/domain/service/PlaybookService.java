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

    // Armazena em memória os itens estáticos carregados do JSON (O Prontuário
    // Ativo)
    private final List<PlaybookItemModel> itensEstaticos = new ArrayList<>();

    // O prontuário agora tem um endereço dinâmico (resolve o problema de salvar
    // dentro do JAR/EXE)
    private String caminhoArquivoFinal;

    public PlaybookService(List<DocumentStrategy> documentStrategies) {
        this.documentStrategies = documentStrategies;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // 1. Define o endereço da ala hospitalar baseada no SO (Windows/Linux/Mac)
        this.caminhoArquivoFinal = obterCaminhoDoProntuario();

        // 2. Garante que o arquivo existe (ou clona o protocolo padrão do JAR)
        garantirArquivoExiste();

        // 3. Carrega os dados para a memória da UTI
        carregarPlaybook();
    }

    private String obterCaminhoDoProntuario() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        String caminhoBase;

        if (os.contains("win")) {
            // Windows: O Arquivo Central (Roaming)
            caminhoBase = System.getenv("APPDATA") + File.separator + "PoderFinanceiro";
        } else if (os.contains("mac")) {
            // macOS: O Suporte de Aplicação
            caminhoBase = home + "/Library/Application Support/PoderFinanceiro";
        } else {
            // Linux e outros Unix: Seguindo o protocolo XDG
            String xdgData = System.getenv("XDG_DATA_HOME");
            caminhoBase = (xdgData != null && !xdgData.isEmpty())
                    ? xdgData + File.separator + "PoderFinanceiro"
                    : home + File.separator + ".local" + File.separator + "share" + File.separator + "PoderFinanceiro";
        }

        // Garante que a "ala" do hospital existe
        File pasta = new File(caminhoBase);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        return caminhoBase + File.separator + "playbook_scripts.json";
    }

    private void garantirArquivoExiste() {
        File arquivo = new File(caminhoArquivoFinal);
        
        // Se o arquivo externo não existe, clonamos o padrão do JAR para fora
        if (!arquivo.exists()) {
            try (InputStream is = new ClassPathResource("playbooks/playbook_scripts.json").getInputStream()) {
                Files.copy(is, arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("Protocolo inicial clonado para a pasta de usuário: " + caminhoArquivoFinal);
            } catch (IOException e) {
                log.error(("Erro crítico: Não foi possível criar o prontuário inicial."));
            }
        }
    }

    private void carregarPlaybook() {
        try {
            File arquivo = new File(caminhoArquivoFinal);
            List<PlaybookItemDTO> dtos = objectMapper.readValue(arquivo, new TypeReference<List<PlaybookItemDTO>>() {});
            
            itensEstaticos.clear();
            for (PlaybookItemDTO dto : dtos) {
                itensEstaticos.add(new PlaybookItemModel(
                        dto.categoria(),
                        dto.titulo(),
                        dto.conteudo(),
                        dto.dica()));
            }
            
            log.info("Sinais vitais do Playbook carregados com sucesso a partir de: " + caminhoArquivoFinal);
        } catch (IOException e) {
            log.error(("Erro ao ler o prontuário: " + e.getMessage()));
        }
    }

    public List<PlaybookItemModel> listarTudoParaOPlaybook() {
        List<PlaybookItemModel> itens = new ArrayList<>(itensEstaticos);
        // Injeta as prescrições dinâmicas (checklists) que não devem ser salvas no JSON
        itens.addAll(gerarChecklistsDeDocumentos());
        return itens;
    }

    // ==========================================
    // NOVO MÉTODO DE SALVAMENTO (CRUD)
    // ==========================================

    public void salvarTodos(List<PlaybookItemModel> todosOsItens) {
        // 1. Filtramos as checklists dinâmicas para não gravá-las no JSON
        List<PlaybookItemDTO> dtosParaSalvar = todosOsItens.stream()
                .filter(item -> item.getCategoria() != null && !item.getCategoria().contains("Checklists de Documentos"))
                .map(item -> new PlaybookItemDTO(
                        item.getCategoria(), 
                        item.getTitulo(), 
                        item.getConteudo(), 
                        item.getDica()))
                .collect(Collectors.toList());

        try {
            // 2. Grava o JSON de forma indentada e bonita (Pretty Printer) sempre no arquivo externo dinâmico
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(caminhoArquivoFinal), dtosParaSalvar);
            
            // 3. Atualiza a memória estática de curto prazo
            itensEstaticos.clear();
            todosOsItens.stream()
                    .filter(item -> item.getCategoria() != null && !item.getCategoria().contains("Checklists de Documentos"))
                    .forEach(itensEstaticos::add);
            
            log.info("Prontuário atualizado e persistido com segurança no disco.");
        } catch (IOException e) {
            log.error(("Erro na 'cirurgia' de salvamento do playbook: " + e.getMessage()));
        }
    }

    private List<PlaybookItemModel> gerarChecklistsDeDocumentos() {
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
                    });
        }
        return checklists;
    }
}