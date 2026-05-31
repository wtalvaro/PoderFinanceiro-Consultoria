package br.com.poderfinanceiro.app.presentation.controller.atendimento;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.application.facade.IEnderecoFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.common.util.EnderecoUtils;
import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import br.com.poderfinanceiro.app.presentation.viewmodel.EnderecoViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * <h1>EnderecoController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pelo formulário de endereço do
 * cliente.
 * Atua como um <b>Humble Object</b>, gerenciando bindings e delegando a busca
 * de CEP
 * para a {@link IEnderecoFacade}.
 * </p>
 */
@Component
@Scope("prototype")
public class EnderecoController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(EnderecoController.class);
    private static final String LOG_PREFIX = "[EnderecoController]";

    private static final int TAMANHO_CEP_VALIDO = 8;

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final EnderecoViewModel viewModel;
    private final IEnderecoFacade enderecoFacade;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML
    private TextField txtCep;
    @FXML
    private ComboBox<TipoLogradouroModel> comboTipoLogradouro;
    @FXML
    private TextField txtLogradouro;
    @FXML
    private TextField txtNumero;
    @FXML
    private TextField txtComplemento;
    @FXML
    private TextField txtBairro;
    @FXML
    private TextField txtCidade;
    @FXML
    private ComboBox<UfModel> comboUf;

    public EnderecoController(EnderecoViewModel viewModel, IEnderecoFacade enderecoFacade) {
        this.viewModel = viewModel;
        this.enderecoFacade = enderecoFacade;
        log.info("{} [SISTEMA] Controlador instanciado com suporte a Virtual Threads.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML
    public void initialize() {
        log.info("{} [SISTEMA] Inicializando formulário de endereço.", LOG_PREFIX);
        configurarInterface();
        estabelecerBindings();
        log.debug("{} [SISTEMA] Inicialização da UI concluída.", LOG_PREFIX);
    }

    public void carregarEndereco(EnderecoProponenteModel endereco) {
        log.info("{} [TELEMETRIA] Solicitando carga de endereço. Presente: {}", LOG_PREFIX, endereco != null);
        if (endereco == null) {
            log.debug("{} [NEGOCIO] Endereço nulo recebido. Resetando ViewModel.", LOG_PREFIX);
            viewModel.reset();
            return;
        }
        viewModel.loadFromModel(endereco);
    }

    public void limparCampos() {
        log.info("{} [SISTEMA] Resetando formulário de endereço.", LOG_PREFIX);
        viewModel.reset();
    }

    // ==========================================================================================
    // MÓDULO 5: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarInterface() {
        log.debug("{} [SISTEMA] Configurando listas de domínios de endereço.", LOG_PREFIX);
        comboTipoLogradouro.setItems(FXCollections.observableArrayList(TipoLogradouroModel.values()));
        comboUf.setItems(FXCollections.observableArrayList(UfModel.values()));
    }

    private void estabelecerBindings() {
        log.info("{} [SISTEMA] Estabelecendo bindings bidirecionais reativos.", LOG_PREFIX);

        // Configuração do CEP com segurança de tipos e sincronização de valor limpo
        TextFormatter<String> cepFormatter = EnderecoUtils.criarFormatadorCep();
        txtCep.setTextFormatter(cepFormatter);

        // VINCULAÇÃO CRÍTICA: Vinculamos a valueProperty (valor limpo) à ViewModel
        // Isso garante que o Dirty Checking funcione e o botão Salvar habilite.
        viewModel.cepProperty().bindBidirectional(cepFormatter.valueProperty());

        log.trace("{} [SISTEMA] Binding de CEP configurado via ValueProperty do Formatter.", LOG_PREFIX);

        viewModel.tipoLogradouroProperty().bindBidirectional(comboTipoLogradouro.valueProperty());
        viewModel.logradouroProperty().bindBidirectional(txtLogradouro.textProperty());
        viewModel.numeroProperty().bindBidirectional(txtNumero.textProperty());
        viewModel.complementoProperty().bindBidirectional(txtComplemento.textProperty());
        viewModel.bairroProperty().bindBidirectional(txtBairro.textProperty());
        viewModel.cidadeProperty().bindBidirectional(txtCidade.textProperty());
        viewModel.ufProperty().bindBidirectional(comboUf.valueProperty());
    }

    // ==========================================================================================
    // MÓDULO 6: INTEGRAÇÃO EXTERNA (BUSCA DE CEP)
    // ==========================================================================================
    @FXML
    private void buscarCep() {
        // Obtemos o valor da ViewModel (que já está limpo pelo binding do formatter)
        String cepSaneado = EnderecoUtils.limparCep(viewModel.cepProperty().get());
        log.info("{} [TELEMETRIA] Iniciando busca de CEP: '{}'", LOG_PREFIX, cepSaneado);

        if (cepSaneado.isEmpty()) {
            log.warn("{} [NEGOCIO] Busca abortada: CEP vazio.", LOG_PREFIX);
            return;
        }

        if (cepSaneado.length() == TAMANHO_CEP_VALIDO) {
            executarBuscaAssincrona(cepSaneado);
        } else {
            log.warn("{} [NEGOCIO] Busca bloqueada: CEP incompleto ({} dígitos).", LOG_PREFIX, cepSaneado.length());
        }
    }

    private void executarBuscaAssincrona(String cep) {
        log.debug("{} [TELEMETRIA] Disparando orquestração assíncrona para ViaCEP.", LOG_PREFIX);

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.trace("{} [NEGOCIO] Invocando Facade de endereço para o CEP: {}", LOG_PREFIX, cep);
                    return enderecoFacade.buscarEnderecoPorCep(cep);
                },
                enderecoEncontrado -> {
                    if (enderecoEncontrado != null) {
                        log.info("{} [AUDITORIA] Dados de endereço recuperados com sucesso.", LOG_PREFIX);
                        preencherCamposEndereco(enderecoEncontrado);
                    } else {
                        log.warn("{} [AUDITORIA] CEP {} não localizado na base externa.", LOG_PREFIX, cep);
                    }
                },
                erro -> log.error("{} [SISTEMA] Falha na comunicação com serviço de CEP: {}", LOG_PREFIX,
                        erro.getMessage()));
    }

    private void preencherCamposEndereco(ViaCepResponse endereco) {
        log.info("{} [NEGOCIO] Atualizando ViewModel com dados da busca externa.", LOG_PREFIX);

        // ATUALIZAÇÃO MVVM: Alteramos a ViewModel. O binding bidirecional atualizará a
        // UI.
        viewModel.logradouroProperty().set(endereco.logradouro());
        viewModel.bairroProperty().set(endereco.bairro());
        viewModel.cidadeProperty().set(endereco.localidade());
        selecionarUfSeguro(endereco.uf());
    }

    private void selecionarUfSeguro(String ufSigla) {
        if (ufSigla == null || ufSigla.trim().isEmpty())
            return;

        try {
            UfModel uf = UfModel.valueOf(ufSigla.toUpperCase());
            viewModel.ufProperty().set(uf);
            log.trace("{} [NEGOCIO] UF selecionada: {}", LOG_PREFIX, uf);
        } catch (IllegalArgumentException ex) {
            log.error("{} [SISTEMA] Sigla de UF inválida retornada pela API: {}", LOG_PREFIX, ufSigla);
        }
    }

    // ==========================================================================================
    // MÓDULO 7: UTILITÁRIOS
    // ==========================================================================================
    public EnderecoViewModel getViewModel() {
        return viewModel;
    }
}
