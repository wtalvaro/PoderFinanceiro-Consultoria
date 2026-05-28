package br.com.poderfinanceiro.app.presentation.viewmodel;

import javafx.beans.Observable;
import javafx.beans.property.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.poderfinanceiro.app.domain.model.BancoModel;

import java.util.Objects;

@Component
@Scope("prototype")
public class BancoViewModel extends BaseViewModel<BancoModel> {

    private static final Logger log = LoggerFactory.getLogger(BancoViewModel.class);

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

    // Construtor adicionado para log (não quebra contrato, pois era implícito)
    public BancoViewModel() {
        log.debug("[BANCO_VM] Instância criada (prototype)");
    }

    // ==========================================================
    // IMPLEMENTAÇÃO DO CONTRATO (Template Methods)
    // ==========================================================

    @Override
    protected void extrairId(BancoModel model) {
        log.debug("[BANCO_VM] extrairId: model ID={}", model != null ? model.getId() : "null");
        // Protege contra NPE caso model seja null
        if (model == null) {
            this.id.set(null);
        } else {
            this.id.set(model.getId());
        }
    }

    @Override
    protected void preencherCampos(BancoModel model) {
        log.debug("[BANCO_VM] preencherCampos: populando campos a partir do model");
        codigo.set(model.getCodigo() != null ? model.getCodigo() : "");
        nome.set(model.getNome() != null ? model.getNome() : "");
        sitePortal.set(model.getSitePortal() != null ? model.getSitePortal() : "");
        telefoneSuporte.set(model.getTelefoneSuporte() != null ? model.getTelefoneSuporte() : "");
        log.trace("[BANCO_VM] Campos preenchidos: codigo='{}', nome='{}'", codigo.get(), nome.get());
    }

    @Override
    protected void limparCampos() {
        log.debug("[BANCO_VM] limparCampos: resetando todos os campos");
        codigo.set("");
        nome.set("");
        sitePortal.set("");
        telefoneSuporte.set("");
        log.trace("[BANCO_VM] Campos limpos");
    }

    @Override
    protected void sincronizarEstadoOriginal() {
        log.debug("[BANCO_VM] sincronizarEstadoOriginal: salvando estado atual como original");
        this.codigoOriginal.set(codigo.get());
        this.nomeOriginal.set(nome.get());
        this.sitePortalOriginal.set(sitePortal.get());
        this.telefoneSuporteOriginal.set(telefoneSuporte.get());
        log.trace("[BANCO_VM] Estado original salvo: codigo='{}', nome='{}'", codigoOriginal.get(), nomeOriginal.get());
    }

    @Override
    protected boolean temAlteracoesPendentes() {
        boolean alterado = !Objects.equals(codigo.get(), codigoOriginal.get()) ||
                !Objects.equals(nome.get(), nomeOriginal.get()) ||
                !Objects.equals(sitePortal.get(), sitePortalOriginal.get()) ||
                !Objects.equals(telefoneSuporte.get(), telefoneSuporteOriginal.get());
        log.trace("[BANCO_VM] temAlteracoesPendentes: {}", alterado);
        return alterado;
    }

    @Override
    public BancoModel atualizarModel(BancoModel model) {
        log.debug("[BANCO_VM] atualizarModel: model fornecido = {}", model != null ? "presente" : "null");
        if (model == null) {
            log.trace("[BANCO_VM] Criando novo model (null -> novo)");
            model = new BancoModel();
        }

        model.setId(this.id.get());
        model.setCodigo(this.codigo.get());
        model.setNome(this.nome.get());
        model.setSitePortal(this.sitePortal.get());
        model.setTelefoneSuporte(this.telefoneSuporte.get());

        log.info("[BANCO_VM] Model atualizado: ID={}, codigo='{}', nome='{}'", model.getId(), model.getCodigo(),
                model.getNome());
        return model;
    }

    @Override
    protected Observable[] getObservaveisParaDirty() {
        Observable[] observaveis = new Observable[] {
                codigo, nome, sitePortal, telefoneSuporte,
                codigoOriginal.getReadOnlyProperty(),
                nomeOriginal.getReadOnlyProperty(),
                sitePortalOriginal.getReadOnlyProperty(),
                telefoneSuporteOriginal.getReadOnlyProperty()
        };
        log.trace("[BANCO_VM] getObservaveisParaDirty: {} observáveis registrados", observaveis.length);
        return observaveis;
    }

    @Override
    public boolean isValido() {
        log.trace("[BANCO_VM] isValido: sempre verdadeiro (sem regras específicas)");
        return true;
    }

    // ==========================================================
    // GETTERS DAS PROPERTIES EXCLUSIVAS
    // ==========================================================

    public StringProperty codigoProperty() {
        log.trace("[BANCO_VM] codigoProperty acessado");
        return codigo;
    }

    public StringProperty nomeProperty() {
        log.trace("[BANCO_VM] nomeProperty acessado");
        return nome;
    }

    public StringProperty sitePortalProperty() {
        log.trace("[BANCO_VM] sitePortalProperty acessado");
        return sitePortal;
    }

    public StringProperty telefoneSuporteProperty() {
        log.trace("[BANCO_VM] telefoneSuporteProperty acessado");
        return telefoneSuporte;
    }

    // Métodos legacy (get) mantidos para compatibilidade
    public StringProperty getCodigo() {
        log.trace("[BANCO_VM] getCodigo (legacy) acessado");
        return codigo;
    }

    public StringProperty getNome() {
        log.trace("[BANCO_VM] getNome (legacy) acessado");
        return nome;
    }

    public StringProperty getSitePortal() {
        log.trace("[BANCO_VM] getSitePortal (legacy) acessado");
        return sitePortal;
    }

    public StringProperty getTelefoneSuporte() {
        log.trace("[BANCO_VM] getTelefoneSuporte (legacy) acessado");
        return telefoneSuporte;
    }
}