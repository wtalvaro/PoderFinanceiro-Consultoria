package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.TabelaJurosArquivadoEvent;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosCriadoEvent;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.infrastructure.repository.BancoRepository;
import br.com.poderfinanceiro.app.infrastructure.repository.TabelaJurosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de Domínio para Gestão de Tabelas de Juros e Comissões. Implementa a
 * "Regra de Ouro": Tabelas vigentes são imutáveis; alterações geram novas
 * versões para preservar o histórico de cálculos de propostas passadas.
 */
@Service
@Transactional(readOnly = true)
public class TabelaJurosService {

    private static final Logger log = LoggerFactory.getLogger(TabelaJurosService.class);
    private static final String LOG_PREFIX = "[TabelaJurosService]";

    private final TabelaJurosRepository tabelaJurosRepository;
    private final BancoRepository bancoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TabelaJurosService(TabelaJurosRepository tabelaJurosRepository, BancoRepository bancoRepository,
            ApplicationEventPublisher eventPublisher) {
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.bancoRepository = bancoRepository;
        this.eventPublisher = eventPublisher;
        log.info("{} [SISTEMA] Serviço de Tabelas de Juros inicializado.", LOG_PREFIX);
    }

    /**
     * Recupera todas as tabelas ativas com seus respectivos bancos carregados
     * (Eager Loading).
     */
    public List<TabelaJurosModel> listarAtivas() {
        log.trace("{} [TELEMETRIA] Solicitada listagem de tabelas ativas.", LOG_PREFIX);
        return tabelaJurosRepository.findAllAtivasWithBanco();
    }

    /**
     * Implementação da Regra de Ouro: Se a tabela for nova, cria. Se for
     * edição, arquiva a atual e gera uma nova versão.
     */
    @Transactional public TabelaJurosModel salvarComRegraDeOuro(TabelaJurosModel model) {
        log.info("{} [TELEMETRIA] Iniciando salvamento de tabela com Regra de Ouro.", LOG_PREFIX);

        if (model == null)
            throw new IllegalArgumentException("Modelo de tabela não pode ser nulo.");

        // CASO 1: Nova Tabela
        if (model.getId() == null) {
            model.setInicioVigencia(LocalDate.now());
            model.setAtivo(true);
            TabelaJurosModel salva = tabelaJurosRepository.save(model);

            log.info("{} [AUDITORIA] Nova tabela de juros criada. ID: {}, Nome: {}", LOG_PREFIX, salva.getId(),
                    salva.getNomeTabela());
            eventPublisher.publishEvent(new TabelaJurosCriadoEvent(salva.getId()));
            return salva;
        }

        // CASO 2: Atualização (Arquivar antiga e criar nova)
        log.info("{} [NEGOCIO] Aplicando versionamento: Arquivando ID {} para criar nova versão.", LOG_PREFIX,
                model.getId());

        TabelaJurosModel antiga = tabelaJurosRepository.findById(model.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tabela original não encontrada."));

        // Arquivamento da versão anterior
        antiga.setFimVigencia(LocalDate.now());
        antiga.setAtivo(false);
        tabelaJurosRepository.save(antiga);

        // Instanciação da nova versão (Deep Copy dos campos editáveis)
        TabelaJurosModel novaVersao = clonarParaNovaVersao(antiga, model);
        novaVersao.setInicioVigencia(LocalDate.now());
        novaVersao.setAtivo(true);

        TabelaJurosModel salva = tabelaJurosRepository.save(novaVersao);

        log.info("{} [AUDITORIA] Tabela versionada com sucesso. Antiga: {} -> Nova: {}", LOG_PREFIX, antiga.getId(),
                salva.getId());
        eventPublisher.publishEvent(new TabelaJurosAtualizadoEvent(salva.getId()));

        return salva;
    }

    /**
     * Desativa uma tabela, definindo o fim de sua vigência para a data atual.
     */
    @Transactional public void arquivarTabela(TabelaJurosModel tabela) {
        log.info("{} [TELEMETRIA] Solicitado arquivamento da tabela ID: {}", LOG_PREFIX,
                tabela != null ? tabela.getId() : "NULL");

        if (tabela == null || tabela.getId() == null)
            return;

        tabelaJurosRepository.findById(tabela.getId()).ifPresent(managed -> {
            managed.setFimVigencia(LocalDate.now());
            managed.setAtivo(false);
            tabelaJurosRepository.save(managed);

            log.info("{} [AUDITORIA] Tabela ID {} arquivada com sucesso.", LOG_PREFIX, managed.getId());
            eventPublisher.publishEvent(new TabelaJurosArquivadoEvent(managed.getId()));
        });
    }

    /**
     * Processa a importação massiva de tabelas vindas do motor de OCR/IA.
     */
    @Transactional public void salvarLoteTabelasImportadas(List<TabelaImportadaDTO> lote) {
        log.info("{} [TELEMETRIA] Iniciando processamento de lote importado. Total: {} itens.", LOG_PREFIX,
                lote != null ? lote.size() : 0);

        if (lote == null || lote.isEmpty())
            return;

        for (TabelaImportadaDTO dto : lote) {
            try {
                processarTabelaIndividual(dto);
            } catch (Exception e) {
                log.error("{} [SISTEMA] Falha ao processar item do lote (Banco: {}): {}", LOG_PREFIX, dto.getBanco(),
                        e.getMessage());
                // Em lote, continuamos o processamento dos demais itens mesmo
                // se um falhar
            }
        }
        log.info("{} [AUDITORIA] Processamento de lote concluído.", LOG_PREFIX);
    }

    private void processarTabelaIndividual(TabelaImportadaDTO dto) {
        // 1. Localizar Banco (Regra de Negócio: Banco deve existir no ERP)
        BancoModel banco = bancoRepository.findFirstByNomeContainingIgnoreCase(dto.getBanco())
                .orElseThrow(() -> new RuntimeException("Banco '" + dto.getBanco() + "' não cadastrado no sistema."));

        // 2. Arquivar versão anterior se houver conflito de nome/banco
        tabelaJurosRepository.findByBancoIdAndNomeTabelaAndAtivoTrue(banco.getId(), dto.getNomeTabela())
                .ifPresent(antiga -> {
                    log.debug("{} [NEGOCIO] Substituindo tabela ativa existente: ID {}", LOG_PREFIX, antiga.getId());
                    antiga.setAtivo(false);
                    antiga.setFimVigencia(LocalDate.now());
                    tabelaJurosRepository.save(antiga);
                });

        // 3. Mapear e Salvar Nova Tabela
        TabelaJurosModel nova = mapearDtoParaModel(dto, banco);
        TabelaJurosModel salva = tabelaJurosRepository.save(nova);

        log.info("{} [AUDITORIA] Tabela importada com sucesso. ID: {}, Banco: {}, Nome: {}", LOG_PREFIX, salva.getId(),
                banco.getNome(), salva.getNomeTabela());
    }

    public Optional<TabelaJurosModel> buscarPorId(Long id) {
        return tabelaJurosRepository.findByIdWithBanco(id);
    }

    // --- Helpers de Mapeamento e Lógica de Domínio ---

    private TabelaJurosModel clonarParaNovaVersao(TabelaJurosModel antiga, TabelaJurosModel editada) {
        TabelaJurosModel nova = new TabelaJurosModel();
        nova.setBanco(antiga.getBanco());
        nova.setNomeTabela(editada.getNomeTabela());
        nova.setTipoConvenio(editada.getTipoConvenio());
        nova.setTaxaMensal(editada.getTaxaMensal());
        nova.setComissaoPercentual(editada.getComissaoPercentual());
        nova.setValorMinimoEmprestimo(editada.getValorMinimoEmprestimo());
        nova.setValorMaximoEmprestimo(editada.getValorMaximoEmprestimo());
        nova.setRendaMinima(editada.getRendaMinima());
        nova.setPrazoMinimo(editada.getPrazoMinimo());
        nova.setPrazoMaximo(editada.getPrazoMaximo());
        nova.setIdadeMinima(editada.getIdadeMinima());
        nova.setIdadeMaxima(editada.getIdadeMaxima());
        return nova;
    }

    private TabelaJurosModel mapearDtoParaModel(TabelaImportadaDTO dto, BancoModel banco) {
        TabelaJurosModel model = new TabelaJurosModel();
        model.setBanco(banco);
        model.setNomeTabela(dto.getNomeTabela());
        model.setValorMinimoEmprestimo(dto.getValorMinimo());
        model.setValorMaximoEmprestimo(dto.getValorMaximo());
        model.setPrazoMinimo(dto.getPrazoMinimo());
        model.setPrazoMaximo(dto.getPrazoMaximo());
        model.setIdadeMinima(dto.getIdadeMinima());
        model.setIdadeMaxima(dto.getIdadeMaxima());
        model.setTaxaMensal(dto.getTaxaMensal());
        model.setComissaoPercentual(dto.getComissaoPercentual());

        // Tratamento de Enum com Fallback
        try {
            model.setTipoConvenio(TipoConvenioModel.valueOf(dto.getTipoConvenio()));
        } catch (Exception e) {
            log.warn("{} [NEGOCIO] Tipo de convênio inválido '{}'. Usando padrão INSS.", LOG_PREFIX,
                    dto.getTipoConvenio());
            model.setTipoConvenio(TipoConvenioModel.INSS_CONSIGNADO);
        }

        // Lógica de Vigência vinda da IA
        LocalDate inicio = parseData(dto.getInicioVigenciaCalculado(), LocalDate.now());
        model.setInicioVigencia(inicio);

        if (dto.getFimVigenciaCalculado() != null && !dto.getFimVigenciaCalculado().equals("null")) {
            LocalDate fim = parseData(dto.getFimVigenciaCalculado(), null);
            if (fim != null && !fim.isEqual(inicio)) {
                model.setFimVigencia(fim);
                model.setAtivo(fim.isAfter(LocalDate.now()));
            } else {
                model.setAtivo(true);
            }
        } else {
            model.setAtivo(true);
        }

        return model;
    }

    private LocalDate parseData(String dataStr, LocalDate fallback) {
        if (dataStr == null || dataStr.isBlank() || dataStr.equalsIgnoreCase("null"))
            return fallback;
        try {
            return LocalDate.parse(dataStr);
        } catch (Exception e) {
            log.warn("{} [SISTEMA] Erro ao parsear data '{}'. Usando fallback.", LOG_PREFIX, dataStr);
            return fallback;
        }
    }
}
