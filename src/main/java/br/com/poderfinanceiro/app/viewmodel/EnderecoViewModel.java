package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.model.enums.UfModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@Scope("prototype")
public class EnderecoViewModel extends BaseViewModel<EnderecoProponenteModel> {

    // --- PROPERTIES EXCLUSIVAS DE ENDEREÇO ---
    private final StringProperty cep = new SimpleStringProperty("");
    private final ObjectProperty<TipoLogradouroModel> tipoLogradouro = new SimpleObjectProperty<>(TipoLogradouroModel.RUA);
    private final StringProperty logradouro = new SimpleStringProperty("");
    private final StringProperty numero = new SimpleStringProperty("");
    private final StringProperty complemento = new SimpleStringProperty("");
    private final StringProperty bairro = new SimpleStringProperty("");
    private final StringProperty cidade = new SimpleStringProperty("");
    private final ObjectProperty<UfModel> uf = new SimpleObjectProperty<>(UfModel.SP);

    // --- ESTADOS ORIGINAIS ---
    private final ReadOnlyStringWrapper cepOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<TipoLogradouroModel> tipoLogradouroOriginal = new ReadOnlyObjectWrapper<>(
            TipoLogradouroModel.RUA);
    private final ReadOnlyStringWrapper logradouroOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper numeroOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper complementoOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper bairroOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper cidadeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<UfModel> ufOriginal = new ReadOnlyObjectWrapper<>(UfModel.SP);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(EnderecoProponenteModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(EnderecoProponenteModel model) {
        this.cep.set(model.getCep() != null ? model.getCep() : "");
        this.tipoLogradouro.set(model.getTipoLogradouro() != null ? model.getTipoLogradouro() : TipoLogradouroModel.RUA);
        this.logradouro.set(model.getLogradouro() != null ? model.getLogradouro() : "");
        this.numero.set(model.getNumero() != null ? model.getNumero() : "");
        this.complemento.set(model.getComplemento() != null ? model.getComplemento() : "");
        this.bairro.set(model.getBairro() != null ? model.getBairro() : "");
        this.cidade.set(model.getCidade() != null ? model.getCidade() : "");
        this.uf.set(model.getUf() != null ? model.getUf() : UfModel.SP);
    }

    @Override
    protected void limparCampos() {
        cep.set("");
        tipoLogradouro.set(TipoLogradouroModel.RUA);
        logradouro.set("");
        numero.set("");
        complemento.set("");
        bairro.set("");
        cidade.set("");
        uf.set(UfModel.SP);
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        this.cepOriginal.set(cep.get());
        this.tipoLogradouroOriginal.set(tipoLogradouro.get());
        this.logradouroOriginal.set(logradouro.get());
        this.numeroOriginal.set(numero.get());
        this.complementoOriginal.set(complemento.get());
        this.bairroOriginal.set(bairro.get());
        this.cidadeOriginal.set(cidade.get());
        this.ufOriginal.set(uf.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(cep.get(), cepOriginal.get()) ||
                !Objects.equals(tipoLogradouro.get(), tipoLogradouroOriginal.get()) ||
                !Objects.equals(logradouro.get(), logradouroOriginal.get()) ||
                !Objects.equals(numero.get(), numeroOriginal.get()) ||
                !Objects.equals(complemento.get(), complementoOriginal.get()) ||
                !Objects.equals(bairro.get(), bairroOriginal.get()) ||
                !Objects.equals(cidade.get(), cidadeOriginal.get()) ||
                !Objects.equals(uf.get(), ufOriginal.get());
    }

    @Override
    public EnderecoProponenteModel atualizarModel(EnderecoProponenteModel model) {
        if (model == null)
            model = new EnderecoProponenteModel();

        model.setId(this.id.get());
        model.setCep(this.cep.get());
        model.setTipoLogradouro(this.tipoLogradouro.get());
        model.setLogradouro(this.logradouro.get());
        model.setNumero(this.numero.get());
        model.setComplemento(this.complemento.get());
        model.setBairro(this.bairro.get());
        model.setCidade(this.cidade.get());
        model.setUf(this.uf.get());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                cep, tipoLogradouro, logradouro, numero, complemento, bairro, cidade, uf,
                cepOriginal.getReadOnlyProperty(), tipoLogradouroOriginal.getReadOnlyProperty(),
                logradouroOriginal.getReadOnlyProperty(), numeroOriginal.getReadOnlyProperty(),
                complementoOriginal.getReadOnlyProperty(), bairroOriginal.getReadOnlyProperty(),
                cidadeOriginal.getReadOnlyProperty(), ufOriginal.getReadOnlyProperty()
        };
    }

    // --- GETTERS DAS PROPERTIES (Somente das específicas desta classe agora) ---
    public StringProperty cepProperty() {
        return cep;
    }

    public ObjectProperty<TipoLogradouroModel> tipoLogradouroProperty() {
        return tipoLogradouro;
    }

    public StringProperty logradouroProperty() {
        return logradouro;
    }

    public StringProperty numeroProperty() {
        return numero;
    }

    public StringProperty complementoProperty() {
        return complemento;
    }

    public StringProperty bairroProperty() {
        return bairro;
    }

    public StringProperty cidadeProperty() {
        return cidade;
    }

    public ObjectProperty<UfModel> ufProperty() {
        return uf;
    }
}