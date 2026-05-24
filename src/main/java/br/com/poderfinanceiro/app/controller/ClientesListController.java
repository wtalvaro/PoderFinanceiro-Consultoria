package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.ContatoUtils;
import br.com.poderfinanceiro.app.util.DocumentoUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

@Component
public class ClientesListController {

    // =========================================================================
    // CONSTANTES (Clean Code)
    // =========================================================================
    private static final String MSG_CARREGANDO_BASE = "Carregando base de clientes...";
    private static final String MSG_TRIAGEM_BUSCA = "Realizando triagem da busca...";
    private static final String MSG_TOTAL_REGISTROS = "Total: %d contato(s)";

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
    private final MainController mainController;
    private final ObservableList<ProponenteModel> listaContatos = FXCollections.observableArrayList();

    public ClientesListController(ProponenteService proponenteService, MainController mainController) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================
    @FXML
    public void initialize() {
        tabelaClientes.setItems(listaContatos);
        configurarColunas();
        configurarInteracaoTabela();
        carregarDados();
    }

    private void configurarColunas() {
        aplicarFormatadorColuna(colCpf, DocumentoUtils::formatarCpf);
        aplicarFormatadorColuna(colTelefone, ContatoUtils::formatarTelefone);
    }

    private void configurarInteracaoTabela() {
        tabelaClientes.setRowFactory(tv -> {
            TableRow<ProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mainController.abrirClienteNoWorkspace(row.getItem());
                }
            });
            return row;
        });
    }

    // =========================================================================
    // EVENTOS DE TELA E BUSCA
    // =========================================================================
    public void carregarDados() {
        executarTarefaAssincrona(
                proponenteService::listarMinhaCarteira,
                MSG_CARREGANDO_BASE);
    }

    @FXML
    private void handleBusca() {
        String termo = txtBusca.getText();

        if (termo == null || termo.isBlank()) {
            carregarDados();
        } else {
            executarTarefaAssincrona(
                    () -> proponenteService.buscaRapida(termo.trim()),
                    MSG_TRIAGEM_BUSCA);
        }
    }

    @FXML
    private void handleNovoContato() {
        mainController.irParaNovoContato();
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS E FORMATAÇÃO (DRY & SRP)
    // =========================================================================
    private void atualizarContador() {
        lblTotalRegistros.setText(String.format(MSG_TOTAL_REGISTROS, listaContatos.size()));
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
                }
            }
        });
    }

    /**
     * O MOTOR DA CLÍNICA: Gerencia o Maqueiro (Task) e a Sala de Espera (Loading).
     */
    private void executarTarefaAssincrona(Supplier<List<ProponenteModel>> acaoBusca, String mensagem) {
        mainController.mostrarLoading(mensagem);

        AsyncUtils.executarTaskAsync(
                acaoBusca::get, // Converte o Supplier no Callable esperado pelo AsyncUtils
                resultado -> {
                    listaContatos.setAll(resultado);
                    atualizarContador();
                    mainController.ocultarLoading();
                },
                erro -> {
                    mainController.ocultarLoading();
                    if (erro != null) {
                        erro.printStackTrace();
                    }
                });
    }
}