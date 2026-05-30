package br.com.poderfinanceiro.app.common.util;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Common] Teste de Unidade - FinanceiroUtils")
class FinanceiroUtilsTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1234.56, '1.234,56'",
            "1000000, '1.000.000,00'",
            "0, '0,00'",
            "-50.5, '-50,50'"
    })
    @DisplayName("Deve formatar BigDecimal para exibição PT-BR")
    void deveFormatarParaExibicao(BigDecimal input, String esperado) {
        assertThat(FinanceiroUtils.formatarParaExibicao(input)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "'1.234,56', 1234.56",
            "'R$ 1.234,56', 1234.56",
            "'1000', 1000.00",
            "'', 0.00",
            "abc, 0.00"
    })
    @DisplayName("Deve extrair valor da String UI para BigDecimal de banco")
    void deveExtrairValorParaBanco(String input, BigDecimal esperado) {
        BigDecimal resultado = FinanceiroUtils.extrairValorParaBanco(input);
        assertThat(resultado).isEqualByComparingTo(esperado.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Deve realizar parse seguro de inteiros")
    void deveParsearInteiroSeguro() {
        assertThat(FinanceiroUtils.parseSafeInt("123")).isEqualTo(123);
        assertThat(FinanceiroUtils.parseSafeInt("123abc456")).isEqualTo(123456);
        assertThat(FinanceiroUtils.parseSafeInt(null)).isZero();
    }

    @Test
    @DisplayName("Deve garantir escala de 2 casas decimais no retorno")
    void deveGarantirEscala() {
        BigDecimal resultado = FinanceiroUtils.extrairValorParaBanco("10");
        assertThat(resultado.scale()).isEqualTo(2);
    }
}
