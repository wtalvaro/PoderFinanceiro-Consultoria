package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.TabelaJuros;
import br.com.poderfinanceiro.app.repository.TabelaJurosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TabelaJurosService {

    private final TabelaJurosRepository repository;

    public TabelaJurosService(TabelaJurosRepository repository) {
        this.repository = repository;
    }

    /**
     * Busca apenas as taxas ativas (que não foram arquivadas).
     */
    public List<TabelaJuros> listarAtivas() {
        return repository.findByAtivoTrueAndFimVigenciaIsNull();
    }

    /**
     * APLICAÇÃO DA REGRA DE OURO:
     * Nunca atualizamos uma tabela usada. Fechamos a antiga e criamos uma nova.
     */
    @Transactional
    public TabelaJuros salvarComRegraDeOuro(TabelaJuros model) {
        // Se for um cadastro novo (sem ID), apenas salva
        if (model.getId() == null) {
            model.setInicioVigencia(LocalDate.now());
            model.setAtivo(true);
            return repository.save(model);
        }

        // Se for atualização, busca o prontuário antigo
        TabelaJuros antiga = repository.findById(model.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tabela original não encontrada no sistema."));

        // 1. Dá "alta" na tabela antiga (arquiva)
        antiga.setFimVigencia(LocalDate.now());
        antiga.setAtivo(false);
        repository.save(antiga);

        // 2. Cria uma "nova vida" com os dados atualizados
        TabelaJuros novaVersao = new TabelaJuros();
        novaVersao.setBanco(antiga.getBanco()); // Mantém o vínculo do banco
        novaVersao.setNomeTabela(model.getNomeTabela());
        novaVersao.setTipoConvenio(model.getTipoConvenio());
        novaVersao.setTaxaMensal(model.getTaxaMensal());
        novaVersao.setComissaoPercentual(model.getComissaoPercentual());
        novaVersao.setValorMinimoEmprestimo(model.getValorMinimoEmprestimo());
        novaVersao.setValorMaximoEmprestimo(model.getValorMaximoEmprestimo());

        // Inicia a nova vigência hoje
        novaVersao.setInicioVigencia(LocalDate.now());
        novaVersao.setAtivo(true);

        return repository.save(novaVersao);
    }

    @Transactional
    public void arquivarTabela(TabelaJuros tabela) {
        if (tabela != null && tabela.getId() != null) {
            TabelaJuros managed = repository.findById(tabela.getId()).orElse(null);
            if (managed != null) {
                managed.setFimVigencia(LocalDate.now());
                managed.setAtivo(false);
                repository.save(managed);
            }
        }
    }
}