package br.com.poderfinanceiro.app.presentation.viewmodel;

import jakarta.annotation.PostConstruct;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe base genérica para todas as ViewModels de Entidades.
 * Implementa o padrão Template Method para gerenciar o ciclo de vida do JavaFX.
 * 
 * @param <T> A Entidade de Domínio (ex: Proponente, EnderecoProponente)
 */
public abstract class BaseViewModel<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseViewModel.class);

    // --- ESTADO GLOBAL (Herdado por todas as ViewModels) ---
    protected final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);
    protected final BooleanProperty editando = new SimpleBooleanProperty(false);
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);

    // Construtor padrão para log
    public BaseViewModel() {
        log.debug("[BASE_VM] Instância criada: {}", getClass().getSimpleName());
    }

    /**
     * O Motor do Dirty Checking.
     * O Spring chama isso e vincula automaticamente os campos que a classe filha
     * fornecer.
     */
    @PostConstruct
    public void initialize() {
        log.debug("[BASE_VM] initialize() chamado para {}", getClass().getSimpleName());
        dirty.bind(Bindings.createBooleanBinding(
                this::temAlteracoesPendentes,
                getObservaveisParaDirty()));
        log.trace("[BASE_VM] Dirty checking configurado com sucesso");
    }

    // --- CICLO DE VIDA PADRONIZADO ---

    public void loadFromModel(T model) {
        log.debug("[BASE_VM] loadFromModel: model={} para {}", model != null ? "presente" : "null",
                getClass().getSimpleName());
        if (model == null) {
            log.warn("[BASE_VM] Model nulo, resetando ViewModel");
            reset();
            return;
        }
        extrairId(model);
        preencherCampos(model);
        sincronizarEstadoOriginal();
        editando.set(true);
        log.info("[BASE_VM] ViewModel carregada com ID={}", id.get());
    }

    public void reset() {
        log.debug("[BASE_VM] reset() chamado para {}", getClass().getSimpleName());
        id.set(null);
        editando.set(false);
        limparCampos();
        sincronizarEstadoOriginal();
        log.trace("[BASE_VM] Reset concluído");
    }

    // --- PROPRIEDADES COMUNS ---
    public ObjectProperty<Long> idProperty() {
        log.trace("[BASE_VM] idProperty acessado para {}", getClass().getSimpleName());
        return id;
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        log.trace("[BASE_VM] dirtyProperty acessado para {}", getClass().getSimpleName());
        return dirty.getReadOnlyProperty();
    }

    public BooleanProperty editandoProperty() {
        log.trace("[BASE_VM] editandoProperty acessado para {}", getClass().getSimpleName());
        return editando;
    }

    public boolean isDirty() {
        boolean dirtyFlag = dirty.get();
        log.trace("[BASE_VM] isDirty = {} para {}", dirtyFlag, getClass().getSimpleName());
        return dirtyFlag;
    }

    // ========================================================================
    // CONTRATO OBRIGATÓRIO (As classes filhas DEVEM implementar isso)
    // ========================================================================

    protected abstract void extrairId(T model);

    protected abstract void preencherCampos(T model);

    protected abstract void limparCampos();

    protected abstract void sincronizarEstadoOriginal();

    protected abstract boolean temAlteracoesPendentes();

    public abstract T atualizarModel(T model);

    /**
     * @return Um array com todas as Properties (atuais e originais) que o JavaFX
     *         deve observar.
     */
    protected abstract Observable[] getObservaveisParaDirty();

    /**
     * 🚀 NOVO CONTRATO: Validação de Regras de Negócio (Gatekeeper)
     * Informa se a classe atual possui todos os dados mínimos obrigatórios
     * para permitir que o botão "Salvar" ou outras ações sejam executadas.
     * 
     * @return true se o formulário for considerado válido
     */
    public abstract boolean isValido();
}