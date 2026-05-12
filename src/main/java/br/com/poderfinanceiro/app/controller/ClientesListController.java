package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.ProponenteModel;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.utils.ContatoUtils;
import br.com.poderfinanceiro.app.utils.DocumentoUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class ClientesListController {

    private final ProponenteService proponenteService;
    private final MainController mainController;
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

    // Lista observável que o JavaFX usa para atualizar a tabela em tempo real
    private ObservableList<ProponenteModel> listaContatos = FXCollections.observableArrayList();

    public ClientesListController(ProponenteService proponenteService,
            MainController mainController,
            LeadController leadController) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        tabelaClientes.setItems(listaContatos);
        configurarFormatacaoCpfNaTabela();
        configurarFormatacaoTelefoneNaTabela();

        tabelaClientes.setRowFactory(tv -> {
            TableRow<ProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    ProponenteModel selecionado = row.getItem();
                    mainController.abrirClienteNoWorkspace(selecionado);
                }
            });
            return row;
        });

        // 1. O primeiro exame: busca inicial ao abrir
        carregarDados();
    }

    /**
     * Aciona o maqueiro para buscar toda a carteira de clientes.
     */
    public void carregarDados() {
        executarTarefaAssincrona(
                () -> proponenteService.listarMinhaCarteira(),
                "Carregando base de clientes...");
    }

    /**
     * Aciona o maqueiro para uma busca específica por termo.
     */
    @FXML
    private void handleBusca() {
        String termo = txtBusca.getText();

        if (termo == null || termo.trim().isEmpty()) {
            carregarDados();
        } else {
            executarTarefaAssincrona(
                    () -> proponenteService.buscaRapida(termo.trim()),
                    "Realizando triagem da busca...");
        }
    }

    @FXML
    private void handleNovoContato() {
        mainController.irParaNovoContato();
    }

    /**
     * O MOTOR DA CLÍNICA: Método auxiliar que gerencia o Maqueiro (Task)
     * e a Sala de Espera (Loading) de forma genérica.
     */
    private void executarTarefaAssincrona(Supplier<List<ProponenteModel>> busca, String mensagem) {
        // A. Acende a luz da Sala de Espera no MainController
        mainController.mostrarLoading(mensagem);

        // B. Prepara o Maqueiro (A Task)
        Task<List<ProponenteModel>> task = new Task<>() {
            @Override
            protected List<ProponenteModel> call() throws Exception {
                // Trabalho pesado fora da recepção (UI Thread)
                return busca.get();
            }
        };

        // C. O que fazer ao finalizar o transporte com sucesso
        task.setOnSucceeded(e -> {
            // Atualiza a lista na recepção (UI Thread)
            listaContatos.setAll(task.getValue());
            atualizarContador();
            mainController.ocultarLoading();
        });

        // D. Caso ocorra uma intercorrência (Erro no Banco)
        task.setOnFailed(e -> {
            mainController.ocultarLoading();
            task.getException().printStackTrace();
            // Dica: Aqui você pode chamar um overlay de erro no futuro
        });

        // E. Dispara o transporte em uma nova linha (Thread)
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Garante que a thread feche se o app fechar
        thread.start();
    }
    
    private void atualizarContador() {
        lblTotalRegistros.setText("Total: " + listaContatos.size() + " contato(s)");
    }

    private void configurarFormatacaoCpfNaTabela() {
        // Usamos o 'setCellFactory' garantindo que o retorno seja o esperado pela
        // coluna <Proponente, String>
        colCpf.setCellFactory(tc -> new TableCell<ProponenteModel, String>() {
            @Override
            protected void updateItem(String cpf, boolean empty) {
                super.updateItem(cpf, empty);

                if (empty || cpf == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Aqui usamos o seu utilitário DRY
                    setText(DocumentoUtils.formatarCpf(cpf));
                }
            }
        });
    }

    private void configurarFormatacaoTelefoneNaTabela() {
        colTelefone.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String tel, boolean empty) {
                super.updateItem(tel, empty);
                if (empty || tel == null) {
                    setText(null);
                } else {
                    setText(ContatoUtils.formatarTelefone(tel));
                }
            }
        });
    }
}