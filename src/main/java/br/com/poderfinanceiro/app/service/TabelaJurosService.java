package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.TabelaJurosModel;
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
    public List<TabelaJurosModel> listarAtivas() {
        return repository.buscarTodasAtivasComBanco();
    }

    /**
     * APLICAÇÃO DA REGRA DE OURO:
     * Nunca atualizamos uma tabela usada. Fechamos a antiga e criamos uma nova.
     */
    @Transactional
    public TabelaJurosModel salvarComRegraDeOuro(TabelaJurosModel model) {
        // Se for um cadastro novo (sem ID), apenas salva
        if (model.getId() == null) {
            model.setInicioVigencia(LocalDate.now());
            model.setAtivo(true);
            return repository.save(model);
        }

        // Se for atualização, busca o prontuário antigo
        TabelaJurosModel antiga = repository.findById(model.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tabela original não encontrada no sistema."));

        // 1. Dá "alta" na tabela antiga (arquiva)
        antiga.setFimVigencia(LocalDate.now());
        antiga.setAtivo(false);
        repository.save(antiga);

        // 2. Cria uma "nova vida" com os dados atualizados
        TabelaJurosModel novaVersao = new TabelaJurosModel();
        novaVersao.setBanco(antiga.getBanco()); // Mantém o vínculo do banco
        novaVersao.setNomeTabela(model.getNomeTabela());
        novaVersao.setTipoConvenio(model.getTipoConvenio());
        novaVersao.setTaxaMensal(model.getTaxaMensal());
        novaVersao.setComissaoPercentual(model.getComissaoPercentual());
        novaVersao.setValorMinimoEmprestimo(model.getValorMinimoEmprestimo());
        novaVersao.setValorMaximoEmprestimo(model.getValorMaximoEmprestimo());

        // 🚀 AQUI ESTÁ A SUTURA! TRANSFERINDO OS NOVOS LIMITES PARA A NOVA VERSÃO
        novaVersao.setRendaMinima(model.getRendaMinima());
        novaVersao.setPrazoMinimo(model.getPrazoMinimo());
        novaVersao.setPrazoMaximo(model.getPrazoMaximo());
        novaVersao.setIdadeMinima(model.getIdadeMinima());
        novaVersao.setIdadeMaxima(model.getIdadeMaxima());

        // Inicia a nova vigência hoje
        novaVersao.setInicioVigencia(LocalDate.now());
        novaVersao.setAtivo(true);

        return repository.save(novaVersao);
    }

    @Transactional
    public void arquivarTabela(TabelaJurosModel tabela) {
        if (tabela != null && tabela.getId() != null) {
            TabelaJurosModel managed = repository.findById(tabela.getId()).orElse(null);
            if (managed != null) {
                managed.setFimVigencia(LocalDate.now());
                managed.setAtivo(false);
                repository.save(managed);
            }
        }
    }
}