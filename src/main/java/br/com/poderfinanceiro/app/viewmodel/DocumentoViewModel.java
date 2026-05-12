package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@Scope("prototype")
public class DocumentoViewModel extends BaseViewModel<DocumentoProponenteModel> {

    private final StringProperty tipoDocumento = new SimpleStringProperty("");
    private final StringProperty arquivoPath = new SimpleStringProperty("");
    private final BooleanProperty verificado = new SimpleBooleanProperty(false);

    // Originais para Dirty Checking
    private final ReadOnlyStringWrapper tipoOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper verificadoOriginal = new ReadOnlyBooleanWrapper(false);

    @Override
    protected void extrairId(DocumentoProponenteModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(DocumentoProponenteModel model) {
        this.tipoDocumento.set(model.getTipoDocumento());
        this.arquivoPath.set(model.getArquivoPath());
        this.verificado.set(model.getVerificado());
    }

    @Override
    protected void limparCampos() {
        tipoDocumento.set("");
        arquivoPath.set("");
        verificado.set(false);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        tipoOriginal.set(tipoDocumento.get());
        verificadoOriginal.set(verificado.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(tipoDocumento.get(), tipoOriginal.get()) ||
                !Objects.equals(verificado.get(), verificadoOriginal.get());
    }

    @Override
    public DocumentoProponenteModel atualizarModel(DocumentoProponenteModel model) {
        if (model == null)
            model = new DocumentoProponenteModel();
        model.setId(this.id.get());
        model.setTipoDocumento(this.tipoDocumento.get());
        model.setVerificado(this.verificado.get());
        // O path e o usuário geralmente não mudam após o upload
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] { tipoDocumento, verificado, tipoOriginal, verificadoOriginal };
    }

    // Getters
    public StringProperty tipoDocumentoProperty() {
        return tipoDocumento;
    }

    public StringProperty arquivoPathProperty() {
        return arquivoPath;
    }

    public BooleanProperty verificadoProperty() {
        return verificado;
    }
}