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
import javafx.scene.layout.VBox;
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
    private final MainController mainController;

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
    @FXML
    private VBox overlayExclusao;
    @FXML
    private Label lblConfirmacaoExclusao;

    // Caches da Memória
    private List<TabelaJurosModel> todasTabelasAtivas;
    private List<TabelaJurosModel> tabelasElegiveisDaTriagem;

    // 🛡️ ANESTESIA LOCAL: Evita que a triagem destrua os dados ao trocar de
    // paciente
    private boolean isUpdatingInterface = false;
    private DocumentoProponenteModel documentoParaExcluir;

    private final ObservableList<DocumentoProponenteModel> listaDocumentos = FXCollections.observableArrayList();

    public PropostaController(PropostaViewModel viewModel, DocumentoService documentoService,
            PropostaService propostaService, TabelaJurosService tabelaJurosService, MainController mainController) {
        this.viewModel = viewModel;
        this.documentoService = documentoService;
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
        this.mainController = mainController;
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

                    // 🩹 A CURA: Usamos o Platform.runLater para garantir que o
                    // JavaFX já processou os valores antes de checar o bloqueio.
                    javafx.application.Platform.runLater(() -> {
                        aplicarBloqueioSePago();
                    });
                    
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

        // 1. Lista de opções para a Solange
        List<String> opcoes = List.of("RG", "CPF", "CNH", "Contracheque", "Comprovante de Residência",
                "Extrato Bancário", "Outros");

        // 2. Abre o diálogo de escolha
        ChoiceDialog<String> dialog = new ChoiceDialog<>("RG", opcoes);
        dialog.setTitle("Classificação de Documento");
        dialog.setHeaderText("Qual documento você está anexando?");
        dialog.setContentText("Tipo:");

        dialog.showAndWait().ifPresent(tipoSelecionado -> {
            // 3. Se ela escolheu, abrimos o seletor de arquivos
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecionar Arquivo: " + tipoSelecionado);
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Arquivos", "*.pdf", "*.jpg", "*.png", "*.jpeg"));

            File file = fileChooser.showOpenDialog(tableDocumentos.getScene().getWindow());

            if (file != null) {
                // 🚀 Passamos o tipo selecionado para o método de upload
                realizarUploadAssincrono(file, tipoSelecionado);
            }
        });
    }

    // 🩹 Alterada a assinatura para receber o tipo
    private void realizarUploadAssincrono(File arquivo, String tipoDoc) {
        Task<DocumentoProponenteModel> uploadTask = new Task<>() {
            @Override
            protected DocumentoProponenteModel call() throws Exception {
                PropostaModel propostaAtual = viewModel.atualizarModel(new PropostaModel());

                // 💉 Agora usamos a variável 'tipoDoc' que veio do diálogo!
                return documentoService.processarUpload(
                        arquivo,
                        tipoDoc,
                        propostaAtual.getProponente(),
                        propostaAtual);
            }
        };

        uploadTask.setOnSucceeded(e -> {
            Long idAtual = viewModel.idProperty().get();
            carregarDocumentosDaProposta(idAtual);
        });

        uploadTask.setOnFailed(e -> {
            System.err.println("Falha no upload: " + uploadTask.getException().getMessage());
        });

        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }

    private void configurarColunasDocumentos() {
        // 1. Resolve o "Nome Genérico": Mapeia a coluna para o tipo do documento
        colTipoDocumento.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTipoDocumento()));

        // 🩹 ADICIONE ISSO: Para a data não aparecer vazia
        colDataUpload.setCellValueFactory(cellData -> {
            var data = cellData.getValue().getDataUpload();
            return new javafx.beans.property.SimpleStringProperty(
                    data != null ? data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-");
        });

        // 2. Resolve a "Coluna Vazia": Cria os botões de ação (👁️ e 🗑️)
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️");
            private final Button btnExcluir = new Button("🗑️");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8, btnAbrir, btnExcluir);

            {
                btnAbrir.getStyleClass().add("flat");
                btnExcluir.getStyleClass().addAll("flat", "danger");
                container.setAlignment(javafx.geometry.Pos.CENTER);

                // 🩹 A CURA PARA OS "...": Força o botão a não encolher
                btnAbrir.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                btnExcluir.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                // Impede que o texto (emoji) sofra overrun
                btnAbrir.setEllipsisString("");
                btnExcluir.setEllipsisString("");

                btnAbrir.setOnAction(event -> {
                    DocumentoProponenteModel doc = getTableView().getItems().get(getIndex());
                    if (doc != null && doc.getArquivoPath() != null) {
                        File file = new File(doc.getArquivoPath());

                        if (file.exists()) {
                            // 🩹 A CURA: Chamada via HostServices do MainController
                            // Convertemos o File para URI (file:/home/...) e depois para String
                            String uri = file.toURI().toString();
                            mainController.getHostServices().showDocument(uri);
                        } else {
                            System.err.println("Arquivo físico não encontrado: " + doc.getArquivoPath());
                        }
                    }
                });

                btnExcluir.setOnAction(event -> {
                    // 🩹 Captura o item da linha e abre o overlay
                    documentoParaExcluir = getTableView().getItems().get(getIndex());
                    lblConfirmacaoExclusao.setText("Deseja remover '" + documentoParaExcluir.getTipoDocumento() + "'?");
                    overlayExclusao.setVisible(true);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                    // Garante que a célula não tente mostrar texto, apenas o gráfico
                    setText(null);
                }
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

    @FXML
    private void cancelarExclusao() {
        this.documentoParaExcluir = null;
        overlayExclusao.setVisible(false);
    }

    @FXML
    private void confirmarExclusao() {
        if (documentoParaExcluir != null) {
            // Disparamos a exclusão em Background para não travar a tela
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    documentoService.excluirDocumento(documentoParaExcluir.getId());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                // Remove da lista observada (a tabela atualiza sozinha)
                listaDocumentos.remove(documentoParaExcluir);
                cancelarExclusao();
                System.out.println("Documento removido com sucesso.");
            });

            task.setOnFailed(e -> {
                System.err.println("Erro ao excluir: " + task.getException().getMessage());
                // Aqui você pode mudar a cor do lblConfirmacaoExclusao para vermelho indicando
                // o erro
            });

            new Thread(task).start();
        }
    }

    /**
     * 🔒 TRAVA DE PERSISTÊNCIA:
     * Bloqueia a edição se a proposta já estiver liquidada no banco de dados.
     */
    public void aplicarBloqueioSePago() {
        // A regra: Se tem ID (já existe no banco) E o status é PAGO, bloqueia.
        boolean jaEstaPagoNoBanco = viewModel.idProperty().get() != null
                && viewModel.statusProperty().get() == StatusPropostaModel.PAGO;

        cbStatus.setDisable(jaEstaPagoNoBanco);
        cbTabela.setDisable(jaEstaPagoNoBanco);
        cbBanco.setDisable(jaEstaPagoNoBanco);
        cbConvenio.setDisable(jaEstaPagoNoBanco);
        txtValorAprovado.setDisable(jaEstaPagoNoBanco);
        txtParcela.setDisable(jaEstaPagoNoBanco);
        spinPrazo.setDisable(jaEstaPagoNoBanco);

        if (jaEstaPagoNoBanco) {
            txtObservacoes.setPromptText("Proposta liquidada. Alterações financeiras não permitidas.");
        } else {
            txtObservacoes.setPromptText("");
        }
    }

    public PropostaViewModel getViewModel() {
        return viewModel;
    }
}