package br.com.poderfinanceiro.app.domain.model.enums;

public enum StatusPropostaModel implements LabeledModel {
    DIGITADA("Digitada"),
    PENDENTE("Pendente"),
    ANALISE_BANCO("Análise do Banco"),
    AGUARDANDO_DOC("Aguardando Documentação"),
    APROVADA("Aprovada"),
    REPROVADA("Reprovada"),
    PAGO("Pago"),
    CANCELADO("Cancelado");

    private final String label;

    StatusPropostaModel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static StatusPropostaModel fromString(String value) {
        if (value == null || value.isBlank()) {
            return DIGITADA; // Valor padrão para casos nulos ou em branco
        }

        for (StatusPropostaModel status : StatusPropostaModel.values()) {
            if (status.name().equalsIgnoreCase(value) || status.getLabel().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return DIGITADA; // Retorna DIGITADA como padrão se não encontrar correspondência
    }
}