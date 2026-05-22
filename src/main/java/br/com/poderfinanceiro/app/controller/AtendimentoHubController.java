package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
import java.util.List;

@Component
@Scope("prototype")
public class AtendimentoHubController {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String URL_WHATSAPP_BASE = "https://wa.me/";
    private static final String PREFIXO_BRASIL_WHATSAPP = "55";
    private static final String MSG_SUCESSO_SALVAR = "Atendimento salvo com sucesso!";
    private static final String MSG_ERRO_VALIDACAO = "⚠️ Verifique os campos obrigatórios do cliente antes de salvar.";
    private static final String MSG_ERRO_WHATSAPP = "Por favor, preencha o número de WhatsApp antes de iniciar a conversa.";
    private static final String MSG_SUCESSO_COPIA = "Relatório copiado com sucesso! Pronto para colar.";

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private LeadController abaLeadController;
    @FXML
    private EnderecoController abaEnderecoController;
    @FXML
    private LinkUtilController abaLinksController;

    @FXML
    private VBox overlayConfirmacaoSaida;
    @FXML
    private VBox overlayMensagem;
    @FXML
    private VBox overlayResumo;

    @FXML
    private Button btnSalvar;
    @FXML
    private Label lblMensagemTexto;
    @FXML
    private Label lblMensagemTitulo;
    @FXML
    private Label lblResumoPreview;

    @FXML
    private TabPane subTabPane;
    @FXML
    private MasterDetailPane masterDetailPane;

    // =========================================================================
    // INJEÇÃO DE DEPENDÊNCIAS E ESTADO DA CLASSE
    // =========================================================================
    private final ProponenteService atendimentoService;
    private final MainController mainController;
    private final AtendimentoContextService contextoService;
    private final ApplicationContext context;

    private ProponenteModel proponenteAberto;
    private Runnable acaoNavegacaoPendente;
    private String resumoGeradoParaCopia;
    private Tab tabPertencente;
    private boolean slaveJaCarregado = false;

    public AtendimentoHubController(ProponenteService atendimentoService, MainController mainController,
            ApplicationContext context, AtendimentoContextService contextoService) {
        this.atendimentoService = atendimentoService;
        this.mainController = mainController;
        this.context = context;
        this.contextoService = contextoService;
    }

    // =========================================================================
    // INICIALIZAÇÃO E BINDINGS
    // =========================================================================
    @FXML
    public void initialize() {
        configurarBloqueioBotaoSalvar();
    }

    private void configurarBloqueioBotaoSalvar() {
        BooleanBinding atendimentoSujo = abaLeadController.getViewModel().dirtyProperty()
                .or(abaEnderecoController.getViewModel().dirtyProperty());

        BooleanBinding dadosValidos = criarBindingValidacaoLead();

        btnSalvar.disableProperty().bind(atendimentoSujo.not().or(dadosValidos.not()));
    }

    private BooleanBinding criarBindingValidacaoLead() {
        return Bindings.createBooleanBinding(() -> {
            String nome = abaLeadController.getViewModel().nomeProperty().get();
            String cpf = abaLeadController.getViewModel().cpfProperty().get();

            boolean isNomeValido = nome != null && !nome.trim().isEmpty();
            String cpfLimpo = cpf != null ? cpf.replaceAll("[^0-9]", "") : "";
            boolean isCpfValido = cpfLimpo.isEmpty() || cpfLimpo.length() == 11;

            return isNomeValido && isCpfValido;
        }, abaLeadController.getViewModel().nomeProperty(), abaLeadController.getViewModel().cpfProperty());
    }

    // =========================================================================
    // GERENCIAMENTO DE ESTADO E CICLO DE VIDA DO ATENDIMENTO
    // =========================================================================
    public void inicializarAtendimento(ProponenteModel proponente) {
        this.proponenteAberto = proponente;

        abaLeadController.getViewModel().loadFromModel(proponente);
        contextoService.setLeadAtivo(proponente);

        if (temEnderecoCadastrado(proponente)) {
            abaEnderecoController.getViewModel().loadFromModel(proponente.getEnderecos().get(0));
        } else {
            abaEnderecoController.getViewModel().reset();
        }

        if (abaLinksController != null) {
            abaLinksController.recarregarLinks();
        }
    }

    public void prepararNovoAtendimento() {
        this.proponenteAberto = new ProponenteModel();
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        contextoService.setLeadAtivo(getProponenteComCamposDaTela());
    }

    public boolean temAlteracoesNaoSalvas() {
        return abaLeadController.getViewModel().isDirty() || abaEnderecoController.getViewModel().isDirty();
    }

    public void limparRecursos() {
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        proponenteAberto = null;
        contextoService.limparContexto();
    }

    public void solicitarFechamento(Runnable acaoFecharAba) {
        if (temAlteracoesNaoSalvas()) {
            this.acaoNavegacaoPendente = acaoFecharAba;
            overlayConfirmacaoSaida.setVisible(true);
        } else {
            acaoFecharAba.run();
        }
    }

    // =========================================================================
    // FLUXO DE SALVAMENTO (ASYNC)
    // =========================================================================
    @FXML
    public void handleSalvar() {
        if (abaLeadController.getViewModel().isDirty() && !abaLeadController.getViewModel().isValido()) {
            subTabPane.getSelectionModel().select(0);
            exibirMensagem(MSG_ERRO_VALIDACAO, false);
            return;
        }

        executarSalvamento(null);
    }

    private void executarSalvamento(Runnable onSucesso) {
        Task<ProponenteModel> task = new Task<>() {
            @Override
            protected ProponenteModel call() {
                return mapearESalvarProponente();
            }
        };

        task.setOnSucceeded(ev -> processarSucessoSalvamento(task.getValue(), onSucesso));
        task.setOnFailed(ev -> processarErroSalvamento(task.getException()));

        new Thread(task).start();
    }

    private ProponenteModel mapearESalvarProponente() {
        ProponenteModel lead = abaLeadController.getViewModel().atualizarModel(proponenteAberto);
        EnderecoProponenteModel enderecoAtual = temEnderecoCadastrado(lead) ? lead.getEnderecos().get(0) : null;

        EnderecoProponenteModel enderecoEditado = abaEnderecoController.getViewModel().atualizarModel(enderecoAtual);
        enderecoEditado.setProponente(lead);
        lead.setEnderecos(new ArrayList<>(List.of(enderecoEditado)));

        return atendimentoService.salvarLead(lead);
    }

    private void processarSucessoSalvamento(ProponenteModel proponenteSalvo, Runnable onSucesso) {
        inicializarAtendimento(proponenteSalvo);

        if (tabPertencente != null && proponenteSalvo.getId() != null) {
            tabPertencente.setUserData(String.valueOf(proponenteSalvo.getId()));
        }

        exibirMensagem(MSG_SUCESSO_SALVAR, true);

        if (onSucesso != null) {
            onSucesso.run();
        }
    }

    private void processarErroSalvamento(Throwable erro) {
        if (erro instanceof IllegalArgumentException || erro instanceof IllegalStateException) {
            exibirMensagem(erro.getMessage(), false);
        } else {
            erro.printStackTrace();
            exibirMensagem("Erro ao salvar: " + erro.getMessage(), false);
        }
    }

    // =========================================================================
    // INTEGRAÇÕES E FERRAMENTAS AUXILIARES
    // =========================================================================
    @FXML
    public void abrirWhatsappRapido() {
        String telefone = abaLeadController.getViewModel().telefoneProperty().get();

        if (telefone == null || telefone.trim().isEmpty()) {
            exibirMensagem(MSG_ERRO_WHATSAPP, false);
            return;
        }

        String numeroLimpo = telefone.replaceAll("[^0-9]", "");
        String linkFinal = numeroLimpo.startsWith(PREFIXO_BRASIL_WHATSAPP) ? numeroLimpo
                : PREFIXO_BRASIL_WHATSAPP + numeroLimpo;

        try {
            mainController.getHostServices().showDocument(URL_WHATSAPP_BASE + linkFinal);
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
            enviarParaAreaDeTransferencia(this.resumoGeradoParaCopia);
            fecharOverlayResumo();
            exibirMensagem(MSG_SUCESSO_COPIA, true);
        }
    }

    private void enviarParaAreaDeTransferencia(String texto) {
        ClipboardContent content = new ClipboardContent();
        content.putString(texto);
        Clipboard.getSystemClipboard().setContent(content);
    }

    // =========================================================================
    // MASTER-SLAVE (PAINEL DE LINKS)
    // =========================================================================
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
            Node viewLinks = loader.load();

            masterDetailPane.setDetailNode(viewLinks);
            slaveJaCarregado = true;
        } catch (Exception e) {
            e.printStackTrace();
            exibirMensagem("Falha ao carregar o painel de links úteis.", false);
        }
    }

    // =========================================================================
    // CONTROLE DE OVERLAYS E MENSAGENS UI
    // =========================================================================
    public void exibirMensagem(String texto, boolean sucesso) {
        lblMensagemTexto.setText(texto);
        lblMensagemTitulo.setText(sucesso ? "✅ Sucesso" : "⚠️ Atenção");
        overlayMensagem.setVisible(true);
    }

    @FXML
    public void esconderMensagem() {
        overlayMensagem.setVisible(false);
    }

    @FXML
    public void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    @FXML
    public void cancelarSaida() {
        overlayConfirmacaoSaida.setVisible(false);
        this.acaoNavegacaoPendente = null;
    }

    @FXML
    public void descartarESair() {
        overlayConfirmacaoSaida.setVisible(false);
        if (acaoNavegacaoPendente != null) {
            acaoNavegacaoPendente.run();
        }
    }

    // =========================================================================
    // GETTERS, SETTERS E HELPERS
    // =========================================================================
    public LeadController getLeadController() {
        return abaLeadController;
    }

    public void setTabPertencente(Tab tab) {
        this.tabPertencente = tab;
    }

    public ProponenteModel getProponenteComCamposDaTela() {
        ProponenteModel proponente = abaLeadController.getViewModel().atualizarModel(this.proponenteAberto);

        if (proponente.getPropostas() == null) {
            proponente.setPropostas(new ArrayList<>());
        }

        return proponente;
    }

    private boolean temEnderecoCadastrado(ProponenteModel proponente) {
        return proponente != null && proponente.getEnderecos() != null && !proponente.getEnderecos().isEmpty();
    }
}