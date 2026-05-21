package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.model.BancoModel;
import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.repository.BancoRepository;
import br.com.poderfinanceiro.app.repository.TabelaJurosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class TabelaJurosService {

    private final TabelaJurosRepository tabelaJurosRepository;
    private final BancoRepository bancoRepository;

    public TabelaJurosService(TabelaJurosRepository tabelaJurosRepository, BancoRepository bancoRepository) {
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.bancoRepository = bancoRepository;
    }

    /**
     * Busca apenas as taxas ativas (que não foram arquivadas).
     */
    public List<TabelaJurosModel> listarAtivas() {
        return tabelaJurosRepository.findAllAtivasWithBanco();
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
            return tabelaJurosRepository.save(model);
        }

        // Se for atualização, busca o prontuário antigo
        TabelaJurosModel antiga = tabelaJurosRepository.findById(model.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tabela original não encontrada no sistema."));

        // 1. Dá "alta" na tabela antiga (arquiva)
        antiga.setFimVigencia(LocalDate.now());
        antiga.setAtivo(false);
        tabelaJurosRepository.save(antiga);

        // 2. Cria uma "nova vida" com os dados atualizados
        TabelaJurosModel novaVersao = new TabelaJurosModel();
        novaVersao.setBanco(antiga.getBanco()); // Mantém o vínculo do banco
        novaVersao.setNomeTabela(model.getNomeTabela());
        novaVersao.setTipoConvenio(model.getTipoConvenio());
        novaVersao.setTaxaMensal(model.getTaxaMensal());
        novaVersao.setComissaoPercentual(model.getComissaoPercentual());
        novaVersao.setValorMinimoEmprestimo(model.getValorMinimoEmprestimo());
        novaVersao.setValorMaximoEmprestimo(model.getValorMaximoEmprestimo());

        // 🚀 TRANSFERINDO OS NOVOS LIMITES PARA A NOVA VERSÃO
        novaVersao.setRendaMinima(model.getRendaMinima());
        novaVersao.setPrazoMinimo(model.getPrazoMinimo());
        novaVersao.setPrazoMaximo(model.getPrazoMaximo());
        novaVersao.setIdadeMinima(model.getIdadeMinima());
        novaVersao.setIdadeMaxima(model.getIdadeMaxima());

        // Inicia a nova vigência hoje
        novaVersao.setInicioVigencia(LocalDate.now());
        novaVersao.setAtivo(true);

        return tabelaJurosRepository.save(novaVersao);
    }

    @Transactional
    public void arquivarTabela(TabelaJurosModel tabela) {
        if (tabela != null && tabela.getId() != null) {
            TabelaJurosModel managed = tabelaJurosRepository.findById(tabela.getId()).orElse(null);
            if (managed != null) {
                managed.setFimVigencia(LocalDate.now());
                managed.setAtivo(false);
                tabelaJurosRepository.save(managed);
            }
        }
    }

    // 🚀 MÉTODO ATUALIZADO: Processamento Transacional de Tabelas em Lote com
    // Ativação Dinâmica
    @Transactional
    public void salvarLoteTabelasImportadas(List<TabelaImportadaDTO> lote) {
        for (TabelaImportadaDTO dto : lote) {

            // 1. Resolve o Banco via pesquisa elástica (Ignorando Case)
            BancoModel bancoModel = bancoRepository
                    .findFirstByNomeContainingIgnoreCase(dto.getBanco())
                    .orElseThrow(() -> new RuntimeException("Banco não cadastrado no ERP: " + dto.getBanco()));

            // 2. Busca e desativa tabela antiga (Soft Delete / Fim de Vigência usando
            // LocalDate)
            tabelaJurosRepository.findByBancoIdAndNomeTabelaAndAtivoTrue(bancoModel.getId(), dto.getNomeTabela())
                    .ifPresent(tabelaAntiga -> {
                        tabelaAntiga.setAtivo(false);
                        tabelaAntiga.setFimVigencia(LocalDate.now());
                        tabelaJurosRepository.save(tabelaAntiga);
                    });

            // 3. Cria a nova estrutura de dados mapeada pelo Gemini
            TabelaJurosModel novaTabela = new TabelaJurosModel();
            novaTabela.setBanco(bancoModel);
            novaTabela.setNomeTabela(dto.getNomeTabela());

            try {
                novaTabela.setTipoConvenio(
                        TipoConvenioModel.valueOf(dto.getTipoConvenio()));
            } catch (Exception e) {
                novaTabela.setTipoConvenio(TipoConvenioModel.INSS_CONSIGNADO);
            }

            novaTabela.setValorMinimoEmprestimo(dto.getValorMinimo());
            novaTabela.setValorMaximoEmprestimo(dto.getValorMaximo());
            novaTabela.setPrazoMinimo(dto.getPrazoMinimo());
            novaTabela.setPrazoMaximo(dto.getPrazoMaximo());
            novaTabela.setIdadeMinima(dto.getIdadeMinima());
            novaTabela.setIdadeMaxima(dto.getIdadeMaxima());
            novaTabela.setTaxaMensal(dto.getTaxaMensal());
            novaTabela.setComissaoPercentual(dto.getComissaoPercentual());
            novaTabela.setInicioVigencia(LocalDate.now());

            // 🛡️ SUTURA DA LÓGICA DINÂMICA DE ATIVAÇÃO POR VIGÊNCIA
            boolean tabelaAtiva = true;

            if (dto.getFimVigenciaCalculado() != null && !dto.getFimVigenciaCalculado().isBlank()
                    && !dto.getFimVigenciaCalculado().equals("null")) {
                try {
                    // O Gemini agora devolve apenas a data em ISO_DATE (LocalDate) conforme o
                    // prompt ajustado
                    LocalDate dataFim = LocalDate.parse(dto.getFimVigenciaCalculado());
                    novaTabela.setFimVigencia(dataFim);

                    // Se a IA capturou uma tabela antiga/expirada em lote, ela já entra arquivada
                    if (dataFim.isBefore(LocalDate.now())) {
                        tabelaAtiva = false;
                    }
                } catch (Exception e) {
                    novaTabela.setFimVigencia(null);
                }
            }

            novaTabela.setAtivo(tabelaAtiva);
            tabelaJurosRepository.save(novaTabela);
        }
    }

    public TabelaJurosModel buscarPorId(Long id) {
        return tabelaJurosRepository.findByIdWithBanco(id).orElse(null);
    }
}