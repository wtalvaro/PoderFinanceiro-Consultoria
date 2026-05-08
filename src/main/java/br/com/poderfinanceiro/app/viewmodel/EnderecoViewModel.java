package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.EnderecoProponente;
import br.com.poderfinanceiro.app.model.TipoLogradouro;
import br.com.poderfinanceiro.app.model.Uf;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@Scope("prototype")
public class EnderecoViewModel extends BaseViewModel<EnderecoProponente> {

    // --- PROPERTIES EXCLUSIVAS DE ENDEREÇO ---
    private final StringProperty cep = new SimpleStringProperty("");
    private final ObjectProperty<TipoLogradouro> tipoLogradouro = new SimpleObjectProperty<>(TipoLogradouro.RUA);
    private final StringProperty logradouro = new SimpleStringProperty("");
    private final StringProperty numero = new SimpleStringProperty("");
    private final StringProperty complemento = new SimpleStringProperty("");
    private final StringProperty bairro = new SimpleStringProperty("");
    private final StringProperty cidade = new SimpleStringProperty("");
    private final ObjectProperty<Uf> uf = new SimpleObjectProperty<>(Uf.RJ);

    // --- ESTADOS ORIGINAIS ---
    private final ReadOnlyStringWrapper cepOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<TipoLogradouro> tipoLogradouroOriginal = new ReadOnlyObjectWrapper<>(
            TipoLogradouro.RUA);
    private final ReadOnlyStringWrapper logradouroOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper numeroOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper complementoOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper bairroOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper cidadeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Uf> ufOriginal = new ReadOnlyObjectWrapper<>(Uf.RJ);

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(EnderecoProponente model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(EnderecoProponente model) {
        this.cep.set(model.getCep() != null ? model.getCep() : "");
        this.tipoLogradouro.set(model.getTipoLogradouro() != null ? model.getTipoLogradouro() : TipoLogradouro.RUA);
        this.logradouro.set(model.getLogradouro() != null ? model.getLogradouro() : "");
        this.numero.set(model.getNumero() != null ? model.getNumero() : "");
        this.complemento.set(model.getComplemento() != null ? model.getComplemento() : "");
        this.bairro.set(model.getBairro() != null ? model.getBairro() : "");
        this.cidade.set(model.getCidade() != null ? model.getCidade() : "");
        this.uf.set(model.getUf() != null ? model.getUf() : Uf.RJ);
    }

    @Override
    protected void limparCampos() {
        cep.set("");
        tipoLogradouro.set(TipoLogradouro.RUA);
        logradouro.set("");
        numero.set("");
        complemento.set("");
        bairro.set("");
        cidade.set("");
        uf.set(Uf.RJ);
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
    public EnderecoProponente atualizarModel(EnderecoProponente model) {
        if (model == null)
            model = new EnderecoProponente();

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

    public ObjectProperty<TipoLogradouro> tipoLogradouroProperty() {
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

    public ObjectProperty<Uf> ufProperty() {
        return uf;
    }
}