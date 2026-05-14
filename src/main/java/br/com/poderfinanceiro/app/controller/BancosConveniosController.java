package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.repository.BancoRepository;
import br.com.poderfinanceiro.app.utils.ContatoUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BancosConveniosController {

    private final BancoRepository bancoRepository;
    private final MainController mainController;

    @FXML
    private FlowPane muralBancos;
    @FXML
    private TextField txtBusca;

    // Elementos do Modal
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

    private BancoModel bancoEmEdicao = null;
    private List<BancoModel> todosBancos;

    public BancosConveniosController(BancoRepository bancoRepository, MainController mainController) {
        this.bancoRepository = bancoRepository;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        carregarBancos();

        // Filtro em tempo real
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> filtrarMural(newVal));

        // 🚀 A SUTURA: Injetando a máscara no campo de texto do modal!
        txtTelefone.setTextFormatter(ContatoUtils.criarFormatadorTelefone());
        
        System.out.println("BancosConveniosController: Mural operante!");
    }

    private void carregarBancos() {
        todosBancos = bancoRepository.findAll();
        filtrarMural(txtBusca.getText());
    }

    private void filtrarMural(String termo) {
        muralBancos.getChildren().clear();

        for (BancoModel banco : todosBancos) {
            if (termo == null || termo.isEmpty() ||
                    banco.getNome().toLowerCase().contains(termo.toLowerCase()) ||
                    (banco.getCodigo() != null && banco.getCodigo().contains(termo))) {

                muralBancos.getChildren().add(criarCardBanco(banco));
            }
        }
    }

    /**
     * O SEGREDO DO DASHBOARD: Desenha um cartão bonito para cada banco direto no
     * código.
     */
    private VBox criarCardBanco(BancoModel banco) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Header: Nome e Código
        Label lblNome = new Label(banco.getNome());
        lblNome.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblCodigo = new Label(banco.getCodigo() != null ? "Cód: " + banco.getCodigo() : "Sem Código");
        lblCodigo.setStyle(
                "-fx-font-size: 12px; -fx-background-color: #ecf0f1; -fx-padding: 3 8; -fx-background-radius: 5; -fx-text-fill: #7f8c8d;");

        HBox header = new HBox(10, lblNome, lblCodigo);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Contato Formatado com WhatsApp
        String telOriginal = banco.getTelefoneSuporte();
        boolean temTelefone = telOriginal != null && !telOriginal.trim().isEmpty();

        String telExibicao = temTelefone ? ContatoUtils.formatarTelefone(telOriginal) : "Sem telefone";
        Label lblTel = new Label("📞 " + telExibicao);
        lblTel.setStyle("-fx-text-fill: #34495e; -fx-font-weight: bold;");

        // Botão do WhatsApp (Só aparece se tiver telefone)
        Button btnZap = new Button("💬 WhatsApp");
        btnZap.setStyle(
                "-fx-background-color: #e8f8f5; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        btnZap.setVisible(temTelefone);
        btnZap.setManaged(temTelefone);
        btnZap.setOnAction(e -> abrirWhatsApp(telOriginal));

        HBox boxContato = new HBox(15, lblTel, btnZap);
        boxContato.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Botões de Ação Principais
        Button btnPortal = new Button("🌐 Acessar Portal");
        btnPortal.setMaxWidth(Double.MAX_VALUE);
        btnPortal.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnPortal.setOnAction(e -> abrirLinkNoNavegador(banco.getSitePortal()));

        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-cursor: hand;");
        btnEditar.setOnAction(e -> abrirModalEdicao(banco));

        HBox rodape = new HBox(btnEditar);
        rodape.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Montando o Card (Notar que lblTel foi trocado por boxContato)
        card.getChildren().addAll(header, boxContato, new Region(), btnPortal, rodape);
        return card;
    }

    // ==========================================
    // AÇÕES DO MODAL (SALVAR/EDITAR)
    // ==========================================

    @FXML
    private void abrirModalNovo() {
        bancoEmEdicao = new BancoModel();
        lblTituloModal.setText("➕ Novo Parceiro");
        txtCodigo.clear();
        txtNome.clear();
        txtLink.clear();
        txtTelefone.clear();
        lblAviso.setVisible(false);
        overlayFormulario.setVisible(true);
    }

    private void abrirModalEdicao(BancoModel banco) {
        bancoEmEdicao = banco;
        lblTituloModal.setText("✏️ Editando: " + banco.getNome());
        txtCodigo.setText(banco.getCodigo());
        txtNome.setText(banco.getNome());
        txtLink.setText(banco.getSitePortal());
        txtTelefone.setText(banco.getTelefoneSuporte());
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
            lblAviso.setText("O Nome do banco é obrigatório!");
            lblAviso.setVisible(true);
            lblAviso.setManaged(true);
            return;
        }

        bancoEmEdicao.setCodigo(txtCodigo.getText());
        bancoEmEdicao.setNome(txtNome.getText());
        bancoEmEdicao.setSitePortal(txtLink.getText());
        bancoEmEdicao.setTelefoneSuporte(txtTelefone.getText());

        bancoRepository.save(bancoEmEdicao);
        fecharModal();
        carregarBancos(); // Atualiza o mural
    }

    // ==========================================
    // UTILITÁRIO: NAVEGADOR E WHATSAPP
    // ==========================================

    /**
     * Utiliza o HostServices injetado no MainController para abrir URLs de forma
     * nativa e segura.
     */
    private void abrirLinkNoNavegador(String url) {
        if (url == null || url.trim().isEmpty())
            return;

        // Normalização simples do link
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

        // Pega apenas os números para a API do WhatsApp
        String numeroLimpo = telefone.replaceAll("[^0-9]", "");

        // Se o número tiver 11 dígitos ou menos (padrão Brasil sem DDI), adiciona o 55
        if (numeroLimpo.length() <= 11) {
            numeroLimpo = "55" + numeroLimpo;
        }

        String url = "https://wa.me/" + numeroLimpo;
        mainController.getHostServices().showDocument(url);
    }
}