package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class AtendimentoHubController {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoHubController.class);

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
    private ProponenteController abaLeadController;
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
    private final HostServices hostServices;
    private final AtendimentoContextService contextoService;
    private final ApplicationContext context;

    private ProponenteModel proponenteAberto;
    private Runnable acaoNavegacaoPendente;
    private String resumoGeradoParaCopia;
    private Tab tabPertencente;
    private boolean slaveJaCarregado = false;

    public AtendimentoHubController(ProponenteService atendimentoService, HostServices hostServices,
            ApplicationContext context, AtendimentoContextService contextoService) {
        this.atendimentoService = atendimentoService;
        this.hostServices = hostServices;
        this.context = context;
        this.contextoService = contextoService;
        log.debug("[HUB] Controller instanciado (prototype). hashCode={}", System.identityHashCode(this));
    }

    // =========================================================================
    // INICIALIZAÇÃO E BINDINGS
    // =========================================================================
    @FXML
    public void initialize() {
        log.info("[HUB] Inicializando AtendimentoHubController...");
        configurarBloqueioBotaoSalvar();
        log.info("[HUB] Inicialização concluída. Bindings de validação ativos.");
    }

    private void configurarBloqueioBotaoSalvar() {
        log.debug("[HUB][BINDING] Configurando binding do botão Salvar...");

        BooleanBinding atendimentoSujo = abaLeadController.getViewModel().dirtyProperty()
                .or(abaEnderecoController.getViewModel().dirtyProperty());

        BooleanBinding dadosValidos = criarBindingValidacaoLead();

        btnSalvar.disableProperty().bind(atendimentoSujo.not().or(dadosValidos.not()));

        // Listener de diagnóstico: loga quando o estado do botão muda,
        // permitindo rastrear se o binding está disparando corretamente
        btnSalvar.disableProperty()
                .addListener((obs, eraDesabilitado, agoraDesabilitado) -> log.debug(
                        "[HUB][BINDING] Botão Salvar alterado → {}. dirty(lead)={} | dirty(endereco)={} | valido={}",
                        agoraDesabilitado ? "DESABILITADO" : "HABILITADO",
                        abaLeadController.getViewModel().isDirty(),
                        abaEnderecoController.getViewModel().isDirty(),
                        dadosValidos.get()));

        log.debug("[HUB][BINDING] Binding do botão Salvar configurado com sucesso.");
    }

    private BooleanBinding criarBindingValidacaoLead() {
        return Bindings.createBooleanBinding(() -> {
            String nome = abaLeadController.getViewModel().nomeProperty().get();
            String cpf = abaLeadController.getViewModel().cpfProperty().get();

            boolean isNomeValido = nome != null && !nome.trim().isEmpty();
            String cpfLimpo = cpf != null ? cpf.replaceAll("[^0-9]", "") : "";
            boolean isCpfValido = cpfLimpo.isEmpty() || cpfLimpo.length() == 11;

            log.debug("[HUB][VALIDAÇÃO] Revalidando formulário. nome='{}' (válido={}) | cpf={} dígitos (válido={})",
                    (nome != null && !nome.trim().isEmpty())
                            ? nome.trim().substring(0, Math.min(nome.trim().length(), 10)) + "..."
                            : "vazio",
                    isNomeValido,
                    cpfLimpo.length(),
                    isCpfValido);

            return isNomeValido && isCpfValido;
        }, abaLeadController.getViewModel().nomeProperty(), abaLeadController.getViewModel().cpfProperty());
    }

    // =========================================================================
    // GERENCIAMENTO DE ESTADO E CICLO DE VIDA DO ATENDIMENTO
    // =========================================================================
    public void inicializarAtendimento(ProponenteModel proponente) {
        boolean isNovo = proponente == null || proponente.getId() == null;
        boolean temEndereco = temEnderecoCadastrado(proponente);

        log.info("[HUB][CICLO] Inicializando atendimento. ID={} | Novo={} | Tem endereço={}",
                (proponente != null && proponente.getId() != null) ? proponente.getId() : "N/A",
                isNovo,
                temEndereco);

        this.proponenteAberto = proponente;

        abaLeadController.getViewModel().loadFromModel(proponente);
        contextoService.setLeadAtivo(proponente);
        log.debug("[HUB][CICLO] ViewModel do lead populado e contexto de atendimento atualizado. ID={}",
                proponente != null ? proponente.getId() : "null");

        if (proponente != null && temEndereco) {
            EnderecoProponenteModel endereco = proponente.getEnderecos().get(0);
            abaEnderecoController.getViewModel().loadFromModel(endereco);
            log.debug("[HUB][CICLO] Endereço carregado no ViewModel.");
        } else {
            abaEnderecoController.getViewModel().reset();
            log.debug("[HUB][CICLO] Nenhum endereço cadastrado. ViewModel de endereço resetado.");
        }

        if (abaLinksController != null) {
            abaLinksController.recarregarLinks();
            log.debug("[HUB][CICLO] Links úteis recarregados.");
        }

        log.info("[HUB][CICLO] Atendimento ID={} pronto para interação.",
                proponente != null ? proponente.getId() : "NOVO");
    }

    public void prepararNovoAtendimento() {
        log.info("[HUB][CICLO] Preparando formulário para novo atendimento (proponente em branco).");
        this.proponenteAberto = new ProponenteModel();
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        contextoService.setLeadAtivo(getProponenteComCamposDaTela());
        log.debug("[HUB][CICLO] Novo atendimento pronto. ViewModels resetados e contexto limpo.");
    }

    public boolean temAlteracoesNaoSalvas() {
        boolean dirty = abaLeadController.getViewModel().isDirty()
                || abaEnderecoController.getViewModel().isDirty();
        log.debug(
                "[HUB][ESTADO] Verificação de alterações não salvas: dirty(lead)={} | dirty(endereço)={} → resultado={}",
                abaLeadController.getViewModel().isDirty(),
                abaEnderecoController.getViewModel().isDirty(),
                dirty);
        return dirty;
    }

    public void limparRecursos() {
        Long idFechado = (proponenteAberto != null) ? proponenteAberto.getId() : null;
        log.info("[HUB][CICLO] Liberando recursos do atendimento. ID={}", idFechado != null ? idFechado : "NOVO");

        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        proponenteAberto = null;
        contextoService.limparContexto();

        log.debug("[HUB][CICLO] Recursos liberados. ViewModels resetados, contexto limpo.");
    }

    public void solicitarFechamento(Runnable acaoFecharAba) {
        boolean dirty = temAlteracoesNaoSalvas();
        log.info("[HUB][NAVEGAÇÃO] Solicitação de fechamento de aba. Alterações pendentes: {}", dirty);

        if (dirty) {
            this.acaoNavegacaoPendente = acaoFecharAba;
            overlayConfirmacaoSaida.setVisible(true);
            log.info("[HUB][NAVEGAÇÃO] Overlay de confirmação exibido. Fechamento aguardando decisão do usuário.");
        } else {
            log.debug("[HUB][NAVEGAÇÃO] Sem alterações pendentes. Fechando aba diretamente.");
            acaoFecharAba.run();
        }
    }

    // =========================================================================
    // FLUXO DE SALVAMENTO (ASYNC)
    // =========================================================================
    @FXML
    public void handleSalvar() {
        boolean leadDirty = abaLeadController.getViewModel().isDirty();
        boolean leadValido = abaLeadController.getViewModel().isValido();

        log.info("[HUB][SALVAR] Ação de salvar disparada. lead dirty={} | lead válido={} | endereço dirty={}",
                leadDirty,
                leadValido,
                abaEnderecoController.getViewModel().isDirty());

        if (leadDirty && !leadValido) {
            log.warn("[HUB][SALVAR] Salvamento bloqueado por validação. Redirecionando para aba do cliente.");
            subTabPane.getSelectionModel().select(0);
            exibirMensagem(MSG_ERRO_VALIDACAO, false);
            return;
        }

        log.debug("[HUB][SALVAR] Validação aprovada. Iniciando fluxo de salvamento assíncrono...");
        executarSalvamento(null);
    }

    private void executarSalvamento(Runnable onSucesso) {
        Long idAtual = (proponenteAberto != null) ? proponenteAberto.getId() : null;
        log.info("[HUB][SALVAR] Disparando task assíncrona. Proponente ID={} ({})",
                idAtual != null ? idAtual : "N/A",
                idAtual == null ? "INSERT" : "UPDATE");

        long inicioSalvamento = System.currentTimeMillis();

        AsyncUtils.executarTaskAsync(
                this::mapearESalvarProponente,
                proponenteSalvo -> {
                    long tempo = System.currentTimeMillis() - inicioSalvamento;
                    log.info("[HUB][SALVAR] Salvo com sucesso em {}ms. ID gerado/confirmado={}",
                            tempo, proponenteSalvo.getId());
                    processarSucessoSalvamento(proponenteSalvo, onSucesso);
                },
                erro -> {
                    long tempo = System.currentTimeMillis() - inicioSalvamento;
                    log.error("[HUB][SALVAR] Falha no salvamento após {}ms. Tipo de erro: {}",
                            tempo, erro.getClass().getSimpleName());
                    processarErroSalvamento(erro);
                });
    }

    private ProponenteModel mapearESalvarProponente() {
        log.debug("[HUB][SALVAR] Mapeando ViewModel → Model para persistência...");

        ProponenteModel lead = abaLeadController.getViewModel().atualizarModel(proponenteAberto);

        EnderecoProponenteModel enderecoAtual = temEnderecoCadastrado(lead)
                ? lead.getEnderecos().get(0)
                : null;

        boolean isNovoEndereco = enderecoAtual == null;
        EnderecoProponenteModel enderecoEditado = abaEnderecoController.getViewModel().atualizarModel(enderecoAtual);
        enderecoEditado.setProponente(lead);
        lead.setEnderecos(new ArrayList<>(List.of(enderecoEditado)));

        log.debug("[HUB][SALVAR] Mapeamento concluído. lead.id={} | endereço: {} | thread='{}'",
                lead.getId() != null ? lead.getId() : "NOVO",
                isNovoEndereco ? "NOVO (INSERT)" : "EXISTENTE (UPDATE)",
                Thread.currentThread().getName());

        return atendimentoService.salvarProponente(lead);
    }

    private void processarSucessoSalvamento(ProponenteModel proponenteSalvo, Runnable onSucesso) {
        log.info("[HUB][SALVAR] Pós-salvamento: inicializando tela com dados persistidos. ID={}",
                proponenteSalvo.getId());

        inicializarAtendimento(proponenteSalvo);

        if (tabPertencente != null && proponenteSalvo.getId() != null) {
            String novoUserData = String.valueOf(proponenteSalvo.getId());
            tabPertencente.setUserData(novoUserData);
            log.debug("[HUB][SALVAR] UserData da aba atualizado para '{}'.", novoUserData);
        }

        exibirMensagem(MSG_SUCESSO_SALVAR, true);

        if (onSucesso != null) {
            log.debug("[HUB][SALVAR] Executando callback onSucesso pós-salvamento.");
            onSucesso.run();
        }
    }

    private void processarErroSalvamento(Throwable erro) {
        if (erro instanceof IllegalArgumentException || erro instanceof IllegalStateException) {
            // Erros de validação de negócio: exibidos para o usuário, sem stack trace
            log.warn("[HUB][SALVAR] Erro de negócio ao salvar: {}", erro.getMessage());
            exibirMensagem(erro.getMessage(), false);
        } else {
            // Erros inesperados (BD, mapeamento, etc.): loga com stack trace para
            // diagnóstico
            log.error("[HUB][SALVAR] Erro inesperado ao persistir proponente ID={}: {}",
                    (proponenteAberto != null ? proponenteAberto.getId() : "N/A"),
                    erro.getMessage(), erro);
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
            log.warn("[HUB][WHATSAPP] Abertura bloqueada: nenhum telefone preenchido. Proponente ID={}",
                    proponenteAberto != null ? proponenteAberto.getId() : "N/A");
            exibirMensagem(MSG_ERRO_WHATSAPP, false);
            return;
        }

        String numeroLimpo = telefone.replaceAll("[^0-9]", "");
        boolean jaTemPrefixo = numeroLimpo.startsWith(PREFIXO_BRASIL_WHATSAPP);
        String linkFinal = jaTemPrefixo ? numeroLimpo : PREFIXO_BRASIL_WHATSAPP + numeroLimpo;

        // Loga o número mascarado (últimos 4 dígitos) para rastrear sem expor dados
        // pessoais
        String numeroMascarado = "55****" + linkFinal.substring(Math.max(0, linkFinal.length() - 4));
        log.info("[HUB][WHATSAPP] Abrindo WhatsApp. Número mascarado: {} | Prefixo já presente: {}",
                numeroMascarado, jaTemPrefixo);

        try {
            hostServices.showDocument(URL_WHATSAPP_BASE + linkFinal);
            log.debug("[HUB][WHATSAPP] Browser aberto com sucesso.");
        } catch (Exception e) {
            log.error("[HUB][WHATSAPP] Falha ao abrir browser para WhatsApp: {}", e.getMessage(), e);
            exibirMensagem("Erro ao tentar abrir o navegador para o WhatsApp.", false);
        }
    }

    @FXML
    public void copiarResumoLead() {
        BigDecimal renda = abaLeadController.getViewModel().rendaProperty().get();
        String rendaStr = (renda != null) ? String.format("%,.2f", renda) : "0,00";

        log.info("[HUB][RESUMO] Gerando resumo do lead para cópia. Proponente ID={} | Renda={}",
                proponenteAberto != null ? proponenteAberto.getId() : "N/A",
                rendaStr);

        this.resumoGeradoParaCopia = SummaryGeneratorUtils.gerar(abaLeadController.getViewModel(), rendaStr);

        log.debug("[HUB][RESUMO] Resumo gerado. Tamanho: {} chars.", resumoGeradoParaCopia.length());
        lblResumoPreview.setText(this.resumoGeradoParaCopia);
        overlayResumo.setVisible(true);
    }

    @FXML
    public void confirmarCopiaResumo() {
        if (this.resumoGeradoParaCopia != null) {
            log.info("[HUB][RESUMO] Usuário confirmou cópia. Enviando para área de transferência ({} chars).",
                    resumoGeradoParaCopia.length());
            enviarParaAreaDeTransferencia(this.resumoGeradoParaCopia);
            fecharOverlayResumo();
            exibirMensagem(MSG_SUCESSO_COPIA, true);
        } else {
            // Não deveria ocorrer em fluxo normal — útil para detectar bug de estado
            log.warn("[HUB][RESUMO] confirmarCopiaResumo chamado mas resumoGeradoParaCopia é null. " +
                    "Possível inconsistência de estado.");
        }
    }

    private void enviarParaAreaDeTransferencia(String texto) {
        ClipboardContent content = new ClipboardContent();
        content.putString(texto);
        Clipboard.getSystemClipboard().setContent(content);
        log.debug("[HUB][RESUMO] Conteúdo enviado para o clipboard do sistema.");
    }

    // =========================================================================
    // MASTER-SLAVE (PAINEL DE LINKS)
    // =========================================================================
    @FXML
    private void alternarPainelLinks() {
        boolean estaAberto = masterDetailPane.isShowDetailNode();
        log.info("[HUB][LINKS] Alternando painel de links. Estado atual: {} → {}",
                estaAberto ? "ABERTO" : "FECHADO",
                estaAberto ? "FECHADO" : "ABERTO");

        if (!estaAberto && !slaveJaCarregado) {
            log.debug("[HUB][LINKS] Primeira abertura detectada. Carregando FXML do painel de links...");
            carregarTelaDeLinks();
        }

        masterDetailPane.setShowDetailNode(!estaAberto);
    }

    private void carregarTelaDeLinks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/links_uteis.fxml"));
            loader.setControllerFactory(context::getBean);
            Node viewLinks = loader.load();

            masterDetailPane.setDetailNode(viewLinks);
            slaveJaCarregado = true;
            log.info("[HUB][LINKS] Painel de links carregado com sucesso (lazy load concluído).");
        } catch (Exception e) {
            // Falha de carregamento de FXML é crítica — o painel ficará em branco sem este
            // log
            log.error("[HUB][LINKS] FALHA ao carregar '/fxml/links_uteis.fxml'. " +
                    "Verifique se o arquivo existe no classpath. Erro: {}", e.getMessage(), e);
            exibirMensagem("Falha ao carregar o painel de links úteis.", false);
        }
    }

    // =========================================================================
    // CONTROLE DE OVERLAYS E MENSAGENS UI
    // =========================================================================
    public void exibirMensagem(String texto, boolean sucesso) {
        log.debug("[HUB][UI] Exibindo overlay de mensagem. Tipo={} | Texto='{}'",
                sucesso ? "SUCESSO" : "ATENÇÃO", texto);
        lblMensagemTexto.setText(texto);
        lblMensagemTitulo.setText(sucesso ? "✅ Sucesso" : "⚠️ Atenção");
        overlayMensagem.setVisible(true);
    }

    @FXML
    public void esconderMensagem() {
        log.debug("[HUB][UI] Overlay de mensagem fechado pelo usuário.");
        overlayMensagem.setVisible(false);
    }

    @FXML
    public void fecharOverlayResumo() {
        log.debug("[HUB][UI] Overlay de resumo fechado. Resumo gerado descartado da memória.");
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    @FXML
    public void cancelarSaida() {
        log.info("[HUB][NAVEGAÇÃO] Usuário cancelou saída. Permanecendo na aba. Proponente ID={}",
                proponenteAberto != null ? proponenteAberto.getId() : "NOVO");
        overlayConfirmacaoSaida.setVisible(false);
        this.acaoNavegacaoPendente = null;
    }

    @FXML
    public void descartarESair() {
        log.warn("[HUB][NAVEGAÇÃO] Usuário descartou alterações não salvas e saiu. Proponente ID={}",
                proponenteAberto != null ? proponenteAberto.getId() : "NOVO");
        overlayConfirmacaoSaida.setVisible(false);
        if (acaoNavegacaoPendente != null) {
            acaoNavegacaoPendente.run();
        }
    }

    // =========================================================================
    // GETTERS, SETTERS E HELPERS
    // =========================================================================
    public ProponenteController getLeadController() {
        return abaLeadController;
    }

    public void setTabPertencente(Tab tab) {
        log.debug("[HUB] Tab associada ao controller definida: '{}'",
                tab != null ? tab.getText() : "null");
        this.tabPertencente = tab;
    }

    public ProponenteModel getProponenteComCamposDaTela() {
        log.debug("[HUB] Montando ProponenteModel a partir dos campos atuais da tela...");
        ProponenteModel proponente = abaLeadController.getViewModel().atualizarModel(this.proponenteAberto);

        if (proponente.getPropostas() == null) {
            proponente.setPropostas(new ArrayList<>());
            log.debug("[HUB] Lista de propostas inicializada (era null).");
        }

        return proponente;
    }

    private boolean temEnderecoCadastrado(ProponenteModel proponente) {
        return proponente != null
                && proponente.getEnderecos() != null
                && !proponente.getEnderecos().isEmpty();
    }
}