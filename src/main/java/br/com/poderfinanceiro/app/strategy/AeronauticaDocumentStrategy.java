package br.com.poderfinanceiro.app.strategy;

import org.springframework.stereotype.Service;

import br.com.poderfinanceiro.app.model.TipoConvenio;

@Service
public class AeronauticaDocumentStrategy implements DocumentStrategy {

    public boolean supports(String c) {
        return TipoConvenio.AERONAUTICA.name().equalsIgnoreCase(c);
    }

    public String getChecklist() {
        return """
                *DOCUMENTAÇÃO OBRIGATÓRIA - FORÇAS ARMADAS*
                ————————————————————————————
                • *Identificação:* Identidade Militar original e legível.
                • *Renda:* Último Bilhete de Pagamento atualizado.
                • *Residência:* Comprovante de endereço em nome do militar ou cônjuge.
                • *Bancário:* Comprovante de conta bancária para recebimento do crédito.
                • *Certidão:* Nada Consta/Folha de Alterações (se solicitado pelo banco).
                """;
    }

}
