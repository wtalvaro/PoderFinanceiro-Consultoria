package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.presentation.viewmodel.EnderecoViewModel;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Especialista em regras de formatação, limpeza e montagem de dados de
 * localização.
 * Provê suporte a máscaras dinâmicas para JavaFX e normalização para
 * persistência.
 */
public final class EnderecoUtils {

    private static final Logger log = LoggerFactory.getLogger(EnderecoUtils.class);
    private static final String LOG_PREFIX = "[EnderecoUtils]";
    private static final String REGEX_APENAS_NUMEROS = "[^0-9]";

    private EnderecoUtils() {
        throw new UnsupportedOperationException("Esta é uma classe utilitária e não pode ser instanciada.");
    }

    static {
        log.info("{} [SISTEMA] Utilitário de endereços inicializado.", LOG_PREFIX);
    }

    /**
     * Remove qualquer caractere não numérico do CEP para persistência.
     */
    public static String limparCep(String cep) {
        if (cep == null || cep.isBlank()) {
            return "";
        }
        return cep.replaceAll(REGEX_APENAS_NUMEROS, "");
    }

    /**
     * Aplica a máscara visual 00.000-000.
     */
    public static String formatarCep(String cep) {
        String numeros = limparCep(cep);
        if (numeros.length() != 8) {
            return numeros;
        }
        return String.format("%s.%s-%s",
                numeros.substring(0, 2),
                numeros.substring(2, 5),
                numeros.substring(5));
    }

    /**
     * Cria um TextFormatter robusto para CEP.
     * A valueProperty conterá o valor limpo (8 dígitos).
     * O TextField exibirá o valor formatado.
     */
    public static TextFormatter<String> criarFormatadorCep() {
        log.debug("{} [SISTEMA] Criando formatador de CEP com StringConverter.", LOG_PREFIX);

        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String valorLimpo) {
                if (valorLimpo == null || valorLimpo.isBlank())
                    return "";
                return formatarCep(valorLimpo);
            }

            @Override
            public String fromString(String valorComMascara) {
                return limparCep(valorComMascara);
            }
        };

        return new TextFormatter<>(converter, "", change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String novoTexto = change.getControlNewText().replaceAll(REGEX_APENAS_NUMEROS, "");
            if (novoTexto.length() > 8) {
                log.trace("{} [UI] Entrada de CEP bloqueada: excede 8 dígitos.", LOG_PREFIX);
                return null;
            }

            return change;
        });
    }

    /**
     * Monta uma representação textual rica do endereço.
     */
    public static String montarEnderecoCompleto(EnderecoViewModel vm) {
        log.debug("{} [TELEMETRIA] Iniciando montagem de endereço completo.", LOG_PREFIX);

        if (vm == null || vm.logradouroProperty().get() == null || vm.logradouroProperty().get().isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de montar endereço incompleto.", LOG_PREFIX);
            return "Endereço não informado.";
        }

        StringBuilder sb = new StringBuilder();
        String tipo = vm.tipoLogradouroProperty().get() != null ? vm.tipoLogradouroProperty().get().toString() : "";
        String num = (vm.numeroProperty().get() == null || vm.numeroProperty().get().isEmpty()) ? "S/N"
                : vm.numeroProperty().get();

        sb.append(tipo).append(" ").append(vm.logradouroProperty().get()).append(", ").append(num);

        if (vm.complementoProperty().get() != null && !vm.complementoProperty().get().isEmpty()) {
            sb.append(" (").append(vm.complementoProperty().get()).append(")");
        }

        sb.append("\n• *Bairro:* ").append(vm.bairroProperty().get());
        sb.append("\n• *Cidade:* ").append(vm.cidadeProperty().get()).append(" - ").append(vm.ufProperty().get());
        sb.append("\n• *CEP:* ").append(formatarCep(vm.cepProperty().get()));

        log.info("{} [AUDITORIA] Endereço completo gerado com sucesso.", LOG_PREFIX);
        return sb.toString();
    }
}
