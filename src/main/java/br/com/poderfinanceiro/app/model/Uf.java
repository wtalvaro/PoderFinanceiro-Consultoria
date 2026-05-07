package br.com.poderfinanceiro.app.model;

public enum Uf implements Labeled {
    AC("Acre"), AL("Alagoas"), AP("Amapá"), AM("Amazonas"),
    BA("Bahia"), CE("Ceará"), DF("Distrito Federal"), ES("Espírito Santo"),
    GO("Goiás"), MA("Maranhão"), MT("Mato Grosso"), MS("Mato Grosso do Sul"),
    MG("Minas Gerais"), PA("Pará"), PB("Paraíba"), PR("Paraná"),
    PE("Pernambuco"), PI("Piauí"), RJ("Rio de Janeiro"), RN("Rio Grande do Norte"),
    RS("Rio Grande do Sul"), RO("Rondônia"), RR("Roraima"), SC("Santa Catarina"),
    SP("São Paulo"), SE("Sergipe"), TO("Tocantins");

    private final String label;

    Uf(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public static Uf fromString(String value) {
        if (value == null || value.isBlank()) {
            return RJ; // Default para Magé/RJ, ou null se preferir
        }

        for (Uf uf : Uf.values()) {
            if (uf.name().equalsIgnoreCase(value) || uf.getLabel().equalsIgnoreCase(value)) {
                return uf;
            }
        }
        return RJ;
    }
}