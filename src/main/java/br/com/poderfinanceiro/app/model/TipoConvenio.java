package br.com.poderfinanceiro.app.model;

public enum TipoConvenio {
    INSS("INSS"),
    SIAPE("SIAPE"),
    EXERCITO("Exército"),
    MARINHA("Marinha"),
    AERONAUTICA("Aeronáutica"),
    GOVERNO("Governo"),
    PREFEITURA("Prefeitura"),
    PADRAO("Padrão");

    private final String label;

    TipoConvenio(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static TipoConvenio fromString(String value) {
        if (value == null || value.isBlank()) {
            return PADRAO;
        }

        for (TipoConvenio tipo : TipoConvenio.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getLabel().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        return PADRAO;
    }
}