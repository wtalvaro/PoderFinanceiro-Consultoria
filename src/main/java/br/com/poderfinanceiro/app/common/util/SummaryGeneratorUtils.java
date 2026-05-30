package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.presentation.viewmodel.LeadViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utilitário de Geração de Contexto e Resumos.
 * Centraliza a transformação de modelos para Markdown (WhatsApp)
 * e JSON (Alimentação de Contexto para IA Gemini).
 */
public final class SummaryGeneratorUtils {

    private static final Logger log = LoggerFactory.getLogger(SummaryGeneratorUtils.class);
    private static final String LOG_PREFIX = "[SummaryGeneratorUtils]";
    private static final String SEPARADOR = "————————————————————————————\n";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ObjectMapper configurado com suporte nativo a datas Java 8+
    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private SummaryGeneratorUtils() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada.");
    }

    static {
        log.info("{} [SISTEMA] Gerador de Sumários inicializado com suporte a JSR-310 (JavaTimeModule).", LOG_PREFIX);
    }

    /**
     * Gera relatório formatado em Markdown para o AtendimentoHubController.
     * Restaurado nome original 'gerar' para compatibilidade de contrato.
     */
    public static String gerar(LeadViewModel viewModel, String rendaFormatada) {
        log.debug("{} [TELEMETRIA] Iniciando geração de relatório Markdown (gerar).", LOG_PREFIX);

        if (viewModel == null) {
            log.warn("{} [NEGOCIO] Falha: ViewModel nulo ao tentar gerar relatório.", LOG_PREFIX);
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📑 *RELATÓRIO DE QUALIFICAÇÃO - PODER FINANCEIRO*\n");
        sb.append(SEPARADOR);

        log.trace("{} [NEGOCIO] Mapeando dados do proponente.", LOG_PREFIX);
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

        log.trace("{} [NEGOCIO] Mapeando perfil financeiro.", LOG_PREFIX);
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
    }

    /**
     * Gera JSON contextual para o Gemini entender o cliente em atendimento.
     */
    public static String gerarJsonContextualParaIA(ProponenteModel model, boolean permitirEndereco) {
        log.debug("{} [TELEMETRIA] Gerando JSON contextual para IA. Proponente ID: {}", LOG_PREFIX,
                model != null ? model.getId() : "NULL");

        if (model == null) {
            log.warn("{} [NEGOCIO] Model nulo recebido para contexto IA.", LOG_PREFIX);
            return "{}";
        }

        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("clienteEmAtendimento", model); // Jackson usa JavaTimeModule para as datas aqui
            root.put("enderecoResidencial",
                    permitirEndereco ? extrairEnderecoPrincipal(model) : "Ocultado por segurança");

            String json = jsonMapper.writeValueAsString(root);
            log.info("{} [AUDITORIA] JSON contextual gerado com sucesso.", LOG_PREFIX);
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao gerar JSON contextual: {}", LOG_PREFIX, e.getMessage());
            return "{}";
        }
    }

    /**
     * Gera JSON de tabelas de juros para o AjudaChatFacadeImpl.
     */
    public static String gerarJsonTabelasJuros(List<TabelaJurosModel> tabelas) {
        log.debug("{} [TELEMETRIA] Gerando JSON de tabelas de juros. Qtd: {}", LOG_PREFIX,
                tabelas != null ? tabelas.size() : 0);

        if (tabelas == null || tabelas.isEmpty())
            return "[]";
        try {
            String json = jsonMapper.writeValueAsString(tabelas);
            log.info("{} [AUDITORIA] JSON de tabelas gerado com sucesso.", LOG_PREFIX);
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar tabelas: {}", LOG_PREFIX, e.getMessage());
            return "[]";
        }
    }

    /**
     * Gera JSON de links úteis para o AjudaChatFacadeImpl.
     */
    public static String gerarJsonLinksUteis(List<LinkUtilModel> links) {
        log.debug("{} [TELEMETRIA] Gerando JSON de links úteis. Qtd: {}", LOG_PREFIX, links != null ? links.size() : 0);

        if (links == null || links.isEmpty())
            return "[]";
        try {
            String json = jsonMapper.writeValueAsString(links);
            log.info("{} [AUDITORIA] JSON de links gerado com sucesso.", LOG_PREFIX);
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar links: {}", LOG_PREFIX, e.getMessage());
            return "[]";
        }
    }

    /**
     * Gera JSON de comissões para o AjudaChatFacadeImpl.
     */
    public static String gerarJsonComissoes(List<ComissaoModel> comissoes) {
        log.debug("{} [TELEMETRIA] Gerando JSON de comissões. Qtd: {}", LOG_PREFIX,
                comissoes != null ? comissoes.size() : 0);

        if (comissoes == null || comissoes.isEmpty())
            return "[]";
        try {
            String json = jsonMapper.writeValueAsString(comissoes);
            log.info("{} [AUDITORIA] JSON de comissões gerado com sucesso.", LOG_PREFIX);
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar comissões: {}", LOG_PREFIX, e.getMessage());
            return "[]";
        }
    }

    /**
     * Gera JSON de uma proposta específica para análise da IA.
     */
    public static String gerarJsonPropostaParaIA(PropostaModel proposta) {
        log.debug("{} [TELEMETRIA] Gerando JSON de proposta para IA. ID: {}", LOG_PREFIX,
                proposta != null ? proposta.getId() : "NULL");

        if (proposta == null)
            return "{}";
        try {
            String json = jsonMapper.writeValueAsString(proposta);
            log.info("{} [AUDITORIA] JSON de proposta gerado com sucesso.", LOG_PREFIX);
            return json;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao serializar proposta: {}", LOG_PREFIX, e.getMessage());
            return "{}";
        }
    }

    /**
     * Helper privado para extração de endereço principal.
     */
    private static String extrairEnderecoPrincipal(ProponenteModel model) {
        log.trace("{} [NEGOCIO] Extraindo endereço principal para sumário.", LOG_PREFIX);

        if (model.getEnderecos() == null || model.getEnderecos().isEmpty()) {
            return "Não cadastrado";
        }

        EnderecoProponenteModel end = model.getEnderecos().stream()
                .filter(e -> Boolean.TRUE.equals(e.getPrincipal()))
                .findFirst()
                .orElse(model.getEnderecos().get(0));

        return String.format("%s, %s - %s, %s/%s",
                end.getLogradouro(),
                end.getNumero() != null ? end.getNumero() : "S/N",
                end.getBairro(),
                end.getCidade(),
                end.getUf() != null ? end.getUf().name() : "N/A");
    }
}
