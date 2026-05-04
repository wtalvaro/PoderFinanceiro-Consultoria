package br.com.poderfinanceiro.app.strategy;

import org.springframework.stereotype.Service;

import br.com.poderfinanceiro.app.model.TipoConvenio;

@Service
public class PrefeituraDocumentStrategy implements DocumentStrategy {

    public boolean supports(String c) {
        return TipoConvenio.PREFEITURA.name().equalsIgnoreCase(c);
    }

    public String getChecklist() {
        return """
                *DOCUMENTAÇÃO OBRIGATÓRIA - PÚBLICO ESTADUAL/MUNICIPAL*
                ————————————————————————————
                • *Identificação:* RG ou CNH.
                • *Renda:* 03 últimos contracheques originais.
                • *Residência:* Comprovante de residência atualizado.
                • *Funcional:* Número de matrícula e data de admissão.
                • *Bancário:* Cartão do banco ou extrato para validação de dados.
                """;
    }

}
