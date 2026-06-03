package br.com.poderfinanceiro.app.presentation.ui.state;

import br.com.poderfinanceiro.app.application.facade.IAjudaChatFacade;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h1>IAModelRegistry</h1>
 * <p>
 * Registro centralizado e curado de modelos de IA (Single Source of Truth).
 * Filtra a lista bruta da API do Google para exibir apenas os modelos
 * homologados
 * para as operações de crédito, OCR e análise do ERP.
 * </p>
 */
@Component
public class IAModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(IAModelRegistry.class);
    private static final String LOG_PREFIX = "[IAModelRegistry]";

    // Modelos Homologados para o ERP (Baseado na lista real do usuário)
    private static final String MODELO_PADRAO = "gemini-3.5-flash";
    private static final String MODELO_ANALISTA_PRO = "gemini-3.1-pro-preview";
    private static final String MODELO_OCR_ESTAVEL = "gemini-2.5-pro";
    private static final String MODELO_LITE = "gemini-3.1-flash-lite";

    private static final Set<String> MODELOS_PERMITIDOS = Set.of(
            MODELO_PADRAO,
            MODELO_ANALISTA_PRO,
            MODELO_OCR_ESTAVEL,
            MODELO_LITE,
            "gemini-2.5-flash",
            "gemini-2.0-flash");

    private final ObservableList<String> modelosDisponiveis = FXCollections.observableArrayList(MODELO_PADRAO);
    private final IAjudaChatFacade chatFacade;
    private boolean carregado = false;

    public IAModelRegistry(IAjudaChatFacade chatFacade) {
        this.chatFacade = chatFacade;
        log.info("{} [SISTEMA] Registro de modelos IA inicializado com curadoria Gold Standard.", LOG_PREFIX);
    }

    /**
     * Retorna a lista observável de modelos para binding na UI.
     */
    public ObservableList<String> getModelosDisponiveis() {
        return modelosDisponiveis;
    }

    /**
     * Orquestra a carga assíncrona e realiza a filtragem por homologação.
     */
    public void carregarModelos() {
        if (carregado)
            return;

        log.info("{} [TELEMETRIA] Iniciando sincronização de modelos homologados.", LOG_PREFIX);

        AsyncUtils.executarTaskAsync(
                chatFacade::listarModelosIADisponiveis,
                modelosBrutos -> {
                    if (modelosBrutos != null && !modelosBrutos.isEmpty()) {
                        // Filtra apenas os modelos que nossa arquitetura suporta e que estão na lista
                        // do usuário
                        List<String> filtrados = modelosBrutos.stream()
                                .filter(MODELOS_PERMITIDOS::contains)
                                .collect(Collectors.toList());

                        Platform.runLater(() -> {
                            if (!filtrados.isEmpty()) {
                                modelosDisponiveis.setAll(filtrados);
                                log.info("{} [AUDITORIA] {} modelos homologados registrados.", LOG_PREFIX,
                                        filtrados.size());
                            } else {
                                log.warn(
                                        "{} [NEGOCIO] Nenhum modelo homologado encontrado na API. Usando lista de segurança.",
                                        LOG_PREFIX);
                                modelosDisponiveis.setAll(List.copyOf(MODELOS_PERMITIDOS));
                            }
                            carregado = true;
                        });
                    } else {
                        log.warn("{} [NEGOCIO] Falha na comunicação com a API. Mantendo modelo padrão.", LOG_PREFIX);
                    }
                },
                erro -> log.error("{} [SISTEMA] Erro crítico na carga de modelos: {}", LOG_PREFIX, erro.getMessage()));
    }

    /**
     * Invalida o estado para permitir nova carga (ex: troca de API Key).
     */
    public void forçarAtualizacao() {
        log.info("{} [SISTEMA] Reiniciando registro global de modelos.", LOG_PREFIX);
        this.carregado = false;
        carregarModelos();
    }

    // --- Getters de Especialidade (Para uso interno nas Facades/Controllers) ---

    public String getModeloPadrao() {
        return MODELO_PADRAO;
    }

    public String getModeloParaOCR() {
        return MODELO_OCR_ESTAVEL;
    }

    public String getModeloParaAnaliseComplexa() {
        return MODELO_ANALISTA_PRO;
    }

    public String getModeloParaTarefasLeves() {
        return MODELO_LITE;
    }
}
