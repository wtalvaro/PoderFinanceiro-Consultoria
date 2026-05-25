package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Service
public class TabelaJurosService {

    private static final Logger log = LoggerFactory.getLogger(TabelaJurosService.class);

    private final TabelaJurosRepository tabelaJurosRepository;
    private final BancoRepository bancoRepository;

    public TabelaJurosService(TabelaJurosRepository tabelaJurosRepository, BancoRepository bancoRepository) {
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.bancoRepository = bancoRepository;
        log.debug("[TABELA_JUROS_SERVICE] Construtor: Serviço instanciado");
    }

    /**
     * Busca apenas as taxas ativas (que não foram arquivadas).
     */
    public List<TabelaJurosModel> listarAtivas() {
        log.debug("[TABELA_JUROS_SERVICE] listarAtivas: Buscando tabelas ativas");
        List<TabelaJurosModel> ativas = tabelaJurosRepository.findAllAtivasWithBanco();
        log.info("[TABELA_JUROS_SERVICE] listarAtivas: {} tabelas ativas encontradas", ativas.size());
        return ativas;
    }

    /**
     * APLICAÇÃO DA REGRA DE OURO:
     * Nunca atualizamos uma tabela usada. Fechamos a antiga e criamos uma nova.
     */
    @Transactional
    public TabelaJurosModel salvarComRegraDeOuro(TabelaJurosModel model) {
        log.debug("[TABELA_JUROS_SERVICE] salvarComRegraDeOuro: Salvando tabela com regra de ouro");
        if (model.getId() == null) {
            log.info(
                    "[TABELA_JUROS_SERVICE] Nova tabela, ID nulo. Criando registro ativo com início de vigência hoje.");
            model.setInicioVigencia(LocalDate.now());
            model.setAtivo(true);
            TabelaJurosModel salva = tabelaJurosRepository.save(model);
            log.info("[TABELA_JUROS_SERVICE] Nova tabela criada com ID={}", salva.getId());
            return salva;
        }

        log.info(
                "[TABELA_JUROS_SERVICE] Atualizando tabela existente ID={}. Aplicando regra de ouro (arquivar antiga, criar nova).",
                model.getId());
        TabelaJurosModel antiga = tabelaJurosRepository.findById(model.getId())
                .orElseThrow(() -> {
                    log.error("[TABELA_JUROS_SERVICE] Tabela original ID={} não encontrada no sistema", model.getId());
                    return new IllegalArgumentException("Tabela original não encontrada no sistema.");
                });

        antiga.setFimVigencia(LocalDate.now());
        antiga.setAtivo(false);
        tabelaJurosRepository.save(antiga);
        log.debug("[TABELA_JUROS_SERVICE] Tabela antiga ID={} arquivada (ativo=false, fimVigencia={})", antiga.getId(),
                antiga.getFimVigencia());

        TabelaJurosModel novaVersao = new TabelaJurosModel();
        novaVersao.setBanco(antiga.getBanco());
        novaVersao.setNomeTabela(model.getNomeTabela());
        novaVersao.setTipoConvenio(model.getTipoConvenio());
        novaVersao.setTaxaMensal(model.getTaxaMensal());
        novaVersao.setComissaoPercentual(model.getComissaoPercentual());
        novaVersao.setValorMinimoEmprestimo(model.getValorMinimoEmprestimo());
        novaVersao.setValorMaximoEmprestimo(model.getValorMaximoEmprestimo());
        novaVersao.setRendaMinima(model.getRendaMinima());
        novaVersao.setPrazoMinimo(model.getPrazoMinimo());
        novaVersao.setPrazoMaximo(model.getPrazoMaximo());
        novaVersao.setIdadeMinima(model.getIdadeMinima());
        novaVersao.setIdadeMaxima(model.getIdadeMaxima());

        novaVersao.setInicioVigencia(LocalDate.now());
        novaVersao.setAtivo(true);

        TabelaJurosModel salva = tabelaJurosRepository.save(novaVersao);
        log.info("[TABELA_JUROS_SERVICE] Nova versão da tabela criada com ID={}, inícioVigencia={}", salva.getId(),
                salva.getInicioVigencia());
        return salva;
    }

    @Transactional
    public void arquivarTabela(TabelaJurosModel tabela) {
        log.debug("[TABELA_JUROS_SERVICE] arquivarTabela: Solicitado arquivamento para tabela ID={}",
                tabela != null ? tabela.getId() : "null");
        if (tabela != null && tabela.getId() != null) {
            TabelaJurosModel managed = tabelaJurosRepository.findById(tabela.getId()).orElse(null);
            if (managed != null) {
                managed.setFimVigencia(LocalDate.now());
                managed.setAtivo(false);
                tabelaJurosRepository.save(managed);
                log.info("[TABELA_JUROS_SERVICE] Tabela ID={} arquivada (ativo=false, fimVigencia={})", managed.getId(),
                        managed.getFimVigencia());
            } else {
                log.warn("[TABELA_JUROS_SERVICE] Tabela ID={} não encontrada no banco para arquivamento",
                        tabela.getId());
            }
        } else {
            log.warn("[TABELA_JUROS_SERVICE] Tentativa de arquivar tabela nula ou sem ID");
        }
    }

    // 🚀 MÉTODO ATUALIZADO: Processamento Transacional de Tabelas em Lote com
    // Ativação Dinâmica e Início de Vigência da IA
    @Transactional
    public void salvarLoteTabelasImportadas(List<TabelaImportadaDTO> lote) {
        log.info("[TABELA_JUROS_SERVICE] salvarLoteTabelasImportadas: Processando lote com {} tabelas",
                lote != null ? lote.size() : 0);
        if (lote == null || lote.isEmpty()) {
            log.warn("[TABELA_JUROS_SERVICE] Lote vazio ou nulo, nada a processar");
            return;
        }

        for (TabelaImportadaDTO dto : lote) {
            log.debug("[TABELA_JUROS_SERVICE] Processando DTO: banco='{}', nomeTabela='{}'", dto.getBanco(),
                    dto.getNomeTabela());

            BancoModel bancoModel = bancoRepository
                    .findFirstByNomeContainingIgnoreCase(dto.getBanco())
                    .orElseThrow(() -> {
                        log.error("[TABELA_JUROS_SERVICE] Banco não cadastrado no ERP: {}", dto.getBanco());
                        return new RuntimeException("Banco não cadastrado no ERP: " + dto.getBanco());
                    });
            log.trace("[TABELA_JUROS_SERVICE] Banco encontrado: ID={}, nome={}", bancoModel.getId(),
                    bancoModel.getNome());

            tabelaJurosRepository.findByBancoIdAndNomeTabelaAndAtivoTrue(bancoModel.getId(), dto.getNomeTabela())
                    .ifPresent(tabelaAntiga -> {
                        log.info("[TABELA_JUROS_SERVICE] Desativando tabela antiga ID={} (banco='{}', nome='{}')",
                                tabelaAntiga.getId(), bancoModel.getNome(), dto.getNomeTabela());
                        tabelaAntiga.setAtivo(false);
                        tabelaAntiga.setFimVigencia(LocalDate.now());
                        tabelaJurosRepository.save(tabelaAntiga);
                    });

            TabelaJurosModel novaTabela = new TabelaJurosModel();
            novaTabela.setBanco(bancoModel);
            novaTabela.setNomeTabela(dto.getNomeTabela());

            try {
                novaTabela.setTipoConvenio(TipoConvenioModel.valueOf(dto.getTipoConvenio()));
                log.trace("[TABELA_JUROS_SERVICE] TipoConvênio definido: {}", dto.getTipoConvenio());
            } catch (Exception e) {
                log.warn("[TABELA_JUROS_SERVICE] TipoConvênio inválido '{}', usando fallback INSS_CONSIGNADO",
                        dto.getTipoConvenio());
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

            LocalDate dataInicioTabela = LocalDate.now();
            if (dto.getInicioVigenciaCalculado() != null && !dto.getInicioVigenciaCalculado().isBlank()
                    && !dto.getInicioVigenciaCalculado().equals("null")) {
                try {
                    dataInicioTabela = LocalDate.parse(dto.getInicioVigenciaCalculado());
                    log.trace("[TABELA_JUROS_SERVICE] Data de início definida pela IA: {}", dataInicioTabela);
                } catch (Exception e) {
                    log.warn("[TABELA_JUROS_SERVICE] Erro ao parsear data de início '{}', usando hoje",
                            dto.getInicioVigenciaCalculado());
                }
            } else {
                log.trace("[TABELA_JUROS_SERVICE] Nenhuma data de início fornecida, usando hoje");
            }
            novaTabela.setInicioVigencia(dataInicioTabela);

            boolean tabelaAtiva = true;
            if (dto.getFimVigenciaCalculado() != null && !dto.getFimVigenciaCalculado().isBlank()
                    && !dto.getFimVigenciaCalculado().equals("null")) {
                try {
                    LocalDate dataFim = LocalDate.parse(dto.getFimVigenciaCalculado());
                    if (dataFim.equals(dataInicioTabela)) {
                        log.warn(
                                "[TABELA_JUROS_SERVICE] Data de fim igual à data de início, ignorando fim de vigência (possível alucinação da IA)");
                        novaTabela.setFimVigencia(null);
                    } else {
                        novaTabela.setFimVigencia(dataFim);
                        if (dataFim.isBefore(LocalDate.now())) {
                            tabelaAtiva = false;
                            log.info(
                                    "[TABELA_JUROS_SERVICE] Tabela com data de fim passada ({}), será criada como inativa",
                                    dataFim);
                        } else {
                            log.trace("[TABELA_JUROS_SERVICE] Data de fim definida: {}", dataFim);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[TABELA_JUROS_SERVICE] Erro ao parsear data de fim '{}', ignorando",
                            dto.getFimVigenciaCalculado());
                    novaTabela.setFimVigencia(null);
                }
            }

            novaTabela.setAtivo(tabelaAtiva);
            TabelaJurosModel salva = tabelaJurosRepository.save(novaTabela);
            log.info("[TABELA_JUROS_SERVICE] Tabela salva: ID={}, nome='{}', banco='{}', ativo={}", salva.getId(),
                    salva.getNomeTabela(), bancoModel.getNome(), salva.getAtivo());
        }
        log.info("[TABELA_JUROS_SERVICE] Processamento do lote concluído com sucesso");
    }

    public TabelaJurosModel buscarPorId(Long id) {
        log.debug("[TABELA_JUROS_SERVICE] buscarPorId: Buscando tabela por ID={}", id);
        TabelaJurosModel tabela = tabelaJurosRepository.findByIdWithBanco(id).orElse(null);
        if (tabela == null) {
            log.warn("[TABELA_JUROS_SERVICE] Tabela ID={} não encontrada", id);
        } else {
            log.trace("[TABELA_JUROS_SERVICE] Tabela ID={} encontrada: nome='{}'", id, tabela.getNomeTabela());
        }
        return tabela;
    }
}