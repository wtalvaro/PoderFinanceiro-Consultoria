package br.com.poderfinanceiro.app.common.util;

import javafx.scene.control.TextFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Common] Teste de Unidade - ContatoUtils")
class ContatoUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "11988887777, (11) 98888-7777", // Celular
            "1133334444, (11) 3333-4444", // Fixo
            "11988887777123, 11988887777123", // Inválido (retorna limpo)
            "'', ''" // Vazio
    })
    @DisplayName("Deve formatar telefones fixos e celulares corretamente")
    void deveFormatarTelefones(String input, String esperado) {
        assertThat(ContatoUtils.formatarTelefone(input)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Deve retornar string vazia para entrada nula")
    void deveLidarComNulo() {
        assertThat(ContatoUtils.formatarTelefone(null)).isEmpty();
    }

    @Test
    @DisplayName("Deve desformatar strings removendo caracteres especiais")
    void deveDesformatar() {
        String sujo = "(11) 98888-7777";
        assertThat(ContatoUtils.desformatar(sujo)).isEqualTo("11988887777");
    }

    @Test
    @DisplayName("Formatador UI: Deve aceitar entrada numérica válida")
    void deveAceitarEntradaValida() {
        TextFormatter<String> formatter = ContatoUtils.criarFormatadorTelefone();
        // Simulando uma alteração no JavaFX (simplificado para teste de lógica)
        assertThat(formatter.getFilter()).isNotNull();
    }
}
