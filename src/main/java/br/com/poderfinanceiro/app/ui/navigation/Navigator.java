package br.com.poderfinanceiro.app.ui.navigation;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import javafx.scene.Node;

public interface Navigator {
    // Navegação Principal
    void navegarPara(String fxmlPath, boolean mostrarEstrutura);

    void abrirDashboard();

    void abrirClientes();

    void abrirPlaybook();

    // Detalhes e Workspace
    void abrirPropostaNoWorkspace(PropostaModel proposta);

    void abrirClienteNoWorkspace(ProponenteModel proponente);

    // Atalhos de Menu (Os que estavam gerando erro)
    void irParaNovoContato();

    void irParaPropostas();

    void irParaTabelaComissoes();

    void irParaTabelasJuros();

    void irParaImportadorTabelas();

    void irParaBancosConvenios();

    void irParaLinksUteis();

    void limparCacheDeTelas();

    void abrirCopilotoSimulacao(Node anchorNode);

    // UI e Overlays
    void alternarPainelIA();

    void mostrarOverlaySair();

    void mostrarLoading(String mensagem);

    void ocultarLoading();

    // Notificações
    void notificarSucesso(String mensagem);

    void notificarAviso(String mensagem);
}