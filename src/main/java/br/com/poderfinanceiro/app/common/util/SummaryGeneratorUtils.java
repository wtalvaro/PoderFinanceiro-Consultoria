package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.presentation.viewmodel.LeadViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Componente de Geração de Contexto e Resumos.
 * Centraliza a transformação de modelos para Markdown e JSON.
 * Implementa rigorosamente o protocolo de logs SLF4J.
 */
@Component
public class SummaryGeneratorUtils {

    private static final Logger log = LoggerFactory.getLogger(SummaryGeneratorUtils.class);
    private static final String LOG_PREFIX = "[SummaryGeneratorUtils]";
    private static final String SEPARADOR = "————————————————————————————\n";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;

    public SummaryGeneratorUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("{} [SISTEMA] Gerador de Sumários inicializado com ObjectMapper injetado.", LOG_PREFIX);
    }

    /**
     * Gera relatório formatado em Markdown para o AtendimentoHubController.
     */
    public String gerar(LeadViewModel viewModel, String rendaFormatada) {
        log.debug("{} [TELEMETRIA] Iniciando geração de relatório Markdown para a UI.", LOG_PREFIX);

        if (viewModel == null) {
            log.warn("{} [NEGOCIO] Falha na geração: ViewModel nulo.", LOG_PREFIX);
            return "";
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("📑 *RELATÓRIO DE QUALIFICAÇÃO - PODER FINANCEIRO*\n");
            sb.append(SEPARADOR);

            log.trace("{} [NEGOCIO] Mapeando dados de identificação do proponente.", LOG_PREFIX);
            sb.append("*[DADOS DO PROPONENTE]*\n");
            sb.append("• *Nome:* ").append(Optional.ofNullable(viewModel.nomeProperty().get()).orElse("").toUpperCase())
                    .append("\n");
            sb.append("• *CPF:* ").append(Optional.ofNullable(viewModel.cpfProperty().get()).orElse("")).append("\n");
            sb.append("• *WhatsApp:* ").append(Optional.ofNullable(viewModel.telefoneProperty().get()).orElse(""))
                    .append("\n");

            if (viewModel.dataNascimentoProperty().get() != null) {
                sb.append("• *Data de Nascimento:* ").append(viewModel.dataNascimentoProperty().get().format(BR_DATE))
                        .append("\n");
            }

            log.trace("{} [NEGOCIO] Mapeando indicadores financeiros do proponente.", LOG_PREFIX);
            sb.append("\n*[PERFIL FINANCEIRO]*\n");
            sb.append("• *Vínculo:* ")
                    .append(viewModel.vinculoProperty().get() != null ? viewModel.vinculoProperty().get().getLabel()
                            : "Não informado")
                    .append("\n");
            sb.append("• *Matrícula:* ").append(Optional.ofNullable(viewModel.matriculaProperty().get())
                    .filter(s -> !s.isEmpty()).orElse("Não informada")).append("\n");
            sb.append("• *Renda Mensal:* R$ ")
                    .append(rendaFormatada == null || rendaFormatada.isEmpty() ? "0,00" : rendaFormatada).append("\n");

            sb.append("\n").append(SEPARADOR);
            sb.append("*Poder Financeiro - Consultoria e Soluções de Crédito*");

            log.info("{} [AUDITORIA] Relatório Markdown gerado com sucesso.", LOG_PREFIX);
            return sb.toString();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro crítico ao formatar relatório Markdown: {}", LOG_PREFIX, e.getMessage());
            return "Erro ao gerar relatório.";
        }
    }

    /**
     * Gera JSON contextual para o Gemini entender o cliente em atendimento.
     */
    public String gerarJsonContextualParaIA(ProponenteModel model, boolean permitirEndereco) {
        log.debug("{} [TELEMETRIA] Iniciando geração de JSON contextual para IA.", LOG_PREFIX);

        if (model == null) {
            log.warn("{} [NEGOCIO] ProponenteModel nulo. Retornando objeto vazio.", LOG_PREFIX);
            return "{}";
        }

        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("clienteEmAtendimento", model);

            log.trace("{} [NEGOCIO] Inclusão de endereço residencial: {}", LOG_PREFIX, permitirEndereco);
            root.put("enderecoResidencial",
                    permitirEndereco ? extrairEnderecoPrincipal(model) : "Ocultado por segurança");

            String json = objectMapper.writeValueAsString(root);
            log.info("{} [AUDITORIA] JSON contextual gerado com sucesso para o proponente ID: {}", LOG_PREFIX,
                    model.getId());
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha na serialização do contexto IA: {}", LOG_PREFIX, e.getMessage());
            return "{}";
        }
    }

    /**
     * Gera JSON de tabelas de juros para o AjudaChatFacadeImpl.
     */
    public String gerarJsonTabelasJuros(List<TabelaJurosModel> tabelas) {
        log.debug("{} [TELEMETRIA] Serializando lista de tabelas de juros para JSON.", LOG_PREFIX);

        if (tabelas == null || tabelas.isEmpty()) {
            log.debug("{} [NEGOCIO] Lista de tabelas vazia ou nula.", LOG_PREFIX);
            return "[]";
        }

        try {
            String json = objectMapper.writeValueAsString(tabelas);
            log.info("{} [AUDITORIA] JSON de {} tabelas gerado com sucesso.", LOG_PREFIX, tabelas.size());
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar tabelas de juros: {}", LOG_PREFIX, e.getMessage());
            return "[]";
        }
    }

    /**
     * Gera JSON de links úteis para o AjudaChatFacadeImpl.
     */
    public String gerarJsonLinksUteis(List<LinkUtilModel> links) {
        log.debug("{} [TELEMETRIA] Serializando lista de links úteis para JSON.", LOG_PREFIX);

        if (links == null || links.isEmpty()) {
            log.debug("{} [NEGOCIO] Lista de links vazia ou nula.", LOG_PREFIX);
            return "[]";
        }

        try {
            String json = objectMapper.writeValueAsString(links);
            log.info("{} [AUDITORIA] JSON de {} links gerado com sucesso.", LOG_PREFIX, links.size());
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar links úteis: {}", LOG_PREFIX, e.getMessage());
            return "[]";
        }
    }

    /**
     * Gera JSON de comissões para o AjudaChatFacadeImpl.
     */
    public String gerarJsonComissoes(List<ComissaoModel> comissoes) {
        log.debug("{} [TELEMETRIA] Serializando lista de comissões para JSON.", LOG_PREFIX);

        if (comissoes == null || comissoes.isEmpty()) {
            log.debug("{} [NEGOCIO] Lista de comissões vazia ou nula.", LOG_PREFIX);
            return "[]";
        }

        try {
            String json = objectMapper.writeValueAsString(comissoes);
            log.info("{} [AUDITORIA] JSON de {} comissões gerado com sucesso.", LOG_PREFIX, comissoes.size());
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar comissões: {}", LOG_PREFIX, e.getMessage());
            return "[]";
        }
    }

    /**
     * Gera JSON de uma proposta específica para análise da IA.
     */
    public String gerarJsonPropostaParaIA(PropostaModel proposta) {
        log.debug("{} [TELEMETRIA] Serializando proposta individual para contexto IA.", LOG_PREFIX);

        if (proposta == null) {
            log.warn("{} [NEGOCIO] PropostaModel nula recebida.", LOG_PREFIX);
            return "{}";
        }

        try {
            String json = objectMapper.writeValueAsString(proposta);
            log.info("{} [AUDITORIA] JSON da proposta ID {} gerado com sucesso.", LOG_PREFIX, proposta.getId());
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar proposta ID {}: {}", LOG_PREFIX, proposta.getId(),
                    e.getMessage());
            return "{}";
        }
    }

    /**
     * Helper privado para extração de endereço principal.
     */
    private String extrairEnderecoPrincipal(ProponenteModel model) {
        log.trace("{} [NEGOCIO] Executando lógica de extração de endereço principal.", LOG_PREFIX);

        if (model.getEnderecos() == null || model.getEnderecos().isEmpty()) {
            log.debug("{} [NEGOCIO] Proponente sem endereços cadastrados.", LOG_PREFIX);
            return "Não cadastrado";
        }

        try {
            EnderecoProponenteModel end = model.getEnderecos().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getPrincipal()))
                    .findFirst()
                    .orElse(model.getEnderecos().get(0));

            String formatado = String.format("%s, %s - %s, %s/%s",
                    end.getLogradouro(),
                    end.getNumero() != null ? end.getNumero() : "S/N",
                    end.getBairro(),
                    end.getCidade(),
                    end.getUf() != null ? end.getUf().name() : "N/A");

            log.trace("{} [NEGOCIO] Endereço extraído e formatado com sucesso.", LOG_PREFIX);
            return formatado;
        } catch (Exception e) {
            log.warn("{} [SISTEMA] Falha ao formatar endereço: {}", LOG_PREFIX, e.getMessage());
            return "Erro na formatação do endereço";
        }
    }
}
