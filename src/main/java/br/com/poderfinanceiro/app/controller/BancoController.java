package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.BancoUIEventHub;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.service.BancoService;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import javafx.beans.binding.Bindings;

@Component
public class BancoController implements Disposable {

    private static final Logger log = LoggerFactory.getLogger(BancoController.class);

    // =========================================================================
    // CONSTANTES DE ESTILIZAÇÃO E MENSAGENS (Clean Code & DRY)
    // =========================================================================
    private static final String STYLE_CARD = "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);";
    private static final String STYLE_LBL_NOME = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
    private static final String STYLE_LBL_CODIGO = "-fx-font-size: 12px; -fx-background-color: #ecf0f1; -fx-padding: 3 8; -fx-background-radius: 5; -fx-text-fill: #7f8c8d;";
    private static final String STYLE_LBL_TEL = "-fx-text-fill: #34495e; -fx-font-weight: bold;";
    private static final String STYLE_BTN_ZAP = "-fx-background-color: #e8f8f5; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;";
    private static final String STYLE_BTN_PORTAL = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;";
    private static final String STYLE_BTN_EDITAR = "-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-cursor: hand;";
    private static final String STYLE_BTN_REMOVER = "-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;";

    private static final String MSG_NOME_OBRIGATORIO = "O Nome do banco é obrigatório!";
    private static final String URL_WHATSAPP_BASE = "https://wa.me/";

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    private final Navigator navigator;
    private final HostServices hostServices;
    private final BancoService bancoService;
    private final BancoUIEventHub bancoEventHub;

    // =========================================================================
    // COMPONENTES DE UI (FXML)
    // =========================================================================
    @FXML
    private FlowPane muralBancos;
    @FXML
    private TextField txtBusca;
    @FXML
    private VBox overlayFormulario;
    @FXML
    private Label lblTituloModal;
    @FXML
    private Label lblAviso;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtLink;
    @FXML
    private TextField txtTelefone;
    @FXML
    private VBox overlayConfirmacaoExclusao;
    @FXML
    private Label lblConfirmacaoBanco;
    @FXML
    private Label lblTotalRegistros;

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private BancoModel bancoParaExcluir = null;
    private BancoModel bancoEmEdicao = null;
    private List<BancoModel> todosBancos;

    public BancoController(Navigator navigator, HostServices hostServices, BancoService bancoService, BancoUIEventHub bancoEventHub) {
        this.navigator = navigator;
        this.hostServices = hostServices;
        this.bancoService = bancoService;
        this.bancoEventHub = bancoEventHub;
    }

    @FXML
    public void initialize() {
        log.info("[BANCOS] Inicializando BancosConveniosController...");
        bancoEventHub.inscrever(this::recarregarBancos);
        recarregarBancos();
        configurarFiltroReativo();
        txtTelefone.setTextFormatter(ContatoUtils.criarFormatadorTelefone());
        lblTotalRegistros.textProperty().bind(
                Bindings.format("Total: %d banco(s)", Bindings.size(muralBancos.getChildren())));
        log.info("[BANCOS] Inicialização concluída. {} banco(s) carregado(s).",
                todosBancos != null ? todosBancos.size() : 0);
    }

    // =========================================================================
    // LÓGICA DE LISTAGEM E FILTRO
    // =========================================================================
    public void recarregarBancos() {
        todosBancos = bancoService.listarTodos();
        filtrarMural(txtBusca.getText());
    }

    private void configurarFiltroReativo() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("[BANCOS][FILTRO] Termo de busca alterado: '{}' → '{}'", oldVal, newVal);
            filtrarMural(newVal);
        });
        log.debug("[BANCOS][FILTRO] Listener de filtro reativo registrado no campo de busca.");
    }

    private void filtrarMural(String termo) {
        muralBancos.getChildren().clear();

        boolean temFiltro = termo != null && !termo.isBlank();

        long count = todosBancos.stream()
                .filter(banco -> atendeCriterioDeBusca(banco, termo))
                .peek(banco -> muralBancos.getChildren().add(criarCardBanco(banco)))
                .count();

        // Log de filtro: permite saber se a busca está retornando resultados esperados
        if (temFiltro) {
            log.debug("[BANCOS][FILTRO] Filtro '{}': {}/{} banco(s) exibido(s).",
                    termo, count, todosBancos.size());
        } else {
            log.debug("[BANCOS][FILTRO] Sem filtro ativo: exibindo todos os {} banco(s).", count);
        }
    }

    private boolean atendeCriterioDeBusca(BancoModel banco, String termo) {
        if (termo == null || termo.isBlank())
            return true;

        String termoLower = termo.toLowerCase();
        boolean matchNome = banco.getNome().toLowerCase().contains(termoLower);
        boolean matchCodigo = banco.getCodigo() != null && banco.getCodigo().contains(termo);

        return matchNome || matchCodigo;
    }

    // =========================================================================
    // RENDERIZAÇÃO DO CARD (SRP Aplicado)
    // =========================================================================
    private VBox criarCardBanco(BancoModel banco) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setStyle(STYLE_CARD);

        card.getChildren().addAll(
                criarHeaderCard(banco),
                criarContatoCard(banco),
                new Region(),
                criarBotaoPortal(banco),
                criarRodapeCard(banco));

        return card;
    }

    private HBox criarHeaderCard(BancoModel banco) {
        Label lblNome = new Label(banco.getNome());
        lblNome.setStyle(STYLE_LBL_NOME);

        String textoCodigo = banco.getCodigo() != null && !banco.getCodigo().isBlank()
                ? "Cód: " + banco.getCodigo()
                : "Sem Código";
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

        boolean temTabelas = banco.getTabelas() != null && !banco.getTabelas().isEmpty();
        btnRemover.setDisable(temTabelas);

        if (temTabelas) {
            // Log de diagnóstico: permite auditar quais bancos têm exclusão bloqueada e por
            // quê
            log.debug("[BANCOS][CARD] Exclusão do banco '{}' (ID={}) bloqueada: possui {} tabela(s) ativa(s).",
                    banco.getNome(), banco.getId(),
                    banco.getTabelas().size());
            Tooltip.install(btnRemover, new Tooltip("Exclusão bloqueada: Este banco possui tabelas ativas."));
        } else {
            btnRemover.setOnAction(e -> solicitarExclusaoBanco(banco));
        }

        HBox rodape = new HBox(15, btnRemover, btnEditar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        return rodape;
    }

    // =========================================================================
    // EXCLUSÃO DE BANCO
    // =========================================================================
    private void solicitarExclusaoBanco(BancoModel banco) {
        log.info("[BANCOS][EXCLUSÃO] Usuário solicitou exclusão do banco '{}' (ID={}).",
                banco.getNome(), banco.getId());
        this.bancoParaExcluir = banco;
        lblConfirmacaoBanco.setText("Você está prestes a excluir o banco:\n\n👉 " + banco.getNome());
        overlayConfirmacaoExclusao.setVisible(true);
    }

    @FXML
    private void confirmarExclusaoBanco() {
        if (this.bancoParaExcluir == null) {
            // Não deveria ocorrer — indica bug de estado se aparecer
            log.warn("[BANCOS][EXCLUSÃO] confirmarExclusaoBanco chamado com bancoParaExcluir null. " +
                    "Possível inconsistência de estado no overlay.");
            return;
        }

        String nomeBanco = bancoParaExcluir.getNome();
        Long idBanco = bancoParaExcluir.getId();
        log.info("[BANCOS][EXCLUSÃO] Confirmando exclusão do banco '{}' (ID={}).", nomeBanco, idBanco);

        try {
            bancoService.excluir(bancoParaExcluir.getId());
            log.info("[BANCOS][EXCLUSÃO] Banco '{}' (ID={}) excluído com sucesso.", nomeBanco, idBanco);
            recarregarBancos();
        } catch (Exception ex) {
            // Substituído o ex.printStackTrace() por log estruturado com contexto
            log.error("[BANCOS][EXCLUSÃO] FALHA ao excluir banco '{}' (ID={}). Erro: {}",
                    nomeBanco, idBanco, ex.getMessage(), ex);
        } finally {
            cancelarExclusaoBanco();
        }
    }

    @FXML
    private void cancelarExclusaoBanco() {
        if (bancoParaExcluir != null) {
            log.info("[BANCOS][EXCLUSÃO] Exclusão do banco '{}' (ID={}) cancelada pelo usuário.",
                    bancoParaExcluir.getNome(), bancoParaExcluir.getId());
        }
        this.bancoParaExcluir = null;
        overlayConfirmacaoExclusao.setVisible(false);
    }

    // =========================================================================
    // GESTÃO DO MODAL (FORMULÁRIO)
    // =========================================================================
    @FXML
    private void abrirModalNovo() {
        log.info("[BANCOS][MODAL] Abrindo formulário para novo banco.");
        prepararModal(new BancoModel(), "➕ Novo Parceiro");
    }

    private void abrirModalEdicao(BancoModel banco) {
        log.info("[BANCOS][MODAL] Abrindo formulário de edição para banco '{}' (ID={}).",
                banco.getNome(), banco.getId());
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

        log.debug("[BANCOS][MODAL] Formulário populado. Campos: codigo='{}' | nome='{}' | temLink={} | temTel={}",
                banco.getCodigo(),
                banco.getNome(),
                banco.getSitePortal() != null && !banco.getSitePortal().isBlank(),
                banco.getTelefoneSuporte() != null && !banco.getTelefoneSuporte().isBlank());
    }

    @FXML
    private void fecharModal() {
        log.debug("[BANCOS][MODAL] Modal fechado pelo usuário. bancoEmEdicao='{}'",
                bancoEmEdicao != null ? bancoEmEdicao.getNome() : "null");
        overlayFormulario.setVisible(false);
    }

    @FXML
    private void salvarBanco() {
        String nome = txtNome.getText();
        boolean isNovo = bancoEmEdicao == null || bancoEmEdicao.getId() == null;

        log.info("[BANCOS][SALVAR] Tentativa de salvar banco. Operação={} | Nome informado='{}'",
                isNovo ? "INSERT" : "UPDATE (ID=" + bancoEmEdicao.getId() + ")",
                nome);

        if (nome == null || nome.trim().isEmpty()) {
            log.warn("[BANCOS][SALVAR] Salvamento bloqueado: nome obrigatório não preenchido.");
            exibirAviso(MSG_NOME_OBRIGATORIO);
            return;
        }

        bancoEmEdicao.setCodigo(txtCodigo.getText());
        bancoEmEdicao.setNome(nome.trim());
        bancoEmEdicao.setSitePortal(txtLink.getText());
        bancoEmEdicao.setTelefoneSuporte(txtTelefone.getText());

        try {
            BancoModel salvo = bancoService.salvar(bancoEmEdicao);
            log.info("[BANCOS][SALVAR] Banco '{}' salvo com sucesso. ID={} | Operação={}",
                    salvo.getNome(), salvo.getId(), isNovo ? "INSERT" : "UPDATE");
            fecharModal();
            recarregarBancos();
        } catch (Exception ex) {
            log.error("[BANCOS][SALVAR] FALHA ao persistir banco '{}'. Erro: {}", nome, ex.getMessage(), ex);
            exibirAviso("Erro ao salvar: " + ex.getMessage());
        }
    }

    private void exibirAviso(String mensagem) {
        log.debug("[BANCOS][UI] Aviso exibido no formulário: '{}'", mensagem);
        lblAviso.setText(mensagem);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }

    // =========================================================================
    // INTEGRAÇÕES EXTERNAS (Navegador e WhatsApp)
    // =========================================================================
    private void abrirLinkNoNavegador(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.warn("[BANCOS][PORTAL] Tentativa de abrir portal com URL vazia ou null. Ignorado.");
            return;
        }

        boolean normalizou = !url.startsWith("http://") && !url.startsWith("https://");
        if (normalizou) {
            url = "https://" + url;
        }

        log.info("[BANCOS][PORTAL] Abrindo portal no browser. URL='{}' | Prefixo normalizado={}",
                url, normalizou);
        hostServices.showDocument(url);
    }

    private void abrirWhatsApp(String telefone) {
        if (telefone == null || telefone.trim().isEmpty()) {
            log.warn("[BANCOS][WHATSAPP] Tentativa de abrir WhatsApp sem telefone cadastrado.");
            navigator.notificarAviso("Este banco não possui um telefone cadastrado.");
            return;
        }

        String numeroLimpo = telefone.replaceAll("[^0-9]", "");
        boolean adicionouPrefixo = numeroLimpo.length() <= 11;

        if (adicionouPrefixo) {
            numeroLimpo = "55" + numeroLimpo;
        }

        // Número mascarado: preserva diagnóstico sem expor dado pessoal completo
        String numeroMascarado = "55****" + numeroLimpo.substring(Math.max(0, numeroLimpo.length() - 4));
        log.info("[BANCOS][WHATSAPP] Abrindo WhatsApp. Número mascarado={} | Prefixo adicionado={}",
                numeroMascarado, adicionouPrefixo);

        hostServices.showDocument(URL_WHATSAPP_BASE + numeroLimpo);
    }

    @Override
    public void dispose() {
        log.info("[BANCOS] dispose: Desinscrevendo dos hubs.");
        bancoEventHub.desinscrever(this::recarregarBancos);
    }
}