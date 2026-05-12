package br.com.poderfinanceiro.app.model.enums;

public enum TipoConvenio implements Labeled {
    INSS_CONSIGNADO("INSS Consignado"),
    CLT_CONSIGNADO("CLT Consignado"),
    BOLSA_FAMILIA("Bolsa Família"),
    CREDITO_PESSOAL("Crédito Pessoal"),
    CONTA_LUZ("Conta de Luz"),
    SIAPE("SIAPE"),
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