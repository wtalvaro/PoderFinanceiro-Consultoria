package br.com.poderfinanceiro.app.model;

public enum TipoLogradouro implements Labeled {
    RUA("Rua"),
    AVENIDA("Avenida"),
    TRAVESSA("Travessa"),
    ALAMEDA("Alameda"),
    PRACA("Praça"),
    RODOVIA("Rodovia"),
    ESTRADA("Estrada"),
    BECO("Beco"),
    LOTEAMENTO("Loteamento"),
    OUTRO("Outro");

    private final String label;

    TipoLogradouro(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public static TipoLogradouro fromString(String value) {
        if (value == null || value.isBlank()) {
            return RUA;
        }

        for (TipoLogradouro tipo : TipoLogradouro.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getLabel().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        return RUA;
    }
}