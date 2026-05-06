package br.com.poderfinanceiro.app.model;

public enum TipoVinculo implements Labeled {
    APOSENTADO("Aposentado"),
    PENSIONISTA("Pensionista"),
    SERVIDOR_ATIVO("Servidor Ativo"),
    MILITAR("Militar"),
    CLT("CLT");

    private final String label;

    TipoVinculo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static TipoVinculo fromString(String value) {
        if (value == null || value.isBlank()) {
            return CLT; // Valor padrão para casos nulos ou em branco
        }

        for (TipoVinculo tipo : TipoVinculo.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getLabel().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        return CLT; // Retorna CLT como padrão se não encontrar correspondência
    }
}
