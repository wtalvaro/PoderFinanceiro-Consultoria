package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
public class PropostaHubController {

    @FXML
    private ListView<PropostaModel> listPropostas;
    @FXML
    private StackPane containerDetalhes;

    // --- Componentes do Overlay ---
    @FXML
    private VBox overlayConfirmacao;
    @FXML
    private Label lblConfirmacaoTexto;
    @FXML 
    private Button btnRemover;
    @FXML
    private PropostaController abaPropostaController;

    private final ApplicationContext context;
    private final PropostaRepository repository;

    // --- Variáveis de Controle de Estado ---
    private Runnable acaoPendente;
    private Runnable acaoCancelamentoPendente;
    private boolean isRevertendoSelecao = false;
    private boolean isAtualizandoInterface = false;

    public PropostaHubController(ApplicationContext context, PropostaRepository repository) {
        this.context = context;
        this.repository = repository;
    }

    @FXML
    public void initialize() {
        carregarFormularioInterno();
        configurarLista();

        // 🧠 Inteligência do Botão (Blindagem Visual):
        // Só habilita se a proposta existir no banco E NÃO estiver PAGA
        btnRemover.disableProperty().bind(
                javafx.beans.binding.Bindings.createBooleanBinding(() -> {
                    PropostaModel selecionada = listPropostas.getSelectionModel().getSelectedItem();

                    // 1. Bloqueia se não tem nada selecionado ou se é uma proposta "Nova" (sem ID
                    // no banco)
                    if (selecionada == null || selecionada.getId() == null) {
                        return true; // Retorna true para DISABLE = true
                    }

                    // 2. 🛡️ REGRA FINANCEIRA: Bloqueia se o status for PAGO
                    return selecionada.getStatus() == StatusPropostaModel.PAGO;

                }, listPropostas.getSelectionModel().selectedItemProperty()));
    }

    private void carregarFormularioInterno() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proposta.fxml"));
            loader.setControllerFactory(context::getBean);
            Node view = loader.load();
            abaPropostaController = loader.getController();
            containerDetalhes.getChildren().add(view);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar o formulário de detalhes da proposta", e);
        }
    }

    /**
     * 🚀 Seleciona cirurgicamente uma proposta específica na lista lateral
     */
    public void selecionarPropostaEspecifica(Long propostaIdAlvo) {
        if (propostaIdAlvo == null || listPropostas.getItems() == null)
            return;

        listPropostas.getItems().stream()
                .filter(p -> p.getId() != null && p.getId().equals(propostaIdAlvo))
                .findFirst()
                .ifPresent(p -> listPropostas.getSelectionModel().select(p));
    }
    
    private void configurarLista() {
        listPropostas.setCellFactory(param -> new ListCell<>() {
            private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            protected void updateItem(PropostaModel p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    String banco = (p.getBanco() != null) ? p.getBanco().getNome() : "Sem Banco";
                    String convenio = (p.getTabela() != null && p.getTabela().getTipoConvenio() != null)
                            ? p.getTabela().getTipoConvenio().toString()
                            : "Geral";
                    String taxa = (p.getTaxaAplicada() != null) ? p.getTaxaAplicada() + "%" : "--%";
                    String valor = (p.getValorSolicitado() != null) ? String.format("R$ %,.2f", p.getValorSolicitado())
                            : "R$ 0,00";
                    String data = (p.getDataSolicitacao() != null) ? p.getDataSolicitacao().format(df) : "--/--/----";
                    String status = (p.getStatus() != null) ? p.getStatus().getLabel() : "DESCONHECIDO";

                    String resumo = String.format(
                            "🏦 %s | 📋 %s\n💰 %s | 📉 %s\n📅 %s | [%s]",
                            banco.toUpperCase(), convenio, valor, taxa, data, status);

                    setText(resumo);
                    setStyle(
                            "-fx-padding: 12; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0; -fx-line-spacing: 5; -fx-font-size: 13px;");

                    if ("PAGO".equals(status)) {
                        setStyle(getStyle() + "-fx-background-color: rgba(0, 255, 0, 0.05);");
                    }
                }
            }
        });

        // 🛡️ O Listener de Seleção 100% Vacinado!
        listPropostas.getSelectionModel().selectedItemProperty().addListener((obs, old, novaProposta) -> {

            // Se o sistema está apenas desfazendo um clique abortado, ignora!
            if (isRevertendoSelecao) {
                isRevertendoSelecao = false;
                return;
            }

            // 💉 SE ESTAMOS SALVANDO/RECARREGANDO, NÃO FAZ PERGUNTAS, SÓ CARREGA!
            if (isAtualizandoInterface) {
                if (novaProposta != null) {
                    abaPropostaController.getViewModel().loadFromModel(novaProposta);
                }
                return;
            }

            if (novaProposta != null) {
                // ⚠️ AQUI ESTÁ A MÁGICA: Removido o "old != null".
                // Não importa se era uma proposta nova (null) ou velha. Se o formulário está
                // sujo, tem que avisar!
                if (abaPropostaController.getViewModel().isDirty()) { // <-- MUDOU PARA isDirty()

                    solicitarConfirmacao(
                            "Você tem alterações não salvas no formulário atual. Deseja descartá-las para abrir esta simulação?",
                            () -> abaPropostaController.getViewModel().loadFromModel(novaProposta), // Se Confirmar
                            () -> { // Se Cancelar
                                isRevertendoSelecao = true; // Liga o escudo
                                javafx.application.Platform.runLater(() -> {
                                    if (old != null) {
                                        listPropostas.getSelectionModel().select(old); // Devolve pro item que estava
                                    } else {
                                        listPropostas.getSelectionModel().clearSelection(); // Devolve pro formulário
                                                                                            // "Novo"
                                    }
                                });
                            });
                } else {
                    // Tudo limpo, pode carregar
                    abaPropostaController.getViewModel().loadFromModel(novaProposta);
                }
            }
        });
    }

    public void inicializarPropostasDoCliente(ProponenteModel proponente) {
        // 🛡️ LIGA A ANESTESIA (Impede que o vigia da lista grite ao trocar de item)
        isAtualizandoInterface = true;

        try {
            Long idSelecionadoAnterior = (listPropostas.getSelectionModel().getSelectedItem() != null)
                    ? listPropostas.getSelectionModel().getSelectedItem().getId()
                    : null;

            if (proponente != null && proponente.getId() != null) {
                List<PropostaModel> propostas = repository.findByProponenteId(proponente.getId());
                listPropostas.setItems(FXCollections.observableArrayList(propostas));

                if (!propostas.isEmpty()) {
                    if (idSelecionadoAnterior != null) {
                        propostas.stream().filter(p -> p.getId().equals(idSelecionadoAnterior))
                                .findFirst().ifPresent(p -> listPropostas.getSelectionModel().select(p));
                    } else {
                        listPropostas.getSelectionModel().select(0);
                    }

                    PropostaModel selecionada = listPropostas.getSelectionModel().getSelectedItem();
                    if (selecionada != null) {
                        abaPropostaController.getViewModel().loadFromModel(selecionada);

                        // 🚀 A CURA: Forçamos o bloqueio aqui, pois o listener está "anestesiado"
                        // Usamos Platform.runLater para garantir que a UI já existe no momento do
                        // disable
                        javafx.application.Platform.runLater(() -> {
                            abaPropostaController.aplicarBloqueio();
                        });
                    }
                } else {
                    criarNovaSimulacao(false); // Ignora checagem se está abrindo cliente novo
                }
            } else {
                listPropostas.getItems().clear();
                criarNovaSimulacao(false);
            }
        } finally {
            // 🔊 DESLIGA A ANESTESIA (Volta o comportamento normal de segurança)
            isAtualizandoInterface = false;
        }
    }

    // 🛡️ O botão de "Nova Simulação" agora usa o Overlay
    @FXML
    public void criarNovaSimulacao() {
        criarNovaSimulacao(true);
    }

    private void criarNovaSimulacao(boolean checarAlteracoes) {
        // <-- MUDOU PARA isDirty() AQUI TAMBÉM
        if (checarAlteracoes && abaPropostaController.getViewModel().isDirty()) {
            solicitarConfirmacao(
                    "Você tem alterações não salvas. Deseja descartar e iniciar uma simulação em branco?",
                    () -> resetarParaNova(),
                    null // Não precisa fazer nada ao cancelar
            );
        } else {
            resetarParaNova();
        }
    }

    private void resetarParaNova() {
        listPropostas.getSelectionModel().clearSelection();
        abaPropostaController.getViewModel().reset();
    }
    
    // ==========================================================
    // PROCEDIMENTO DE AMPUTAÇÃO (Remoção)
    // ==========================================================

    @FXML
    public void solicitarRemocao() {
        PropostaModel selecionada = listPropostas.getSelectionModel().getSelectedItem();

        if (selecionada != null && selecionada.getId() != null) {
            solicitarConfirmacao(
                    "🗑️ EXCLUSÃO PERMANENTE\n\nTem certeza que deseja excluir esta simulação? Esta ação não poderá ser desfeita e a proposta será apagada do banco de dados.",
                    () -> executarRemocao(selecionada), // O que fazer se confirmar
                    null // Não faz nada se cancelar
            );
        }
    }

    private void executarRemocao(PropostaModel proposta) {
        // 🛡️ Liga a anestesia para a tela não gritar enquanto mexemos na lista
        isAtualizandoInterface = true;

        try {
            // 1. Deleta fisicamente do Banco de Dados
            repository.deleteById(proposta.getId());

            // 2. Remove da visão da Solange (a ListView do JavaFX atualiza automaticamente)
            listPropostas.getItems().remove(proposta);

            // 3. Limpa o formulário para uma simulação em branco
            criarNovaSimulacao(false);

        } catch (Exception e) {
            e.printStackTrace();
            // Opcional: Aqui você pode disparar um alerta se o banco de dados bloquear a
            // exclusão
        } finally {
            // 🔊 Desliga a anestesia
            isAtualizandoInterface = false;
        }
    }

    // ==========================================================
    // SISTEMA DE OVERLAY DE CONFIRMAÇÃO (O Neurologista)
    // ==========================================================

    private void solicitarConfirmacao(String mensagem, Runnable onConfirm, Runnable onCancel) {
        lblConfirmacaoTexto.setText(mensagem);
        this.acaoPendente = onConfirm;
        this.acaoCancelamentoPendente = onCancel;
        overlayConfirmacao.setVisible(true);
    }

    @FXML
    public void confirmarDescarte() {
        overlayConfirmacao.setVisible(false);
        if (acaoPendente != null)
            acaoPendente.run();
        limparAcoesPendentes();
    }

    @FXML
    public void cancelarAcao() {
        overlayConfirmacao.setVisible(false);
        if (acaoCancelamentoPendente != null)
            acaoCancelamentoPendente.run();
        limparAcoesPendentes();
    }

    private void limparAcoesPendentes() {
        acaoPendente = null;
        acaoCancelamentoPendente = null;
    }

    public PropostaViewModel getViewModel() {
        return abaPropostaController.getViewModel();
    }

    // 🩹 O REPASSE: Método para o Hub conseguir acessar a Proposta
    public PropostaController getAbaPropostaController() {
        return abaPropostaController;
    }
}