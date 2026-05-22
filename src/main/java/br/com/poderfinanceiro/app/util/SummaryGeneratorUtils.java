package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class SummaryGeneratorUtils {

    private static final String SEPARADOR = "————————————————————————————\n";
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static String gerar(LeadViewModel viewModel, String rendaFormatada) {
        StringBuilder sb = new StringBuilder();
        sb.append("📑 *RELATÓRIO DE QUALIFICAÇÃO - PODER FINANCEIRO*\n");
        sb.append(SEPARADOR);
        appendDadosPessoais(sb, viewModel);
        appendPerfilFinanceiro(sb, viewModel, rendaFormatada);
        sb.append("\n").append(SEPARADOR);
        sb.append("*Poder Financeiro - Consultoria e Soluções de Crédito*");
        return sb.toString();
    }

    private static void appendDadosPessoais(StringBuilder sb, LeadViewModel vm) {
        sb.append("*[DADOS DO PROPONENTE]*\n");
        sb.append("• *Nome:* ").append(vm.nomeProperty().get() == null ? "" : vm.nomeProperty().get().toUpperCase())
                .append("\n");
        sb.append("• *CPF:* ").append(vm.cpfProperty().get() == null ? "" : vm.cpfProperty().get()).append("\n");
        sb.append("• *WhatsApp:* ").append(vm.telefoneProperty().get() == null ? "" : vm.telefoneProperty().get())
                .append("\n");

        if (vm.dataNascimentoProperty().get() != null) {
            String dataNasc = vm.dataNascimentoProperty().get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            sb.append("• *Data de Nascimento:* ").append(dataNasc).append("\n");
        }
    }

    private static void appendPerfilFinanceiro(StringBuilder sb, LeadViewModel vm, String renda) {
        sb.append("\n*[PERFIL FINANCEIRO]*\n");
        sb.append("• *Vínculo:* ")
                .append(vm.vinculoProperty().get() == null ? "Não informado" : vm.vinculoProperty().get().getLabel())
                .append("\n");
        sb.append("• *Matrícula:* ")
                .append(vm.matriculaProperty().get() == null || vm.matriculaProperty().get().isEmpty() ? "Não informada"
                        : vm.matriculaProperty().get())
                .append("\n");
        sb.append("• *Renda Mensal:* R$ ").append(renda == null || renda.isEmpty() ? "0,00" : renda).append("\n");
    }

    public static String gerarJsonContextualParaIA(br.com.poderfinanceiro.app.domain.model.ProponenteModel model,
            boolean permitirEndereco) {
        if (model == null)
            return "{}";
        try {
            Map<String, Object> contextoGlobal = new LinkedHashMap<>();
            Map<String, Object> dadosPessoais = new LinkedHashMap<>();
            dadosPessoais.put("nome", model.getNomeCompleto());
            dadosPessoais.put("cpf", model.getCpf());
            dadosPessoais.put("whatsapp", model.getTelefone());
            if (model.getDataNascimento() != null)
                dadosPessoais.put("dataNascimento", model.getDataNascimento().toString());

            String enderecoFormatado = "Ocultado (Abra a aba de detalhes do cliente para liberar)";
            if (permitirEndereco) {
                enderecoFormatado = "Não cadastrado";
                if (model.getEnderecos() != null && !model.getEnderecos().isEmpty()) {
                    var end = model.getEnderecos().stream().filter(e -> e.getPrincipal() != null && e.getPrincipal())
                            .findFirst().orElse(model.getEnderecos().get(0));
                    String logradouro = end.getLogradouro() != null ? end.getLogradouro().trim() : "";
                    String numero = end.getNumero() != null ? end.getNumero().trim() : "";
                    String bairro = end.getBairro() != null ? end.getBairro().trim() : "";
                    String cidade = end.getCidade() != null ? end.getCidade().trim() : "";
                    String uf = end.getUf() != null ? end.getUf().name() : "";
                    String cep = end.getCep() != null ? end.getCep().trim() : "";

                    if (!logradouro.isEmpty() || !cidade.isEmpty()) {
                        enderecoFormatado = String.format("%s, %s%s%s%s",
                                logradouro.isEmpty() ? "Logradouro não informado" : logradouro,
                                numero.isEmpty() ? "S/N" : "nº " + numero,
                                bairro.isEmpty() ? "" : " - " + bairro,
                                cidade.isEmpty() ? "" : " - " + cidade + (uf.isEmpty() ? "" : "/" + uf.toUpperCase()),
                                cep.isEmpty() ? "" : " (CEP: " + cep + ")");
                    }
                }
            }

            Map<String, Object> perfilFinanceiro = new LinkedHashMap<>();
            perfilFinanceiro.put("vinculo",
                    model.getTipoVinculo() != null ? model.getTipoVinculo().name() : "NAO_INFORMADO");
            perfilFinanceiro.put("matricula", model.getMatricula());
            perfilFinanceiro.put("rendaMensalBruta",
                    model.getRendaMensal() != null ? model.getRendaMensal().doubleValue() : 0.0);

            contextoGlobal.put("clienteEmAtendimento", dadosPessoais);
            contextoGlobal.put("enderecoResidencial", enderecoFormatado);
            contextoGlobal.put("perfilFinanceiro", perfilFinanceiro);

            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextoGlobal);
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao serializar contexto completo: " + e.getMessage());
            return "{}";
        }
    }

    // 🚀 NOVO GERADOR: Exclusivo para as propostas na Esteira
    public static String gerarJsonPropostaParaIA(br.com.poderfinanceiro.app.domain.model.PropostaModel proposta) {
        if (proposta == null)
            return "{}";
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("contexto", "O operador está na Esteira de Crédito avaliando esta Proposta específica:");

            // Dados do Cliente associado à proposta
            if (proposta.getProponente() != null) {
                Map<String, Object> dadosPessoais = new LinkedHashMap<>();
                dadosPessoais.put("nome", proposta.getProponente().getNomeCompleto());
                dadosPessoais.put("cpf", proposta.getProponente().getCpf());
                dadosPessoais.put("rendaMensal", proposta.getProponente().getRendaMensal());
                map.put("cliente_associado", dadosPessoais);
            }

            // Dados profundos da Proposta
            Map<String, Object> detalhesProposta = new LinkedHashMap<>();
            detalhesProposta.put("id_proposta", proposta.getId() != null ? proposta.getId() : "Nova (Não salva)");
            detalhesProposta.put("status_atual",
                    proposta.getStatus() != null ? proposta.getStatus().name() : "Desconhecido");
            detalhesProposta.put("convenio",
                    proposta.getConvenioOrgao() != null ? proposta.getConvenioOrgao().name() : "N/A");
            detalhesProposta.put("banco_parceiro",
                    proposta.getBanco() != null ? proposta.getBanco().getNome() : "Não definido");
            detalhesProposta.put("valor_solicitado", proposta.getValorSolicitado());
            detalhesProposta.put("valor_aprovado", proposta.getValorAprovado());
            detalhesProposta.put("quantidade_parcelas", proposta.getQuantidadeParcelas());
            detalhesProposta.put("valor_parcela", proposta.getValorParcela());
            detalhesProposta.put("comissao_estimada", proposta.getComissaoEstimada());
            detalhesProposta.put("observacoes_operador", proposta.getObservacoes());

            map.put("detalhes_da_proposta", detalhesProposta);

            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            System.err.println("⚠️ Falha ao processar proposta para o Gemini: " + e.getMessage());
            return "{}";
        }
    }

    public static String gerarJsonTabelasJuros(
            java.util.List<br.com.poderfinanceiro.app.domain.model.TabelaJurosModel> tabelas) {
        if (tabelas == null || tabelas.isEmpty())
            return "[]";
        try {
            java.util.List<Map<String, Object>> listaTabelas = new java.util.ArrayList<>();
            for (br.com.poderfinanceiro.app.domain.model.TabelaJurosModel t : tabelas) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("banco", t.getBanco() != null ? t.getBanco().getNome() : "Desconhecido");
                map.put("convenio", t.getTipoConvenio() != null ? t.getTipoConvenio().name() : "GERAL");
                map.put("taxaMensalBase", t.getTaxaMensal());
                map.put("comissaoPercentual", t.getComissaoPercentual());
                listaTabelas.add(map);
            }
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listaTabelas);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static String gerarJsonLinksUteis(java.util.List<br.com.poderfinanceiro.app.domain.model.LinkUtilModel> links) {
        if (links == null || links.isEmpty())
            return "[]";
        try {
            java.util.List<Map<String, Object>> listaLinks = new java.util.ArrayList<>();
            for (br.com.poderfinanceiro.app.domain.model.LinkUtilModel l : links) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("categoria", l.getCategoria() != null ? l.getCategoria().getLabel() : "OUTROS");
                map.put("titulo", l.getTitulo());
                map.put("tags", l.getTags());
                listaLinks.add(map);
            }
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listaLinks);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static String gerarJsonComissoes(java.util.List<br.com.poderfinanceiro.app.domain.model.ComissaoModel> comissoes) {
        if (comissoes == null || comissoes.isEmpty())
            return "[]";
        try {
            java.util.List<Map<String, Object>> listaComissoes = new java.util.ArrayList<>();
            for (br.com.poderfinanceiro.app.domain.model.ComissaoModel c : comissoes) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("comissaoId", c.getId());
                map.put("cliente", c.getProposta().getProponente().getNomeCompleto());
                map.put("banco", c.getProposta().getBanco().getNome());
                map.put("valorBruto",
                        c.getValorBrutoComissao() != null ? c.getValorBrutoComissao().doubleValue() : 0.0);
                map.put("valorLiquidoConsultor",
                        c.getValorLiquidoConsultor() != null ? c.getValorLiquidoConsultor().doubleValue() : 0.0);
                map.put("valorPagoPelaPoder",
                        c.getValorPagoPelaPoder() != null ? c.getValorPagoPelaPoder().doubleValue() : 0.0);
                map.put("statusPagamento", c.getStatusPagamento() != null ? c.getStatusPagamento() : "Pendente");
                map.put("cicloReferencia", c.getCicloReferencia() != null ? c.getCicloReferencia() : "Legado");
                map.put("contestada", c.isContestada());
                map.put("verificadoConsultor", c.isVerificadoConsultor());

                if (c.getDataRecebimentoBanco() != null)
                    map.put("dataRecebimentoBanco", c.getDataRecebimentoBanco().toString());
                if (c.getPrevisaoPagamento() != null)
                    map.put("previsaoPagamento", c.getPrevisaoPagamento().toString());
                if (c.getDataLimiteContestacao() != null)
                    map.put("dataLimiteContestacao", c.getDataLimiteContestacao().toString());
                if (c.getObservacaoAjuste() != null && !c.getObservacaoAjuste().isBlank()) {
                    map.put("observacoesAuditoria", c.getObservacaoAjuste());
                }
                listaComissoes.add(map);
            }
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listaComissoes);
        } catch (Exception e) {
            System.err.println("⚠️ Falha ao processar metadados de repasses para o Gemini: " + e.getMessage());
            return "[]";
        }
    }
}