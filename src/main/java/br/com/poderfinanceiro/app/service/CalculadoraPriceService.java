package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.TabelaJuros;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CalculadoraPriceService {

    public BigDecimal calcularParcela(BigDecimal valorEmprestimo, TabelaJuros tabela, int quantidadeParcelas) {
        if (valorEmprestimo == null || tabela == null || quantidadeParcelas <= 0) {
            return BigDecimal.ZERO;
        }

        // A taxa vem do banco em decimal (ex: 0.0205 para 2.05%)
        double i = tabela.getTaxaMensal().doubleValue();
        double pv = valorEmprestimo.doubleValue();
        int n = quantidadeParcelas;

        // Aplicação da fórmula Price
        double numerador = i * Math.pow(1 + i, n);
        double denominador = Math.pow(1 + i, n) - 1;
        double pmt = pv * (numerador / denominador);

        // Retorna arredondando para 2 casas decimais (Padrão Monetário)
        return BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP);
    }
}