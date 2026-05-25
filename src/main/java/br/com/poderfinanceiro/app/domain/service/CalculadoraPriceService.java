package br.com.poderfinanceiro.app.domain.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CalculadoraPriceService {

    private static final Logger log = LoggerFactory.getLogger(CalculadoraPriceService.class);

    public BigDecimal calcularParcela(BigDecimal valorEmprestimo, TabelaJurosModel tabela, int quantidadeParcelas) {
        log.debug("[PRICE_SERVICE] calcularParcela: valorEmprestimo={}, tabela={}, quantidadeParcelas={}",
                valorEmprestimo, tabela != null ? tabela.getId() : "null", quantidadeParcelas);

        if (valorEmprestimo == null || tabela == null || quantidadeParcelas <= 0) {
            log.warn(
                    "[PRICE_SERVICE] calcularParcela: Parâmetros inválidos - valorEmprestimo={}, tabela={}, quantidadeParcelas={}",
                    valorEmprestimo, tabela, quantidadeParcelas);
            return BigDecimal.ZERO;
        }

        double i = tabela.getTaxaMensal().doubleValue();
        double pv = valorEmprestimo.doubleValue();
        int n = quantidadeParcelas;

        log.trace("[PRICE_SERVICE] calcularParcela: taxaMensal={}, valorPresente={}, parcelas={}", i, pv, n);

        double numerador = i * Math.pow(1 + i, n);
        double denominador = Math.pow(1 + i, n) - 1;
        double pmt = pv * (numerador / denominador);

        BigDecimal parcela = BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP);
        log.info("[PRICE_SERVICE] calcularParcela: Resultado parcela = {}", parcela);
        return parcela;
    }
}