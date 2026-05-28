package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.BancoUIEventHub;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.facade.IBancoFacade;
import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <h1>BancoController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar o mural de Bancos e
 * Convênios. Implementa o padrão <b>Humble Object</b>, delegando a
 * persistência, filtros e formatações para a {@link IBancoFacade}.
 * </p>
 */
@Component
public class BancoController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(BancoController.class);
    private static final String LOG_PREFIX = "[BancoController]";

    private static final String STYLE_CARD = "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);";
    private static final String STYLE_LBL_NOME = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
    private static final String STYLE_LBL_CODIGO = "-fx-font-size: 12px; -fx-background-color: #ecf0f1; -fx-padding: 3 8; -fx-background-radius: 5; -fx-text-fill: #7f8c8d;";
    private static final String STYLE_LBL_TEL = "-fx-text-fill: #34495e; -fx-font-weight: bold;";
    private static final String STYLE_BTN_ZAP = "-fx-background-color: #e8f8f5; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;";
    private static final String STYLE_BTN_PORTAL = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;";
    private static final String STYLE_BTN_EDITAR = "-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-cursor: hand;";
    private static final String STYLE_BTN_REMOVER = "-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final Navigator navigator;
    private final HostServices hostServices;
    private final IBancoFacade bancoFacade;
    private final BancoUIEventHub bancoEventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private FlowPane muralBancos;
    @FXML private TextField txtBusca;
    @FXML private VBox overlayFormulario;
    @FXML private Label lblTituloModal;
    @FXML private Label lblAviso;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNome;
    @FXML private TextField txtLink;
    @FXML private TextField txtTelefone;
    @FXML private VBox overlayConfirmacaoExclusao;
    @FXML private Label lblConfirmacaoBanco;
    @FXML private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private BancoModel bancoParaExcluir = null;
    private BancoModel bancoEmEdicao = null;

    public BancoController(Navigator navigator, HostServices hostServices, IBancoFacade bancoFacade, BancoUIEventHub bancoEventHub) {
        this.navigator = navigator;
        this.hostServices = hostServices;
        this.bancoFacade = bancoFacade;
        this.bancoEventHub = bancoEventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring (Injeção por Construtor).", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Bancos e Convênios...", LOG_PREFIX);
        bancoEventHub.inscrever(this::recarregarBancos);
        recarregarBancos();
        configurarFiltroReativo();
        txtTelefone.setTextFormatter(ContatoUtils.criarFormatadorTelefone());
        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d banco(s)", Bindings.size(muralBancos.getChildren())));
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo dos hubs de eventos.", LOG_PREFIX);
        bancoEventHub.desinscrever(this::recarregarBancos);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    public void recarregarBancos() {
        log.trace("{} [TELEMETRIA] Recarregando mural de bancos.", LOG_PREFIX);
        filtrarMural(txtBusca.getText());
    }

    private void configurarFiltroReativo() {
        log.trace("{} [UI] Configurando listener de busca reativa.", LOG_PREFIX);
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("{} [UI] Termo de busca alterado: '{}'", LOG_PREFIX, newVal);
            filtrarMural(newVal);
        });
    }

    private void filtrarMural(String termo) {
        AsyncUtils.executarTaskAsync(() -> bancoFacade.filtrarBancos(termo), bancosFiltrados -> {
            muralBancos.getChildren().clear();
            bancosFiltrados.forEach(banco -> muralBancos.getChildren().add(criarCardBanco(banco)));
            log.info("{} [TELEMETRIA] Mural atualizado. {} banco(s) exibido(s).", LOG_PREFIX, bancosFiltrados.size());
        }, erro -> log.error("{} [SISTEMA] Erro ao filtrar bancos: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: RENDERIZAÇÃO DO CARD (UI)
    // ==========================================================================================
    private VBox criarCardBanco(BancoModel banco) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setStyle(STYLE_CARD);

        card.getChildren().addAll(criarHeaderCard(banco), criarContatoCard(banco), new Region(), criarBotaoPortal(banco),
                criarRodapeCard(banco));

        return card;
    }

    private HBox criarHeaderCard(BancoModel banco) {
        Label lblNome = new Label(banco.getNome());
        lblNome.setStyle(STYLE_LBL_NOME);

        String textoCodigo = banco.getCodigo() != null && !banco.getCodigo().isBlank() ? "Cód: " + banco.getCodigo() : "Sem Código";
        Label lblCodigo = new Label(textoCodigo);
        lblCodigo.setStyle(STYLE_LBL_CODIGO);

        HBox header = new HBox(10, lblNome, lblCodigo);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox criarContatoCard(BancoModel banco) {
        String telOriginal = banco.getTelefoneSuporte();
        boolean temTelefone = telOriginal != null && !telOriginal.trim().isEmpty();

        String telExibicao = temTelefone ? ContatoUtils.formatarTelefone(telOriginal) : "Sem telefone";
        Label lblTel = new Label("📞 " + telExibicao);
        lblTel.setStyle(STYLE_LBL_TEL);

        Button btnZap = new Button("💬 WhatsApp");
        btnZap.setStyle(STYLE_BTN_ZAP);
        btnZap.setVisible(temTelefone);
        btnZap.setManaged(temTelefone);
        btnZap.setOnAction(e -> abrirWhatsApp(telOriginal));

        HBox boxContato = new HBox(15, lblTel, btnZap);
        boxContato.setAlignment(Pos.CENTER_LEFT);
        return boxContato;
    }

    private Button criarBotaoPortal(BancoModel banco) {
        Button btnPortal = new Button("🌐 Acessar Portal");
        btnPortal.setMaxWidth(Double.MAX_VALUE);
        btnPortal.setStyle(STYLE_BTN_PORTAL);
        btnPortal.setOnAction(e -> abrirLinkNoNavegador(banco.getSitePortal()));
        return btnPortal;
    }

    private HBox criarRodapeCard(BancoModel banco) {
        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setStyle(STYLE_BTN_EDITAR);
        btnEditar.setOnAction(e -> abrirModalEdicao(banco));

        Button btnRemover = new Button("🗑️ Remover");
        btnRemover.setStyle(STYLE_BTN_REMOVER);

        boolean bloqueada = bancoFacade.isExclusaoBloqueada(banco);
        btnRemover.setDisable(bloqueada);

        if (bloqueada) {
            Tooltip.install(btnRemover, new Tooltip("Exclusão bloqueada: Este banco possui tabelas ativas."));
        } else {
            btnRemover.setOnAction(e -> solicitarExclusaoBanco(banco));
        }

        HBox rodape = new HBox(15, btnRemover, btnEditar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        return rodape;
    }

    // ==========================================================================================
    // MÓDULO 8: AÇÕES DE NEGÓCIO (CRUD)
    // ==========================================================================================
    private void solicitarExclusaoBanco(BancoModel banco) {
        log.info("{} [TELEMETRIA] Usuário solicitou exclusão do banco ID: {}", LOG_PREFIX, banco.getId());
        this.bancoParaExcluir = banco;
        lblConfirmacaoBanco.setText("Você está prestes a excluir o banco:\n\n👉 " + banco.getNome());
        overlayConfirmacaoExclusao.setVisible(true);
    }

    @FXML private void confirmarExclusaoBanco() {
        if (this.bancoParaExcluir == null)
            return;

        Long idBanco = bancoParaExcluir.getId();
        log.info("{} [TELEMETRIA] Confirmando exclusão do banco ID: {}", LOG_PREFIX, idBanco);

        AsyncUtils.executarTaskAsync(() -> {
            bancoFacade.excluirBanco(idBanco);
            return null;
        }, sucesso -> {
            log.info("{} [AUDITORIA] Banco ID {} excluído com sucesso.", LOG_PREFIX, idBanco);
            recarregarBancos();
            cancelarExclusaoBanco();
        }, erro -> {
            log.error("{} [AUDITORIA] Falha ao excluir banco ID {}: {}", LOG_PREFIX, idBanco, erro.getMessage());
            cancelarExclusaoBanco();
            navigator.notificarAviso("Erro ao excluir: " + erro.getMessage());
        });
    }

    @FXML private void cancelarExclusaoBanco() {
        log.trace("{} [UI] Exclusão cancelada pelo usuário.", LOG_PREFIX);
        this.bancoParaExcluir = null;
        overlayConfirmacaoExclusao.setVisible(false);
    }

    @FXML private void salvarBanco() {
        log.info("{} [TELEMETRIA] Tentativa de salvar banco.", LOG_PREFIX);

        if (bancoEmEdicao == null)
            bancoEmEdicao = new BancoModel();

        bancoEmEdicao.setCodigo(txtCodigo.getText());
        bancoEmEdicao.setNome(txtNome.getText());
        bancoEmEdicao.setSitePortal(txtLink.getText());
        bancoEmEdicao.setTelefoneSuporte(txtTelefone.getText());

        AsyncUtils.executarTaskAsync(() -> bancoFacade.salvarBanco(bancoEmEdicao), salvo -> {
            log.info("{} [AUDITORIA] Banco salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
            fecharModal();
            recarregarBancos();
        }, erro -> {
            log.error("{} [AUDITORIA] Falha ao persistir banco: {}", LOG_PREFIX, erro.getMessage());
            exibirAviso(erro.getMessage());
        });
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO MODAL (FORMULÁRIO)
    // ==========================================================================================
    @FXML private void abrirModalNovo() {
        log.trace("{} [UI] Abrindo formulário para novo banco.", LOG_PREFIX);
        prepararModal(new BancoModel(), "➕ Novo Parceiro");
    }

    private void abrirModalEdicao(BancoModel banco) {
        log.trace("{} [UI] Abrindo formulário de edição para banco ID: {}", LOG_PREFIX, banco.getId());
        prepararModal(banco, "✏️ Editando: " + banco.getNome());
    }

    private void prepararModal(BancoModel banco, String titulo) {
        this.bancoEmEdicao = banco;
        lblTituloModal.setText(titulo);

        txtCodigo.setText(banco.getCodigo() != null ? banco.getCodigo() : "");
        txtNome.setText(banco.getNome() != null ? banco.getNome() : "");
        txtLink.setText(banco.getSitePortal() != null ? banco.getSitePortal() : "");
        txtTelefone.setText(banco.getTelefoneSuporte() != null ? banco.getTelefoneSuporte() : "");

        lblAviso.setVisible(false);
        overlayFormulario.setVisible(true);
    }

    @FXML private void fecharModal() {
        log.trace("{} [UI] Modal fechado pelo usuário.", LOG_PREFIX);
        overlayFormulario.setVisible(false);
    }

    private void exibirAviso(String mensagem) {
        log.trace("{} [UI] Exibindo aviso no formulário: {}", LOG_PREFIX, mensagem);
        lblAviso.setText(mensagem);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }

    // ==========================================================================================
    // MÓDULO 10: INTEGRAÇÕES EXTERNAS
    // ==========================================================================================
    private void abrirLinkNoNavegador(String url) {
        String linkFormatado = bancoFacade.formatarUrlPortal(url);
        if (linkFormatado == null) {
            log.warn("{} [NEGOCIO] Tentativa de abrir portal com URL vazia.", LOG_PREFIX);
            return;
        }
        log.info("{} [TELEMETRIA] Abrindo portal no browser: {}", LOG_PREFIX, linkFormatado);
        hostServices.showDocument(linkFormatado);
    }

    private void abrirWhatsApp(String telefone) {
        String linkFormatado = bancoFacade.formatarLinkWhatsApp(telefone);
        if (linkFormatado == null) {
            log.warn("{} [NEGOCIO] Tentativa de abrir WhatsApp sem telefone.", LOG_PREFIX);
            navigator.notificarAviso("Este banco não possui um telefone cadastrado.");
            return;
        }
        log.info("{} [TELEMETRIA] Abrindo WhatsApp Web.", LOG_PREFIX);
        hostServices.showDocument(linkFormatado);
    }
}
