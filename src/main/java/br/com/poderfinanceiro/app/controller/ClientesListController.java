package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.service.ProponenteService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
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

@Component
public class ClientesListController {

    private final ProponenteService proponenteService;
    private final MainController mainController;
    private final LeadController leadController; // Injetamos o parceiro aqui!

    @FXML
    private TableView<Proponente> tabelaClientes;
    @FXML
    private TextField txtBusca;
    @FXML
    private Label lblTotalRegistros;
    @FXML
    private TableColumn<Proponente, String> colNome;
    @FXML
    private TableColumn<Proponente, String> colCpf;
    @FXML
    private TableColumn<Proponente, String> colTelefone;
    @FXML
    private TableColumn<Proponente, String> colOrigem;
    @FXML
    private TableColumn<Proponente, String> colClassificacao;

    // Lista observável que o JavaFX usa para atualizar a tabela em tempo real
    private ObservableList<Proponente> listaContatos = FXCollections.observableArrayList();

    public ClientesListController(ProponenteService proponenteService,
            MainController mainController,
            LeadController leadController) {
        this.proponenteService = proponenteService;
        this.mainController = mainController;
        this.leadController = leadController;
    }

    @FXML
    public void initialize() {
        tabelaClientes.setItems(listaContatos);

        // 2. Aplica a formatação visual na coluna de CPF
        configurarFormatacaoCpfNaTabela();
        configurarFormatacaoTelefoneNaTabela();

        // Configura o Duplo Clique na Tabela
        tabelaClientes.setRowFactory(tv -> {
            TableRow<Proponente> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Proponente clienteSelecionado = row.getItem();
                    abrirEdicao(clienteSelecionado);
                }
            });
            return row;
        });

        // Carrega os dados do banco assim que a tela abre
        carregarDados();
    }

    /**
     * Busca os dados no banco usando o Service.
     */
    public void carregarDados() {
        // Limpa a lista atual e busca tudo novamente
        listaContatos.clear();
        List<Proponente> carteira = proponenteService.listarMinhaCarteira();
        listaContatos.addAll(carteira);

        atualizarContador();
    }

    @FXML
    private void handleBusca() {
        String termo = txtBusca.getText();
        listaContatos.clear();

        if (termo == null || termo.trim().isEmpty()) {
            // Se a busca estiver vazia, traz todo mundo
            listaContatos.addAll(proponenteService.listarMinhaCarteira());
        } else {
            // Se digitou algo, usa a busca rápida do Service
            listaContatos.addAll(proponenteService.buscaRapida(termo.trim()));
        }
        atualizarContador();
    }

    @FXML
    private void handleNovoContato() {
        mainController.irParaNovoContato();
    }

    private void abrirEdicao(Proponente cliente) {
        // 1. Passa o cliente para o LeadController
        leadController.prepararEdicao(cliente);

        // 2. Manda o MainController trocar a tela para mostrar o formulário
        mainController.navegarPara("/fxml/lead.fxml", true);
    }

    private void atualizarContador() {
        lblTotalRegistros.setText("Total: " + listaContatos.size() + " contato(s)");
    }

    private void configurarFormatacaoCpfNaTabela() {
        // Usamos o 'setCellFactory' garantindo que o retorno seja o esperado pela
        // coluna <Proponente, String>
        colCpf.setCellFactory(tc -> new TableCell<Proponente, String>() {
            @Override
            protected void updateItem(String cpf, boolean empty) {
                super.updateItem(cpf, empty);

                if (empty || cpf == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Aqui usamos o seu utilitário DRY
                    setText(FinanceiroUtils.formatarCpf(cpf));
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
                    setText(FinanceiroUtils.formatarTelefone(tel));
                }
            }
        });
    }
}