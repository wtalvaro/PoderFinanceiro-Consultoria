package br.com.poderfinanceiro.app.viewmodel;

import jakarta.annotation.PostConstruct;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

/**
 * Classe base genérica para todas as ViewModels de Entidades.
 * Implementa o padrão Template Method para gerenciar o ciclo de vida do JavaFX.
 * 
 * @param <T> A Entidade de Domínio (ex: Proponente, EnderecoProponente)
 */
public abstract class BaseViewModel<T> {

    // --- ESTADO GLOBAL (Herdado por todas as ViewModels) ---
    protected final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);
    protected final BooleanProperty editando = new SimpleBooleanProperty(false);
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);

    /**
     * O Motor do Dirty Checking.
     * O Spring chama isso e vincula automaticamente os campos que a classe filha
     * fornecer.
     */
    @PostConstruct
    public void initialize() {
        dirty.bind(Bindings.createBooleanBinding(
                this::temAlteracoesPendentes,
                getObservaveisParaDirty() // <-- Chama o método da classe filha!
        ));
    }

    // --- CICLO DE VIDA PADRONIZADO ---

    public void loadFromModel(T model) {
        if (model == null) {
            reset();
            return;
        }
        extrairId(model);
        preencherCampos(model);
        sincronizarEstadoOriginal();
        editando.set(true);
    }

    public void reset() {
        id.set(null);
        editando.set(false);
        limparCampos();
        sincronizarEstadoOriginal();
    }

    // --- PROPRIEDADES COMUNS ---
    public ObjectProperty<Long> idProperty() {
        return id;
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty.getReadOnlyProperty();
    }

    public BooleanProperty editandoProperty() {
        return editando;
    }

    public boolean isDirty() {
        return dirty.get();
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