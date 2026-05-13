package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.utils.SummaryGeneratorUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import org.controlsfx.control.MasterDetailPane;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
@Scope("prototype")
public class AtendimentoHubController {

    @FXML
    private LeadController abaLeadController;
    @FXML
    private EnderecoController abaEnderecoController;
    @FXML
    private PropostaHubController abaPropostaHubController;
    @FXML
    private LinkUtilController abaLinksController;
    @FXML
    private VBox overlayConfirmacaoSaida, overlayMensagem, overlayResumo;
    @FXML
    private Button btnSalvar;
    @FXML
    private Label lblMensagemTexto, lblMensagemTitulo, lblResumoPreview;

    private final ProponenteService atendimentoService;
    private final MainController mainController;
    private final PropostaService propostaService;

    private ProponenteModel proponenteAberto;
    private Runnable acaoNavegacaoPendente;
    private String resumoGeradoParaCopia;
    private Tab tabPertencente;

    @FXML
    private MasterDetailPane masterDetailPane;
    private boolean slaveJaCarregado = false;
    private final ApplicationContext context;

    public AtendimentoHubController(ProponenteService atendimentoService, MainController mainController,
            PropostaService propostaService, ApplicationContext context) {
        this.atendimentoService = atendimentoService;
        this.mainController = mainController;
        this.propostaService = propostaService;
        this.context = context;
    }

    @FXML
    public void initialize() {
        // 1. O atendimento está "sujo" se a Lead, o Endereço OU a Proposta forem
        // alterados!
        BooleanBinding atendimentoSujo = abaLeadController.getViewModel().dirtyProperty()
                .or(abaEnderecoController.getViewModel().dirtyProperty())
                .or(abaPropostaHubController.getViewModel().dirtyProperty());

        // 2. Validação Centralizada no Hub: Nome é obrigatório e CPF (se preenchido)
        // deve ter 11 dígitos
        BooleanBinding dadosValidos = Bindings.createBooleanBinding(() -> {
            String nome = abaLeadController.getViewModel().nomeProperty().get();
            String cpf = abaLeadController.getViewModel().cpfProperty().get();

            boolean nomeValido = nome != null && !nome.trim().isEmpty();

            String cpfLimpo = cpf != null ? cpf.replaceAll("[^0-9]", "") : "";
            boolean cpfValido = cpfLimpo.isEmpty() || cpfLimpo.length() == 11;

            return nomeValido && cpfValido;
        },
                // Observadores: O JavaFX reavaliará a regra acima sempre que você digitar uma
                // destas propriedades
                abaLeadController.getViewModel().nomeProperty(),
                abaLeadController.getViewModel().cpfProperty());

        // 3. A Mágica: O botão fica bloqueado (disable = true) se NÃO estiver sujo OU
        // se os dados NÃO forem válidos
        btnSalvar.disableProperty().bind(atendimentoSujo.not().or(dadosValidos.not()));
    }

    public void inicializarAtendimento(ProponenteModel proponente) {
        // 1. Carregar Lead: Se o proponente for null, as abas devem resetar para estado
        // "novo"
        this.proponenteAberto = proponente;
        abaLeadController.getViewModel().loadFromModel(proponente);

        // 2. Carregar Endereço: Se o proponente tiver endereços, carrega o primeiro.
        // Caso
        if (proponente != null && proponente.getEnderecos() != null && !proponente.getEnderecos().isEmpty()) {
            abaEnderecoController.getViewModel().loadFromModel(proponente.getEnderecos().get(0));
        } else {
            abaEnderecoController.getViewModel().reset();
        }

        // 4. Carregar Links Úteis (se a aba estiver presente)
        if (abaLinksController != null) {
            abaLinksController.recarregarLinks();
        }

        // 5. CARREGAR A PROPOSTA (Ligar o Monitor)
        abaPropostaHubController.inicializarPropostasDoCliente(proponente);
    }

    public void prepararNovoAtendimento() {
        this.proponenteAberto = new ProponenteModel();
        abaLeadController.getViewModel().reset();
        abaPropostaHubController.getViewModel().reset();
    }

    public boolean temAlteracoesNaoSalvas() {
        return abaLeadController.getViewModel().isDirty()
                || abaEnderecoController.getViewModel().isDirty()
                || abaPropostaHubController.getViewModel().isDirty(); // <- AQUI
    }

    @FXML
    public void handleSalvar() {
        executarSalvamento(null);
    }

    public LeadController getLeadController() {
        return abaLeadController;
    }

    public void setTabPertencente(Tab tab) {
        this.tabPertencente = tab;
    }

    private void executarSalvamento(Runnable onSucesso) {
        Task<ProponenteModel> task = new Task<>() {
            @Override
            protected ProponenteModel call() throws Exception {
                // 1. Salva o Lead e o Endereço (Gera o ID do Cliente)
                ProponenteModel p = abaLeadController.getViewModel().atualizarModel(proponenteAberto);
                EnderecoProponenteModel e = abaEnderecoController.getViewModel().atualizarModel(
                        (p.getEnderecos() != null && !p.getEnderecos().isEmpty()) ? p.getEnderecos().get(0) : null);

                e.setProponente(p);
                p.setEnderecos(new ArrayList<>(java.util.List.of(e)));

                ProponenteModel proponenteSalvo = atendimentoService.salvarLead(p);

                // 2. Se a aba de proposta foi preenchida, salva a proposta vinculada a ele!
                if (abaPropostaHubController.getViewModel().isDirty()) {
                    PropostaModel prop = abaPropostaHubController.getViewModel().atualizarModel(new PropostaModel());
                    prop.setProponente(proponenteSalvo);

                    // Temporário: Define o usuário como ID 1 (Wagner) até ligarmos o login real
                    UsuarioModel u = new UsuarioModel();
                    u.setId(1L);
                    prop.setUsuario(u);

                    propostaService.salvarProposta(prop);
                }

                return proponenteSalvo;
            }
        };

        task.setOnSucceeded(ev -> {
            // 1. Pegamos o proponente que acabou de voltar do banco de dados (agora com ID)
            ProponenteModel proponenteSalvo = task.getValue();

            // 2. A sua linha original intacta que reseta a tela e o estado 'Dirty'
            inicializarAtendimento(proponenteSalvo);

            // 2.1 🚀 A CURA: Se houver uma proposta salva e ela for PAGO,
            // mandamos a aba de proposta aplicar o bloqueio imediatamente.
            if (abaPropostaHubController != null) {
                // 🚀 O COMANDO:
                abaPropostaHubController.getAbaPropostaController().aplicarBloqueioSePago();
            }

            // 3. A NOVA MÁGICA: Avisamos a Aba que este contato não é mais "NOVO",
            // e sim um contato real com ID no banco.
            if (tabPertencente != null && proponenteSalvo.getId() != null) {
                tabPertencente.setUserData(String.valueOf(proponenteSalvo.getId()));
            }

            exibirMensagem("Atendimento salvo com sucesso!", true);
            if (onSucesso != null) {
                onSucesso.run();
            }
        });

        task.setOnFailed(ev -> {
            Throwable erro = task.getException();

            // Verifica se o erro foi uma validação de negócio que nós criamos
            if (erro instanceof IllegalArgumentException || erro instanceof IllegalStateException) {
                exibirMensagem(erro.getMessage(), false);
            } else {
                // Se foi um erro de banco (ex: DataIntegrityViolationException) ou de código
                erro.printStackTrace();
                exibirMensagem("Erro ao salvar: " + erro.getMessage(), false);
            }
        });

        new Thread(task).start();
    }

    public void solicitarFechamento(Runnable acaoFecharAba) {
        if (temAlteracoesNaoSalvas()) {
            this.acaoNavegacaoPendente = acaoFecharAba;
            overlayConfirmacaoSaida.setVisible(true);
        } else {
            acaoFecharAba.run();
        }
    }

    @FXML
    public void abrirWhatsappRapido() {
        String tel = abaLeadController.getViewModel().telefoneProperty().get();

        if (tel == null || tel.trim().isEmpty()) {
            exibirMensagem("Por favor, preencha o número de WhatsApp antes de iniciar a conversa.", false);
            return;
        }

        // Limpeza rigorosa: remove máscaras para enviar apenas os números
        String telLimpo = tel.replaceAll("[^0-9]", "");
        String linkFinal = telLimpo.startsWith("55") ? telLimpo : "55" + telLimpo;

        try {
            // Chamada elegante usando o HostServices do seu MainController
            mainController.getHostServices().showDocument("https://wa.me/" + linkFinal);
        } catch (Exception e) {
            exibirMensagem("Erro ao tentar abrir o navegador para o WhatsApp.", false);
        }
    }

    @FXML
    public void copiarResumoLead() {
        BigDecimal renda = abaLeadController.getViewModel().rendaProperty().get();
        String rendaStr = (renda != null) ? String.format("%,.2f", renda) : "0,00";

        // Delegação limpa para a sua classe utilitária
        this.resumoGeradoParaCopia = SummaryGeneratorUtils.gerar(abaLeadController.getViewModel(), rendaStr);

        lblResumoPreview.setText(this.resumoGeradoParaCopia);
        overlayResumo.setVisible(true);
    }

    @FXML
    public void confirmarCopiaResumo() {
        if (this.resumoGeradoParaCopia != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(this.resumoGeradoParaCopia);
            clipboard.setContent(content);

            fecharOverlayResumo();
            exibirMensagem("Relatório copiado com sucesso! Pronto para colar.", true);
        }
    }

    @FXML
    public void fecharOverlayResumo() {
        overlayResumo.setVisible(false);
        this.resumoGeradoParaCopia = null;
    }

    @FXML
    public void esconderMensagem() {
        overlayMensagem.setVisible(false);
    }

    @FXML
    public void cancelarSaida() {
        overlayConfirmacaoSaida.setVisible(false);
        this.acaoNavegacaoPendente = null;
    }

    @FXML
    public void descartarESair() {
        overlayConfirmacaoSaida.setVisible(false);
        if (acaoNavegacaoPendente != null)
            acaoNavegacaoPendente.run();
    }

    public void exibirMensagem(String texto, boolean sucesso) {
        lblMensagemTexto.setText(texto);
        lblMensagemTitulo.setText(sucesso ? "✅ Sucesso" : "⚠️ Atenção");
        overlayMensagem.setVisible(true);
    }

    public void limparRecursos() {
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        proponenteAberto = null;
    }

    // ==========================================================
    // SISTEMA MASTER-SLAVE (Links Úteis)
    // ==========================================================

    @FXML
    private void alternarPainelLinks() {
        // Inverte o status de visibilidade
        boolean isAberto = masterDetailPane.isShowDetailNode();

        if (!isAberto && !slaveJaCarregado) {
            carregarTelaDeLinks();
        }

        masterDetailPane.setShowDetailNode(!isAberto);
    }

    private void carregarTelaDeLinks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/links_uteis.fxml"));
            loader.setControllerFactory(context::getBean);
            javafx.scene.Node viewLinks = loader.load();

            masterDetailPane.setDetailNode(viewLinks);
            slaveJaCarregado = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao carregar a tela de Links no Slave: " + e.getMessage());
        }
    }
}