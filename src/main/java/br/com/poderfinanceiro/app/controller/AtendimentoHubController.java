package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.facade.IAtendimentoFacade;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * <h1>AtendimentoHubController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por orquestrar as abas de um
 * Atendimento (Lead, Endereço, Links). Implementa o padrão <b>Humble
 * Object</b>, delegando a persistência e regras de negócio para a
 * {@link IAtendimentoFacade}.
 * </p>
 */
@Component @Scope("prototype")
public class AtendimentoHubController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(AtendimentoHubController.class);
    private static final String LOG_PREFIX = "[AtendimentoHubController]";

    private static final String MSG_SUCESSO_SALVAR = "Atendimento salvo com sucesso!";
    private static final String MSG_ERRO_VALIDACAO = "⚠️ Verifique os campos obrigatórios do cliente antes de salvar.";
    private static final String MSG_ERRO_WHATSAPP = "Por favor, preencha o número de WhatsApp antes de iniciar a conversa.";
    private static final String MSG_SUCESSO_COPIA = "Relatório copiado com sucesso! Pronto para colar.";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IAtendimentoFacade atendimentoFacade;
    private final HostServices hostServices;
    private final ApplicationContext context;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private ProponenteController abaLeadController;
    @FXML private EnderecoController abaEnderecoController;
    @FXML private LinkUtilController abaLinksController;
    @FXML private VBox overlayConfirmacaoSaida, overlayMensagem, overlayResumo;
    @FXML private Button btnSalvar;
    @FXML private Label lblMensagemTexto, lblMensagemTitulo, lblResumoPreview;
    @FXML private TabPane subTabPane;
    @FXML private MasterDetailPane masterDetailPane;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private ProponenteModel proponenteAberto;
    private Runnable acaoNavegacaoPendente;
    private String resumoGeradoParaCopia;
    private Tab tabPertencente;
    private boolean slaveJaCarregado = false;

    public AtendimentoHubController(IAtendimentoFacade atendimentoFacade, HostServices hostServices, ApplicationContext context) {
        this.atendimentoFacade = atendimentoFacade;
        this.hostServices = hostServices;
        this.context = context;
        log.debug("{} [SISTEMA] Controller de AtendimentoHub instanciado.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E BINDINGS
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando Hub de Atendimento...", LOG_PREFIX);
        configurarBloqueioBotaoSalvar();
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    private void configurarBloqueioBotaoSalvar() {
        log.trace("{} [UI] Configurando binding de validação do botão Salvar.", LOG_PREFIX);
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

    // ==========================================================================================
    // MÓDULO 6: CICLO DE VIDA DO ATENDIMENTO
    // ==========================================================================================
    public void inicializarAtendimento(ProponenteModel proponente) {
        log.info("{} [TELEMETRIA] Inicializando atendimento na UI. ID: {}", LOG_PREFIX, proponente != null ? proponente.getId() : "NOVO");
        this.proponenteAberto = proponente;

        abaLeadController.getViewModel().loadFromModel(proponente);
        atendimentoFacade.definirLeadAtivo(proponente);

        if (proponente != null && proponente.getEnderecos() != null && !proponente.getEnderecos().isEmpty()) {
            abaEnderecoController.getViewModel().loadFromModel(proponente.getEnderecos().get(0));
        } else {
            abaEnderecoController.getViewModel().reset();
        }

        if (abaLinksController != null)
            abaLinksController.recarregarLinks();
    }

    public void prepararNovoAtendimento() {
        log.info("{} [TELEMETRIA] Preparando formulário para novo atendimento.", LOG_PREFIX);
        this.proponenteAberto = new ProponenteModel();
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        atendimentoFacade.definirLeadAtivo(getProponenteComCamposDaTela());
    }

    public void limparRecursos() {
        log.trace("{} [LIFECYCLE] Liberando recursos do atendimento.", LOG_PREFIX);
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        proponenteAberto = null;
        atendimentoFacade.limparContextoAtendimento();
    }

    public void solicitarFechamento(Runnable acaoFecharAba) {
        boolean dirty = abaLeadController.getViewModel().isDirty() || abaEnderecoController.getViewModel().isDirty();
        log.info("{} [UI] Solicitação de fechamento. Alterações pendentes: {}", LOG_PREFIX, dirty);

        if (dirty) {
            this.acaoNavegacaoPendente = acaoFecharAba;
            overlayConfirmacaoSaida.setVisible(true);
        } else {
            acaoFecharAba.run();
        }
    }

    // ==========================================================================================
    // MÓDULO 7: FLUXO DE SALVAMENTO
    // ==========================================================================================
    @FXML public void handleSalvar() {
        log.info("{} [TELEMETRIA] Ação acionada: Salvar Atendimento.", LOG_PREFIX);

        if (abaLeadController.getViewModel().isDirty() && !abaLeadController.getViewModel().isValido()) {
            log.warn("{} [NEGOCIO] Salvamento bloqueado por validação.", LOG_PREFIX);
            subTabPane.getSelectionModel().select(0);
            exibirMensagem(MSG_ERRO_VALIDACAO, false);
            return;
        }

        // Snapshot na UI Thread
        ProponenteModel leadSnapshot = abaLeadController.getViewModel().atualizarModel(proponenteAberto);
        EnderecoProponenteModel enderecoSnapshot = abaEnderecoController.getViewModel().atualizarModel(
                (proponenteAberto != null && proponenteAberto.getEnderecos() != null && !proponenteAberto.getEnderecos().isEmpty())
                        ? proponenteAberto.getEnderecos().get(0)
                        : null);

        AsyncUtils.executarTaskAsync(() -> atendimentoFacade.salvarAtendimentoCompleto(leadSnapshot, enderecoSnapshot), proponenteSalvo -> {
            log.info("{} [AUDITORIA] Atendimento salvo com sucesso. ID: {}", LOG_PREFIX, proponenteSalvo.getId());
            inicializarAtendimento(proponenteSalvo);
            if (tabPertencente != null)
                tabPertencente.setUserData(String.valueOf(proponenteSalvo.getId()));
            exibirMensagem(MSG_SUCESSO_SALVAR, true);
        }, erro -> {
            log.error("{} [AUDITORIA] Erro ao salvar atendimento: {}", LOG_PREFIX, erro.getMessage());
            exibirMensagem("Erro ao salvar: " + erro.getMessage(), false);
        });
    }

    // ==========================================================================================
    // MÓDULO 8: INTEGRAÇÕES (WHATSAPP E RESUMO)
    // ==========================================================================================
    @FXML public void abrirWhatsappRapido() {
        String telefone = abaLeadController.getViewModel().telefoneProperty().get();
        String link = atendimentoFacade.formatarLinkWhatsApp(telefone);

        if (link == null) {
            log.warn("{} [NEGOCIO] Abertura de WhatsApp bloqueada: telefone vazio.", LOG_PREFIX);
            exibirMensagem(MSG_ERRO_WHATSAPP, false);
            return;
        }

        log.info("{} [TELEMETRIA] Abrindo WhatsApp Web.", LOG_PREFIX);
        try {
            hostServices.showDocument(link);
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao abrir browser para WhatsApp: {}", LOG_PREFIX, e.getMessage());
            exibirMensagem("Erro ao tentar abrir o navegador.", false);
        }
    }

    @FXML public void copiarResumoLead() {
        log.info("{} [TELEMETRIA] Gerando resumo do lead para cópia.", LOG_PREFIX);
        BigDecimal renda = abaLeadController.getViewModel().rendaProperty().get();
        String rendaStr = (renda != null) ? String.format("%,.2f", renda) : "0,00";

        // Mantido o SummaryGeneratorUtils aqui pois ele depende do ViewModel
        // diretamente
        this.resumoGeradoParaCopia = SummaryGeneratorUtils.gerar(abaLeadController.getViewModel(), rendaStr);

        lblResumoPreview.setText(this.resumoGeradoParaCopia);
        overlayResumo.setVisible(true);
    }

    @FXML public void confirmarCopiaResumo() {
        if (this.resumoGeradoParaCopia != null) {
            log.info("{} [TELEMETRIA] Resumo copiado para a área de transferência.", LOG_PREFIX);
            ClipboardContent content = new ClipboardContent();
            content.putString(this.resumoGeradoParaCopia);
            Clipboard.getSystemClipboard().setContent(content);
            fecharOverlayResumo();
            exibirMensagem(MSG_SUCESSO_COPIA, true);
        }
    }

    // ==========================================================================================
    // MÓDULO 9: MASTER-DETAIL (PAINEL DE LINKS)
    // ==========================================================================================
    @FXML private void alternarPainelLinks() {
        boolean estaAberto = masterDetailPane.isShowDetailNode();
        log.trace("{} [UI] Alternando painel de links. Novo estado: {}", LOG_PREFIX, !estaAberto);

        if (!estaAberto && !slaveJaCarregado) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/links_uteis.fxml"));
                loader.setControllerFactory(context::getBean);
                masterDetailPane.setDetailNode(loader.load());
                slaveJaCarregado = true;
                log.info("{} [SISTEMA] Painel de links carregado via Lazy Load.", LOG_PREFIX);
            } catch (Exception e) {
                log.error("{} [SISTEMA] Falha ao carregar painel de links: {}", LOG_PREFIX, e.getMessage());
                exibirMensagem("Falha ao carregar o painel de links úteis.", false);
            }
        }
        masterDetailPane.setShowDetailNode(!estaAberto);
    }

    // ==========================================================================================
    // MÓDULO 10: OVERLAYS E UTILITÁRIOS
    // ==========================================================================================
    public void exibirMensagem(String texto, boolean sucesso) {
        log.trace("{} [UI] Exibindo overlay de mensagem: {}", LOG_PREFIX, texto);
        lblMensagemTexto.setText(texto);
        lblMensagemTitulo.setText(sucesso ? "✅ Sucesso" : "⚠️ Atenção");
        overlayMensagem.setVisible(true);
    }

    @FXML public void esconderMensagem() {
        overlayMensagem.setVisible(false);
    }

    @FXML public void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    @FXML public void cancelarSaida() {
        overlayConfirmacaoSaida.setVisible(false);
        this.acaoNavegacaoPendente = null;
    }

    @FXML public void descartarESair() {
        log.warn("{} [TELEMETRIA] Usuário descartou alterações e fechou a aba.", LOG_PREFIX);
        overlayConfirmacaoSaida.setVisible(false);
        if (acaoNavegacaoPendente != null)
            acaoNavegacaoPendente.run();
    }

    public ProponenteController getLeadController() {
        return abaLeadController;
    }

    public void setTabPertencente(Tab tab) {
        this.tabPertencente = tab;
    }

    public ProponenteModel getProponenteComCamposDaTela() {
        ProponenteModel proponente = abaLeadController.getViewModel().atualizarModel(this.proponenteAberto);
        if (proponente.getPropostas() == null)
            proponente.setPropostas(new ArrayList<>());
        return proponente;
    }
}
