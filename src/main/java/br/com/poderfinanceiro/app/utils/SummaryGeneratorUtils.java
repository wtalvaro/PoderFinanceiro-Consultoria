package br.com.poderfinanceiro.app.utils;

import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import java.time.format.DateTimeFormatter;

/**
 * Especializada em transformar o estado da ViewModel em texto formatado.
 */
public class SummaryGeneratorUtils {

    private static final String SEPARADOR = "————————————————————————————\n";

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
        sb.append("• *Nome:* ").append(vm.nomeProperty().get().toUpperCase()).append("\n");
        sb.append("• *CPF:* ").append(vm.cpfProperty().get()).append("\n");
        sb.append("• *WhatsApp:* ").append(vm.telefoneProperty().get()).append("\n");

        if (vm.dataNascimentoProperty().get() != null) {
            String dataNasc = vm.dataNascimentoProperty().get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            sb.append("• *Data de Nascimento:* ").append(dataNasc).append("\n");
        }
    }

    private static void appendPerfilFinanceiro(StringBuilder sb, LeadViewModel vm, String renda) {
        sb.append("\n*[PERFIL FINANCEIRO]*\n");
        sb.append("• *Convênio:* ")
                .append(vm.convenioProperty().get() != null ? vm.convenioProperty().get().getLabel() : "A definir")
                .append("\n");
        sb.append("• *Vínculo:* ")
                .append(vm.vinculoProperty().get() == null ? "Não informado" : vm.vinculoProperty().get().getLabel())
                .append("\n");
        sb.append("• *Matrícula:* ")
                .append(vm.matriculaProperty().get().isEmpty() ? "Não informada" : vm.matriculaProperty().get())
                .append("\n");
        sb.append("• *Renda Mensal:* R$ ").append(renda.isEmpty() ? "0,00" : renda).append("\n");
    }
}