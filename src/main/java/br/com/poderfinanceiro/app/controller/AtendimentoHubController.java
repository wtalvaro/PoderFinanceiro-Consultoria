package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.service.AtendimentoContextService; // 🚀 IMPORT ADICIONADO
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.utils.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import org.controlsfx.control.MasterDetailPane;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
@Scope("prototype")
public class AtendimentoHubController {

    @FXML
    private LeadController abaLeadController;
    @FXML
    private EnderecoController abaEnderecoController;
    @FXML
    private PropostaHubController abaPropostaHubController;
    @FXML
    private LinkUtilController abaLinksController;
    @FXML
    private VBox overlayConfirmacaoSaida, overlayMensagem, overlayResumo;
    @FXML
    private Button btnSalvar;
    @FXML
    private Label lblMensagemTexto, lblMensagemTitulo, lblResumoPreview;
    @FXML
    private TabPane subTabPane;

    private final ProponenteService atendimentoService;
    private final MainController mainController;
    private final PropostaService propostaService;
    private final AtendimentoContextService contextoService; // 🚀 INJEÇÃO DE DEPENDÊNCIA

    private ProponenteModel proponenteAberto;
    private Runnable acaoNavegacaoPendente;
    private String resumoGeradoParaCopia;
    private Tab tabPertencente;

    @FXML
    private MasterDetailPane masterDetailPane;
    private boolean slaveJaCarregado = false;
    private final ApplicationContext context;

    // 🚀 CONSTRUTOR ATUALIZADO
    public AtendimentoHubController(ProponenteService atendimentoService, MainController mainController,
            PropostaService propostaService, ApplicationContext context, AtendimentoContextService contextoService) {
        this.atendimentoService = atendimentoService;
        this.mainController = mainController;
        this.propostaService = propostaService;
        this.context = context;
        this.contextoService = contextoService;
    }

    @FXML
    public void initialize() {
        BooleanBinding atendimentoSujo = abaLeadController.getViewModel().dirtyProperty()
                .or(abaEnderecoController.getViewModel().dirtyProperty())
                .or(abaPropostaHubController.getViewModel().dirtyProperty());

        BooleanBinding dadosValidos = Bindings.createBooleanBinding(() -> {
            String nome = abaLeadController.getViewModel().nomeProperty().get();
            String cpf = abaLeadController.getViewModel().cpfProperty().get();

            boolean nomeValido = nome != null && !nome.trim().isEmpty();

            String cpfLimpo = cpf != null ? cpf.replaceAll("[^0-9]", "") : "";
            boolean cpfValido = cpfLimpo.isEmpty() || cpfLimpo.length() == 11;

            return nomeValido && cpfValido;
        },
                abaLeadController.getViewModel().nomeProperty(),
                abaLeadController.getViewModel().cpfProperty());

        btnSalvar.disableProperty().bind(atendimentoSujo.not().or(dadosValidos.not()));
    }

    public void inicializarAtendimento(ProponenteModel proponente) {
        this.proponenteAberto = proponente;
        abaLeadController.getViewModel().loadFromModel(proponente);

        // 🚀 A LINHA MÁGICA: Avisa a IA que o foco mudou!
        contextoService.setLeadAtivo(proponente);

        if (proponente != null && proponente.getEnderecos() != null && !proponente.getEnderecos().isEmpty()) {
            abaEnderecoController.getViewModel().loadFromModel(proponente.getEnderecos().get(0));
        } else {
            abaEnderecoController.getViewModel().reset();
        }

        if (abaLinksController != null) {
            abaLinksController.recarregarLinks();
        }

        abaPropostaHubController.inicializarPropostasDoCliente(proponente);
    }

    public void inicializarAtendimento(ProponenteModel proponente, Long propostaIdAlvo) {
        inicializarAtendimento(proponente);

        if (propostaIdAlvo != null && abaPropostaHubController != null) {
            abaPropostaHubController.selecionarPropostaEspecifica(propostaIdAlvo);

            if (subTabPane != null) {
                subTabPane.getSelectionModel().select(2);
            }
        }
    }

    public void prepararNovoAtendimento() {
        this.proponenteAberto = new ProponenteModel();
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        abaPropostaHubController.getViewModel().reset();

        // 🚀 Ajustado: Fornece a instância correta unificada
        contextoService.setLeadAtivo(getProponenteComCamposDaTela());
    }

    public boolean temAlteracoesNaoSalvas() {
        return abaLeadController.getViewModel().isDirty()
                || abaEnderecoController.getViewModel().isDirty()
                || abaPropostaHubController.getViewModel().isDirty();
    }

    @FXML
    public void handleSalvar() {
        // ==========================================================
        // 🚀 O GUARDA-COSTAS (Validação das ViewModels)
        // ==========================================================

        // 1. Valida a Aba do Cliente (Lead) - Caso no futuro você adicione regras lá
        if (abaLeadController.getViewModel().isDirty() && !abaLeadController.getViewModel().isValido()) {
            subTabPane.getSelectionModel().select(0); // Muda para a aba "Cliente"
            exibirMensagem("⚠️ Verifique os campos obrigatórios do cliente antes de salvar.", false);
            return; // Bloqueia a execução
        }

        // 2. Valida a Aba de Proposta (Garante as Foreign Keys de Banco e Tabela)
        // Só valida se a aba da proposta estiver "suja" (sendo criada ou editada)
        if (abaPropostaHubController.getViewModel().isDirty() && !abaPropostaHubController.getViewModel().isValido()) {
            subTabPane.getSelectionModel().select(2); // Muda automaticamente para a aba "Propostas"
            exibirMensagem(
                    "⚠️ Atenção: Preencha os campos obrigatórios da Proposta (Convênio, Banco e Tabela) antes de salvar.",
                    false);
            return; // Bloqueia a execução antes de chamar o Hibernate!
        }

        // ==========================================================
        // Se passou por todas as barreiras, autoriza o salvamento
        // ==========================================================
        executarSalvamento(null);
    }

    public LeadController getLeadController() {
        return abaLeadController;
    }

    public void setTabPertencente(Tab tab) {
        this.tabPertencente = tab;
    }

    private void executarSalvamento(Runnable onSucesso) {
        Task<ProponenteModel> task = new Task<>() {
            @Override
            protected ProponenteModel call() throws Exception {
                ProponenteModel p = abaLeadController.getViewModel().atualizarModel(proponenteAberto);
                EnderecoProponenteModel e = abaEnderecoController.getViewModel().atualizarModel(
                        (p.getEnderecos() != null && !p.getEnderecos().isEmpty()) ? p.getEnderecos().get(0) : null);

                e.setProponente(p);
                p.setEnderecos(new ArrayList<>(java.util.List.of(e)));

                ProponenteModel proponenteSalvo = atendimentoService.salvarLead(p);

                if (abaPropostaHubController.getViewModel().isDirty()) {
                    PropostaModel prop = abaPropostaHubController.getViewModel().atualizarModel(new PropostaModel());
                    prop.setProponente(proponenteSalvo);

                    UsuarioModel u = new UsuarioModel();
                    u.setId(1L);
                    prop.setUsuario(u);

                    propostaService.salvarProposta(prop);
                }

                return proponenteSalvo;
            }
        };

        task.setOnSucceeded(ev -> {
            ProponenteModel proponenteSalvo = task.getValue();
            inicializarAtendimento(proponenteSalvo);

            if (abaPropostaHubController != null) {
                abaPropostaHubController.getAbaPropostaController().aplicarBloqueio();
            }

            if (tabPertencente != null && proponenteSalvo.getId() != null) {
                tabPertencente.setUserData(String.valueOf(proponenteSalvo.getId()));
            }

            exibirMensagem("Atendimento salvo com sucesso!", true);
            if (onSucesso != null) {
                onSucesso.run();
            }
        });

        task.setOnFailed(ev -> {
            Throwable erro = task.getException();
            if (erro instanceof IllegalArgumentException || erro instanceof IllegalStateException) {
                exibirMensagem(erro.getMessage(), false);
            } else {
                erro.printStackTrace();
                exibirMensagem("Erro ao salvar: " + erro.getMessage(), false);
            }
        });

        new Thread(task).start();
    }

    public void solicitarFechamento(Runnable acaoFecharAba) {
        if (temAlteracoesNaoSalvas()) {
            this.acaoNavegacaoPendente = acaoFecharAba;
            overlayConfirmacaoSaida.setVisible(true);
        } else {
            acaoFecharAba.run();
        }
    }

    @FXML
    public void abrirWhatsappRapido() {
        String tel = abaLeadController.getViewModel().telefoneProperty().get();

        if (tel == null || tel.trim().isEmpty()) {
            exibirMensagem("Por favor, preencha o número de WhatsApp antes de iniciar a conversa.", false);
            return;
        }

        String telLimpo = tel.replaceAll("[^0-9]", "");
        String linkFinal = telLimpo.startsWith("55") ? telLimpo : "55" + telLimpo;

        try {
            mainController.getHostServices().showDocument("https://wa.me/" + linkFinal);
        } catch (Exception e) {
            exibirMensagem("Erro ao tentar abrir o navegador para o WhatsApp.", false);
        }
    }

    @FXML
    public void copiarResumoLead() {
        BigDecimal renda = abaLeadController.getViewModel().rendaProperty().get();
        String rendaStr = (renda != null) ? String.format("%,.2f", renda) : "0,00";

        this.resumoGeradoParaCopia = SummaryGeneratorUtils.gerar(abaLeadController.getViewModel(), rendaStr);

        lblResumoPreview.setText(this.resumoGeradoParaCopia);
        overlayResumo.setVisible(true);
    }

    @FXML
    public void confirmarCopiaResumo() {
        if (this.resumoGeradoParaCopia != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(this.resumoGeradoParaCopia);
            clipboard.setContent(content);

            fecharOverlayResumo();
            exibirMensagem("Relatório copiado com sucesso! Pronto para colar.", true);
        }
    }

    @FXML
    public void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    @FXML
    public void esconderMensagem() {
        overlayMensagem.setVisible(false);
    }

    @FXML
    public void cancelarSaida() {
        overlayConfirmacaoSaida.setVisible(false);
        this.acaoNavegacaoPendente = null;
    }

    @FXML
    public void descartarESair() {
        overlayConfirmacaoSaida.setVisible(false);
        if (acaoNavegacaoPendente != null)
            acaoNavegacaoPendente.run();
    }

    public void exibirMensagem(String texto, boolean sucesso) {
        lblMensagemTexto.setText(texto);
        lblMensagemTitulo.setText(sucesso ? "✅ Sucesso" : "⚠️ Atenção");
        overlayMensagem.setVisible(true);
    }

    public void limparRecursos() {
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        proponenteAberto = null;

        // 🚀 Opcional: Se o consultor fechar a aba do cliente, a IA "esquece" dele
        contextoService.limparContexto();
    }

    // ==========================================================
    // SISTEMA MASTER-SLAVE (Links Úteis)
    // ==========================================================

    @FXML
    private void alternarPainelLinks() {
        boolean isAberto = masterDetailPane.isShowDetailNode();

        if (!isAberto && !slaveJaCarregado) {
            carregarTelaDeLinks();
        }

        masterDetailPane.setShowDetailNode(!isAberto);
    }

    private void carregarTelaDeLinks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/links_uteis.fxml"));
            loader.setControllerFactory(context::getBean);
            javafx.scene.Node viewLinks = loader.load();

            masterDetailPane.setDetailNode(viewLinks);
            slaveJaCarregado = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao carregar a tela de Links no Slave: " + e.getMessage());
        }
    }

    // ========================================================================
    // 🚀 CANAL DE CONTEXTO EM TEMPO REAL PARA A IA
    // ========================================================================
    /**
     * Mescla os dados atuais das telas de cadastro com o modelo persistido,
     * preservando os relacionamentos do Hibernate para consumo do Gemini.
     */
    public ProponenteModel getProponenteComCamposDaTela() {
        ProponenteModel p = abaLeadController.getViewModel().atualizarModel(this.proponenteAberto);

      // 🎯 CONSERTADO: Captura o histórico completo E injeta a proposta ativa/em aberto da tela em tempo real
        if (abaPropostaHubController != null) {
            java.util.List<PropostaModel> propostasFinais = new java.util.ArrayList<>();

            // 1. Coleta o histórico que já está listado na barra lateral da UI
            if (abaPropostaHubController.getListaPropostas() != null) {
                propostasFinais.addAll(abaPropostaHubController.getListaPropostas());
            }

            // 2. Extrai a proposta que está aberta no formulário (Nova ou em Edição)
            if (abaPropostaHubController.getAbaPropostaController() != null) {
                PropostaViewModel propVm = abaPropostaHubController.getAbaPropostaController().getViewModel();
                
                // Extrai um modelo limpo da proposta com os dados atuais digitados na tela
                PropostaModel propostaDaTela = propVm.atualizarModel(new PropostaModel());
                
                // Vincula o proponente contextualizado para o JSON do Gemini não perder a referência
                propostaDaTela.setProponente(p);

                if (propostaDaTela.getId() == null) {
                    // Caso A: É uma NOVA SIMULAÇÃO rascunhada na tela (ainda sem ID no banco).
                    // Filtramos para não injetar lixo/propostas totalmente em branco.
                    if (propostaDaTela.getBanco() != null || 
                       (propostaDaTela.getValorSolicitado() != null && propostaDaTela.getValorSolicitado().compareTo(java.math.BigDecimal.ZERO) > 0)) {
                        propostasFinais.add(propostaDaTela);
                    }
                } else {
                    // Caso B: É uma proposta que já existe no banco, mas está sendo EDITADA/Triada na tela.
                    // Substitui a cópia antiga da lista pela versão fresquinha e modificada da tela!
                    for (int i = 0; i < propostasFinais.size(); i++) {
                        if (propostasFinais.get(i).getId().equals(propostaDaTela.getId())) {
                            propostasFinais.set(i, propostaDaTela);
                            break;
                        }
                    }
                }
            }

            // Injeta a lista consolidada de volta no Proponente que vai para a IA
            p.setPropostas(propostasFinais);
        }

        return p;
    }
}