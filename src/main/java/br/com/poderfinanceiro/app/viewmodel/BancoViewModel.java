package br.com.poderfinanceiro.app.viewmodel;

import br.com.poderfinanceiro.app.model.BancoModel;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope("prototype") // Garante que cada tela/modal ganhe um ViewModel zerado
public class BancoViewModel extends BaseViewModel<BancoModel> {

    // --- 1. PROPERTIES ESPECÍFICAS DO BANCO ---
    private final StringProperty codigo = new SimpleStringProperty("");
    private final StringProperty nome = new SimpleStringProperty("");
    private final StringProperty sitePortal = new SimpleStringProperty("");
    private final StringProperty telefoneSuporte = new SimpleStringProperty("");

    // --- 2. ESTADOS ORIGINAIS (Para o Dirty Checking) ---
    private final ReadOnlyStringWrapper codigoOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper nomeOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper sitePortalOriginal = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper telefoneSuporteOriginal = new ReadOnlyStringWrapper("");

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(BancoModel model) {
        this.id.set(model.getId());
    }

    @Override
    protected void preencherCampos(BancoModel model) {
        codigo.set(model.getCodigo() != null ? model.getCodigo() : "");
        nome.set(model.getNome() != null ? model.getNome() : "");
        sitePortal.set(model.getSitePortal() != null ? model.getSitePortal() : "");
        telefoneSuporte.set(model.getTelefoneSuporte() != null ? model.getTelefoneSuporte() : "");
    }

    @Override
    protected void limparCampos() {
        codigo.set("");
        nome.set("");
        sitePortal.set("");
        telefoneSuporte.set("");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        this.codigoOriginal.set(codigo.get());
        this.nomeOriginal.set(nome.get());
        this.sitePortalOriginal.set(sitePortal.get());
        this.telefoneSuporteOriginal.set(telefoneSuporte.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        return !Objects.equals(codigo.get(), codigoOriginal.get()) ||
                !Objects.equals(nome.get(), nomeOriginal.get()) ||
                !Objects.equals(sitePortal.get(), sitePortalOriginal.get()) ||
                !Objects.equals(telefoneSuporte.get(), telefoneSuporteOriginal.get());
    }

    @Override
    public BancoModel atualizarModel(BancoModel model) {
        if (model == null) {
            model = new BancoModel();
        }

        model.setId(this.id.get());
        model.setCodigo(this.codigo.get());
        model.setNome(this.nome.get());
        model.setSitePortal(this.sitePortal.get());
        model.setTelefoneSuporte(this.telefoneSuporte.get());

        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        return new Observable[] {
                codigo, nome, sitePortal, telefoneSuporte,
                codigoOriginal.getReadOnlyProperty(),
                nomeOriginal.getReadOnlyProperty(),
                sitePortalOriginal.getReadOnlyProperty(),
                telefoneSuporteOriginal.getReadOnlyProperty()
        };
    }

    @Override
    public boolean isValido() {
        // Por padrão, se não tiver regras específicas, retorna verdadeiro
        return true;
    }

    // ==========================================================
    // GETTERS DAS PROPERTIES EXCLUSIVAS
    // ==========================================================

    public StringProperty codigoProperty() {
        return codigo;
    }

    public StringProperty nomeProperty() {
        return nome;
    }

    public StringProperty sitePortalProperty() {
        return sitePortal;
    }

    public StringProperty telefoneSuporteProperty() {
        return telefoneSuporte;
    }

    // Mantive os métodos abaixo com "get" para garantir que o Controller
    // que te enviei antes (que chamava viewModel.getCodigo()) não quebre,
    // mas na convenção pura do JavaFX usaríamos apenas os "Property()" acima.
    public StringProperty getCodigo() {
        return codigo;
    }

    public StringProperty getNome() {
        return nome;
    }

    public StringProperty getSitePortal() {
        return sitePortal;
    }

    public StringProperty getTelefoneSuporte() {
        return telefoneSuporte;
    }
}