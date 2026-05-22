package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;

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
    // Ativação Dinâmica e Início de Vigência da IA
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

            // 4. Define a data de início (Usa a da IA ou assume o dia de hoje como
            // fallback)
            LocalDate dataInicioTabela = LocalDate.now();
            if (dto.getInicioVigenciaCalculado() != null && !dto.getInicioVigenciaCalculado().isBlank()
                    && !dto.getInicioVigenciaCalculado().equals("null")) {
                try {
                    dataInicioTabela = LocalDate.parse(dto.getInicioVigenciaCalculado());
                } catch (Exception e) {
                    // Mantém o LocalDate.now() em caso de falha de conversão
                }
            }
            novaTabela.setInicioVigencia(dataInicioTabela);

            // 5. 🛡️ LÓGICA DINÂMICA E BLINDAGEM CONTRA ALUCINAÇÃO DE DUPLICAÇÃO
            boolean tabelaAtiva = true;

            if (dto.getFimVigenciaCalculado() != null && !dto.getFimVigenciaCalculado().isBlank()
                    && !dto.getFimVigenciaCalculado().equals("null")) {
                try {
                    LocalDate dataFim = LocalDate.parse(dto.getFimVigenciaCalculado());

                    // 🛡️ TRAVA DE SEGURANÇA: Se a data de fim for igual à data de início, a IA
                    // cometeu um falso positivo de leitura.
                    if (dataFim.equals(dataInicioTabela)) {
                        novaTabela.setFimVigencia(null); // Ignora o fim de vigência incorreto
                    } else {
                        novaTabela.setFimVigencia(dataFim);

                        // Se a IA capturar uma tabela cuja data de fim real já passou, ela entra
                        // desativada
                        if (dataFim.isBefore(LocalDate.now())) {
                            tabelaAtiva = false;
                        }
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