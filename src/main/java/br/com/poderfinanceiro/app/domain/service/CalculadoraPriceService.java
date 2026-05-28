package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Serviço especializado no cálculo de parcelas utilizando a Tabela Price
 * (Sistema Francês de Amortização). Implementado com precisão arbitrária para
 * garantir conformidade financeira.
 */
@Service
public class CalculadoraPriceService {

    private static final Logger log = LoggerFactory.getLogger(CalculadoraPriceService.class);
    private static final String LOG_PREFIX = "[CalculadoraPriceService]";

    // Precisão de 10 casas decimais para cálculos intermediários para evitar
    // perda de centavos
    private static final MathContext PRECISAO_CALCULO = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Calcula o valor da parcela mensal fixa (PMT) com base na Tabela Price.
     * Fórmula: PMT = PV * [ (i * (1 + i)^n) / ((1 + i)^n - 1) ]
     * 
     * @param valorEmprestimo Valor presente (PV)
     * @param tabela Objeto contendo a taxa de juros mensal (i)
     * @param quantidadeParcelas Número de períodos (n)
     * @return BigDecimal Valor da parcela arredondado para 2 casas decimais
     */
    public BigDecimal calcularParcela(BigDecimal valorEmprestimo, TabelaJurosModel tabela, int quantidadeParcelas) {
        log.info("{} [TELEMETRIA] Iniciando cálculo de parcela Price. Valor: {}, Parcelas: {}", LOG_PREFIX,
                valorEmprestimo, quantidadeParcelas);

        // 1. Validação de Regras de Negócio
        if (valorEmprestimo == null || valorEmprestimo.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("{} [NEGOCIO] Cálculo abortado: Valor do empréstimo inválido.", LOG_PREFIX);
            return BigDecimal.ZERO;
        }

        if (tabela == null || tabela.getTaxaMensal() == null) {
            log.warn("{} [NEGOCIO] Cálculo abortado: Tabela de juros ou taxa ausente.", LOG_PREFIX);
            return BigDecimal.ZERO;
        }

        if (quantidadeParcelas <= 0) {
            log.warn("{} [NEGOCIO] Cálculo abortado: Quantidade de parcelas deve ser maior que zero.", LOG_PREFIX);
            return BigDecimal.ZERO;
        }

        BigDecimal taxaMensal = tabela.getTaxaMensal();

        // 2. Tratamento de Cenário de Taxa Zero (Evita divisão por zero)
        if (taxaMensal.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal parcelaSemJuros = valorEmprestimo.divide(BigDecimal.valueOf(quantidadeParcelas), 2,
                    RoundingMode.HALF_UP);
            log.info("{} [AUDITORIA] Cálculo concluído (Taxa Zero). Parcela: {}", LOG_PREFIX, parcelaSemJuros);
            return parcelaSemJuros;
        }

        try {
            // 3. Execução da Fórmula Price com BigDecimal
            // i = taxaMensal
            // n = quantidadeParcelas
            // PV = valorEmprestimo

            // (1 + i)
            BigDecimal umMaisI = BigDecimal.ONE.add(taxaMensal);

            // (1 + i)^n
            BigDecimal fatorPotencia = umMaisI.pow(quantidadeParcelas, PRECISAO_CALCULO);

            // Numerador: i * (1 + i)^n
            BigDecimal numerador = taxaMensal.multiply(fatorPotencia, PRECISAO_CALCULO);

            // Denominador: (1 + i)^n - 1
            BigDecimal denominador = fatorPotencia.subtract(BigDecimal.ONE, PRECISAO_CALCULO);

            // Coeficiente: Numerador / Denominador
            BigDecimal coeficiente = numerador.divide(denominador, PRECISAO_CALCULO);

            // PMT = PV * Coeficiente
            BigDecimal resultadoFinal = valorEmprestimo.multiply(coeficiente).setScale(2, RoundingMode.HALF_UP);

            log.info("{} [AUDITORIA] Cálculo Price finalizado com sucesso. Parcela: {}", LOG_PREFIX, resultadoFinal);
            return resultadoFinal;

        } catch (ArithmeticException e) {
            log.error("{} [SISTEMA] Erro aritmético grave no cálculo Price: {}", LOG_PREFIX, e.getMessage());
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro inesperado na Calculadora Price: {}", LOG_PREFIX, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
