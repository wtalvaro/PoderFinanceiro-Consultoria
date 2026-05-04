package br.com.poderfinanceiro.app.strategy;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(Ordered.LOWEST_PRECEDENCE) // Garante que seja a última da lista
public class DefaultDocumentStrategy implements DocumentStrategy {

    @Override
    public boolean supports(String c) {
        // Suporta se for explicitamente "PADRAO" ou se o convênio for nulo/vazio
        return "PADRAO".equalsIgnoreCase(c) || c == null || c.isBlank();
    }

    public String getChecklist() {
        return """
                *DOCUMENTAÇÃO GERAL PARA ANÁLISE*
                ————————————————————————————
                • *Identificação:* RG e CPF ou CNH.
                • *Residência:* Comprovante de endereço nominal.
                • *Financeiro:* 03 últimos comprovantes de renda (Holerites).
                • *Extrato:* Extrato bancário dos últimos 90 dias (para crédito pessoal).
                • *FGTS:* Print do extrato do FGTS com saldo total disponível (se aplicável).
                """;
    }

}
