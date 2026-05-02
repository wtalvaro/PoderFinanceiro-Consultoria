package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

@Component
public class LeadController {

    private final ProponenteService proponenteService;
    private final MainController mainController;

    private Proponente proponenteEmEdicao = null;

    // FXML - Cabeçalho
    @FXML
    private Label lblTituloTela;
    @FXML
    private Label lblMensagem;

    // FXML - Seção 1 (Básico)
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtCpf;
    @FXML
    private TextField txtTelefone;
    @FXML
    private ComboBox<String> cbOrigem;

    // FXML - Seção 2 (Operacional)
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ComboBox<String> cbConvenio;
    @FXML
    private ComboBox<String> cbVinculo;
    @FXML
    private TextField txtMatricula;
    @FXML
    private TextField txtRenda;

    // FXML - Seção 3 (Produtos)
    @FXML
    private CheckBox chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz;
    @FXML
    private CheckBox chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal;

    // FXML - Controles
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Button btnSalvar;
    @FXML
    private Button btnCancelar;

    public LeadController(ProponenteService proponenteService, MainController mainController) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // 1. Configurações de UI
        configurarListas();
        FinanceiroUtils.configurarCampoMoeda(txtRenda);
        FinanceiroUtils.configurarMascaraCpf(txtCpf);
        FinanceiroUtils.configurarMascaraTelefone(txtTelefone);
        esconderMensagem();

        // 2. Unificação do Estado (O Pulo do Gato)
        if (this.proponenteEmEdicao != null) {
            exibirDadosNoFormulario(this.proponenteEmEdicao);
        } else {
            limparFormulario();
        }
    }

    private void configurarListas() {
        cbOrigem.getItems().setAll("WhatsApp", "Panfleto", "Indicação", "Facebook", "Passou na porta");
        cbConvenio.getItems().setAll("INSS", "SIAPE", "Exército", "Marinha", "Aeronáutica", "Governo RJ", "Prefeitura");
        cbVinculo.getItems().setAll("Aposentado", "Pensionista", "Servidor Ativo", "Militar", "CLT");
    }

    private void exibirDadosNoFormulario(Proponente cliente) {
        lblTituloTela.setText("Editando Contato: " + cliente.getNomeCompleto());

        txtNome.setText(cliente.getNomeCompleto());
        txtCpf.setText(FinanceiroUtils.formatarCpf(cliente.getCpf()));
        txtTelefone.setText(FinanceiroUtils.formatarTelefone(cliente.getTelefone()));
        cbOrigem.setValue(cliente.getOrigemConsentimento());

        dpDataNascimento.setValue(cliente.getDataNascimento());
        cbConvenio.setValue(cliente.getConvenioOrgao());
        cbVinculo.setValue(cliente.getTipoVinculo());
        txtMatricula.setText(cliente.getMatricula());
        txtRenda.setText(FinanceiroUtils.formatarParaExibicao(cliente.getRendaMensal()));

        desmarcarProdutos();
    }

    public void prepararEdicao(Proponente cliente) {
        this.proponenteEmEdicao = cliente;
    }

    public void prepararNovoContato() {
        this.proponenteEmEdicao = null;
    }

    @FXML
    private void handleSalvar() {
        String nome = txtNome.getText();
        String cpf = txtCpf.getText();

        if (nome == null || nome.trim().isEmpty() || cpf == null || cpf.trim().isEmpty()) {
            exibirMensagem("Nome e CPF são campos obrigatórios.", false);
            return;
        }

        setLoading(true);

        Task<Proponente> salvarTask = new Task<>() {
            @Override
            protected Proponente call() throws Exception {
                Proponente contato = (proponenteEmEdicao != null) ? proponenteEmEdicao : new Proponente();

                contato.setNomeCompleto(nome.trim());
                contato.setCpf(cpf.trim());
                contato.setTelefone(txtTelefone.getText());
                contato.setOrigemConsentimento(cbOrigem.getValue());
                contato.setDataNascimento(dpDataNascimento.getValue());
                contato.setConvenioOrgao(cbConvenio.getValue());
                contato.setTipoVinculo(cbVinculo.getValue());
                contato.setMatricula(txtMatricula.getText());
                contato.setRendaMensal(FinanceiroUtils.extrairValorParaBanco(txtRenda.getText()));

                return proponenteService.salvarLead(contato);
            }
        };

        salvarTask.setOnSucceeded(event -> {
            setLoading(false);
            exibirMensagem("✅ Contato salvo com sucesso!", true);
            if (proponenteEmEdicao == null)
                limparFormulario();
        });

        salvarTask.setOnFailed(event -> {
            setLoading(false);
            Throwable ex = salvarTask.getException();
            exibirMensagem(ex instanceof IllegalArgumentException ? ex.getMessage() : "Erro ao salvar.", false);
        });

        new Thread(salvarTask).start();
    }

    @FXML
    private void handleCancelar() {
        proponenteEmEdicao = null;
        mainController.navegarPara("/fxml/workspace.fxml", true);
    }

    private void limparFormulario() {
        proponenteEmEdicao = null;
        lblTituloTela.setText("Cadastrar Novo Contato");
        txtNome.clear();
        txtCpf.clear();
        txtTelefone.clear();
        cbOrigem.getSelectionModel().clearSelection();
        dpDataNascimento.setValue(null);
        cbConvenio.getSelectionModel().clearSelection();
        cbVinculo.getSelectionModel().clearSelection();
        txtMatricula.clear();
        txtRenda.clear();
        desmarcarProdutos();
    }

    private void desmarcarProdutos() {
        CheckBox[] boxes = { chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
                chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal };
        for (CheckBox cb : boxes)
            if (cb != null)
                cb.setSelected(false);
    }

    private void exibirMensagem(String texto, boolean sucesso) {
        lblMensagem.setText(texto);
        lblMensagem.setVisible(true);
        lblMensagem.setManaged(true);
        String color = sucesso ? "#e8f5e9" : "#ffebee";
        String text = sucesso ? "#2e7d32" : "#c62828";
        lblMensagem.setStyle("-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5; -fx-background-color: "
                + color + "; -fx-text-fill: " + text + ";");
    }

    private void esconderMensagem() {
        lblMensagem.setVisible(false);
        lblMensagem.setManaged(false);
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        progress.setManaged(loading);
        btnSalvar.setDisable(loading);
        btnCancelar.setDisable(loading);
        if (loading)
            esconderMensagem();
    }
}