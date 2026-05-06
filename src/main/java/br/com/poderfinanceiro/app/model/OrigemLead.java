package br.com.poderfinanceiro.app.model;

public enum OrigemLead {
    WHATSAPP("WhatsApp"),
    PANFLETO("Panfleto"),
    INDICACAO("Indicação"),
    FACEBOOK("Facebook"),
    PASSOU_NA_PORTA("Passou na porta");

    private final String label;

    OrigemLead(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante ou pelo label amigável.
     */
    public static OrigemLead fromString(String value) {
        if (value == null || value.isBlank()) {
            return WHATSAPP; // Valor padrão para casos nulos ou em branco
        }

        for (OrigemLead origem : OrigemLead.values()) {
            if (origem.name().equalsIgnoreCase(value) || origem.getLabel().equalsIgnoreCase(value)) {
                return origem;
            }
        }
        return WHATSAPP;
    }
}
