package br.com.poderfinanceiro.app.common.util;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Common] Teste de Unidade - DocumentoUtils")
class DocumentoUtilsTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
        }
    }

    @Test
    @DisplayName("Deve formatar CPF válido corretamente")
    void deveFormatarCpf() {
        String cpf = "12345678901";
        assertThat(DocumentoUtils.formatarCpf(cpf)).isEqualTo("123.456.789-01");
    }

    @Test
    @DisplayName("Deve retornar string vazia para CPF nulo ou vazio")
    void deveLidarComCpfVazio() {
        assertThat(DocumentoUtils.formatarCpf(null)).isEmpty();
        assertThat(DocumentoUtils.formatarCpf("   ")).isEmpty();
    }

    @Test
    @DisplayName("Deve desformatar CPF removendo pontos e traços")
    void deveDesformatarCpf() {
        String formatado = "123.456.789-01";
        assertThat(DocumentoUtils.desformatar(formatado)).isEqualTo("12345678901");
    }

    @ParameterizedTest
    @CsvSource({
            "12345678909, true", // CPF válido (exemplo)
            "11111111111, false", // CPF com dígitos iguais
            "1234567890, false", // Tamanho insuficiente
            "123456789012, false" // Tamanho excessivo
    })
    @DisplayName("Deve validar integridade do CPF via algoritmo de dígitos")
    void deveValidarCpf(String cpf, boolean esperado) {
        // Nota: CPFs reais devem ser usados para testes rigorosos de algoritmo
        assertThat(DocumentoUtils.isCpfValido(cpf)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Deve criar formatador JavaFX com sucesso")
    void deveCriarFormatador() {
        assertThat(DocumentoUtils.criarFormatadorCpf()).isNotNull();
    }
}
