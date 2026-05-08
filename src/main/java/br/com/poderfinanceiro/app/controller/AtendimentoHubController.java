package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.EnderecoProponente;
import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.utils.SummaryGeneratorUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

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
    private DocumentoController abaDocumentoController;
    @FXML
    private VBox overlayConfirmacaoSaida, overlayMensagem, overlayResumo;
    @FXML
    private Button btnSalvar;
    @FXML
    private Label lblMensagemTexto, lblMensagemTitulo, lblResumoPreview;

    private final ProponenteService atendimentoService;
    private final MainController mainController;

    private Proponente proponenteAberto;
    private Runnable acaoNavegacaoPendente;
    private String resumoGeradoParaCopia;

    public AtendimentoHubController(ProponenteService atendimentoService, MainController mainController) {
        this.atendimentoService = atendimentoService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // 1. Estado de Alteração: O atendimento está "sujo" se QUALQUER UMA das abas
        // for alterada
        BooleanBinding atendimentoSujo = abaLeadController.getViewModel().dirtyProperty()
                .or(abaEnderecoController.getViewModel().dirtyProperty());

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

    public void inicializarAtendimento(Proponente proponente) {
        this.proponenteAberto = proponente;
        abaLeadController.getViewModel().loadFromModel(proponente);

        if (proponente != null && proponente.getEnderecos() != null && !proponente.getEnderecos().isEmpty()) {
            abaEnderecoController.getViewModel().loadFromModel(proponente.getEnderecos().get(0));
        } else {
            abaEnderecoController.getViewModel().reset();
        }

        // 3. Carregar Documentos
        if (proponente != null && proponente.getId() != null) {
            abaDocumentoController.carregarDocumentos(proponente);
        } else {
            abaDocumentoController.carregarDocumentos(null);
        }
    }

    public void prepararNovoAtendimento() {
        this.proponenteAberto = null;

        // Reseta (limpa e sincroniza o estado original) de todas as abas
        abaLeadController.getViewModel().reset();
        abaEnderecoController.getViewModel().reset();
        abaDocumentoController.carregarDocumentos(null);
    }

    public boolean temAlteracoesNaoSalvas() {
        // CORREÇÃO: Se QUALQUER aba estiver suja, o Hub está sujo.
        return abaLeadController.getViewModel().isDirty()
                || abaEnderecoController.getViewModel().isDirty();
    }

    @FXML
    public void handleSalvar() {
        executarSalvamento(null);
    }

    public LeadController getLeadController() {
        return abaLeadController;
    }

    private void executarSalvamento(Runnable onSucesso) {
        Task<Proponente> task = new Task<>() {
            @Override
            protected Proponente call() throws Exception {
                // Consolida os modelos
                Proponente p = abaLeadController.getViewModel().atualizarModel(proponenteAberto);
                EnderecoProponente e = abaEnderecoController.getViewModel().atualizarModel(
                        (p.getEnderecos() != null && !p.getEnderecos().isEmpty()) ? p.getEnderecos().get(0) : null);

                e.setProponente(p);
                p.setEnderecos(new ArrayList<>(java.util.List.of(e)));

                return atendimentoService.salvarLead(p);
            }
        };

        task.setOnSucceeded(ev -> {
            inicializarAtendimento(task.getValue()); // Isso reseta o estado 'Dirty'
            exibirMensagem("Atendimento salvo com sucesso!", true);
            if (onSucesso != null)
                onSucesso.run();
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
        abaDocumentoController.carregarDocumentos(null);
    }
}