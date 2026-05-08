package br.com.poderfinanceiro.app.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.poderfinanceiro.app.model.PlaybookItem;
import br.com.poderfinanceiro.app.model.PlaybookItemDTO;
import br.com.poderfinanceiro.app.model.enums.TipoConvenio;
import br.com.poderfinanceiro.app.strategy.DocumentStrategy;

@Service
public class PlaybookService {

        private final List<DocumentStrategy> documentStrategies;
        private final ObjectMapper objectMapper;

        // Armazena em memória os itens estáticos carregados do JSON
        private final List<PlaybookItem> itensEstaticos = new ArrayList<>();

        public PlaybookService(List<DocumentStrategy> documentStrategies) {
                this.documentStrategies = documentStrategies;
                this.objectMapper = new ObjectMapper();
        }

        @PostConstruct
        public void init() {
                carregarDoJson();
        }

        private void carregarDoJson() {
                try {
                        ClassPathResource resource = new ClassPathResource("playbooks/playbook_scripts.json");
                        if (!resource.exists()) {
                                System.out.println("Arquivo de playbook não encontrado no classpath.");
                                return;
                        }

                        try (InputStream is = resource.getInputStream()) {
                                List<PlaybookItemDTO> dtos = objectMapper.readValue(is,
                                                new TypeReference<List<PlaybookItemDTO>>() {
                                                });

                                itensEstaticos.clear(); // Limpa a lista antes de recarregar
                                for (PlaybookItemDTO dto : dtos) {
                                        itensEstaticos.add(new PlaybookItem(
                                                        dto.categoria(),
                                                        dto.titulo(),
                                                        dto.conteudo(),
                                                        dto.dica()));
                                }
                        }
                } catch (IOException e) {
                        System.err.println("Erro ao carregar o playbook comercial: " + e.getMessage());
                }
        }

        public List<PlaybookItem> listarTudoParaOPlaybook() {
                List<PlaybookItem> itens = new ArrayList<>(itensEstaticos);
                itens.addAll(gerarChecklistsDeDocumentos());
                return itens;
        }

        // ==========================================
        // NOVO MÉTODO DE SALVAMENTO (CRUD)
        // ==========================================

        public void salvarTodos(List<PlaybookItem> todosOsItens) {
                // 1. Filtramos as checklists dinâmicas para não gravá-las no JSON
                List<PlaybookItemDTO> dtosParaSalvar = todosOsItens.stream()
                                .filter(item -> item.getCategoria() != null
                                                && !item.getCategoria().contains("Checklists de Documentos"))
                                .map(item -> new PlaybookItemDTO(
                                                item.getCategoria(),
                                                item.getTitulo(),
                                                item.getConteudo(),
                                                item.getDica()))
                                .collect(Collectors.toList());

                // 2. Caminho fixo para o ambiente de desenvolvimento
                File arquivoJson = new File("src/main/resources/playbooks/playbook_scripts.json");

                try {
                        // Grava o JSON de forma indentada e bonita (Pretty Printer)
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(arquivoJson, dtosParaSalvar);

                        // 3. Atualiza a memória estática
                        itensEstaticos.clear();
                        todosOsItens.stream()
                                        .filter(item -> item.getCategoria() != null
                                                        && !item.getCategoria().contains("Checklists de Documentos"))
                                        .forEach(itensEstaticos::add);

                        System.out.println("Playbook salvo com sucesso!");

                } catch (IOException e) {
                        System.err.println("Erro ao salvar o playbook: " + e.getMessage());
                }
        }

        private List<PlaybookItem> gerarChecklistsDeDocumentos() {
                List<PlaybookItem> checklists = new ArrayList<>();

                for (TipoConvenio convenio : TipoConvenio.values()) {
                        documentStrategies.stream()
                                        .filter(s -> s.supports(convenio.name()))
                                        .findFirst()
                                        .ifPresent(strategy -> {
                                                String categoria = convenio.getLabel()
                                                                + " / 4. Checklists de Documentos";
                                                checklists.add(new PlaybookItem(
                                                                categoria,
                                                                "Documentação Exigida",
                                                                strategy.getChecklist(),
                                                                "Fotos nítidas, sem cortes e sem reflexos para o convênio "
                                                                                + convenio.getLabel() + "."));
                                        });
                }
                return checklists;
        }
}