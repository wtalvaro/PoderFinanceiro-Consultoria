package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.strategy.DocumentStrategy;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class LeadController {

    // --- DEPENDÊNCIAS ---
    private final ProponenteService proponenteService;
    private final MainController mainController;
    private final AuthService authService;
    private final List<DocumentStrategy> documentStrategies;
    private final LeadViewModel viewModel;

    // --- ESTADO INTERNO ---
    private Proponente proponenteEmEdicao = null;
    private String resumoGeradoParaCopia;
    private String checklistGeradoParaCopia;

    // --- FXML BINDINGS ---
    @FXML
    private Label lblTituloTela, lblMensagem;
    @FXML
    private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML
    private ComboBox<String> cbOrigem, cbVinculo;
    @FXML
    private ComboBox<TipoConvenio> cbConvenio;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Button btnSalvar, btnCancelar;

    // Componente estrutural (adicionado para refletir o FXML)
    @FXML
    private ScrollPane scrollPrincipal;

    // Todos os 12 CheckBoxes de Modalidades
    @FXML
    private CheckBox chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
            chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal;

    // Overlays e Previews
    @FXML
    private VBox overlayDocs, overlayResumo, overlayMensagens;
    @FXML
    private Label lblChecklistTexto, lblResumoPreview;
    @FXML
    private Label lblPreviewSaudacao, lblPreviewAnalise, lblPreviewFechamento;

    // --- CONSTANTES ---
    private static final String TEMPLATE_SAUDACAO = "Olá! Sou o *%CONSULTOR%*, da *Poder Financeiro*. Recebi seu contato e estou à disposição para ajudar com seu crédito.";
    private static final String TEMPLATE_ANALISE = "Aqui é o *%CONSULTOR%*. Já iniciei a consulta da sua margem. Por favor, aguarde um momento enquanto verifico as melhores taxas.";
    private static final String TEMPLATE_FECHAMENTO = "Qualquer dúvida, pode me chamar aqui. Tenha um excelente dia! Atenciosamente, *%CONSULTOR%* | *Poder Financeiro*.";

    public LeadController(ProponenteService proponenteService, MainController mainController,
            AuthService authService, List<DocumentStrategy> documentStrategies,
            LeadViewModel viewModel) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
        this.authService = authService;
        this.documentStrategies = documentStrategies;
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        javafx.scene.text.Font.loadFont("file:/usr/share/fonts/google-noto-emoji/NotoColorEmoji.ttf", 18);

        configurarListasEFormatores();
        estabelecerBindings();
        esconderMensagem();

        if (this.proponenteEmEdicao != null) {
            lblTituloTela.setText("Editando Contato: " + proponenteEmEdicao.getNomeCompleto());
            viewModel.loadFromModel(this.proponenteEmEdicao);
        } else {
            limparFormulario();
        }

        btnSalvar.disableProperty().bind(viewModel.podeSalvarProperty().not());
    }

    // ========================================================================
    // SETUP E BINDINGS
    // ========================================================================

    private void configurarListasEFormatores() {
        cbOrigem.getItems().setAll("WhatsApp", "Panfleto", "Indicação", "Facebook", "Passou na porta");
        cbVinculo.getItems().setAll("Aposentado", "Pensionista", "Servidor Ativo", "Militar", "CLT");

        cbConvenio.setConverter(new StringConverter<>() {
            @Override
            public String toString(TipoConvenio obj) {
                return obj != null ? obj.getLabel() : "";
            }

            @Override
            public TipoConvenio fromString(String str) {
                return null;
            }
        });
        cbConvenio.getItems().setAll(TipoConvenio.values());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new javafx.util.converter.LocalDateStringConverter(formatter, formatter));

        FinanceiroUtils.configurarCampoMoeda(txtRenda);
        FinanceiroUtils.configurarMascaraCpf(txtCpf);
        FinanceiroUtils.configurarMascaraTelefone(txtTelefone);
    }

    private void estabelecerBindings() {
        // TextFields e ComboBoxes
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        txtCpf.textProperty().bindBidirectional(viewModel.cpfProperty());
        txtTelefone.textProperty().bindBidirectional(viewModel.telefoneProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        dpDataNascimento.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());
        cbConvenio.valueProperty().bindBidirectional(viewModel.convenioProperty());
        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());

        // Vinculação de TODAS as 12 modalidades (Checkboxes)
        chkFgts.selectedProperty().bindBidirectional(viewModel.chkFgtsProperty());
        chkInss.selectedProperty().bindBidirectional(viewModel.chkInssProperty());
        chkSiape.selectedProperty().bindBidirectional(viewModel.chkSiapeProperty());
        chkForcas.selectedProperty().bindBidirectional(viewModel.chkForcasProperty());
        chkBolsaFamilia.selectedProperty().bindBidirectional(viewModel.chkBolsaFamiliaProperty());
        chkContaLuz.selectedProperty().bindBidirectional(viewModel.chkContaLuzProperty());
        chkCartao.selectedProperty().bindBidirectional(viewModel.chkCartaoProperty());
        chkPortabilidade.selectedProperty().bindBidirectional(viewModel.chkPortabilidadeProperty());
        chkRefin.selectedProperty().bindBidirectional(viewModel.chkRefinProperty());
        chkGarantia.selectedProperty().bindBidirectional(viewModel.chkGarantiaProperty());
        chkConsigPrivado.selectedProperty().bindBidirectional(viewModel.chkConsigPrivadoProperty());
        chkPessoal.selectedProperty().bindBidirectional(viewModel.chkPessoalProperty());

        // Tratamento especial para o BigDecimal da Renda
        txtRenda.textProperty().addListener((obs, oldV, newV) -> {
            try {
                viewModel.rendaProperty().set(FinanceiroUtils.extrairValorParaBanco(newV));
            } catch (Exception e) {
                viewModel.rendaProperty().set(BigDecimal.ZERO);
            }
        });
        viewModel.rendaProperty().addListener((obs, oldV, newV) -> {
            String formatado = FinanceiroUtils.formatarParaExibicao(newV);
            if (!txtRenda.getText().equals(formatado))
                txtRenda.setText(formatado);
        });
    }

    // ========================================================================
    // AÇÕES PRINCIPAIS (SALVAR, LIMPAR, PREPARAR)
    // ========================================================================

    public void prepararEdicao(Proponente cliente) {
        this.proponenteEmEdicao = cliente;
    }

    public void prepararNovoContato() {
        this.proponenteEmEdicao = null;
    }

    @FXML
    private void limparFormulario() {
        proponenteEmEdicao = null;
        lblTituloTela.setText("Cadastrar Novo Contato");
        viewModel.reset();
    }

    @FXML
    private void handleSalvar() {
        if (viewModel.nomeProperty().get().isBlank() || viewModel.cpfProperty().get().isBlank()) {
            exibirMensagem("Nome e CPF são campos obrigatórios.", false);
            return;
        }

        setLoading(true);

        Task<Proponente> salvarTask = new Task<>() {
            @Override
            protected Proponente call() throws Exception {
                Proponente contato = (proponenteEmEdicao != null) ? proponenteEmEdicao : new Proponente();
                contato = viewModel.mapToModel(contato);
                return proponenteService.salvarLead(contato);
            }
        };

        salvarTask.setOnSucceeded(event -> {
            // 1. Pega o proponente atualizado (com ID e campos do banco)
            Proponente proponenteSalvo = salvarTask.getValue();

            if (proponenteEmEdicao == null) {
                // Se era um cadastro novo, limpamos o formulário
                limparFormulario();
            } else {
                // 2. Se era edição, atualizamos a referência local
                this.proponenteEmEdicao = proponenteSalvo;

                // CORREÇÃO DA TELA: Atualiza o título com o novo nome que veio do banco
                lblTituloTela.setText("Editando Contato: " + proponenteSalvo.getNomeCompleto());

                // CORREÇÃO DO BOTÃO (Passo 1): Atualiza o ViewModel e os carimbos "Original"
                viewModel.loadFromModel(proponenteSalvo);
            }

            // CORREÇÃO DO BOTÃO (Passo 2): Desligamos o loading APÓS a atualização dos
            // dados.
            // Quando carregando vira 'false', o JavaFX reavalia o botão, percebe que
            // a tela == original, e desativa o botão instantaneamente!
            setLoading(false);

            // 3. Dá o feedback visual ao consultor
            exibirMensagem("✅ Contato salvo com sucesso!", true);
        });

        salvarTask.setOnFailed(event -> {
            setLoading(false);
            Throwable ex = salvarTask.getException();
            exibirMensagem(ex instanceof IllegalArgumentException ? ex.getMessage() : "Erro ao salvar.", false);
        });

        new Thread(salvarTask).start();
    }

    // ========================================================================
    // LÓGICA DE NEGÓCIO (DOCUMENTOS E RESUMO)
    // ========================================================================

    @FXML
    private void verDocumentosNecessarios() {
        TipoConvenio convenio = viewModel.convenioProperty().get();
        String busca = (convenio != null) ? convenio.name() : "PADRAO";

        DocumentStrategy strategy = documentStrategies.stream()
                .filter(s -> s.supports(busca))
                .findFirst()
                .orElseGet(() -> documentStrategies.stream()
                        .filter(s -> s.supports("PADRAO"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Estratégia PADRAO não encontrada.")));

        String labelTitulo = (convenio != null) ? convenio.getLabel() : "GERAL";
        StringBuilder sb = new StringBuilder();

        sb.append("📋 *INSTRUÇÕES PARA FORMALIZAÇÃO - ").append(labelTitulo.toUpperCase()).append("*\n");
        sb.append("————————————————————————————\n");
        sb.append(
                "Para prosseguirmos com a sua análise de crédito no *Poder Financeiro*, por favor, envie fotos nítidas dos seguintes documentos:\n\n");
        sb.append(strategy.getChecklist());
        sb.append("\n⚠️ *OBSERVAÇÃO:* As imagens devem estar nítidas, sem cortes nas bordas e sem reflexos.\n");
        sb.append("————————————————————————————\n");
        sb.append("_Informações processadas em: ")
                .append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("_\n");
        sb.append("*Poder Financeiro - Consultoria e Soluções de Crédito*");

        this.checklistGeradoParaCopia = sb.toString();
        lblChecklistTexto.setText(this.checklistGeradoParaCopia);
        overlayDocs.setVisible(true);
    }

    @FXML
    private void copiarResumoLead() {
        StringBuilder resumo = new StringBuilder();

        resumo.append("📑 *RELATÓRIO DE QUALIFICAÇÃO - PODER FINANCEIRO*\n");
        resumo.append("————————————————————————————\n");
        resumo.append("*[DADOS DO PROPONENTE]*\n");
        resumo.append("• *Nome:* ").append(viewModel.nomeProperty().get().toUpperCase()).append("\n");
        resumo.append("• *CPF:* ").append(viewModel.cpfProperty().get()).append("\n");
        resumo.append("• *WhatsApp:* ").append(viewModel.telefoneProperty().get()).append("\n");

        if (viewModel.dataNascimentoProperty().get() != null) {
            String dataNasc = viewModel.dataNascimentoProperty().get()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            resumo.append("• *Data de Nascimento:* ").append(dataNasc).append("\n");
        }

        resumo.append("\n*[PERFIL FINANCEIRO]*\n");
        resumo.append("• *Convênio:* ")
                .append(viewModel.convenioProperty().get() != null ? viewModel.convenioProperty().get().getLabel()
                        : "A definir")
                .append("\n");
        resumo.append("• *Vínculo:* ").append(
                viewModel.vinculoProperty().get().isEmpty() ? "Não informado" : viewModel.vinculoProperty().get())
                .append("\n");
        resumo.append("• *Matrícula:* ").append(
                viewModel.matriculaProperty().get().isEmpty() ? "Não informada" : viewModel.matriculaProperty().get())
                .append("\n");
        resumo.append("• *Renda Mensal:* R$ ").append(txtRenda.getText().isEmpty() ? "0,00" : txtRenda.getText())
                .append("\n");

        resumo.append("\n*[MODALIDADES DE INTERESSE]*\n");
        boolean produtoSelecionado = false;

        // Leitura de todas as 12 modalidades no ViewModel para o Relatório Copiável
        if (viewModel.chkFgtsProperty().get()) {
            resumo.append("✔️ Antecipação Saque Aniversário (FGTS)\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkInssProperty().get()) {
            resumo.append("✔️ Consignado INSS\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkSiapeProperty().get()) {
            resumo.append("✔️ Consignado Público (SIAPE/Gov/Pref)\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkForcasProperty().get()) {
            resumo.append("✔️ Consignado Forças Armadas\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkBolsaFamiliaProperty().get()) {
            resumo.append("✔️ Consig. Auxílio/Bolsa Família\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkContaLuzProperty().get()) {
            resumo.append("✔️ Débito na Conta de Luz\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkCartaoProperty().get()) {
            resumo.append("✔️ Cartão RMC / Benefício RCC\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkPortabilidadeProperty().get()) {
            resumo.append("✔️ Portabilidade de Crédito\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkRefinProperty().get()) {
            resumo.append("✔️ Refinanciamento (Refin)\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkGarantiaProperty().get()) {
            resumo.append("✔️ Empréstimo com Garantia\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkConsigPrivadoProperty().get()) {
            resumo.append("✔️ Consignado Privado (CLT)\n");
            produtoSelecionado = true;
        }
        if (viewModel.chkPessoalProperty().get()) {
            resumo.append("✔️ Empréstimo Pessoal (CP)\n");
            produtoSelecionado = true;
        }

        if (!produtoSelecionado) {
            resumo.append("• Aguardando definição da modalidade ideal.\n");
        }

        resumo.append("\n————————————————————————————\n");
        resumo.append("*Poder Financeiro - Consultoria e Soluções de Crédito*");

        this.resumoGeradoParaCopia = resumo.toString();
        lblResumoPreview.setText(this.resumoGeradoParaCopia);
        overlayResumo.setVisible(true);
    }

    // ========================================================================
    // FERRAMENTAS E MENSAGENS DO WHATSAPP
    // ========================================================================

    @FXML
    private void abrirWhatsappRapido() {
        String tel = viewModel.telefoneProperty().get().replaceAll("[^0-9]", "");
        if (tel.isEmpty()) {
            exibirMensagem("⚠️ Digite um telefone para abrir o WhatsApp.", false);
            return;
        }
        String linkFinal = tel.startsWith("55") ? tel : "55" + tel;
        try {
            mainController.getHostServices().showDocument("https://wa.me/" + linkFinal);
        } catch (Exception e) {
            exibirMensagem("Não foi possível abrir o navegador.", false);
        }
    }

    private void executarCopia(String template, String categoria) {
        String textoFinal = template.replace("%CONSULTOR%", getNomeConsultorLogado());
        final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(textoFinal);
        clipboard.setContent(content);
        exibirMensagem("✅ Mensagem de " + categoria + " copiada!", true);
    }

    @FXML
    private void copiarSaudacao() {
        executarCopia(TEMPLATE_SAUDACAO, "Saudação");
    }

    @FXML
    private void copiarAvisoAnalise() {
        executarCopia(TEMPLATE_ANALISE, "Análise");
    }

    @FXML
    private void copiarFechamento() {
        executarCopia(TEMPLATE_FECHAMENTO, "Fechamento");
    }

    @FXML
    private void confirmarCopiaChecklist() {
        if (this.checklistGeradoParaCopia != null) {
            final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(this.checklistGeradoParaCopia);
            clipboard.setContent(content);
            fecharOverlayDocs();
            exibirMensagem("✅ Lista de documentos copiada para o WhatsApp!", true);
        }
    }

    @FXML
    private void confirmarCopiaResumo() {
        if (this.resumoGeradoParaCopia != null) {
            final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(this.resumoGeradoParaCopia);
            clipboard.setContent(content);
            fecharOverlayResumo();
            exibirMensagem("✅ Relatório copiado para a área de transferência!", true);
        }
    }

    // ========================================================================
    // CONTROLES DE INTERFACE (UI)
    // ========================================================================

    private String getNomeConsultorLogado() {
        return authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor Poder Financeiro";
    }

    @FXML
    private void fecharOverlayDocs() {
        overlayDocs.setVisible(false);
        this.checklistGeradoParaCopia = null;
    }

    @FXML
    private void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    @FXML
    private void fecharCentralMensagens() {
        if (overlayMensagens != null)
            overlayMensagens.setVisible(false);
    }

    @FXML
    private void abrirCentralMensagens() {
        if (overlayMensagens != null) {
            String nome = getNomeConsultorLogado();
            if (lblPreviewSaudacao != null)
                lblPreviewSaudacao.setText(TEMPLATE_SAUDACAO.replace("%CONSULTOR%", nome));
            if (lblPreviewAnalise != null)
                lblPreviewAnalise.setText(TEMPLATE_ANALISE.replace("%CONSULTOR%", nome));
            if (lblPreviewFechamento != null)
                lblPreviewFechamento.setText(TEMPLATE_FECHAMENTO.replace("%CONSULTOR%", nome));
            overlayMensagens.setVisible(true);
        }
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
        // 1. Avise ao ViewModel. Isso desativará o botão automaticamente via Bind
        viewModel.carregandoProperty().set(loading);

        // 2. Gerencie apenas o feedback visual do progresso
        progress.setVisible(loading);
        progress.setManaged(loading);

        // REMOVA A LINHA: btnSalvar.setDisable(loading); <-- Isso causava o erro

        if (loading) {
            esconderMensagem();
        }
    }
}