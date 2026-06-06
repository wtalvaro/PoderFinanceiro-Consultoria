package br.com.poderfinanceiro.app.presentation.ui.navigation;

import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.TipoTelaFocada;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;
import java.util.Optional;

/**
 * <h1>AppRoute</h1>
 * <p>
 * Registry central e único de rotas da aplicação.
 * Define o mapeamento entre constantes, caminhos FXML, comportamento de
 * layout e o contexto de foco para a IA.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum AppRoute {

    // Rotas de Autenticação (Fora do Workspace)
    LOGIN("/fxml/login.fxml", false, "Login", null),
    CADASTRO_USUARIO("/fxml/cadastro.fxml", false, "Criar Conta", null),

    // Estrutura Principal
    WORKSPACE("/fxml/workspace.fxml", true, "Área de Trabalho", null),

    // Abas do Workspace (Dentro do TabPane)
    DASHBOARD("/fxml/dashboard.fxml", true, "📊 Visão Geral", TipoTelaFocada.DASHBOARD),
    CLIENTES("/fxml/proponente_list.fxml", true, "👥 Clientes", TipoTelaFocada.LISTA_CLIENTES),
    CADASTRO_PROPONENTE("/fxml/proponente.fxml", true, "📝 Novo Contato", TipoTelaFocada.LISTA_CLIENTES),
    ESTEIRA_PROPOSTAS("/fxml/esteira_propostas.fxml", true, "📄 Esteira de Propostas",
            TipoTelaFocada.ESTEIRA_PROPOSTAS),
    COMISSOES("/fxml/comissoes.fxml", true, "💰 Comissões", TipoTelaFocada.GESTAO_COMISSOES),
    TABELAS_JUROS("/fxml/tabelas_juros.fxml", true, "📈 Tabelas de Juros", TipoTelaFocada.TABELAS_JUROS),
    IMPORTADOR_TABELAS("/fxml/importador_tabelas.fxml", true, "📥 Importador IA", TipoTelaFocada.IMPORTADOR_IA),
    BANCOS_CONVENIOS("/fxml/bancos_convenios.fxml", true, "🏦 Bancos e Convênios", TipoTelaFocada.GESTAO_BANCOS),
    LINKS_UTEIS("/fxml/links_uteis.fxml", true, "🔗 Links Úteis", TipoTelaFocada.LINKS_UTEIS),
    PLAYBOOK("/fxml/playbook.fxml", true, "📚 Playbook", TipoTelaFocada.PLAYBOOK_VENDAS),
    COPILOTO("/fxml/copiloto_simulacao.fxml", true, "✨ Copiloto de Vendas", TipoTelaFocada.COPILOTO_SIMULACAO);

    private final String fxmlPath;
    private final boolean exibirEstruturaMestre;
    private final String titulo;
    private final TipoTelaFocada tipoTelaFocada;

    /**
     * Determina se a rota deve ser aberta como uma nova aba no Workspace.
     */
    public boolean isAba() {
        return this != LOGIN && this != WORKSPACE && this != CADASTRO_USUARIO;
    }

    /**
     * Resolve uma rota a partir do caminho do arquivo FXML.
     */
    public static AppRoute fromPath(String path) {
        return Arrays.stream(values())
                .filter(r -> r.getFxmlPath().equals(path))
                .findFirst()
                .orElse(WORKSPACE);
    }

    /**
     * Resolve uma rota a partir do nome da constante (utilizado no userData das
     * Tabs).
     */
    public static Optional<AppRoute> fromName(String name) {
        if (name == null)
            return Optional.empty();
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
