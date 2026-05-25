package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.DocumentoUtils;
import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ProponenteListController implements Disposable {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String MSG_CARREGANDO_BASE = "Carregando base de clientes...";
    private static final String MSG_TRIAGEM_BUSCA = "Realizando triagem da busca...";
    private static final String MSG_TOTAL_REGISTROS = "Total: %d contato(s)";
    private static final Logger log = LoggerFactory.getLogger(ProponenteListController.class);

    // =========================================================================
    // DEPENDÊNCIAS DE UI E FXML
    // =========================================================================
    @FXML
    private TableView<ProponenteModel> tabelaClientes;
    @FXML
    private TextField txtBusca;
    @FXML
    private Label lblTotalRegistros;
    @FXML
    private TableColumn<ProponenteModel, String> colNome;
    @FXML
    private TableColumn<ProponenteModel, String> colCpf;
    @FXML
    private TableColumn<ProponenteModel, String> colTelefone;
    @FXML
    private TableColumn<ProponenteModel, String> colOrigem;
    @FXML
    private TableColumn<ProponenteModel, String> colClassificacao;

    // =========================================================================
    // INJEÇÃO DE DEPENDÊNCIAS E ESTADO DA CLASSE
    // =========================================================================
    private final ProponenteService proponenteService;
    private final Navigator navigator;
    private final ObservableList<ProponenteModel> listaContatos = FXCollections.observableArrayList();
    private final ProponenteUIEventHub eventHub;

    public ProponenteListController(ProponenteService proponenteService, Navigator navigator,
            ProponenteUIEventHub eventHub) {
        this.proponenteService = proponenteService;
        this.navigator = navigator;
        this.eventHub = eventHub;
        log.debug("[PROPONENTE_LIST] Construtor: Controller instanciado");
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        log.debug("[PROPONENTE_LIST] initialize: Configurando tabela e carregando dados");
        tabelaClientes.setItems(listaContatos);
        configurarColunas();
        configurarInteracaoTabela();
        carregarDados();
        eventHub.inscrever(this::carregarDados);
        log.info("[PROPONENTE_LIST] initialize: Configuração concluída e inscrita no event hub");
    }

    private void configurarColunas() {
        log.debug("[PROPONENTE_LIST] configurarColunas: Aplicando formatadores nas colunas CPF e Telefone");
        aplicarFormatadorColuna(colCpf, DocumentoUtils::formatarCpf);
        aplicarFormatadorColuna(colTelefone, ContatoUtils::formatarTelefone);
    }

    private void configurarInteracaoTabela() {
        log.debug("[PROPONENTE_LIST] configurarInteracaoTabela: Adicionando evento de duplo clique para abrir cliente");
        tabelaClientes.setRowFactory(tv -> {
            TableRow<ProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ProponenteModel cliente = row.getItem();
                    log.info("[PROPONENTE_LIST] Duplo clique: abrindo cliente '{}' (ID={})",
                            cliente.getNomeCompleto(), cliente.getId());
                    navigator.abrirClienteNoWorkspace(cliente);
                }
            });
            return row;
        });
    }

    // =========================================================================
    // EVENTOS DE TELA E BUSCA
    // =========================================================================
    public void carregarDados() {
        log.debug("[PROPONENTE_LIST] carregarDados: Iniciando carregamento da lista completa");
        executarTarefaAssincrona(
                proponenteService::listarMinhaCarteira,
                MSG_CARREGANDO_BASE);
    }

    @FXML
    private void handleBusca() {
        String termo = txtBusca.getText();
        log.debug("[PROPONENTE_LIST] handleBusca: termo='{}'", termo);

        if (termo == null || termo.isBlank()) {
            log.debug("[PROPONENTE_LIST] Busca vazia, carregando dados completos");
            carregarDados();
        } else {
            log.info("[PROPONENTE_LIST] Busca por termo: '{}'", termo);
            executarTarefaAssincrona(
                    () -> proponenteService.buscaRapida(termo.trim()),
                    MSG_TRIAGEM_BUSCA);
        }
    }

    @FXML
    private void handleNovoProponente() {
        log.info("[PROPONENTE_LIST] Usuário acionou 'Novo Proponente'");
        navigator.irParaNovoContato();
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS E FORMATAÇÃO (DRY & SRP)
    // =========================================================================
    private void atualizarContador() {
        int total = listaContatos.size();
        lblTotalRegistros.setText(String.format(MSG_TOTAL_REGISTROS, total));
        log.trace("[PROPONENTE_LIST] Contador atualizado: {} contato(s)", total);
    }

    /**
     * Aplica formatação visual em células de tabela de forma reutilizável.
     */
    private <T> void aplicarFormatadorColuna(TableColumn<ProponenteModel, T> coluna, Function<T, String> formatador) {
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatador.apply(item));
                    log.trace("[PROPONENTE_LIST] Célula formatada: {}", formatador.apply(item));
                }
            }
        });
    }

    /**
     * O MOTOR DA CLÍNICA: Gerencia o Maqueiro (Task) e a Sala de Espera (Loading).
     */
    private void executarTarefaAssincrona(Supplier<List<ProponenteModel>> acaoBusca, String mensagem) {
        log.debug("[PROPONENTE_LIST] executarTarefaAssincrona: {}", mensagem);
        navigator.mostrarLoading(mensagem);

        AsyncUtils.executarTaskAsync(
                acaoBusca::get,
                resultado -> {
                    log.info("[PROPONENTE_LIST] Tarefa concluída: {} registros retornados", resultado.size());
                    listaContatos.setAll(resultado);
                    atualizarContador();
                    navigator.ocultarLoading();
                },
                erro -> {
                    navigator.ocultarLoading();
                    log.error("[PROPONENTE_LIST] Erro na tarefa assíncrona: {}",
                            erro != null ? erro.getMessage() : "exceção nula", erro);
                });
    }

    @Override
    public void dispose() {
        log.info("[PROPONENTE_LIST] dispose: Desinscrevendo do event hub");
        eventHub.desinscrever(this::carregarDados);
    }
}