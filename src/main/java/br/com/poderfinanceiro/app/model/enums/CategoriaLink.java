package br.com.poderfinanceiro.app.model.enums;

public enum CategoriaLink implements Labeled {
    BANCO("Instituições Financeiras"),
    GOVERNO("Portais Governamentais"),
    CONSULTA("Consultas (CPF/FGTS)"),
    INTERNO("Manuais e Intranet"),
    OUTROS("Diversos");

    private final String label;

    CategoriaLink(String label) {
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
    public static CategoriaLink fromString(String value) {
        if (value == null || value.isBlank()) {
            return OUTROS; // Valor padrão seguro
        }

        for (CategoriaLink categoria : CategoriaLink.values()) {
            if (categoria.name().equalsIgnoreCase(value) || categoria.getLabel().equalsIgnoreCase(value)) {
                return categoria;
            }
        }

        return OUTROS; // Retorna OUTROS como padrão se não encontrar correspondência
    }
}