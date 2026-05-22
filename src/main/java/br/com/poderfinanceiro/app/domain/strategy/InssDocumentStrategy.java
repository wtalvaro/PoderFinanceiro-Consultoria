package br.com.poderfinanceiro.app.domain.strategy;

import org.springframework.stereotype.Service;

import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;

@Service
public class InssDocumentStrategy implements DocumentStrategy {

    public boolean supports(String c) {
        return TipoConvenioModel.INSS_CONSIGNADO.name().equalsIgnoreCase(c);
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
