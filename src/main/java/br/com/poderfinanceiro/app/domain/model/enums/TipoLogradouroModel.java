package br.com.poderfinanceiro.app.domain.model.enums;

public enum TipoLogradouroModel implements LabeledModel {
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

    TipoLogradouroModel(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public static TipoLogradouroModel fromString(String value) {
        if (value == null || value.isBlank()) {
            return RUA;
        }

        for (TipoLogradouroModel tipo : TipoLogradouroModel.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getLabel().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        return RUA;
    }
}