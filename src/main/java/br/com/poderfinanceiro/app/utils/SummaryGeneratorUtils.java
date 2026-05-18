package br.com.poderfinanceiro.app.utils;

import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Especializada em transformar o estado da ViewModel em texto formatado ou JSON
 * contextual para a IA.
 */
public class SummaryGeneratorUtils {

    private static final String SEPARADOR = "————————————————————————————\n";

    // 1. Instância única e limpa do Jackson (Sem dependências extras de Data)
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    // =========================================================================
    // MÉTODOS DE RELATÓRIO EM TEXTO (ANTIGOS MANTIDOS INTACTOS)
    // =========================================================================

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

    // =========================================================================
    // 🚀 NOVO MOTOR CORRIGIDO: OPERA DIRETAMENTE SOBRE O PROPONENTE_MODEL
    // =========================================================================
    public static String gerarJsonContextualParaIA(br.com.poderfinanceiro.app.model.ProponenteModel model,
            boolean permitirEndereco) {
        if (model == null)
            return "{}";

        try {
            Map<String, Object> contextoGlobal = new LinkedHashMap<>();

            // 1. Bloco de Dados Pessoais
            Map<String, Object> dadosPessoais = new LinkedHashMap<>();
            dadosPessoais.put("nome", model.getNomeCompleto());
            dadosPessoais.put("cpf", model.getCpf());
            dadosPessoais.put("whatsapp", model.getTelefone());
            if (model.getDataNascimento() != null) {
                dadosPessoais.put("dataNascimento", model.getDataNascimento().toString());
            }

            // 2. Bloco de Endereço Residencial
            String enderecoFormatado = "Ocultado (Abra a aba de detalhes do cliente para liberar)";
            if (permitirEndereco) {
                enderecoFormatado = "Não cadastrado";
                if (model.getEnderecos() != null && !model.getEnderecos().isEmpty()) {
                    var end = model.getEnderecos().stream()
                            .filter(e -> e.getPrincipal() != null && e.getPrincipal())
                            .findFirst()
                            .orElse(model.getEnderecos().get(0));

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

            // 3. Bloco Financeiro
            Map<String, Object> perfilFinanceiro = new LinkedHashMap<>();
            perfilFinanceiro.put("vinculo",
                    model.getTipoVinculo() != null ? model.getTipoVinculo().name() : "NAO_INFORMADO");
            perfilFinanceiro.put("matricula", model.getMatricula());
            perfilFinanceiro.put("rendaMensalBruta",
                    model.getRendaMensal() != null ? model.getRendaMensal().doubleValue() : 0.0);

            // 🎯 4. NOVO BLOCO: HISTÓRICO DE PROPOSTAS DO CLIENTE (Otimizado para Tokens)
            java.util.List<Map<String, Object>> arrayPropostas = new java.util.ArrayList<>();
            if (model.getPropostas() != null && !model.getPropostas().isEmpty()) {
                for (br.com.poderfinanceiro.app.model.PropostaModel prop : model.getPropostas()) {
                    Map<String, Object> pMap = new LinkedHashMap<>();
                    pMap.put("id", prop.getId());
                    pMap.put("banco", prop.getBanco() != null ? prop.getBanco().getNome() : "Não informado");
                    pMap.put("convenio", prop.getConvenioOrgao() != null ? prop.getConvenioOrgao().getLabel() : "PADRAO");
                    pMap.put("status", prop.getStatus() != null ? prop.getStatus().name() : "EM_ANALISE");
                    pMap.put("valorSolicitado",
                            prop.getValorSolicitado() != null ? prop.getValorSolicitado().doubleValue() : 0.0);
                    pMap.put("valorAprovado",
                            prop.getValorAprovado() != null ? prop.getValorAprovado().doubleValue() : 0.0);
                    pMap.put("parcelas", prop.getQuantidadeParcelas());
                    pMap.put("valorParcela",
                            prop.getValorParcela() != null ? prop.getValorParcela().doubleValue() : 0.0);
                    pMap.put("comissaoEstimada",
                            prop.getComissaoEstimada() != null ? prop.getComissaoEstimada().doubleValue() : 0.0);

                    arrayPropostas.add(pMap);
                }
            }

            // Monta a árvore consolidada
            contextoGlobal.put("clienteEmAtendimento", dadosPessoais);
            contextoGlobal.put("enderecoResidencial", enderecoFormatado);
            contextoGlobal.put("perfilFinanceiro", perfilFinanceiro);
            contextoGlobal.put("propostasDesteCliente", arrayPropostas); // 🚀 Injetado!

            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextoGlobal);

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao serializar contexto completo: " + e.getMessage());
            return "{}";
        }
    }

    // =========================================================================
    // MÓDULOS DE CONHECIMENTO GLOBAL (Bancos, Taxas e Links)
    // =========================================================================

    public static String gerarJsonTabelasJuros(
            java.util.List<br.com.poderfinanceiro.app.model.TabelaJurosModel> tabelas) {
        if (tabelas == null || tabelas.isEmpty())
            return "[]";
        try {
            java.util.List<Map<String, Object>> listaTabelas = new java.util.ArrayList<>();

            for (br.com.poderfinanceiro.app.model.TabelaJurosModel t : tabelas) {
                Map<String, Object> map = new LinkedHashMap<>();
                // Adapte os 'getters' abaixo caso o nome exato na sua classe seja diferente
                map.put("banco", t.getBanco() != null ? t.getBanco().getNome() : "Desconhecido");
                map.put("convenio", t.getTipoConvenio() != null ? t.getTipoConvenio().name() : "GERAL");
                map.put("taxaMensalBase", t.getTaxaMensal());
                listaTabelas.add(map);
            }
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listaTabelas);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static String gerarJsonLinksUteis(java.util.List<br.com.poderfinanceiro.app.model.LinkUtilModel> links) {
        if (links == null || links.isEmpty())
            return "[]";
        try {
            java.util.List<Map<String, Object>> listaLinks = new java.util.ArrayList<>();

            for (br.com.poderfinanceiro.app.model.LinkUtilModel l : links) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("categoria", l.getCategoria() != null ? l.getCategoria().getLabel() : "OUTROS");
                map.put("titulo", l.getTitulo());
                map.put("tags", l.getTags());
                // Não enviamos a URL completa para poupar tokens, a IA só precisa saber que o
                // link existe e onde está
                listaLinks.add(map);
            }
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listaLinks);
        } catch (Exception e) {
            return "[]";
        }
    }
}