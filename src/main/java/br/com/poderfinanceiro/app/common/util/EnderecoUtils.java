package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.presentation.viewmodel.EnderecoViewModel;
import javafx.scene.control.TextFormatter;
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
     * 
     * @param cep String original do CEP.
     * @return String contendo apenas os 8 dígitos.
     */
    public static String limparCep(String cep) {
        if (cep == null) {
            return "";
        }
        String limpo = cep.replaceAll(REGEX_APENAS_NUMEROS, "");
        log.trace("{} [TELEMETRIA] CEP limpo para persistência: {}", LOG_PREFIX, limpo);
        return limpo;
    }

    /**
     * Aplica a máscara visual {@code 00.000-000} para exibição em labels e tabelas.
     * 
     * @param cep String de 8 dígitos.
     * @return String formatada ou original se o tamanho for inválido.
     */
    public static String formatarCep(String cep) {
        String numeros = limparCep(cep);
        if (numeros.length() != 8) {
            log.debug("{} [NEGOCIO] CEP com tamanho inválido para máscara: {} dígitos", LOG_PREFIX, numeros.length());
            return numeros;
        }

        String formatado = String.format("%s.%s-%s",
                numeros.substring(0, 2),
                numeros.substring(2, 5),
                numeros.substring(5));

        log.trace("{} [TELEMETRIA] CEP formatado com sucesso: {}", LOG_PREFIX, formatado);
        return formatado;
    }

    /**
     * Monta uma representação textual rica do endereço para uso em resumos ou
     * integrações.
     * 
     * @param vm ViewModel contendo os dados do endereço.
     * @return String formatada com Markdown para WhatsApp/Relatórios.
     */
    public static String montarEnderecoCompleto(EnderecoViewModel vm) {
        log.debug("{} [TELEMETRIA] Iniciando montagem de endereço completo.", LOG_PREFIX);

        if (vm == null || vm.logradouroProperty().get() == null || vm.logradouroProperty().get().isBlank()) {
            log.warn("{} [NEGOCIO] Tentativa de montar endereço com ViewModel nulo ou incompleto.", LOG_PREFIX);
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

        String resultado = sb.toString();
        log.info("{} [AUDITORIA] Endereço completo gerado com sucesso.", LOG_PREFIX);
        return resultado;
    }

    /**
     * Cria um TextFormatter para campos de CEP no JavaFX com máscara em tempo real.
     * 
     * @return TextFormatter configurado para CEP.
     */
    public static TextFormatter<String> criarFormatadorCep() {
        log.debug("{} [SISTEMA] Criando formatador de CEP para UI.", LOG_PREFIX);

        return new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String novoTexto = change.getControlNewText().replaceAll(REGEX_APENAS_NUMEROS, "");

            if (novoTexto.length() > 8) {
                log.trace("{} [UI] Entrada de CEP bloqueada: excede 8 dígitos.", LOG_PREFIX);
                return null;
            }

            // Aplica a formatação visual enquanto o usuário digita
            String formatado = formatarCep(novoTexto);

            change.setRange(0, change.getControlText().length());
            change.setText(formatado);
            change.setCaretPosition(formatado.length());
            change.setAnchor(formatado.length());

            return change;
        });
    }
}
