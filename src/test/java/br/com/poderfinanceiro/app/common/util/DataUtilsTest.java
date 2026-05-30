package br.com.poderfinanceiro.app.common.util;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Common] Teste de Unidade - DataUtils")
class DataUtilsTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Toolkit já rodando
        }
    }

    @Test
    @DisplayName("Deve formatar LocalDate para String dd/MM/yyyy")
    void deveFormatarData() {
        LocalDate data = LocalDate.of(2026, 5, 29);
        assertThat(DataUtils.formatar(data)).isEqualTo("29/05/2026");
    }

    @Test
    @DisplayName("Deve retornar string vazia ao formatar data nula")
    void deveRetornarVazioParaDataNula() {
        assertThat(DataUtils.formatar(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "29/05/2026, 2026-05-29",
            "01/01/2000, 2000-01-01",
            "31/12/1999, 1999-12-31"
    })
    @DisplayName("Deve parsear strings válidas para LocalDate")
    void deveParsearDatasValidas(String input, String esperado) {
        assertThat(DataUtils.parse(input)).isEqualTo(LocalDate.parse(esperado));
    }

    @ParameterizedTest
    @CsvSource({
            "29-05-2026",
            "2026/05/29",
            "data_invalida",
            "32/01/2026"
    })
    @DisplayName("Deve retornar nulo para formatos de data inválidos")
    void deveRetornarNuloParaDatasInvalidas(String input) {
        assertThat(DataUtils.parse(input)).isNull();
    }

    @Test
    @DisplayName("Deve criar formatador JavaFX com sucesso")
    void deveCriarFormatador() {
        assertThat(DataUtils.criarFormatadorData()).isNotNull();
    }
}
