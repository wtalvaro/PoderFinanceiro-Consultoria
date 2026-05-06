package br.com.poderfinanceiro.app.model;

public enum StatusProposta {
    DIGITADA("Digitada"),
    PENDENTE("Pendente"),
    ANALISE_BANCO("Análise do Banco"),
    AGUARDANDO_DOC("Aguardando Documentação"),
    APROVADA("Aprovada"),
    REPROVADA("Reprovada"),
    PAGO("Pago"),
    CANCELADO("Cancelado");

    private final String label;

    StatusProposta(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static StatusProposta fromString(String value) {
        if (value == null || value.isBlank()) {
            return DIGITADA; // Valor padrão para casos nulos ou em branco
        }

        for (StatusProposta status : StatusProposta.values()) {
            if (status.name().equalsIgnoreCase(value) || status.getLabel().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return DIGITADA; // Retorna DIGITADA como padrão se não encontrar correspondência
    }
}