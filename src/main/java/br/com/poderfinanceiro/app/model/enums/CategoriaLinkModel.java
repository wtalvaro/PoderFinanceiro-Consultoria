package br.com.poderfinanceiro.app.model.enums;

public enum CategoriaLinkModel implements LabeledModel {
    BANCO("Instituições Financeiras"),
    GOVERNO("Portais Governamentais"),
    CONSULTA("Consultas (CPF/FGTS)"),
    INTERNO("Manuais e Intranet"),
    OUTROS("Diversos");

    private final String label;

    CategoriaLinkModel(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Método de busca robusto para converter Strings em Enums.
     * Tenta encontrar pelo nome da constante (ex: BANCO) ou pelo label (ex:
     * Instituições Financeiras).
     */
    public static CategoriaLinkModel fromString(String value) {
        if (value == null || value.isBlank()) {
            return OUTROS; // Valor padrão seguro
        }

        for (CategoriaLinkModel categoria : CategoriaLinkModel.values()) {
            if (categoria.name().equalsIgnoreCase(value) || categoria.getLabel().equalsIgnoreCase(value)) {
                return categoria;
            }
        }

        return OUTROS; // Retorna OUTROS como padrão se não encontrar correspondência
    }
}