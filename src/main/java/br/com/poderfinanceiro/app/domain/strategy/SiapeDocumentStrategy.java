package br.com.poderfinanceiro.app.domain.strategy;

import org.springframework.stereotype.Service;

import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;

@Service
public class SiapeDocumentStrategy implements DocumentStrategy {

    public boolean supports(String c) {
        return TipoConvenioModel.SIAPE.name().equalsIgnoreCase(c);
    }

    public String getChecklist() {
        return """
                *DOCUMENTAÇÃO OBRIGATÓRIA - SIAPE*
                ————————————————————————————
                • *Identificação:* Identidade Funcional ou CNH.
                • *Renda:* 02 últimos contracheques (extraídos do portal SouGov).
                • *Residência:* Comprovante de endereço nominal e atualizado.
                • *Autorização:* Chave de Autorização ativa gerada no SouGov.
                • *Bancário:* Comprovante de conta corrente ativa para crédito.
                """;
    }

}
