package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.service.DocumentoService;
import br.com.poderfinanceiro.app.service.PropostaService;
import br.com.poderfinanceiro.app.service.TabelaJurosService;
import br.com.poderfinanceiro.app.utils.FinanceiroUtils;
import br.com.poderfinanceiro.app.viewmodel.PropostaViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

@Component
@Scope("prototype")
public class PropostaController {

    private final PropostaViewModel viewModel;
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;
    private final DocumentoService documentoService;

    // --- NOVOS ELEMENTOS DA TRIAGEM ---
    @FXML
    private ComboBox<TipoConvenioModel> cbConvenio;
    @FXML
    private TableView<DocumentoProponenteModel> tableDocumentos;
    @FXML
    private ComboBox<BancoModel> cbBanco;
    @FXML
    private ComboBox<TabelaJurosModel> cbTabela;
    @FXML
    private ComboBox<StatusPropostaModel> cbStatus;
    @FXML
    private TextField txtValorSolicitado;
    @FXML
    private TextField txtValorAprovado;
    @FXML
    private TextField txtParcela;
    @FXML
    private Spinner<Integer> spinPrazo;
    @FXML
    private TextArea txtObservacoes;
    @FXML
    private Label lblComissaoEstimada;
    @FXML
    private Label lblTituloComissao;
    @FXML
    private Label lblTotalPago;
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colTipoDocumento; // Nome deve ser igual ao fx:id no FXML
    @FXML
    private TableColumn<DocumentoProponenteModel, String> colDataUpload; // Nome deve ser igual ao fx:id no FXML
    @FXML
    private TableColumn<DocumentoProponenteModel, Void> colAcoes;

    // Caches da Memória
    private List<TabelaJurosModel> todasTabelasAtivas;
    private List<TabelaJurosModel> tabelasElegiveisDaTriagem;

    // 🛡️ ANESTESIA LOCAL: Evita que a triagem destrua os dados ao trocar de
    // paciente
    private boolean isUpdatingInterface = false;

    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();

    public PropostaController(PropostaViewModel viewModel, DocumentoService documentoService,
            PropostaService propostaService, TabelaJurosService tabelaJurosService) {
        this.viewModel = viewModel;
        this.documentoService = documentoService;
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
    }

    @FXML
    public void initialize() {
        // Conecte a tabela à lista mestre
        tableDocumentos.setItems(listaDocumentos);

        configurarColunasDocumentos();
        carregarListasBase(); // Chamada única
        configurarFormatadoresEBindings();
        configurarGatilhosDaTriagem();
        configurarAutoSelecao();
    }

    private void carregarListasBase() {
        todasTabelasAtivas = tabelaJurosService.listarAtivas();
        cbStatus.setItems(FXCollections.observableArrayList(StatusPropostaModel.values()));

        // Inicializa o Combo de Convênio (Se você adicionou no FXML)
        if (cbConvenio != null) {
            cbConvenio.setItems(FXCollections.observableArrayList(TipoConvenioModel.values()));

            // 🩹 CURATIVO 1: Ensina o ComboBox a mostrar o nome bonito (Label)
            cbConvenio.setConverter(new StringConverter<>() {
                @Override
                public String toString(TipoConvenioModel tipo) {
                    return tipo != null ? tipo.getLabel() : "Selecione o Convênio...";
                }

                @Override
                public TipoConvenioModel fromString(String s) {
                    return null; // Apenas para exibição
                }
            });

            // Sugere o convênio...
            viewModel.proponenteProperty().addListener((obs, old, paciente) -> {
                if (paciente != null && paciente.getConvenioOrgao() != null && cbConvenio.getValue() == null) {
                    cbConvenio.setValue(paciente.getConvenioOrgao());
                }
            });
        }

        // Conversores Visuais
        cbBanco.setConverter(new StringConverter<>() {
            @Override
            public String toString(BancoModel b) {
                return b != null ? b.getNome() : "Aguardando Triagem...";
            }

            @Override
            public BancoModel fromString(String s) {
                return null;
            }
        });

        cbTabela.setConverter(new StringConverter<>() {
            @Override
            public String toString(TabelaJurosModel t) {
                return t != null ? t.getNomeTabela() + " (" + t.getComissaoPercentual() + "%)"
                        : "Selecione a Tabela...";
            }

            @Override
            public TabelaJurosModel fromString(String s) {
                return null;
            }
        });
    }

    private void configurarFormatadoresEBindings() {
        cbStatus.valueProperty().bindBidirectional(viewModel.statusProperty());
        txtObservacoes.textProperty().bindBidirectional(viewModel.observacoesProperty());

        // --- CONFIGURAÇÃO DO PRAZO (SPINNER) ---
        spinPrazo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 0));
        spinPrazo.setEditable(true); // Permite digitação livre

        // "Hack" nativo de UX do JavaFX:
        // Garante que se ela digitar "84" e clicar em outro campo, o valor é salvo sem
        // precisar dar Enter.
        spinPrazo.getEditor().focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
            if (!agoraFocado) {
                spinPrazo.increment(0); // Força o JavaFX a fazer o "commit" do texto digitado para a ValueFactory
            }
        });

        spinPrazo.getValueFactory().valueProperty().bindBidirectional(viewModel.quantidadeParcelasProperty());

        // Máscaras Monetárias
        TextFormatter<BigDecimal> fmtSolicitado = FinanceiroUtils.criarFormatadorMoeda();
        txtValorSolicitado.setTextFormatter(fmtSolicitado);
        fmtSolicitado.valueProperty().bindBidirectional(viewModel.valorSolicitadoProperty());

        TextFormatter<BigDecimal> fmtAprovado = FinanceiroUtils.criarFormatadorMoeda();
        txtValorAprovado.setTextFormatter(fmtAprovado);
        fmtAprovado.valueProperty().bindBidirectional(viewModel.valorAprovadoProperty());

        TextFormatter<BigDecimal> fmtParcela = FinanceiroUtils.criarFormatadorMoeda();
        txtParcela.setTextFormatter(fmtParcela);
        fmtParcela.valueProperty().bindBidirectional(viewModel.valorParcelaProperty());

        // Quando carrega uma proposta do banco, ajusta a UI sob anestesia
        viewModel.tabelaIdProperty().addListener((obs, old, idNovo) -> {
            if (isUpdatingInterface)
                return; // Se está dopado, não repete a dose

            if (idNovo != null) {
                TabelaJurosModel tab = todasTabelasAtivas.stream()
                        .filter(t -> t.getId().equals(idNovo)).findFirst().orElse(null);

                if (tab != null) {
                    isUpdatingInterface = true; // 🛡️ LIGA ANESTESIA
                    try {
                        cbConvenio.setValue(tab.getTipoConvenio());
                        realizarTriagem(); // Carrega os bancos do convênio, mas sem Piloto Automático
                        cbBanco.setValue(tab.getBanco());
                        atualizarTabelasDoBanco(tab.getBanco()); // Carrega as tabelas do banco
                        cbTabela.setValue(tab);
                        dispararCalculo(); // Atualiza os R$ na tela
                        // 🩹 O GATILHO: Dispara o carregamento dos documentos desta proposta específica
                        Long propostaId = viewModel.idProperty().get();
                        if (propostaId != null) {
                            carregarDocumentosDaProposta(propostaId); // Agora o método é utilizado!
                        }
                    } finally {
                        isUpdatingInterface = false; // 🔊 DESLIGA ANESTESIA
                    }
                }
            } else {
                isUpdatingInterface = true; // 🛡️ LIGA ANESTESIA PARA LIMPAR A TELA ("Nova Simulação")
                try {
                    cbConvenio.setValue(null);
                    cbBanco.getItems().clear();
                    cbTabela.getItems().clear();
                    cbBanco.setValue(null);
                    cbTabela.setValue(null);
                    dispararCalculo();
                } finally {
                    isUpdatingInterface = false;
                }
            }
        });

        cbTabela.valueProperty().addListener((obs, old, novaTabela) -> {
            if (isUpdatingInterface)
                return; // Impede que a UI suje o ViewModel enquanto carrega
            viewModel.tabelaIdProperty().set(novaTabela != null ? novaTabela.getId() : null);
            dispararCalculo();
        });

        // Garante que o Banco selecionado no combo atualize o ViewModel (Para salvar no
        // banco de dados)
        cbBanco.valueProperty().bindBidirectional(viewModel.bancoProperty());

        // ==========================================================
        // MONITORES DE SINAIS VITAIS (UX Dinâmica)
        // ==========================================================

        // 1. O Título Dinâmico da Comissão (Psicologia de Vendas)
        lblTituloComissao.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    BigDecimal aprovado = viewModel.valorAprovadoProperty().get();
                    // Se o valor aprovado for maior que zero, é certeza. Senão, é estimativa.
                    if (aprovado != null && aprovado.compareTo(BigDecimal.ZERO) > 0) {
                        return "Valor da Comissão";
                    }
                    return "Comissão Estimada";
                }, viewModel.valorAprovadoProperty()));

        // 2. O Calculadora de Transparência (Total a Pagar)
        lblTotalPago.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    Integer prazo = viewModel.quantidadeParcelasProperty().get();
                    BigDecimal parcela = viewModel.valorParcelaProperty().get();

                    // Só calcula se os dois campos estiverem preenchidos e maiores que zero
                    if (prazo != null && prazo > 0 && parcela != null && parcela.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal total = parcela.multiply(new BigDecimal(prazo));
                        String formatado = FinanceiroUtils.formatarParaExibicao(total);
                        return "Total a Pagar: " + formatado;
                    }

                    return "Total a Pagar: R$ 0,00";
                }, viewModel.quantidadeParcelasProperty(), viewModel.valorParcelaProperty()));
    }

    // ========================================================================
    // O CORAÇÃO DA TRIAGEM (A Mágica dos Filtros)
    // ========================================================================

    private void configurarGatilhosDaTriagem() {
        // Só dispara triagem se o usuário mexeu. Se for o sistema carregando, ignora.
        viewModel.valorSolicitadoProperty().addListener((obs, old, val) -> {
            if (!isUpdatingInterface)
                realizarTriagem();
        });

        if (cbConvenio != null) {
            cbConvenio.valueProperty().addListener((obs, old, val) -> {
                if (!isUpdatingInterface)
                    realizarTriagem();
            });
        }

        cbBanco.valueProperty().addListener((obs, old, bancoNovo) -> {
            if (!isUpdatingInterface)
                atualizarTabelasDoBanco(bancoNovo);
        });

        viewModel.valorAprovadoProperty().addListener((obs, old, val) -> {
            if (!isUpdatingInterface)
                dispararCalculo();
        });
    }

    // 🩹 NOVO MÉTODO AUXILIAR: Isola o carregamento das tabelas
    private void atualizarTabelasDoBanco(BancoModel bancoNovo) {
        if (bancoNovo != null && tabelasElegiveisDaTriagem != null) {
            List<TabelaJurosModel> tabelasDoBanco = tabelasElegiveisDaTriagem.stream()
                    .filter(t -> t.getBanco().getId().equals(bancoNovo.getId()))
                    .sorted(java.util.Comparator.comparing(TabelaJurosModel::getComissaoPercentual).reversed())
                    .toList();

            TabelaJurosModel tabelaAtual = cbTabela.getValue();
            cbTabela.setItems(FXCollections.observableArrayList(tabelasDoBanco));

            // Piloto Automático só funciona se o paciente estiver acordado (não
            // anestesiado)
            if (!isUpdatingInterface) {
                if (tabelasDoBanco.size() == 1) {
                    cbTabela.setValue(tabelasDoBanco.get(0));
                } else if (tabelaAtual != null && tabelasDoBanco.contains(tabelaAtual)) {
                    cbTabela.setValue(tabelaAtual);
                } else {
                    cbTabela.setValue(null);
                }
            }
        } else {
            cbTabela.getItems().clear();
            if (!isUpdatingInterface)
                cbTabela.setValue(null);
        }
    }

    private void realizarTriagem() {
        TipoConvenioModel convenio = (cbConvenio != null) ? cbConvenio.getValue() : null;
        BigDecimal valorSolicitado = viewModel.valorSolicitadoProperty().get();

        if (convenio == null || valorSolicitado == null || valorSolicitado.compareTo(BigDecimal.ZERO) <= 0) {
            cbBanco.getItems().clear();
            cbTabela.getItems().clear();
            tabelasElegiveisDaTriagem = null;
            return;
        }

        // 1. Filtra as Tabelas Elegíveis (Poder do Stream Nativo)
        tabelasElegiveisDaTriagem = todasTabelasAtivas.stream()
                .filter(t -> t.getTipoConvenio() == convenio)
                .filter(t -> {
                    BigDecimal min = t.getValorMinimoEmprestimo() != null ? t.getValorMinimoEmprestimo()
                            : BigDecimal.ZERO;
                    return valorSolicitado.compareTo(min) >= 0;
                })
                .filter(t -> t.getValorMaximoEmprestimo() == null
                        || t.getValorMaximoEmprestimo().compareTo(BigDecimal.ZERO) <= 0
                        || valorSolicitado.compareTo(t.getValorMaximoEmprestimo()) <= 0)
                .toList();

        // 2. Extrai os Bancos (Sem HashMap! Agora o Hibernate confia no distinct do
        // Java)
        List<BancoModel> bancosElegiveis = tabelasElegiveisDaTriagem.stream()
                .map(TabelaJurosModel::getBanco)
                .filter(java.util.Objects::nonNull) // Evita nulos caso alguma tabela esteja sem banco
                .distinct() // O Java usa o equals() nativo da entidade perfeitamente agora!
                .toList();

        // 3. Atualiza o ComboBox de Bancos mantendo a seleção se for válida
        BancoModel bancoAtual = cbBanco.getValue();
        cbBanco.setItems(FXCollections.observableArrayList(bancosElegiveis));

        // 4. 🧠 Piloto Automático SOMENTE se não estiver carregando uma proposta salva
        if (!isUpdatingInterface) {
            if (bancosElegiveis.size() == 1) {
                cbBanco.setValue(bancosElegiveis.get(0));
            } else if (bancoAtual != null && bancosElegiveis.contains(bancoAtual)) {
                cbBanco.setValue(bancoAtual);
            } else {
                cbBanco.setValue(null);
            }
            dispararCalculo(); // Recalcula apenas se foi ação manual
        }
    }

    /**
     * Calcula a Comissão e mostra o R$ e a % na UI.
     * Prioriza o Valor Liberado (Aprovado), mas usa o Solicitado se o Aprovado
     * estiver vazio.
     */
    private void dispararCalculo() {
        BigDecimal vAprovado = viewModel.valorAprovadoProperty().get();
        BigDecimal vSolicitado = viewModel.valorSolicitadoProperty().get();
        TabelaJurosModel tabela = cbTabela.getValue();

        // Qual valor usar para calcular? Se tem liberado, usa liberado. Senão, usa
        // solicitado.
        BigDecimal valorBase = (vAprovado != null && vAprovado.compareTo(BigDecimal.ZERO) > 0) ? vAprovado
                : vSolicitado;

        if (valorBase != null && valorBase.compareTo(BigDecimal.ZERO) > 0 && tabela != null) {
            // Calcula no Backend/Service
            BigDecimal comissaoCalculada = propostaService.calcularComissaoEstimada(valorBase, tabela.getId());
            viewModel.comissaoEstimadaProperty().set(comissaoCalculada);

            // Atualiza a UI (Valor + Porcentagem)
            String formatado = FinanceiroUtils.formatarParaExibicao(comissaoCalculada);
            lblComissaoEstimada.setText(String.format("%s (%s%%)", formatado, tabela.getComissaoPercentual()));
            lblComissaoEstimada.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); // Dá um destaque clínico
        } else {
            viewModel.comissaoEstimadaProperty().set(BigDecimal.ZERO);
            lblComissaoEstimada.setText("R$ 0,00 (0%)");
            lblComissaoEstimada.setStyle("-fx-text-fill: -color-fg-default;");
        }
    }

    private void configurarAutoSelecao() {
        TextField[] camposFinanceiros = { txtValorSolicitado, txtValorAprovado, txtParcela };
        for (TextField campo : camposFinanceiros) {
            campo.focusedProperty().addListener((obs, estavaFocado, agoraFocado) -> {
                if (agoraFocado) {
                    javafx.application.Platform.runLater(campo::selectAll);
                }
            });
        }
    }

    private void carregarDocumentosDaProposta(Long propostaId) {
        if (propostaId == null) {
            listaDocumentos.clear();
            return;
        }

        Task<List<DocumentoProponenteModel>> task = new Task<>() {
            @Override
            protected List<DocumentoProponenteModel> call() throws Exception {
                return documentoService.buscarPorProposta(propostaId);
            }
        };

        task.setOnSucceeded(e -> {
            // Atualiza a lista observada com os novos dados do banco
            listaDocumentos.setAll(task.getValue());
            tableDocumentos.refresh(); // Garante que a UI redesenhe as linhas
        });

        new Thread(task).start();
    }

    @FXML
    private void handleAnexarDocumento() {
        // 1. Verificação de Pré-requisitos
        if (viewModel.idProperty().get() == null) {
            System.out.println("Erro: Salve a proposta antes de anexar documentos.");
            // Opcional: Mostrar um alerta para a Solange aqui
            return;
        }

        // 2. Abertura do Seletor de Arquivos
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Documento da Proposta");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

        // Usa o rootPane como dono da janela (Window Owner)
        File file = fileChooser.showOpenDialog(cbBanco.getScene().getWindow());

        if (file != null) {
            realizarUploadAssincrono(file);
        }
    }

    private void realizarUploadAssincrono(File arquivo) {
        // 3. Preparação do Procedimento (Task)
        Task<DocumentoProponenteModel> uploadTask = new Task<>() {
            @Override
            protected DocumentoProponenteModel call() throws Exception {
                // Sincroniza o estado da tela com o modelo
                PropostaModel propostaAtual = viewModel.atualizarModel(new PropostaModel());

                // 🩹 A CURA: Acessamos o proponente através do objeto da proposta
                return documentoService.processarUpload(
                        arquivo,
                        "DOCUMENTO_PROPOSTA",
                        propostaAtual.getProponente(),
                        propostaAtual);
            }
        };

        // 4. Pós-Operatório (Sucesso)
        uploadTask.setOnSucceeded(e -> {
            Long idAtual = viewModel.idProperty().get();
            carregarDocumentosDaProposta(idAtual);
        });

        // 5. Tratamento de Rejeição (Erro)
        uploadTask.setOnFailed(e -> {
            Throwable erro = uploadTask.getException();
            System.err.println("Falha no upload: " + erro.getMessage());
            // Aqui você pode usar o Notifications do ControlsFX para avisar a Solange
        });

        // Dispara a Thread sem travar a UI
        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }

    private void configurarColunasDocumentos() {
        // 1. Resolve o "Nome Genérico": Mapeia a coluna para o tipo do documento
        colTipoDocumento.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTipoDocumento()));

        // 2. Resolve a "Coluna Vazia": Cria os botões de ação (👁️ e 🗑️)
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️");
            private final Button btnExcluir = new Button("🗑️");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8, btnAbrir, btnExcluir);

            {
                btnAbrir.getStyleClass().add("flat");
                btnExcluir.getStyleClass().addAll("flat", "danger");
                container.setAlignment(javafx.geometry.Pos.CENTER);

                // Ação de Abrir
                btnAbrir.setOnAction(event -> {
                    DocumentoProponenteModel doc = getTableView().getItems().get(getIndex());
                    documentoService.abrirDocumento(doc);
                });

                // Ação de Excluir
                btnExcluir.setOnAction(event -> {
                    DocumentoProponenteModel doc = getTableView().getItems().get(getIndex());
                    confirmarExclusaoDocumento(doc);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        // 3. Clique duplo na linha também abre o documento
        tableDocumentos.setRowFactory(tv -> {
            TableRow<DocumentoProponenteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    documentoService.abrirDocumento(row.getItem());
                }
            });
            return row;
        });
    }

    private void confirmarExclusaoDocumento(DocumentoProponenteModel doc) {
        // Aqui você pode usar um Alert do JavaFX para confirmar
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Excluir Documento");
        alert.setHeaderText("Deseja remover este documento do prontuário?");
        alert.setContentText(doc.getTipoDocumento());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                documentoService.excluirDocumento(doc.getId());
                // Atualiza a tabela após a exclusão
                listaDocumentos.remove(doc);
                System.out.println("Documento removido com sucesso.");
            }
        });
    }

    public PropostaViewModel getViewModel() {
        return viewModel;
    }
}