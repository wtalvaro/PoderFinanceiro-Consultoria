package br.com.poderfinanceiro.app.util;

import br.com.poderfinanceiro.app.viewmodel.EnderecoViewModel;
import javafx.scene.control.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Especialista em regras de formatação e limpeza de dados de localização.
 */
public final class EnderecoUtils {

    private static final Logger log = LoggerFactory.getLogger(EnderecoUtils.class);
    private static final String REGEX_APENAS_NUMEROS = "[^0-9]";

    private EnderecoUtils() {
        throw new UnsupportedOperationException("Classe utilitária.");
    }

    static {
        log.debug("[ENDERECO_UTILS] Classe utilitária carregada");
    }

    /**
     * Remove pontos e traços do CEP para salvar no banco.
     */
    public static String limparCep(String cep) {
        log.debug("[ENDERECO_UTILS] limparCep: cep original='{}'", cep);
        if (cep == null) {
            log.trace("[ENDERECO_UTILS] limparCep: cep nulo, retornando vazio");
            return "";
        }
        String limpo = cep.replaceAll(REGEX_APENAS_NUMEROS, "");
        log.info("[ENDERECO_UTILS] limparCep: CEP limpo='{}'", limpo);
        return limpo;
    }

    /**
     * Aplica a máscara 00.000-000 para exibição.
     */
    public static String formatarCep(String cep) {
        log.debug("[ENDERECO_UTILS] formatarCep: cep original='{}'", cep);
        String numeros = limparCep(cep);
        if (numeros.length() != 8) {
            log.warn("[ENDERECO_UTILS] formatarCep: CEP com tamanho inválido ({}) - retornando como está",
                    numeros.length());
            return numeros;
        }
        String formatado = numeros.substring(0, 2) + "." + numeros.substring(2, 5) + "-" + numeros.substring(5);
        log.info("[ENDERECO_UTILS] formatarCep: CEP formatado='{}'", formatado);
        return formatado;
    }

    /**
     * Monta uma String única e elegante com o endereço completo.
     * Útil para o resumo do WhatsApp ou etiquetas.
     */
    public static String montarEnderecoCompleto(EnderecoViewModel vm) {
        log.debug("[ENDERECO_UTILS] montarEnderecoCompleto: gerando resumo do endereço");
        if (vm == null) {
            log.warn("[ENDERECO_UTILS] ViewModel nulo, retornando 'Endereço não informado'");
            return "Endereço não informado.";
        }

        if (vm.logradouroProperty().get().isEmpty()) {
            log.debug("[ENDERECO_UTILS] Logradouro vazio, retornando 'Endereço não informado'");
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

        String resultado = sb.toString();
        log.info("[ENDERECO_UTILS] Endereço completo montado com sucesso (tamanho={})", resultado.length());
        return resultado;
    }

    /**
     * Cria o formatador para campos de texto JavaFX.
     */
    public static TextFormatter<String> criarFormatadorCep() {
        log.debug("[ENDERECO_UTILS] criarFormatadorCep: criando formatador de CEP");
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                log.trace("[ENDERECO_UTILS] Alteração não é de conteúdo, ignorando");
                return change;
            }

            String novoTexto = change.getControlNewText().replaceAll(REGEX_APENAS_NUMEROS, "");
            log.trace("[ENDERECO_UTILS] Texto após limpeza: '{}'", novoTexto);

            if (novoTexto.length() > 8) {
                log.trace("[ENDERECO_UTILS] Alteração rejeitada: excede 8 dígitos ({} dígitos)", novoTexto.length());
                return null;
            }

            String formatado = formatarCep(novoTexto);
            log.trace("[ENDERECO_UTILS] Texto formatado: '{}'", formatado);

            change.setRange(0, change.getControlText().length());
            change.setText(formatado);
            change.setCaretPosition(formatado.length());
            change.setAnchor(formatado.length());

            return change;
        });
        log.info("[ENDERECO_UTILS] Formatador de CEP criado com sucesso");
        return formatter;
    }
}