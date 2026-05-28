package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import br.com.poderfinanceiro.app.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.facade.IEnderecoFacade;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import br.com.poderfinanceiro.app.util.EnderecoUtils;
import br.com.poderfinanceiro.app.viewmodel.EnderecoViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * <h1>EnderecoController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pelo formulário de endereço do
 * cliente. Atua como um <b>Humble Object</b>, gerenciando bindings e delegando
 * a busca de CEP para a {@link IEnderecoFacade}. A persistência é orquestrada
 * pelo AtendimentoHubController.
 * </p>
 */
@Component @Scope("prototype")
public class EnderecoController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(EnderecoController.class);
    private static final String LOG_PREFIX = "[EnderecoController]";

    private static final int TAMANHO_CEP_VALIDO = 8;
    private static final String REGEX_SOMENTE_NUMEROS = "\\D";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final EnderecoViewModel viewModel;
    private final IEnderecoFacade enderecoFacade;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private TextField txtCep;
    @FXML private ComboBox<TipoLogradouroModel> comboTipoLogradouro;
    @FXML private TextField txtLogradouro;
    @FXML private TextField txtNumero;
    @FXML private TextField txtComplemento;
    @FXML private TextField txtBairro;
    @FXML private TextField txtCidade;
    @FXML private ComboBox<UfModel> comboUf;

    public EnderecoController(EnderecoViewModel viewModel, IEnderecoFacade enderecoFacade) {
        this.viewModel = viewModel;
        this.enderecoFacade = enderecoFacade;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring (Prototype).", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando formulário de endereço...", LOG_PREFIX);
        configurarInterface();
        estabelecerBindings();
        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    public void carregarEndereco(EnderecoProponenteModel endereco) {
        log.info("{} [TELEMETRIA] Carregando endereço no formulário. Endereço presente? {}", LOG_PREFIX, endereco != null);
        if (endereco == null) {
            viewModel.reset();
            return;
        }
        viewModel.loadFromModel(endereco);
    }

    public void limparCampos() {
        log.trace("{} [UI] Resetando formulário de endereço.", LOG_PREFIX);
        viewModel.reset();
    }

    // ==========================================================================================
    // MÓDULO 5: CONFIGURAÇÃO DE UI E BINDINGS
    // ==========================================================================================
    private void configurarInterface() {
        log.trace("{} [UI] Carregando ComboBoxes e aplicando formatador de CEP.", LOG_PREFIX);
        comboTipoLogradouro.setItems(FXCollections.observableArrayList(TipoLogradouroModel.values()));
        comboUf.setItems(FXCollections.observableArrayList(UfModel.values()));
        txtCep.setTextFormatter(EnderecoUtils.criarFormatadorCep());
    }

    private void estabelecerBindings() {
        log.trace("{} [UI] Estabelecendo bindings bidirecionais com o ViewModel.", LOG_PREFIX);
        viewModel.cepProperty().bindBidirectional(txtCep.textProperty());
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
    @FXML private void buscarCep() {
        String cepDigitado = txtCep.getText();
        log.info("{} [TELEMETRIA] Usuário acionou busca de CEP: '{}'", LOG_PREFIX, cepDigitado);

        if (cepDigitado == null || cepDigitado.trim().isEmpty()) {
            log.warn("{} [NEGOCIO] Busca ignorada: CEP vazio.", LOG_PREFIX);
            return;
        }

        String apenasNumeros = cepDigitado.replaceAll(REGEX_SOMENTE_NUMEROS, "");

        if (apenasNumeros.length() == TAMANHO_CEP_VALIDO) {
            executarBuscaAssincrona(apenasNumeros);
        } else {
            log.warn("{} [NEGOCIO] Busca bloqueada: Tamanho do CEP inválido ({} dígitos).", LOG_PREFIX, apenasNumeros.length());
        }
    }

    private void executarBuscaAssincrona(String cep) {
        log.trace("{} [SISTEMA] Disparando task assíncrona para busca de CEP.", LOG_PREFIX);

        AsyncUtils.executarTaskAsync(() -> enderecoFacade.buscarEnderecoPorCep(cep), enderecoEncontrado -> {
            if (enderecoEncontrado != null) {
                log.info("{} [AUDITORIA] Endereço retornado pela API com sucesso.", LOG_PREFIX);
                preencherCamposEndereco(enderecoEncontrado);
            } else {
                log.warn("{} [NEGOCIO] CEP não encontrado na base dos Correios.", LOG_PREFIX);
            }
        }, erro -> log.error("{} [SISTEMA] Erro de comunicação com a API ViaCEP: {}", LOG_PREFIX, erro.getMessage()));
    }

    private void preencherCamposEndereco(ViaCepResponse endereco) {
        log.trace("{} [UI] Preenchendo campos com os dados da API.", LOG_PREFIX);
        txtLogradouro.setText(endereco.logradouro());
        txtBairro.setText(endereco.bairro());
        txtCidade.setText(endereco.localidade());
        selecionarUfSeguro(endereco.uf());
    }

    private void selecionarUfSeguro(String ufSigla) {
        if (ufSigla == null || ufSigla.trim().isEmpty())
            return;

        try {
            comboUf.setValue(UfModel.valueOf(ufSigla.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            log.error("{} [SISTEMA] UF retornada pela API não existe no Enum: {}", LOG_PREFIX, ufSigla);
        }
    }

    // ==========================================================================================
    // MÓDULO 7: UTILITÁRIOS
    // ==========================================================================================
    public EnderecoViewModel getViewModel() {
        return viewModel;
    }
}
