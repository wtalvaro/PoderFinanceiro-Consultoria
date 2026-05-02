package br.com.poderfinanceiro.app.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceiroUtils {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    /**
     * Transforma BigDecimal do banco em String formatada "2.500,00" para o
     * TextField.
     * Já limpa os caracteres invisíveis que o JavaFX rejeita.
     */
    public static String formatarParaExibicao(BigDecimal valor) {
        if (valor == null)
            return "";
        NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
        String bruto = nf.format(valor);
        return limparCaracteresInvalidos(bruto);
    }

    /**
     * Transforma a String da tela "2.500,00" em BigDecimal para o Banco/PostgreSQL.
     */
    public static BigDecimal extrairValorParaBanco(String texto) {
        if (texto == null || texto.trim().isEmpty())
            return null;

        try {
            // 1. Remove TUDO que não for dígito ou vírgula.
            // Isso elimina pontos de milhar, R$ e aquele espaço invisível de tamanho 9.
            String limpo = texto.replaceAll("[^\\d,]", "").replace(",", ".");

            // 2. Agora temos algo como "2500.00" pronto para o BigDecimal
            return new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            // Log para depuração caso algo muito estranho aconteça
            System.err.println("Erro ao converter valor para o banco: " + texto);
            return null;
        }
    }

    /**
     * Configura o comportamento completo de um TextField:
     * 1. Filtra entrada (apenas números, pontos e vírgulas)
     * 2. Formata ao perder o foco (Blur)
     */
    public static void configurarCampoMoeda(TextField campo) {
        // Filtro de digitação (TextFormatter)
        campo.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("([\\d\\.,]*)") ? change : null));

        // Formatação ao sair do campo
        campo.focusedProperty().addListener((obs, antigoFoco, novoFoco) -> {
            if (!novoFoco) {
                String texto = campo.getText().replaceAll("[^\\d]", "");
                if (!texto.isEmpty()) {
                    double valorNumerico = Double.parseDouble(texto) / 100.0;
                    NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
                    campo.setText(limparCaracteresInvalidos(nf.format(valorNumerico)));
                }
            }
        });
    }

    // Dentro de FinanceiroUtils.java

    /**
     * Transforma o CPF limpo (12345678901) em formatado (123.456.789-01)
     */
    public static String formatarCpf(String cpf) {
        if (cpf == null || cpf.replaceAll("[^0-9]", "").length() != 11) {
            return cpf; // Retorna original se não for um CPF válido
        }
        String limpo = cpf.replaceAll("[^0-9]", "");
        return String.format("%s.%s.%s-%s",
                limpo.substring(0, 3),
                limpo.substring(3, 6),
                limpo.substring(6, 9),
                limpo.substring(9, 11));
    }

    public static void configurarMascaraCpf(TextField campo) {
        campo.textProperty().addListener((obs, antigo, novo) -> {
            if (novo == null)
                return;

            // Remove tudo que não é número
            String apenasNumeros = novo.replaceAll("[^0-9]", "");

            // Limita a 11 dígitos
            if (apenasNumeros.length() > 11) {
                apenasNumeros = apenasNumeros.substring(0, 11);
            }

            // Aplica a formatação dinamicamente se o usuário não estiver apagando
            if (apenasNumeros.length() == 11 && !novo.equals(antigo)) {
                campo.setText(formatarCpf(apenasNumeros));
            }
        });
    }

    /**
     * Transforma 21988887777 em (21) 98888-7777
     */
    public static String formatarTelefone(String tel) {
        if (tel == null)
            return "";
        String limpo = tel.replaceAll("[^0-9]", "");

        if (limpo.length() == 11) { // Celular: (XX) 9XXXX-XXXX
            return String.format("(%s) %s-%s",
                    limpo.substring(0, 2), limpo.substring(2, 7), limpo.substring(7));
        } else if (limpo.length() == 10) { // Fixo: (XX) XXXX-XXXX
            return String.format("(%s) %s-%s",
                    limpo.substring(0, 2), limpo.substring(2, 6), limpo.substring(6));
        }
        return tel; // Se for fora do padrão, retorna como está
    }

    /**
     * Máscara para o TextField de telefone no LeadController
     */
    public static void configurarMascaraTelefone(TextField campo) {
        campo.focusedProperty().addListener((obs, antigo, novoFoco) -> {
            if (!novoFoco) { // Ao sair do campo (Blur)
                campo.setText(formatarTelefone(campo.getText()));
            }
        });
    }

    /**
     * O "Exorcista": Remove R$ e caracteres invisíveis (\u00A0) que quebram o
     * JavaFX.
     */
    private static String limparCaracteresInvalidos(String texto) {
        return texto.replaceAll("[^\\d\\.,]", "");
    }
}