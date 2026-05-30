package br.com.poderfinanceiro.app.domain.model.enums;

import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.TipoTelaFocada;

public enum RotaAba {
    DASHBOARD("ABA_DASHBOARD", TipoTelaFocada.DASHBOARD),
    CLIENTES("ABA_CLIENTES", TipoTelaFocada.LISTA_CLIENTES),
    LINKS("ABA_LINKS", TipoTelaFocada.LINKS_UTEIS),
    JUROS("ABA_JUROS", TipoTelaFocada.TABELAS_JUROS),
    BANCOS("ABA_BANCOS", TipoTelaFocada.GESTAO_BANCOS),
    COMISSOES("ABA_COMISSOES", TipoTelaFocada.GESTAO_COMISSOES),
    PROPOSTAS("ABA_PROPOSTAS", TipoTelaFocada.ESTEIRA_PROPOSTAS),
    IMPORTADOR_TABELAS("ABA_IMPORTADOR_TABELAS", TipoTelaFocada.IMPORTADOR_IA),
    COPILOTO("ABA_COPILOTO", TipoTelaFocada.COPILOTO_SIMULACAO),
    PLAYBOOK("ABA_PLAYBOOK", TipoTelaFocada.PLAYBOOK_VENDAS);

    private final String id;
    private final TipoTelaFocada tipoTelaFocada;

    RotaAba(String id, TipoTelaFocada tipoTelaFocada) {
        this.id = id;
        this.tipoTelaFocada = tipoTelaFocada;
    }

    public String getId() {
        return id;
    }

    public TipoTelaFocada getTipoTelaFocada() {
        return tipoTelaFocada;
    }

    public static RotaAba fromId(String id) {
        if (id == null)
            return null;
        for (RotaAba rota : values()) {
            if (rota.getId().equals(id)) {
                return rota;
            }
        }
        return null;
    }
}