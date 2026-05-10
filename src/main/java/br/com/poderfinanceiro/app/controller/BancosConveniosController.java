package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Banco;
import br.com.poderfinanceiro.app.repository.BancoRepository;
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

    private Banco bancoEmEdicao = null;
    private List<Banco> todosBancos;

    public BancosConveniosController(BancoRepository bancoRepository, MainController mainController) {
        this.bancoRepository = bancoRepository;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        carregarBancos();

        // Filtro em tempo real
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> filtrarMural(newVal));
        System.out.println("BancosConveniosController: Mural operante!");
    }

    private void carregarBancos() {
        todosBancos = bancoRepository.findAll();
        filtrarMural(txtBusca.getText());
    }

    private void filtrarMural(String termo) {
        muralBancos.getChildren().clear();

        for (Banco banco : todosBancos) {
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
    private VBox criarCardBanco(Banco banco) {
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

        // Contato
        Label lblTel = new Label(
                "📞 " + (banco.getTelefoneSuporte() != null ? banco.getTelefoneSuporte() : "Sem telefone"));
        lblTel.setStyle("-fx-text-fill: #34495e;");

        // Botões de Ação
        Button btnPortal = new Button("🌐 Acessar Portal");
        btnPortal.setMaxWidth(Double.MAX_VALUE);
        btnPortal.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnPortal.setCursor(javafx.scene.Cursor.HAND);
        btnPortal.setOnAction(e -> abrirLinkNoNavegador(banco.getSitePortal()));

        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-cursor: hand;");
        btnEditar.setOnAction(e -> abrirModalEdicao(banco));

        HBox rodape = new HBox(btnEditar);
        rodape.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        card.getChildren().addAll(header, lblTel, new Region(), btnPortal, rodape);
        return card;
    }

    // ==========================================
    // AÇÕES DO MODAL (SALVAR/EDITAR)
    // ==========================================

    @FXML
    private void abrirModalNovo() {
        bancoEmEdicao = new Banco();
        lblTituloModal.setText("➕ Novo Parceiro");
        txtCodigo.clear();
        txtNome.clear();
        txtLink.clear();
        txtTelefone.clear();
        lblAviso.setVisible(false);
        overlayFormulario.setVisible(true);
    }

    private void abrirModalEdicao(Banco banco) {
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
    // UTILITÁRIO: ABRIR NAVEGADOR
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
}