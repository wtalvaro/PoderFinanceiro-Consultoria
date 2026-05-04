package br.com.poderfinanceiro.app.strategy;

import org.springframework.stereotype.Service;

import br.com.poderfinanceiro.app.model.TipoConvenio;

@Service
public class InssDocumentStrategy implements DocumentStrategy {

    public boolean supports(String c) {
        return TipoConvenio.INSS.name().equalsIgnoreCase(c);
    }

    public String getChecklist() {
        return """
                    *DOCUMENTAÇÃO OBRIGATÓRIA - INSS*
                    ————————————————————————————
                    • *Identificação:* RG ou CNH original (dentro da validade).
                    • *Residência:* Comprovante de endereço nominal e atualizado (máximo 60 dias).
                    • *Extrato HISCON:* Extrato de Empréstimos Consignados (obtido via Meu INSS).
                    • *Detalhamento:* Extrato de Pagamento (Detalhamento de Crédito - DC).
                    • *Bancário:* Comprovante de conta onde recebe o benefício (Extrato ou Cartão).
                    • *Identificação Visual:* Selfie com documento de identidade (para formalização digital).
                    """;
    }
}
