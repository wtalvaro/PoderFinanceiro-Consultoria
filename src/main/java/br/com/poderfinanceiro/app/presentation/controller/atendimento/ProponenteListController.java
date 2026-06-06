package br.com.poderfinanceiro.app.presentation.controller.atendimento;

import br.com.poderfinanceiro.app.application.facade.IProponenteFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.ContatoUtils;
import br.com.poderfinanceiro.app.common.util.Disposable;
import br.com.poderfinanceiro.app.common.util.DocumentoUtils;
import br.com.poderfinanceiro.app.domain.event.ProponenteUIEventHub;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.presentation.ui.navigation.AppRoute;
import br.com.poderfinanceiro.app.presentation.ui.navigation.Navigator;
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
 * Controlador de Interface responsável pela gestão da listagem de clientes
 * (CRM).
 * Utiliza o Hub de Eventos para atualização reativa da tabela e navegação
 * tipada.
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
    @FXML
    private TableView<ProponenteModel> tabelaClientes;
    @FXML
    private TextField txtBusca;
    @FXML
    private Label lblTotalRegistros;
    @FXML
    private TableColumn<ProponenteModel, String> colNome, colCpf, colTelefone, colOrigem, colClassificacao;

    private final ObservableList<ProponenteModel> listaContatos = FXCollections.observableArrayList();

    public ProponenteListController(IProponenteFacade proponenteFacade, Navigator navigator,
            ProponenteUIEventHub eventHub) {
        this.proponenteFacade = proponenteFacade;
        this.navigator = navigator;
        this.eventHub = eventHub;
        log.info("{} [SISTEMA] Controlador de listagem instanciado.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E EVENTOS
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando interface de CRM.", LOG_PREFIX);
        tabelaClientes.setItems(listaContatos);
        configurarColunas();
        configurarInteracaoTabela();
        carregarDados();

        log.debug("{} [SISTEMA] Inscrevendo controlador no Hub de Eventos.", LOG_PREFIX);
        eventHub.inscrever(this::carregarDados);

        lblTotalRegistros.textProperty()
                .bind(Bindings.format("Total: %d contato(s)", Bindings.size(tabelaClientes.getItems())));
    }

    @Override
    public void dispose() {
        log.info("{} [SISTEMA] Liberando recursos e desinscrevendo de eventos.", LOG_PREFIX);
        eventHub.desinscrever(this::carregarDados);
    }

    private void configurarColunas() {
        log.trace("{} [UI] Aplicando formatadores de coluna (CPF/Telefone).", LOG_PREFIX);
        aplicarFormatadorColuna(colCpf, DocumentoUtils::formatarCpf);
        aplicarFormatadorColuna(colTelefone, ContatoUtils::formatarTelefone);
    }

    private void configurarInteracaoTabela() {
        tabelaClientes.setRowFactory(tv -> {
            TableRow<ProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ProponenteModel cliente = row.getItem();
                    log.info("{} [TELEMETRIA] Abertura de cliente solicitada via duplo clique. ID: {}", LOG_PREFIX,
                            cliente.getId());
                    navigator.abrirClienteNoWorkspace(cliente);
                }
            });
            return row;
        });
    }

    private <T> void aplicarFormatadorColuna(TableColumn<ProponenteModel, T> coluna, Function<T, String> formatador) {
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatador.apply(item));
                }
            }
        });
    }

    // ==========================================================================================
    // MÓDULO 5: LÓGICA DE NEGÓCIO E BUSCA
    // ==========================================================================================
    public void carregarDados() {
        log.info("{} [TELEMETRIA] Iniciando carga assíncrona da carteira de clientes.", LOG_PREFIX);
        executarTarefaAssincrona(proponenteFacade::listarClientesCarteira, MSG_CARREGANDO_BASE);
    }

    @FXML
    private void handleBusca() {
        String termo = txtBusca.getText();
        if (termo == null || termo.isBlank()) {
            log.debug("{} [TELEMETRIA] Busca vazia. Resetando para lista completa.", LOG_PREFIX);
            carregarDados();
        } else {
            log.info("{} [TELEMETRIA] Executando triagem de busca para o termo: '{}'", LOG_PREFIX, termo);
            executarTarefaAssincrona(() -> proponenteFacade.buscarClientes(termo), MSG_TRIAGEM_BUSCA);
        }
    }

    // ==========================================================================================
    // MÓDULO 6: NAVEGAÇÃO
    // ==========================================================================================
    @FXML
    private void handleNovoProponente() {
        log.info("{} [TELEMETRIA] Redirecionando para criação de novo contato via Registry.", LOG_PREFIX);
        // CORREÇÃO: Uso da navegação tipada com AppRoute
        navigator.navegarPara(AppRoute.CADASTRO_PROPONENTE);
    }

    // ==========================================================================================
    // MÓDULO 7: ORQUESTRAÇÃO ASSÍNCRONA
    // ==========================================================================================
    private void executarTarefaAssincrona(Supplier<List<ProponenteModel>> acaoBusca, String mensagem) {
        navigator.mostrarLoading(mensagem);

        AsyncUtils.executarTaskAsync(
                acaoBusca::get,
                resultado -> {
                    log.info("{} [AUDITORIA] Carga finalizada. {} registros recuperados.", LOG_PREFIX,
                            resultado.size());
                    listaContatos.setAll(resultado);
                    navigator.ocultarLoading();
                },
                erro -> {
                    log.error("{} [SISTEMA] Falha na recuperação de dados: {}", LOG_PREFIX, erro.getMessage());
                    navigator.ocultarLoading();
                    navigator.notificarAviso("Erro ao buscar dados: " + erro.getMessage());
                });
    }
}
