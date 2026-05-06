package br.com.poderfinanceiro.app.model;

public enum TipoRelacionamento implements Labeled {
    LEAD("Lead"),
    PROPONENTE("Proponente"),
    CLIENTE("Cliente");

    private final String label;

    TipoRelacionamento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static TipoRelacionamento fromString(String value) {
        if (value == null || value.isBlank()) {
            return LEAD; // Valor padrão para casos nulos ou em branco
        }

        for (TipoRelacionamento tipo : TipoRelacionamento.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getLabel().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        return LEAD; // Retorna LEAD como padrão se não encontrar correspondência
    }
}