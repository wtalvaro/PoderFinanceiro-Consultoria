package br.com.poderfinanceiro.app.presentation.controller.financeiro;

import br.com.poderfinanceiro.app.application.facade.IBancoFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.ContatoUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.ValidationUtils;
import br.com.poderfinanceiro.app.domain.event.BancoUIEventHub;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
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
 * Convênios.
 * Implementa o padrão <b>Humble Object</b>, utilizando ValidationUtils para
 * triagem
 * e AsyncUtils para orquestração via Virtual Threads.
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
    @FXML
    private FlowPane muralBancos;
    @FXML
    private TextField txtBusca;
    @FXML
    private VBox overlayFormulario;
    @FXML
    private Label lblTituloModal;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtLink;
    @FXML
    private TextField txtTelefone;
    @FXML
    private Label lblTotalRegistros;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private BancoModel bancoEmEdicao = null;

    public BancoController(Navigator navigator, HostServices hostServices, IBancoFacade bancoFacade,
            BancoUIEventHub bancoEventHub) {
        this.navigator = navigator;
        this.hostServices = hostServices;
        this.bancoFacade = bancoFacade;
        this.bancoEventHub = bancoEventHub;
        log.info("{} [SISTEMA] Controlador de Bancos instanciado com suporte a Navigator.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface de Bancos e Convênios.", LOG_PREFIX);
        bancoEventHub.inscrever(this::recarregarBancos);
        recarregarBancos();
        configurarFiltroReativo();

        txtTelefone.setTextFormatter(ContatoUtils.criarFormatadorTelefone());

        lblTotalRegistros.textProperty()
                .bind(Bindings.format("Total: %d banco(s)", Bindings.size(muralBancos.getChildren())));

        log.debug("{} [SISTEMA] Inicialização concluída.", LOG_PREFIX);
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos e desinscrevendo de eventos.", LOG_PREFIX);
        bancoEventHub.desinscrever(this::recarregarBancos);
    }

    // ==========================================================================================
    // MÓDULO 6: LÓGICA DE LISTAGEM E FILTRO
    // ==========================================================================================
    public void recarregarBancos() {
        log.info("{} [TELEMETRIA] Iniciando atualização do mural de bancos.", LOG_PREFIX);
        filtrarMural(txtBusca.getText());
    }

    private void configurarFiltroReativo() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.trace("{} [UI] Filtro de busca alterado: '{}'", LOG_PREFIX, newVal);
            filtrarMural(newVal);
        });
    }

    private void filtrarMural(String termo) {
        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Filtrando bancos na base de dados.", LOG_PREFIX);
                    return bancoFacade.filtrarBancos(termo);
                },
                bancosFiltrados -> {
                    muralBancos.getChildren().clear();
                    bancosFiltrados.forEach(banco -> muralBancos.getChildren().add(criarCardBanco(banco)));
                    log.info("{} [AUDITORIA] Mural renderizado com {} bancos.", LOG_PREFIX, bancosFiltrados.size());
                },
                erro -> log.error("{} [SISTEMA] Falha ao filtrar bancos: {}", LOG_PREFIX, erro.getMessage()));
    }

    // ==========================================================================================
    // MÓDULO 7: RENDERIZAÇÃO DO CARD (UI)
    // ==========================================================================================
    private VBox criarCardBanco(BancoModel banco) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setStyle(STYLE_CARD);
        card.getChildren().addAll(criarHeaderCard(banco), criarContatoCard(banco), new Region(),
                criarBotaoPortal(banco), criarRodapeCard(banco));
        return card;
    }

    private HBox criarHeaderCard(BancoModel banco) {
        Label lblNome = new Label(banco.getNome());
        lblNome.setStyle(STYLE_LBL_NOME);
        String textoCodigo = banco.getCodigo() != null && !banco.getCodigo().isBlank() ? "Cód: " + banco.getCodigo()
                : "S/C";
        Label lblCodigo = new Label(textoCodigo);
        lblCodigo.setStyle(STYLE_LBL_CODIGO);
        HBox header = new HBox(10, lblNome, lblCodigo);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox criarContatoCard(BancoModel banco) {
        String telOriginal = banco.getTelefoneSuporte();
        boolean temTelefone = ValidationUtils.isPreenchido(telOriginal);
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
        log.info("{} [TELEMETRIA] Solicitando confirmação para exclusão do banco ID: {}", LOG_PREFIX, banco.getId());

        navigator.solicitarConfirmacao("🗑️ Excluir Parceiro",
                "Tem certeza que deseja excluir o banco " + banco.getNome() + "?\nEsta ação não poderá ser desfeita.",
                "Sim, Excluir", "#e74c3c", () -> executarExclusaoReal(banco.getId()));
    }

    private void executarExclusaoReal(Long idBanco) {
        log.info("{} [TELEMETRIA] Iniciando exclusão assíncrona do banco ID: {}", LOG_PREFIX, idBanco);
        navigator.mostrarLoading("Excluindo parceiro...");

        AsyncUtils.executarTaskAsync(
                () -> {
                    bancoFacade.excluirBanco(idBanco);
                    return null;
                },
                sucesso -> {
                    navigator.ocultarLoading();
                    log.info("{} [AUDITORIA] Banco ID {} removido com sucesso.", LOG_PREFIX, idBanco);
                    recarregarBancos();
                    navigator.notificarSucesso("Banco removido da base de parceiros.");
                },
                erro -> {
                    navigator.ocultarLoading();
                    log.error("{} [AUDITORIA] Falha ao excluir banco: {}", LOG_PREFIX, erro.getMessage());
                    navigator.notificarAviso("Erro ao excluir: " + erro.getMessage());
                });
    }

    @FXML
    private void salvarBanco() {
        String codigo = txtCodigo.getText().trim();
        String nome = txtNome.getText().trim();
        String link = txtLink.getText().trim();
        String telefone = txtTelefone.getText().trim();

        log.info("{} [TELEMETRIA] Tentativa de salvamento de banco iniciada. Nome: '{}'", LOG_PREFIX, nome);

        if (!validarFormulario(codigo, nome, link)) {
            return;
        }

        if (bancoEmEdicao == null)
            bancoEmEdicao = new BancoModel();

        bancoEmEdicao.setCodigo(codigo);
        bancoEmEdicao.setNome(nome);
        bancoEmEdicao.setSitePortal(link);
        bancoEmEdicao.setTelefoneSuporte(telefone);

        navigator.mostrarLoading("Salvando dados do banco...");

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("{} [NEGOCIO] Invocando Facade para persistência do banco.", LOG_PREFIX);
                    return bancoFacade.salvarBanco(bancoEmEdicao);
                },
                salvo -> {
                    navigator.ocultarLoading();
                    log.info("{} [AUDITORIA] Banco salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
                    fecharModal();
                    recarregarBancos();
                    navigator.notificarSucesso("Dados do parceiro atualizados com sucesso!");
                },
                erro -> {
                    navigator.ocultarLoading();
                    log.error("{} [AUDITORIA] Falha crítica ao persistir banco: {}", LOG_PREFIX, erro.getMessage());
                    navigator.notificarAviso("Erro ao salvar: " + erro.getMessage());
                });
    }

    private boolean validarFormulario(String codigo, String nome, String link) {
        if (!ValidationUtils.isPreenchido(codigo) || !ValidationUtils.isPreenchido(nome)) {
            log.warn("{} [NEGOCIO] Validação falhou: Nome ou Código ausentes.", LOG_PREFIX);
            navigator.notificarAviso("Nome e Código do banco são obrigatórios.");
            return false;
        }

        if (ValidationUtils.isPreenchido(link) && !ValidationUtils.isUrlValida(link)) {
            log.warn("{} [NEGOCIO] Validação falhou: URL do portal inválida.", LOG_PREFIX);
            navigator.notificarAviso("A URL do portal informada é inválida.");
            return false;
        }

        return true;
    }

    // ==========================================================================================
    // MÓDULO 9: GESTÃO DO MODAL (FORMULÁRIO)
    // ==========================================================================================
    @FXML
    private void abrirModalNovo() {
        log.info("{} [TELEMETRIA] Abrindo formulário para novo banco.", LOG_PREFIX);
        prepararModal(new BancoModel(), "➕ Novo Parceiro");
    }

    private void abrirModalEdicao(BancoModel banco) {
        log.info("{} [TELEMETRIA] Abrindo formulário de edição para o banco ID: {}", LOG_PREFIX, banco.getId());
        prepararModal(banco, "✏️ Editando: " + banco.getNome());
    }

    private void prepararModal(BancoModel banco, String titulo) {
        this.bancoEmEdicao = banco;
        lblTituloModal.setText(titulo);
        txtCodigo.setText(banco.getCodigo() != null ? banco.getCodigo() : "");
        txtNome.setText(banco.getNome() != null ? banco.getNome() : "");
        txtLink.setText(banco.getSitePortal() != null ? banco.getSitePortal() : "");
        txtTelefone.setText(banco.getTelefoneSuporte() != null ? banco.getTelefoneSuporte() : "");
        overlayFormulario.setVisible(true);
    }

    @FXML
    private void fecharModal() {
        log.trace("{} [UI] Fechando overlay de formulário.", LOG_PREFIX);
        overlayFormulario.setVisible(false);
    }

    // ==========================================================================================
    // MÓDULO 10: INTEGRAÇÕES EXTERNAS
    // ==========================================================================================
    private void abrirLinkNoNavegador(String url) {
        String link = bancoFacade.formatarUrlPortal(url);
        if (link != null) {
            log.info("{} [TELEMETRIA] Invocando navegador para URL: {}", LOG_PREFIX, link);
            hostServices.showDocument(link);
        }
    }

    private void abrirWhatsApp(String telefone) {
        String link = bancoFacade.formatarLinkWhatsApp(telefone);
        if (link != null) {
            log.info("{} [TELEMETRIA] Invocando WhatsApp para o número: {}", LOG_PREFIX, telefone);
            hostServices.showDocument(link);
        } else {
            log.warn("{} [NEGOCIO] Tentativa de abrir WhatsApp sem número cadastrado.", LOG_PREFIX);
            navigator.notificarAviso("Este banco não possui um WhatsApp de suporte cadastrado.");
        }
    }
}
