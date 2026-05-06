package br.com.poderfinanceiro.app.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct; // Usando o jakarta conforme ajustado anteriormente

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.poderfinanceiro.app.model.PlaybookItem;
import br.com.poderfinanceiro.app.model.PlaybookItemDTO;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.strategy.DocumentStrategy;

@Service
public class PlaybookService {

        private final List<DocumentStrategy> documentStrategies;
        private final ObjectMapper objectMapper;

        // Armazena em memória os itens estáticos carregados do JSON
        private final List<PlaybookItem> itensEstaticos = new ArrayList<>();

        // 1. REMOVA o ObjectMapper daqui dos parâmetros
        public PlaybookService(List<DocumentStrategy> documentStrategies) {
                this.documentStrategies = documentStrategies;
                // 2. INSTANCIE diretamente aqui
                this.objectMapper = new ObjectMapper();
        }

        @PostConstruct
        public void init() {
                try {
                        ClassPathResource resource = new ClassPathResource("playbooks/playbook_scripts.json");
                        try (InputStream is = resource.getInputStream()) {
                                List<PlaybookItemDTO> dtos = objectMapper.readValue(is,
                                                new TypeReference<List<PlaybookItemDTO>>() {
                                                });

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