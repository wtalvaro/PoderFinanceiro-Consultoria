package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.facade.IProponenteFacade;
import br.com.poderfinanceiro.app.infrastructure.ui.navigation.Navigator;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.Disposable;
import br.com.poderfinanceiro.app.util.DocumentoUtils;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <h1>ProponenteListController</h1>
 * <p>
 * Controlador de Interface (UI) responsável por gerenciar a listagem de
 * clientes (CRM). Implementa o padrão <b>Humble Object</b>, delegando buscas e
 * filtros para a {@link IProponenteFacade}.
 * </p>
 */
@Component
public class ProponenteListController implements Disposable {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(ProponenteListController.class);
    private static final String LOG_PREFIX = "[ProponenteListController]";
    private static final String MSG_CARREGANDO_BASE = "Carregando base de clientes...";
    private static final String MSG_TRIAGEM_BUSCA = "Realizando triagem da busca...";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IProponenteFacade proponenteFacade;
    private final Navigator navigator;
    private final ProponenteUIEventHub eventHub;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TableView<ProponenteModel> tabelaClientes;
    @FXML private TextField txtBusca;
    @FXML private Label lblTotalRegistros;
    @FXML private TableColumn<ProponenteModel, String> colNome, colCpf, colTelefone, colOrigem, colClassificacao;

    // ==========================================================================================
    // MÓDULO 4: ESTADO INTERNO DA TELA
    // ==========================================================================================
    private final ObservableList<ProponenteModel> listaContatos = FXCollections.observableArrayList();

    public ProponenteListController(IProponenteFacade proponenteFacade, Navigator navigator, ProponenteUIEventHub eventHub) {
        this.proponenteFacade = proponenteFacade;
        this.navigator = navigator;
        this.eventHub = eventHub;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando interface de Listagem de Clientes...", LOG_PREFIX);
        tabelaClientes.setItems(listaContatos);
        configurarColunas();
        configurarInteracaoTabela();
        carregarDados();

        eventHub.inscrever(this::carregarDados);
        lblTotalRegistros.textProperty().bind(Bindings.format("Total: %d contato(s)", Bindings.size(tabelaClientes.getItems())));

        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    @Override public void dispose() {
        log.info("{} [LIFECYCLE] Desinscrevendo do hub de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::carregarDados);
    }

    // ==========================================================================================
    // MÓDULO 6: CONFIGURAÇÃO DE UI
    // ==========================================================================================
    private void configurarColunas() {
        log.trace("{} [UI] Aplicando formatadores visuais nas colunas.", LOG_PREFIX);
        aplicarFormatadorColuna(colCpf, DocumentoUtils::formatarCpf);
        aplicarFormatadorColuna(colTelefone, ContatoUtils::formatarTelefone);
    }

    private void configurarInteracaoTabela() {
        log.trace("{} [UI] Configurando eventos de duplo clique na tabela.", LOG_PREFIX);
        tabelaClientes.setRowFactory(tv -> {
            TableRow<ProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ProponenteModel cliente = row.getItem();
                    log.info("{} [TELEMETRIA] Duplo clique detectado. Abrindo cliente ID: {}", LOG_PREFIX, cliente.getId());
                    navigator.abrirClienteNoWorkspace(cliente);
                }
            });
            return row;
        });
    }

    private <T> void aplicarFormatadorColuna(TableColumn<ProponenteModel, T> coluna, Function<T, String> formatador) {
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatador.apply(item));
                }
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 7: AÇÕES E BUSCAS
    // ==========================================================================================
    public void carregarDados() {
        log.debug("{} [TELEMETRIA] Solicitando carregamento completo da carteira.", LOG_PREFIX);
        executarTarefaAssincrona(proponenteFacade::listarClientesCarteira, MSG_CARREGANDO_BASE);
    }

    @FXML private void handleBusca() {
        String termo = txtBusca.getText();
        log.info("{} [TELEMETRIA] Usuário acionou busca. Termo: '{}'", LOG_PREFIX, termo);

        if (termo == null || termo.isBlank()) {
            carregarDados();
        } else {
            executarTarefaAssincrona(() -> proponenteFacade.buscarClientes(termo), MSG_TRIAGEM_BUSCA);
        }
    }

    @FXML private void handleNovoProponente() {
        log.info("{} [TELEMETRIA] Usuário acionou 'Novo Proponente'.", LOG_PREFIX);
        navigator.irParaNovoContato();
    }

    // ==========================================================================================
    // MÓDULO 8: MOTOR ASSÍNCRONO
    // ==========================================================================================
    private void executarTarefaAssincrona(Supplier<List<ProponenteModel>> acaoBusca, String mensagem) {
        log.trace("{} [SISTEMA] Iniciando tarefa assíncrona: {}", LOG_PREFIX, mensagem);
        navigator.mostrarLoading(mensagem);

        AsyncUtils.executarTaskAsync(acaoBusca::get, resultado -> {
            log.info("{} [TELEMETRIA] Tarefa concluída. {} registros retornados.", LOG_PREFIX, resultado.size());
            listaContatos.setAll(resultado);
            navigator.ocultarLoading();
        }, erro -> {
            log.error("{} [SISTEMA] Erro na tarefa assíncrona: {}", LOG_PREFIX, erro.getMessage());
            navigator.ocultarLoading();
            navigator.notificarAviso("Erro ao buscar dados: " + erro.getMessage());
        });
    }
}
