package br.com.poderfinanceiro.app.domain.model.enums;

public enum TipoVinculoModel implements LabeledModel {
    APOSENTADO("Aposentado"),
    PENSIONISTA("Pensionista"),
    SERVIDOR_ATIVO("Servidor Ativo"),
    MILITAR("Militar"),
    CLT("CLT"),
    OUTROS("Outros");

    private final String label;

    TipoVinculoModel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static TipoVinculoModel fromString(String value) {
        if (value == null || value.isBlank()) {
            return CLT; // Valor padrão para casos nulos ou em branco
        }

        for (TipoVinculoModel tipo : TipoVinculoModel.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getLabel().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        return CLT; // Retorna CLT como padrão se não encontrar correspondência
    }
}
