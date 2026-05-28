package br.com.poderfinanceiro.app.presentation.viewmodel;

import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;

import java.util.Objects;

@Component
@Scope("prototype")
public class DocumentoViewModel extends BaseViewModel<DocumentoProponenteModel> {

    private static final Logger log = LoggerFactory.getLogger(DocumentoViewModel.class);

    private final StringProperty tipoDocumento = new SimpleStringProperty("");
    private final StringProperty arquivoPath = new SimpleStringProperty("");
    private final BooleanProperty verificado = new SimpleBooleanProperty(false);

    // Originais para Dirty Checking
    private final ReadOnlyStringWrapper tipoOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper verificadoOriginal = new ReadOnlyBooleanWrapper(false);

    // Construtor adicionado para log (não quebra contrato, pois era implícito)
    public DocumentoViewModel() {
        log.debug("[DOCUMENTO_VM] Instância criada (prototype)");
    }

    @Override
    protected void extrairId(DocumentoProponenteModel model) {
        log.debug("[DOCUMENTO_VM] extrairId: model ID={}", model != null ? model.getId() : "null");
        this.id.set(model != null ? model.getId() : null);
    }

    @Override
    protected void preencherCampos(DocumentoProponenteModel model) {
        log.debug("[DOCUMENTO_VM] preencherCampos: populando campos a partir do model ID={}",
                model != null ? model.getId() : "null");
        if (model == null) {
            log.warn("[DOCUMENTO_VM] preencherCampos chamado com model nulo, campos serão limpos");
            limparCampos();
            return;
        }
        this.tipoDocumento.set(model.getTipoDocumento());
        this.arquivoPath.set(model.getArquivoPath());
        this.verificado.set(model.getVerificado());
        log.trace("[DOCUMENTO_VM] Campos preenchidos: tipo='{}', verificado={}", tipoDocumento.get(), verificado.get());
    }

    @Override
    protected void limparCampos() {
        log.debug("[DOCUMENTO_VM] limparCampos: resetando todos os campos");
        tipoDocumento.set("");
        arquivoPath.set("");
        verificado.set(false);
        log.trace("[DOCUMENTO_VM] Campos limpos");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        log.debug("[DOCUMENTO_VM] sincronizarEstadoOriginal: salvando estado atual como original");
        tipoOriginal.set(tipoDocumento.get());
        verificadoOriginal.set(verificado.get());
        log.trace("[DOCUMENTO_VM] Estado original salvo: tipo='{}', verificado={}", tipoOriginal.get(),
                verificadoOriginal.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        boolean alterado = !Objects.equals(tipoDocumento.get(), tipoOriginal.get()) ||
                !Objects.equals(verificado.get(), verificadoOriginal.get());
        log.trace("[DOCUMENTO_VM] temAlteracoesPendentes: {}", alterado);
        return alterado;
    }

    @Override
    public DocumentoProponenteModel atualizarModel(DocumentoProponenteModel model) {
        log.debug("[DOCUMENTO_VM] atualizarModel: model fornecido = {}", model != null ? "presente" : "null");
        if (model == null) {
            log.trace("[DOCUMENTO_VM] Criando novo model (null -> novo)");
            model = new DocumentoProponenteModel();
        }
        model.setId(this.id.get());
        model.setTipoDocumento(this.tipoDocumento.get());
        model.setVerificado(this.verificado.get());
        // O path e o usuário geralmente não mudam após o upload
        log.info("[DOCUMENTO_VM] Model atualizado: ID={}, tipo='{}', verificado={}", model.getId(),
                model.getTipoDocumento(), model.getVerificado());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        Observable[] observaveis = new Observable[] { tipoDocumento, verificado, tipoOriginal, verificadoOriginal };
        log.trace("[DOCUMENTO_VM] getObservaveisParaDirty: {} observáveis registrados", observaveis.length);
        return observaveis;
    }

    @Override
    public boolean isValido() {
        log.trace("[DOCUMENTO_VM] isValido: sempre verdadeiro (sem regras específicas)");
        return true;
    }

    // Getters
    public StringProperty tipoDocumentoProperty() {
        log.trace("[DOCUMENTO_VM] tipoDocumentoProperty acessado");
        return tipoDocumento;
    }

    public StringProperty arquivoPathProperty() {
        log.trace("[DOCUMENTO_VM] arquivoPathProperty acessado");
        return arquivoPath;
    }

    public BooleanProperty verificadoProperty() {
        log.trace("[DOCUMENTO_VM] verificadoProperty acessado");
        return verificado;
    }
}