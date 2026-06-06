package br.com.poderfinanceiro.app.presentation.ui.navigation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

/**
 * <h1>AppRoute</h1>
 * <p>
 * Registry centralizado de rotas FXML. Fonte única de verdade para navegação.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum AppRoute {

    // --- ESTRUTURA ---
    LOGIN("/fxml/login.fxml", false, "Login"),
    CADASTRO_USUARIO("/fxml/cadastro.fxml", false, "Criar Conta"), // Pasta auth
    WORKSPACE("/fxml/workspace.fxml", true, "Área de Trabalho"),

    // --- OPERACIONAL (ABAS) ---
    DASHBOARD("/fxml/dashboard.fxml", true, "📊 Visão Geral"),
    CLIENTES("/fxml/proponente_list.fxml", true, "👥 Listagem de Clientes"),
    PROPONENTE_FORM("/fxml/proponente.fxml", true, "📝 Cadastro de Proponente"),
    ATENDIMENTO_HUB("/fxml/atendimento_hub.fxml", true, "👤 Hub de Atendimento"),
    ESTEIRA_PROPOSTAS("/fxml/esteira_propostas.fxml", true, "📄 Esteira de Propostas"),
    PROPOSTA_FORM("/fxml/proposta.fxml", true, "📑 Detalhes da Proposta"),

    // --- FINANCEIRO E SUPORTE ---
    COMISSOES("/fxml/comissoes.fxml", true, "💰 Gestão de Repasses"),
    TABELAS_JUROS("/fxml/tabelas_juros.fxml", true, "📈 Tabelas de Juros"),
    BANCOS_CONVENIOS("/fxml/bancos_convenios.fxml", true, "🏦 Bancos e Convênios"),
    IMPORTADOR_IA("/fxml/importador_tabelas.fxml", true, "📥 Importador IA"),
    LINKS_UTEIS("/fxml/links_uteis.fxml", true, "🔗 Links Úteis"),
    PLAYBOOK("/fxml/playbook.fxml", true, "📚 Playbook"),

    // --- IA ---
    COPILOTO("/fxml/copiloto_simulacao.fxml", true, "✨ Copiloto de Vendas");

    private final String fxmlPath;
    private final boolean exibirEstruturaMestre;
    private final String titulo;

    public static AppRoute fromPath(String path) {
        return Arrays.stream(values())
                .filter(r -> r.getFxmlPath().equals(path))
                .findFirst()
                .orElse(WORKSPACE);
    }
}
