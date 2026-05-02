package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.service.AuthService;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
    @FXML
    private VBox overlayDocs;
    @FXML
    private Label lblChecklistTexto;
    @FXML
    private VBox overlayResumo;
    @FXML
    private Label lblResumoPreview;
    @FXML
    private Label lblPreviewSaudacao;
    @FXML
    private Label lblPreviewAnalise;
    @FXML
    private Label lblPreviewFechamento;

    private String resumoGeradoParaCopia;
    private String checklistGeradoParaCopia;

    private final AuthService authService;
    
    // 2. Defina os templates como constantes para evitar divergências entre o
    // preview e a cópia
    private static final String TEMPLATE_SAUDACAO = "Olá! Sou o *%CONSULTOR%*, da *Poder Financeiro*. Recebi seu contato e estou à disposição para ajudar com seu crédito.";
    private static final String TEMPLATE_ANALISE = "Aqui é o *%CONSULTOR%*. Já iniciei a consulta da sua margem. Por favor, aguarde um momento enquanto verifico as melhores taxas.";
    private static final String TEMPLATE_FECHAMENTO = "Qualquer dúvida, pode me chamar aqui. Tenha um excelente dia! Atenciosamente, *%CONSULTOR%* | *Poder Financeiro*.";

    public LeadController(ProponenteService proponenteService, MainController mainController, AuthService authService) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        // Tenta carregar a fonte de emojis do sistema para a memória do JavaFX
        javafx.scene.text.Font.loadFont("file:/usr/share/fonts/google-noto-emoji/NotoColorEmoji.ttf", 18);

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

    // --- MÉTODOS DA TOOLBAR ---

    @FXML
    private void abrirWhatsappRapido() {
        // 1. Sanitização: Garante apenas números
        String tel = txtTelefone.getText().replaceAll("[^0-9]", "");

        if (tel.isEmpty()) {
            exibirMensagem("⚠️ Digite um telefone para abrir o WhatsApp.", false);
            return;
        }

        // 2. Inteligência de Código de País:
        // Se o consultor em Magé digitar apenas o DDD + número (11 dígitos),
        // nós adicionamos o 55 (Brasil) automaticamente.
        String linkFinal = tel.startsWith("55") ? tel : "55" + tel;
        String url = "https://wa.me/" + linkFinal;

        try {
            // 3. Execução via HostServices (Melhor suporte para Fedora/Wayland)
            // Nota: mainController deve expor o getHostServices() da sua classe Application
            mainController.getHostServices().showDocument(url);

        } catch (Exception e) {
            exibirMensagem("Não foi possível abrir o navegador.", false);
        }
    }

    @FXML
    private void verDocumentosNecessarios() {
        String convenio = cbConvenio.getValue();
        StringBuilder sb = new StringBuilder();

        // 1. Cabeçalho Institucional
        sb.append("📋 *INSTRUÇÕES PARA FORMALIZAÇÃO - ").append(convenio != null ? convenio.toUpperCase() : "GERAL")
                .append("*\n");
        sb.append("————————————————————————————\n");
        sb.append(
                "Para prosseguirmos com a sua análise de crédito no *Poder Financeiro*, por favor, envie fotos nítidas dos seguintes documentos:\n\n");

        // 2. Lógica de Checklist Técnica (Text Blocks)
        String docs = switch (convenio != null ? convenio : "Padrão") {
            case "INSS" -> """
                    *DOCUMENTAÇÃO OBRIGATÓRIA - INSS*
                    ————————————————————————————
                    • *Identificação:* RG ou CNH original (dentro da validade).
                    • *Residência:* Comprovante de endereço nominal e atualizado (máximo 60 dias).
                    • *Extrato HISCON:* Extrato de Empréstimos Consignados (obtido via Meu INSS).
                    • *Detalhamento:* Extrato de Pagamento (Detalhamento de Crédito - DC).
                    • *Bancário:* Comprovante de conta onde recebe o benefício (Extrato ou Cartão).
                    • *Identificação Visual:* Selfie com documento de identidade (para formalização digital).
                    """;

            case "SIAPE" -> """
                    *DOCUMENTAÇÃO OBRIGATÓRIA - SIAPE*
                    ————————————————————————————
                    • *Identificação:* Identidade Funcional ou CNH.
                    • *Renda:* 02 últimos contracheques (extraídos do portal SouGov).
                    • *Residência:* Comprovante de endereço nominal e atualizado.
                    • *Autorização:* Chave de Autorização ativa gerada no SouGov.
                    • *Bancário:* Comprovante de conta corrente ativa para crédito.
                    """;

            case "Exército", "Marinha", "Aeronáutica" -> """
                    *DOCUMENTAÇÃO OBRIGATÓRIA - FORÇAS ARMADAS*
                    ————————————————————————————
                    • *Identificação:* Identidade Militar original e legível.
                    • *Renda:* Último Bilhete de Pagamento atualizado.
                    • *Residência:* Comprovante de endereço em nome do militar ou cônjuge.
                    • *Bancário:* Comprovante de conta bancária para recebimento do crédito.
                    • *Certidão:* Nada Consta/Folha de Alterações (se solicitado pelo banco).
                    """;

            case "Governo RJ", "Prefeitura" -> """
                    *DOCUMENTAÇÃO OBRIGATÓRIA - PÚBLICO ESTADUAL/MUNICIPAL*
                    ————————————————————————————
                    • *Identificação:* RG ou CNH.
                    • *Renda:* 03 últimos contracheques originais.
                    • *Residência:* Comprovante de residência atualizado.
                    • *Funcional:* Número de matrícula e data de admissão.
                    • *Bancário:* Cartão do banco ou extrato para validação de dados.
                    """;

            default -> """
                    *DOCUMENTAÇÃO GERAL PARA ANÁLISE*
                    ————————————————————————————
                    • *Identificação:* RG e CPF ou CNH.
                    • *Residência:* Comprovante de endereço nominal.
                    • *Financeiro:* 03 últimos comprovantes de renda (Holerites).
                    • *Extrato:* Extrato bancário dos últimos 90 dias (para crédito pessoal).
                    • *FGTS:* Print do extrato do FGTS com saldo total disponível (se aplicável).
                    """;
        };

        sb.append(docs);
        sb.append("\n⚠️ *OBSERVAÇÃO:* As imagens devem estar nítidas, sem cortes nas bordas e sem reflexos.");

        // Rodapé de Encerramento
        sb.append("\n————————————————————————————\n");
        sb.append("_Informações processadas em: ").append(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("_\n");
        sb.append("*Poder Financeiro - Consultoria e Soluções de Crédito*");

        // 3. Configuração do Preview e Exibição do Overlay
        this.checklistGeradoParaCopia = sb.toString();
        lblChecklistTexto.setText(this.checklistGeradoParaCopia);
        overlayDocs.setVisible(true);
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
    private void fecharOverlayDocs() {
        overlayDocs.setVisible(false);
        this.checklistGeradoParaCopia = null;
    }

    @FXML
    private void copiarResumoLead() {
        StringBuilder resumo = new StringBuilder();

        // Cabeçalho Formal
        resumo.append("📑 *RELATÓRIO DE QUALIFICAÇÃO - PODER FINANCEIRO*\n");
        resumo.append("————————————————————————————\n");
        resumo.append("Este documento contém o resumo dos dados coletados para análise de crédito.\n\n");

        // Seção 1: Identificação do Proponente
        resumo.append("*[DADOS DO PROPONENTE]*\n");
        resumo.append("• *Nome:* ").append(txtNome.getText().toUpperCase()).append("\n");
        resumo.append("• *CPF:* ").append(txtCpf.getText()).append("\n");
        resumo.append("• *WhatsApp:* ").append(txtTelefone.getText()).append("\n");

        if (dpDataNascimento.getValue() != null) {
            String dataNasc = dpDataNascimento.getValue()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            resumo.append("• *Data de Nascimento:* ").append(dataNasc).append("\n");
        }

        // Seção 2: Perfil Operacional e Financeiro
        resumo.append("\n*[PERFIL FINANCEIRO]*\n");
        resumo.append("• *Convênio/Órgão:* ")
                .append(cbConvenio.getValue() != null ? cbConvenio.getValue() : "A definir").append("\n");
        resumo.append("• *Vínculo:* ").append(cbVinculo.getValue() != null ? cbVinculo.getValue() : "Não informado")
                .append("\n");
        resumo.append("• *Matrícula/Benefício:* ")
                .append(txtMatricula.getText().isEmpty() ? "Não informada" : txtMatricula.getText()).append("\n");
        resumo.append("• *Renda Mensal:* R$ ").append(txtRenda.getText().isEmpty() ? "0,00" : txtRenda.getText())
                .append("\n");

        // Seção 3: Intenção de Negócio (Modalidades)
        resumo.append("\n*[MODALIDADES DE INTERESSE]*\n");

        CheckBox[] produtos = {
                chkFgts, chkInss, chkSiape, chkForcas, chkBolsaFamilia, chkContaLuz,
                chkCartao, chkPortabilidade, chkRefin, chkGarantia, chkConsigPrivado, chkPessoal
        };

        boolean produtoSelecionado = false;
        for (CheckBox cb : produtos) {
            if (cb != null && cb.isSelected()) {
                resumo.append("✔️ ").append(cb.getText()).append("\n");
                produtoSelecionado = true;
            }
        }

        if (!produtoSelecionado) {
            resumo.append("• Aguardando definição da modalidade ideal.\n");
        }

        // Rodapé de Encerramento
        resumo.append("\n————————————————————————————\n");
        resumo.append("_Informações processadas em: ").append(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("_\n");
        resumo.append("*Poder Financeiro - Consultoria e Soluções de Crédito*");

        // CONFIGURAÇÃO DO PREVIEW
        this.resumoGeradoParaCopia = resumo.toString();
        lblResumoPreview.setText(this.resumoGeradoParaCopia);

        // Exibe o Overlay para revisão
        overlayResumo.setVisible(true);
    }

    @FXML
    private void confirmarCopiaResumo() {
        if (this.resumoGeradoParaCopia != null) {
            // Executa a cópia física
            final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(this.resumoGeradoParaCopia);
            clipboard.setContent(content);

            // Fecha o modal e dá o feedback
            fecharOverlayResumo();
            exibirMensagem("✅ Relatório copiado para a área de transferência!", true);
        }
    }

    @FXML
    private void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    /**
     * Lógica centralizada para cópia de textos dinâmicos
     */
    private void executarCopia(String template, String categoria) {
        String nomeConsultor = getNomeConsultorLogado();

        // Substitui o marcador pelo nome real vindo do AuthService
        String textoFinal = template.replace("%CONSULTOR%", nomeConsultor);

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

    /**
     * Recupera o nome do consultor logado de forma segura
     */
    private String getNomeConsultorLogado() {
        if (authService.estaLogado()) {
            return authService.getUsuarioLogado().getNome(); //
        }
        return "Consultor Poder Financeiro";
    }

    // 1. Certifique-se de declarar o overlay que o FXML vai manipular
    @FXML
    private VBox overlayMensagens;

    // 2. O método que o FXML acusou erro (abrirCentralMensagens)
    @FXML
    private void abrirCentralMensagens() {
        if (overlayMensagens != null) {
            atualizarPreviewsMensagens();
            overlayMensagens.setVisible(true);
        }
    }

    // 3. O método para fechar (que provavelmente está no seu FXML também)
    @FXML
    private void fecharCentralMensagens() {
        if (overlayMensagens != null) {
            overlayMensagens.setVisible(false);
        }
    }

    /**
     * Atualiza os labels da interface com o nome real do consultor vindo do
     * AuthService
     */
    private void atualizarPreviewsMensagens() {
        String nome = getNomeConsultorLogado(); // Retornará "Wagner Teles Alvaro" se logado

        // Atualiza os Labels do FXML com o texto processado
        if (lblPreviewSaudacao != null)
            lblPreviewSaudacao.setText(TEMPLATE_SAUDACAO.replace("%CONSULTOR%", nome));
        if (lblPreviewAnalise != null)
            lblPreviewAnalise.setText(TEMPLATE_ANALISE.replace("%CONSULTOR%", nome));
        if (lblPreviewFechamento != null)
            lblPreviewFechamento.setText(TEMPLATE_FECHAMENTO.replace("%CONSULTOR%", nome));
    }
}