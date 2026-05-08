package br.com.poderfinanceiro.app.utils;

import br.com.poderfinanceiro.app.viewmodel.EnderecoViewModel;
import javafx.scene.control.TextFormatter;

/**
 * Especialista em regras de formatação e limpeza de dados de localização.
 */
public final class EnderecoUtils {

    private static final String REGEX_APENAS_NUMEROS = "[^0-9]";

    private EnderecoUtils() {
        throw new UnsupportedOperationException("Classe utilitária.");
    }

    /**
     * Remove pontos e traços do CEP para salvar no banco.
     */
    public static String limparCep(String cep) {
        if (cep == null)
            return "";
        return cep.replaceAll(REGEX_APENAS_NUMEROS, "");
    }

    /**
     * Aplica a máscara 00.000-000 para exibição.
     */
    public static String formatarCep(String cep) {
        String numeros = limparCep(cep);
        if (numeros.length() != 8)
            return numeros;
        return numeros.substring(0, 2) + "." + numeros.substring(2, 5) + "-" + numeros.substring(5);
    }

    /**
     * Monta uma String única e elegante com o endereço completo.
     * Útil para o resumo do WhatsApp ou etiquetas.
     */
    public static String montarEnderecoCompleto(EnderecoViewModel vm) {
        if (vm.logradouroProperty().get().isEmpty()) {
            return "Endereço não informado.";
        }

        StringBuilder sb = new StringBuilder();

        String tipo = vm.tipoLogradouroProperty().get() != null ? vm.tipoLogradouroProperty().get().toString() : "";
        String num = vm.numeroProperty().get().isEmpty() ? "S/N" : vm.numeroProperty().get();

        sb.append(tipo).append(" ").append(vm.logradouroProperty().get()).append(", ").append(num);

        if (!vm.complementoProperty().get().isEmpty()) {
            sb.append(" (").append(vm.complementoProperty().get()).append(")");
        }

        sb.append("\n• *Bairro:* ").append(vm.bairroProperty().get());
        sb.append("\n• *Cidade:* ").append(vm.cidadeProperty().get()).append(" - ").append(vm.ufProperty().get());
        sb.append("\n• *CEP:* ").append(formatarCep(vm.cepProperty().get()));

        return sb.toString();
    }

    /**
     * Cria o formatador para campos de texto JavaFX.
     */
    public static TextFormatter<String> criarFormatadorCep() {
        return new TextFormatter<>(change -> {
            if (!change.isContentChange())
                return change;

            String novoTexto = change.getControlNewText().replaceAll(REGEX_APENAS_NUMEROS, "");
            if (novoTexto.length() > 8)
                return null;

            String formatado = formatarCep(novoTexto);

            change.setRange(0, change.getControlText().length());
            change.setText(formatado);
            change.setCaretPosition(formatado.length());
            change.setAnchor(formatado.length());

            return change;
        });
    }
}