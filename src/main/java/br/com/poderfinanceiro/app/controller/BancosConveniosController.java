package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BancosConveniosController {

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
    private final BancoRepository bancoRepository;
    private final MainController mainController;

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

    // =========================================================================
    // ESTADO DA CLASSE
    // =========================================================================
    private BancoModel bancoParaExcluir = null;
    private BancoModel bancoEmEdicao = null;
    private List<BancoModel> todosBancos;

    public BancosConveniosController(BancoRepository bancoRepository, MainController mainController) {
        this.bancoRepository = bancoRepository;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        carregarBancos();
        configurarFiltroReativo();
        txtTelefone.setTextFormatter(ContatoUtils.criarFormatadorTelefone());
    }

    // =========================================================================
    // LÓGICA DE LISTAGEM E FILTRO
    // =========================================================================
    private void carregarBancos() {
        todosBancos = bancoRepository.findAll();
        filtrarMural(txtBusca.getText());
    }

    private void configurarFiltroReativo() {
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> filtrarMural(newVal));
    }

    private void filtrarMural(String termo) {
        muralBancos.getChildren().clear();

        todosBancos.stream()
                .filter(banco -> atendeCriterioDeBusca(banco, termo))
                .map(this::criarCardBanco)
                .forEach(muralBancos.getChildren()::add);
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
                new Region(), // Espaçador dinâmico
                criarBotaoPortal(banco),
                criarRodapeCard(banco));

        return card;
    }

    private HBox criarHeaderCard(BancoModel banco) {
        Label lblNome = new Label(banco.getNome());
        lblNome.setStyle(STYLE_LBL_NOME);

        String textoCodigo = banco.getCodigo() != null && !banco.getCodigo().isBlank() ? "Cód: " + banco.getCodigo()
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
        this.bancoParaExcluir = banco;
        lblConfirmacaoBanco.setText("Você está prestes a excluir o banco:\n\n👉 " + banco.getNome());
        overlayConfirmacaoExclusao.setVisible(true);
    }

    @FXML
    private void confirmarExclusaoBanco() {
        if (this.bancoParaExcluir != null) {
            try {
                bancoRepository.delete(this.bancoParaExcluir);
                carregarBancos();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                cancelarExclusaoBanco();
            }
        }
    }

    @FXML
    private void cancelarExclusaoBanco() {
        this.bancoParaExcluir = null;
        overlayConfirmacaoExclusao.setVisible(false);
    }

    // =========================================================================
    // GESTÃO DO MODAL (FORMULÁRIO)
    // =========================================================================
    @FXML
    private void abrirModalNovo() {
        prepararModal(new BancoModel(), "➕ Novo Parceiro");
    }

    private void abrirModalEdicao(BancoModel banco) {
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

    @FXML
    private void fecharModal() {
        overlayFormulario.setVisible(false);
    }

    @FXML
    private void salvarBanco() {
        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            exibirAviso(MSG_NOME_OBRIGATORIO);
            return;
        }

        bancoEmEdicao.setCodigo(txtCodigo.getText());
        bancoEmEdicao.setNome(txtNome.getText());
        bancoEmEdicao.setSitePortal(txtLink.getText());
        bancoEmEdicao.setTelefoneSuporte(txtTelefone.getText());

        bancoRepository.save(bancoEmEdicao);
        fecharModal();
        carregarBancos();
    }

    private void exibirAviso(String mensagem) {
        lblAviso.setText(mensagem);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }

    // =========================================================================
    // INTEGRAÇÕES EXTERNAS (Navegador e WhatsApp)
    // =========================================================================
    private void abrirLinkNoNavegador(String url) {
        if (url == null || url.trim().isEmpty())
            return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        mainController.getHostServices().showDocument(url);
    }

    private void abrirWhatsApp(String telefone) {
        if (telefone == null || telefone.trim().isEmpty()) {
            mainController.notificarAviso("Este banco não possui um telefone cadastrado.");
            return;
        }

        String numeroLimpo = telefone.replaceAll("[^0-9]", "");

        if (numeroLimpo.length() <= 11) {
            numeroLimpo = "55" + numeroLimpo;
        }

        mainController.getHostServices().showDocument(URL_WHATSAPP_BASE + numeroLimpo);
    }
}