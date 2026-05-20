package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.TabelaJurosModel;
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
        return tabelaJurosRepository.buscarTodasAtivasComBanco();
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

        // 🚀 AQUI ESTÁ A SUTURA! TRANSFERINDO OS NOVOS LIMITES PARA A NOVA VERSÃO
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

    // 🚀 NOVO MÉTODO: Processamento Transacional de Tabelas em Lote
    @org.springframework.transaction.annotation.Transactional
    public void salvarLoteTabelasImportadas(java.util.List<br.com.poderfinanceiro.app.dto.TabelaImportadaDTO> lote) {
        for (br.com.poderfinanceiro.app.dto.TabelaImportadaDTO dto : lote) {

            // 1. Resolve o Banco via pesquisa elástica (Note o uso de 'Nome' em vez de
            // 'NomeBanco')
            br.com.poderfinanceiro.app.model.BancoModel bancoModel = bancoRepository
                    .findFirstByNomeContainingIgnoreCase(dto.getBanco())
                    .orElseThrow(() -> new RuntimeException("Banco não cadastrado no ERP: " + dto.getBanco()));

            // 2. Busca e desativa tabela antiga (Soft Delete / Fim de Vigência usando
            // LocalDate)
            tabelaJurosRepository.findByBancoIdAndNomeTabelaAndAtivoTrue(bancoModel.getId(), dto.getNomeTabela())
                    .ifPresent(tabelaAntiga -> {
                        tabelaAntiga.setAtivo(false);
                        tabelaAntiga.setFimVigencia(java.time.LocalDate.now()); // Correção para LocalDate
                        tabelaJurosRepository.save(tabelaAntiga);
                    });

            // 3. Cria e salva a nova tabela
            br.com.poderfinanceiro.app.model.TabelaJurosModel novaTabela = new br.com.poderfinanceiro.app.model.TabelaJurosModel();
            novaTabela.setBanco(bancoModel);
            novaTabela.setNomeTabela(dto.getNomeTabela());

            try {
                novaTabela.setTipoConvenio(
                        br.com.poderfinanceiro.app.model.enums.TipoConvenioModel.valueOf(dto.getTipoConvenio()));
            } catch (Exception e) {
                novaTabela.setTipoConvenio(br.com.poderfinanceiro.app.model.enums.TipoConvenioModel.INSS_CONSIGNADO);
            }

            novaTabela.setValorMinimoEmprestimo(dto.getValorMinimo());
            novaTabela.setValorMaximoEmprestimo(dto.getValorMaximo());
            novaTabela.setPrazoMinimo(dto.getPrazoMinimo());
            novaTabela.setPrazoMaximo(dto.getPrazoMaximo());
            novaTabela.setIdadeMinima(dto.getIdadeMinima());
            novaTabela.setIdadeMaxima(dto.getIdadeMaxima());
            novaTabela.setTaxaMensal(dto.getTaxaMensal());
            novaTabela.setComissaoPercentual(dto.getComissaoPercentual());
            novaTabela.setAtivo(true);
            novaTabela.setInicioVigencia(java.time.LocalDate.now()); // Correção para LocalDate

            // Gerencia a Vigência Programada: A IA devolve DateTime, extraímos só a Data.
            if (dto.getFimVigenciaCalculado() != null && !dto.getFimVigenciaCalculado().isBlank()
                    && !dto.getFimVigenciaCalculado().equals("null")) {
                try {
                    java.time.LocalDate dataVigencia = java.time.LocalDateTime.parse(dto.getFimVigenciaCalculado())
                            .toLocalDate();
                    novaTabela.setFimVigencia(dataVigencia);
                } catch (Exception e) {
                    novaTabela.setFimVigencia(null);
                }
            }

            tabelaJurosRepository.save(novaTabela);
        }
    }

    public TabelaJurosModel buscarPorId(Long id) {
        return tabelaJurosRepository.findByIdWithBanco(id).orElse(null);
    }
}