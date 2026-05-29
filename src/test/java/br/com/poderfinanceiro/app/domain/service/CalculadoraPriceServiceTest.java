package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h1>CalculadoraPriceServiceTest</h1>
 * <p>
 * Testes de Unidade para a Calculadora Price.
 * Valida a precisão matemática e a resiliência do motor de amortização.
 * </p>
 */
class CalculadoraPriceServiceTest {

    private CalculadoraPriceService service;

    @BeforeEach
    void setUp() {
        this.service = new CalculadoraPriceService();
    }

    @Test
    @DisplayName("Deve retornar ZERO quando o valor do empréstimo for nulo ou inválido")
    void deveRetornarZeroParaValorInvalido() {
        TabelaJurosModel tabela = criarTabelaComTaxa("0.01");

        assertThat(service.calcularParcela(null, tabela, 12)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.calcularParcela(BigDecimal.ZERO, tabela, 12)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.calcularParcela(new BigDecimal("-100.00"), tabela, 12))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar ZERO quando a tabela ou a taxa forem nulas")
    void deveRetornarZeroParaTabelaInvalida() {
        BigDecimal valor = new BigDecimal("1000.00");

        assertThat(service.calcularParcela(valor, null, 12)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.calcularParcela(valor, new TabelaJurosModel(), 12)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar ZERO quando a quantidade de parcelas for menor ou igual a zero")
    void deveRetornarZeroParaPrazoInvalido() {
        BigDecimal valor = new BigDecimal("1000.00");
        TabelaJurosModel tabela = criarTabelaComTaxa("0.01");

        assertThat(service.calcularParcela(valor, tabela, 0)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.calcularParcela(valor, tabela, -5)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve calcular parcela sem juros (divisão simples) quando a taxa for zero")
    void deveCalcularDivisaoSimplesParaTaxaZero() {
        BigDecimal valor = new BigDecimal("1200.00");
        TabelaJurosModel tabela = criarTabelaComTaxa("0.00");

        BigDecimal resultado = service.calcularParcela(valor, tabela, 12);

        // 1200 / 12 = 100.00
        assertThat(resultado).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @DisplayName("Deve calcular parcelas Price com precisão bancária")
    @CsvSource({
            "1000.00, 0.01, 12, 88.85", // 1000 a 1% em 12x
            "5000.00, 0.02, 24, 264.36", // CORREÇÃO: Valor exato Price é 264.36
            "157.00, 0.05, 12, 17.71", // CORREÇÃO: Bolsa Família exato é 17.71
            "10000.00, 0.015, 48, 293.75" // 10k a 1.5% em 48x
    })
    void deveCalcularParcelasComSucesso(String valor, String taxa, int prazo, String esperado) {
        BigDecimal valorEmprestimo = new BigDecimal(valor);
        TabelaJurosModel tabela = criarTabelaComTaxa(taxa);

        BigDecimal resultado = service.calcularParcela(valorEmprestimo, tabela, prazo);

        assertThat(resultado).isEqualByComparingTo(esperado);
    }

    private TabelaJurosModel criarTabelaComTaxa(String taxa) {
        TabelaJurosModel tabela = new TabelaJurosModel();
        tabela.setTaxaMensal(new BigDecimal(taxa));
        return tabela;
    }
}
