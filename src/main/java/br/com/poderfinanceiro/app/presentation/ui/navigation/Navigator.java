package br.com.poderfinanceiro.app.presentation.ui.navigation;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import javafx.scene.Node;

/**
 * <h1>Navigator</h1>
 * <p>
 * Interface de navegação unificada.
 * Refatorada para utilizar AppRoute (Padrão Registry), eliminando a necessidade
 * de métodos específicos para cada tela.
 * </p>
 */
public interface Navigator {

    // --- NAVEGAÇÃO GENÉRICA (Substitui todos os métodos 'irPara' e
    // 'abrirDashboard/Clientes') ---
    /**
     * Navega para uma rota definida no Registry.
     * O Navigator decidirá se abre na área principal ou como aba no Workspace.
     */
    void navegarPara(AppRoute rota);

    // --- NAVEGAÇÃO DE CONTEXTO (Exigem parâmetros de domínio) ---
    void abrirClienteNoWorkspace(ProponenteModel proponente);

    void abrirPropostaNoWorkspace(PropostaModel proposta);

    void abrirCopilotoSimulacao(Node anchorNode);

    // --- AÇÕES GLOBAIS ---
    void alternarPainelIA();

    void mostrarOverlaySair();

    void limparCacheDeTelas();

    // --- OVERLAYS E FEEDBACK ---
    void mostrarLoading(String mensagem);

    void ocultarLoading();

    void notificarSucesso(String mensagem);

    void notificarAviso(String mensagem);

    /** Diálogo global de confirmação (Sim/Não) */
    void solicitarConfirmacao(String titulo, String mensagem, String textoBotaoConfirmar, String estiloBotao,
            Runnable acaoConfirmar);
}
