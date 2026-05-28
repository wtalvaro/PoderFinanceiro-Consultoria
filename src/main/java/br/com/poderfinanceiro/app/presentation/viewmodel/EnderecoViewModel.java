package br.com.poderfinanceiro.app.presentation.viewmodel;

import br.com.poderfinanceiro.app.common.util.EnderecoUtils;
import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Component
@Scope("prototype")
public class EnderecoViewModel extends BaseViewModel<EnderecoProponenteModel> {

    private static final Logger log = LoggerFactory.getLogger(EnderecoViewModel.class);

    // --- PROPERTIES EXCLUSIVAS DE ENDEREÇO ---
    private final StringProperty cep = new SimpleStringProperty("");
    private final ObjectProperty<TipoLogradouroModel> tipoLogradouro = new SimpleObjectProperty<>(
            TipoLogradouroModel.RUA);
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

    // Construtor para log (não quebra contrato, era implícito)
    public EnderecoViewModel() {
        log.debug("[ENDERECO_VM] Instância criada (prototype)");
    }

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(EnderecoProponenteModel model) {
        log.debug("[ENDERECO_VM] extrairId: model ID={}", model != null ? model.getId() : "null");
        this.id.set(model != null ? model.getId() : null);
    }

    @Override
    protected void preencherCampos(EnderecoProponenteModel model) {
        log.debug("[ENDERECO_VM] preencherCampos: populando campos a partir do model ID={}",
                model != null ? model.getId() : "null");

        if (model == null) {
            log.trace("[ENDERECO_VM] preencherCampos: model é null, limpando campos");
            limparCampos();
            return;
        }

        this.cep.set(model.getCep() != null ? model.getCep() : "");
        this.tipoLogradouro
                .set(model.getTipoLogradouro() != null ? model.getTipoLogradouro() : TipoLogradouroModel.RUA);
        this.logradouro.set(model.getLogradouro() != null ? model.getLogradouro() : "");
        this.numero.set(model.getNumero() != null ? model.getNumero() : "");
        this.complemento.set(model.getComplemento() != null ? model.getComplemento() : "");
        this.bairro.set(model.getBairro() != null ? model.getBairro() : "");
        this.cidade.set(model.getCidade() != null ? model.getCidade() : "");
        this.uf.set(model.getUf() != null ? model.getUf() : UfModel.SP);
        log.trace("[ENDERECO_VM] Campos preenchidos: cep='{}', logradouro='{}', cidade='{}'", cep.get(),
                logradouro.get(), cidade.get());
    }

    @Override
    protected void limparCampos() {
        log.debug("[ENDERECO_VM] limparCampos: resetando todos os campos");
        cep.set("");
        tipoLogradouro.set(TipoLogradouroModel.RUA);
        logradouro.set("");
        numero.set("");
        complemento.set("");
        bairro.set("");
        cidade.set("");
        uf.set(UfModel.SP);
        log.trace("[ENDERECO_VM] Campos limpos");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        log.debug("[ENDERECO_VM] sincronizarEstadoOriginal: salvando estado atual como original");
        this.cepOriginal.set(cep.get());
        this.tipoLogradouroOriginal.set(tipoLogradouro.get());
        this.logradouroOriginal.set(logradouro.get());
        this.numeroOriginal.set(numero.get());
        this.complementoOriginal.set(complemento.get());
        this.bairroOriginal.set(bairro.get());
        this.cidadeOriginal.set(cidade.get());
        this.ufOriginal.set(uf.get());
        log.trace("[ENDERECO_VM] Estado original salvo: cep='{}', logradouro='{}', cidade='{}'", cepOriginal.get(),
                logradouroOriginal.get(), cidadeOriginal.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        boolean alterado = !Objects.equals(cep.get(), cepOriginal.get()) ||
                !Objects.equals(tipoLogradouro.get(), tipoLogradouroOriginal.get()) ||
                !Objects.equals(logradouro.get(), logradouroOriginal.get()) ||
                !Objects.equals(numero.get(), numeroOriginal.get()) ||
                !Objects.equals(complemento.get(), complementoOriginal.get()) ||
                !Objects.equals(bairro.get(), bairroOriginal.get()) ||
                !Objects.equals(cidade.get(), cidadeOriginal.get()) ||
                !Objects.equals(uf.get(), ufOriginal.get());
        log.trace("[ENDERECO_VM] temAlteracoesPendentes: {}", alterado);
        return alterado;
    }

    @Override
    public EnderecoProponenteModel atualizarModel(EnderecoProponenteModel model) {
        log.debug("[ENDERECO_VM] atualizarModel: model fornecido = {}", model != null ? "presente" : "null");
        if (model == null) {
            log.trace("[ENDERECO_VM] Criando novo model (null -> novo)");
            model = new EnderecoProponenteModel();
        }

        model.setId(this.id.get());
        model.setCep(EnderecoUtils.limparCep(this.cep.get()));
        model.setTipoLogradouro(this.tipoLogradouro.get());
        model.setLogradouro(this.logradouro.get());
        model.setNumero(this.numero.get());
        model.setComplemento(this.complemento.get());
        model.setBairro(this.bairro.get());
        model.setCidade(this.cidade.get());
        model.setUf(this.uf.get());

        log.info("[ENDERECO_VM] Model atualizado: ID={}, cep='{}', logradouro='{}', cidade='{}'", model.getId(),
                model.getCep(), model.getLogradouro(), model.getCidade());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        Observable[] observaveis = new Observable[] {
                cep, tipoLogradouro, logradouro, numero, complemento, bairro, cidade, uf,
                cepOriginal.getReadOnlyProperty(), tipoLogradouroOriginal.getReadOnlyProperty(),
                logradouroOriginal.getReadOnlyProperty(), numeroOriginal.getReadOnlyProperty(),
                complementoOriginal.getReadOnlyProperty(), bairroOriginal.getReadOnlyProperty(),
                cidadeOriginal.getReadOnlyProperty(), ufOriginal.getReadOnlyProperty()
        };
        log.trace("[ENDERECO_VM] getObservaveisParaDirty: {} observáveis registrados", observaveis.length);
        return observaveis;
    }

    @Override
    public boolean isValido() {
        log.trace("[ENDERECO_VM] isValido: sempre verdadeiro (sem regras específicas)");
        return true;
    }

    // --- GETTERS DAS PROPERTIES (Somente das específicas desta classe agora) ---
    public StringProperty cepProperty() {
        log.trace("[ENDERECO_VM] cepProperty acessado");
        return cep;
    }

    public ObjectProperty<TipoLogradouroModel> tipoLogradouroProperty() {
        log.trace("[ENDERECO_VM] tipoLogradouroProperty acessado");
        return tipoLogradouro;
    }

    public StringProperty logradouroProperty() {
        log.trace("[ENDERECO_VM] logradouroProperty acessado");
        return logradouro;
    }

    public StringProperty numeroProperty() {
        log.trace("[ENDERECO_VM] numeroProperty acessado");
        return numero;
    }

    public StringProperty complementoProperty() {
        log.trace("[ENDERECO_VM] complementoProperty acessado");
        return complemento;
    }

    public StringProperty bairroProperty() {
        log.trace("[ENDERECO_VM] bairroProperty acessado");
        return bairro;
    }

    public StringProperty cidadeProperty() {
        log.trace("[ENDERECO_VM] cidadeProperty acessado");
        return cidade;
    }

    public ObjectProperty<UfModel> ufProperty() {
        log.trace("[ENDERECO_VM] ufProperty acessado");
        return uf;
    }
}