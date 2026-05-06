package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.OrigemLead;
import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.model.TipoRelacionamento;
import br.com.poderfinanceiro.app.model.TipoVinculo;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.LeadViewModel;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.Duration;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype") // Essencial para isolar cada cliente
public class LeadController {

    private PauseTransition timerMensagem;

    // --- DEPENDÊNCIAS ---
    private final ProponenteService proponenteService;
    private final MainController mainController;
    private final LeadViewModel viewModel;

    private String resumoGeradoParaCopia;
    private Runnable acaoNavegacaoPendente;

    // --- FXML BINDINGS ---
    @FXML
    private Label lblTituloTela, lblMensagem;
    @FXML
    private TextField txtNome, txtCpf, txtTelefone, txtMatricula, txtRenda;
    @FXML
    private ComboBox<OrigemLead> cbOrigem;
    @FXML
    private ComboBox<TipoVinculo> cbVinculo;
    @FXML
    private ComboBox<TipoConvenio> cbConvenio;
    @FXML
    private ComboBox<TipoRelacionamento> cbClassificacao;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Button btnSalvar, btnCancelar;
    @FXML
    private ScrollPane scrollPrincipal;

    // Todos os 12 CheckBoxes de Modalidades
    @FXML
    private CheckBox chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
            chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal;

    // Overlays e Previews
    @FXML
    private VBox overlayResumo, overlayConfirmacaoSaida;
    @FXML
    private Label lblChecklistTexto, lblResumoPreview;

    public LeadController(ProponenteService proponenteService, MainController mainController, LeadViewModel viewModel) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        javafx.scene.text.Font.loadFont("file:/usr/share/fonts/google-noto-emoji/NotoColorEmoji.ttf", 18);

        configurarListasEFormatores();
        estabelecerBindings();
        esconderMensagem();

        // A Label agora vai reagir automaticamente a qualquer mudança no nome (seja do
        // banco ou da digitação)
        lblTituloTela.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String nome = viewModel.nomeProperty().get();
            return (nome == null || nome.trim().isEmpty()) ? "Cadastrar Novo Contato" : "Editando Contato: " + nome;
        }, viewModel.nomeProperty()));

        btnSalvar.disableProperty().bind(viewModel.podeSalvarProperty().not());
    }

    public LeadViewModel getViewModel() {
        return viewModel;
    }

    // ========================================================================
    // SETUP E BINDINGS
    // ========================================================================

    private void configurarListasEFormatores() {
        // 1. Populando as listas
        cbOrigem.getItems().setAll(OrigemLead.values());
        cbVinculo.getItems().setAll(TipoVinculo.values());
        cbClassificacao.getItems().setAll(TipoRelacionamento.values());
        cbConvenio.getItems().setAll(TipoConvenio.values());

        // 2. Converter para OrigemLead (Usa o getLabel() que você criou)
        cbOrigem.setConverter(new StringConverter<OrigemLead>() {
            @Override
            public String toString(OrigemLead obj) {
                return obj != null ? obj.getLabel() : "";
            }

            @Override
            public OrigemLead fromString(String str) {
                return null;
            }
        });

        // 3. Converter para TipoVinculo (Usa o getLabel() que você criou)
        cbVinculo.setConverter(new StringConverter<TipoVinculo>() {
            @Override
            public String toString(TipoVinculo obj) {
                return obj != null ? obj.getLabel() : "";
            }

            @Override
            public TipoVinculo fromString(String str) {
                return null;
            }
        });

        // 4. Já existente no seu código para Convênio
        cbConvenio.setConverter(new StringConverter<TipoConvenio>() {
            @Override
            public String toString(TipoConvenio obj) {
                return obj != null ? obj.getLabel() : "";
            }

            @Override
            public TipoConvenio fromString(String str) {
                return null;
            }
        });

        // Formatador de data para o padrão brasileiro
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dpDataNascimento.setConverter(new javafx.util.converter.LocalDateStringConverter(formatter, formatter));
    }

    private void estabelecerBindings() {
        // 1. TextFields e ComboBoxes (Sem máscara especial)
        txtNome.textProperty().bindBidirectional(viewModel.nomeProperty());
        cbOrigem.valueProperty().bindBidirectional(viewModel.origemProperty());
        cbClassificacao.valueProperty().bindBidirectional(viewModel.classificacaoProperty());
        // ====================================================================
        // BINDING DA DATA DE NASCIMENTO (Com Máscara Automática)
        // ====================================================================

        TextFormatter<LocalDate> dataFormatter = FinanceiroUtils.criarFormatadorData();

        // Aplicamos o formatador ao campo de texto interno do DatePicker
        dpDataNascimento.getEditor().setTextFormatter(dataFormatter);

        // Sincronizamos o valor do formatador com o ViewModel
        dataFormatter.valueProperty().bindBidirectional(viewModel.dataNascimentoProperty());

        // Sincronizamos o valor do DatePicker com o formatador para que
        // ao selecionar no calendário, o texto também seja atualizado corretamente.
        dpDataNascimento.valueProperty().bindBidirectional(dataFormatter.valueProperty());

        cbConvenio.valueProperty().bindBidirectional(viewModel.convenioProperty());
        cbVinculo.valueProperty().bindBidirectional(viewModel.vinculoProperty());
        txtMatricula.textProperty().bindBidirectional(viewModel.matriculaProperty());

        // 2. Modalidades de Interesse (Checkboxes)
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

        // ====================================================================
        // 3. CAMPOS COM MÁSCARA (TextFormatter)
        // ====================================================================

        // --- RENDA ---
        TextFormatter<BigDecimal> rendaFormatter = FinanceiroUtils.criarFormatadorMoeda();
        txtRenda.setTextFormatter(rendaFormatter);
        rendaFormatter.valueProperty().bindBidirectional(viewModel.rendaProperty());

        // --- CPF ---
        TextFormatter<String> cpfFormatter = FinanceiroUtils.criarFormatadorCpf();
        txtCpf.setTextFormatter(cpfFormatter);
        cpfFormatter.valueProperty().bindBidirectional(viewModel.cpfProperty());

        // --- TELEFONE ---
        TextFormatter<String> telefoneFormatter = FinanceiroUtils.criarFormatadorTelefone();
        txtTelefone.setTextFormatter(telefoneFormatter);
        telefoneFormatter.valueProperty().bindBidirectional(viewModel.telefoneProperty());
    }

    // ========================================================================
    // LÓGICA DE NAVEGAÇÃO SEGURA E SALVAMENTO
    // ========================================================================

    public void tentarNavegar(Runnable acaoNavegacao) {
        if (!viewModel.temAlteracoesPendentes()) {
            acaoNavegacao.run();
            return;
        }

        this.acaoNavegacaoPendente = acaoNavegacao;
        overlayConfirmacaoSaida.setVisible(true);
    }

    @FXML
    private void cancelarSaida() {
        overlayConfirmacaoSaida.setVisible(false);
        this.acaoNavegacaoPendente = null;
    }

    @FXML
    private void descartarESair() {
        overlayConfirmacaoSaida.setVisible(false);
        limparFormulario();

        if (acaoNavegacaoPendente != null) {
            acaoNavegacaoPendente.run();
            acaoNavegacaoPendente = null;
        }
    }

    @FXML
    private void salvarESair() {
        if (viewModel.podeSalvarProperty().get()) {
            overlayConfirmacaoSaida.setVisible(false);
            executarSalvamento(this.acaoNavegacaoPendente);
            this.acaoNavegacaoPendente = null;
        } else {
            exibirMensagem("⚠️ Impossível salvar. Verifique se Nome e CPF estão corretos.", false);
            overlayConfirmacaoSaida.setVisible(false);
        }
    }

    @FXML
    private void handleSalvar() {
        executarSalvamento(null);
    }

    private void executarSalvamento(Runnable onSucesso) {
        // NOVA REGRA: Agora apenas o Nome é estritamente obrigatório para salvar
        if (viewModel.nomeProperty().get().isBlank()) {
            exibirMensagem("O Nome é um campo obrigatório.", false);
            return;
        }

        setLoading(true);

        Task<Proponente> salvarTask = new Task<>() {
            @Override
            protected Proponente call() throws Exception {
                Proponente contato = viewModel.mapToModel(new Proponente());
                return proponenteService.salvarLead(contato);
            }
        };

        salvarTask.setOnSucceeded(event -> {
            Proponente proponenteSalvo = salvarTask.getValue();

            // 1. Atualiza o ViewModel (isso reseta a flag 'editando' e os campos
            // 'originais')
            viewModel.loadFromModel(proponenteSalvo);

            // --- 2. O PULO DO GATO: SINCRONIZAÇÃO DA ABA ---
            // Se era um "Novo Contato", a aba estava com um UUID.
            // Agora, forçamos a aba a assumir o ID real do banco para não duplicar no
            // próximo clique.
            if (scrollPrincipal.getScene() != null) {
                TabPane tabPane = (TabPane) scrollPrincipal.getScene().lookup("#tabPanePrincipal");
                if (tabPane != null) {
                    Tab abaAtiva = tabPane.getSelectionModel().getSelectedItem();
                    if (abaAtiva != null) {
                        // Converte o ID para String para manter o padrão que criamos no
                        // WorkspaceController
                        abaAtiva.setUserData(String.valueOf(proponenteSalvo.getId()));
                    }
                }
            }
            // ------------------------------------------------

            setLoading(false);
            exibirMensagem("✅ Contato salvo com sucesso!", true);

            if (onSucesso != null) {
                onSucesso.run();
            }
        });

        salvarTask.setOnFailed(event -> {
            setLoading(false);
            Throwable ex = salvarTask.getException();
            exibirMensagem(ex instanceof IllegalArgumentException ? ex.getMessage() : "Erro ao salvar.", false);
        });

        new Thread(salvarTask).start();
    }

    /**
     * Prepara a tela para editar um cliente já existente no banco de dados.
     */
    public void prepararEdicao(Proponente cliente) {
        if (cliente != null) {
            // Carrega o proponente no ViewModel, o que atualiza todos os campos
            // da tela automaticamente e injeta o ID para evitar erro de CPF duplicado.
            viewModel.loadFromModel(cliente);
        }
    }

    /**
     * Limpa o formulário para iniciar o cadastro de um novo proponente do zero.
     */
    public void prepararNovoContato() {
        // Reseta todas as propriedades do ViewModel para o estado inicial (vazio).
        // Graças ao Binding no initialize, o título da tela mudará sozinho.
        viewModel.reset();
    }

    @FXML
    private void limparFormulario() {
        viewModel.reset();
    }

    // ========================================================================
    // FERRAMENTAS DO WHATSAPP, DOCUMENTOS E RESUMO
    // ========================================================================

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
                viewModel.vinculoProperty().get() == null ? "Não informado"
                        : viewModel.vinculoProperty().get().getLabel())
                .append("\n");
        resumo.append("• *Matrícula:* ").append(
                viewModel.matriculaProperty().get().isEmpty() ? "Não informada" : viewModel.matriculaProperty().get())
                .append("\n");
        resumo.append("• *Renda Mensal:* R$ ").append(txtRenda.getText().isEmpty() ? "0,00" : txtRenda.getText())
                .append("\n");

        resumo.append("\n*[MODALIDADES DE INTERESSE]*\n");
        boolean produtoSelecionado = false;

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

    @FXML
    private void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    private void exibirMensagem(String texto, boolean sucesso) {
        // 1. Configuração visual imediata
        lblMensagem.setText(texto);
        lblMensagem.setVisible(true);
        lblMensagem.setManaged(true);

        String color = sucesso ? "#e8f5e9" : "#ffebee";
        String text = sucesso ? "#2e7d32" : "#c62828";
        lblMensagem.setStyle("-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5; " +
                "-fx-background-color: " + color + "; -fx-text-fill: " + text + ";");

        // 2. Lógica do Timer (Onde o PauseTransition é usado)
        if (timerMensagem != null) {
            timerMensagem.stop(); // Reinicia o tempo se houver uma nova mensagem
        }

        // Criamos a transição de pausa para 5 segundos
        timerMensagem = new PauseTransition(Duration.seconds(5));

        // Quando o tempo acabar, chamamos o método para esconder
        timerMensagem.setOnFinished(event -> esconderMensagem());

        // Inicia a contagem
        timerMensagem.play();
    }

    private void esconderMensagem() {
        lblMensagem.setVisible(false);
        lblMensagem.setManaged(false);
    }

    private void setLoading(boolean loading) {
        viewModel.carregandoProperty().set(loading);
        progress.setVisible(loading);
        progress.setManaged(loading);

        if (loading) {
            esconderMensagem();
        }
    }

    /**
     * Método chamado quando a aba é definitivamente fechada.
     * Desliga processos em background para evitar vazamento de memória.
     */
    public void liberarRecursos() {
        if (this.timerMensagem != null) {
            this.timerMensagem.stop();
            this.timerMensagem = null;
        }
        System.out.println("Recursos do LeadController liberados com sucesso!");
    }
}