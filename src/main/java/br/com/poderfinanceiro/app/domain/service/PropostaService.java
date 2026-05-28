package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.PropostaAtualizadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaExcluidaEvent;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.domain.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PropostaService {

    private static final Logger log = LoggerFactory.getLogger(PropostaService.class);

    private final PropostaRepository propostaRepository;
    private final TabelaJurosRepository tabelaJurosRepository;
    private final ComissaoRepository comissaoRepository;
    private final DocumentoProponenteRepository documentoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthService authService;

    public PropostaService(PropostaRepository propostaRepository,
            TabelaJurosRepository tabelaJurosRepository,
            ComissaoRepository comissaoRepository,
            DocumentoProponenteRepository documentoRepository,
            ApplicationEventPublisher eventPublisher,
            AuthService authService) {
        this.propostaRepository = propostaRepository;
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.comissaoRepository = comissaoRepository;
        this.documentoRepository = documentoRepository;
        this.eventPublisher = eventPublisher;
        this.authService = authService;
        log.debug("[PROPOSTA_SERVICE] Construtor: Serviço instanciado");
    }

    /**
     * Calcula o repasse baseado na tabela de juros selecionada.
     */
    public BigDecimal calcularComissaoEstimada(BigDecimal valorAprovado, Long tabelaId) {
        log.debug("[PROPOSTA_SERVICE] calcularComissaoEstimada: valor={}, tabelaId={}", valorAprovado, tabelaId);
        if (valorAprovado == null || valorAprovado.compareTo(BigDecimal.ZERO) <= 0 || tabelaId == null) {
            log.warn("[PROPOSTA_SERVICE] calcularComissaoEstimada: Parâmetros inválidos, retornando ZERO");
            return BigDecimal.ZERO;
        }

        return tabelaJurosRepository.findById(tabelaId)
                .map(tabela -> {
                    BigDecimal percentual = tabela.getComissaoPercentual()
                            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    BigDecimal comissao = valorAprovado.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
                    log.info("[PROPOSTA_SERVICE] calcularComissaoEstimada: Comissão calculada = {}", comissao);
                    return comissao;
                })
                .orElseGet(() -> {
                    log.warn(
                            "[PROPOSTA_SERVICE] calcularComissaoEstimada: Tabela ID={} não encontrada, retornando ZERO",
                            tabelaId);
                    return BigDecimal.ZERO;
                });
    }

    @Transactional
    public PropostaModel salvarProposta(PropostaModel proposta) {
        log.debug("[PROPOSTA_SERVICE] salvarProposta: Salvando proposta ID={}",
                proposta != null ? proposta.getId() : "null");
        if (proposta == null) {
            log.warn("[PROPOSTA_SERVICE] salvarProposta: Proposta nula recebida");
            throw new IllegalArgumentException("Proposta não pode ser nula");
        }

        boolean isNovo = proposta.getId() == null;
        log.trace("[PROPOSTA_SERVICE] Proposta é nova? {}", isNovo);

        if (proposta.getUsuario() == null) {
            proposta.setUsuario(authService.getUsuarioLogado());
            log.debug("[PROPOSTA_SERVICE] Usuário logado associado à proposta");
        }

        sincronizarDadosTabela(proposta);
        PropostaModel propostaSalva = propostaRepository.save(proposta);
        log.info("[PROPOSTA_SERVICE] Proposta salva com ID={}, status={}", propostaSalva.getId(),
                propostaSalva.getStatus());

        if (propostaSalva.getStatus() == StatusPropostaModel.PAGO) {
            log.info("[PROPOSTA_SERVICE] Proposta ID={} está com status PAGO, processando ciclo de pagamento",
                    propostaSalva.getId());
            processarCicloPagamento(propostaSalva);
        }

        if (isNovo) {
            log.debug("[PROPOSTA_SERVICE] Publicando evento PropostaCriadaEvent para ID={}", propostaSalva.getId());
            eventPublisher.publishEvent(new PropostaCriadaEvent(propostaSalva.getId()));
        } else {
            log.debug("[PROPOSTA_SERVICE] Publicando evento PropostaAtualizadaEvent para ID={}", propostaSalva.getId());
            eventPublisher.publishEvent(new PropostaAtualizadaEvent(propostaSalva.getId()));
        }

        return propostaSalva;
    }

    @Transactional
    public PropostaModel salvarProposta(PropostaModel proposta, List<DocumentoProponenteModel> documentos) {
        log.debug("[PROPOSTA_SERVICE] salvarProposta (com documentos): Proposta ID={}, documentos size={}",
                proposta != null ? proposta.getId() : "null", documentos != null ? documentos.size() : 0);
        PropostaModel propostaSalva = this.salvarProposta(proposta);

        if (documentos != null && !documentos.isEmpty()) {
            documentos.forEach(doc -> {
                doc.setProposta(propostaSalva);
                documentoRepository.save(doc);
                log.trace("[PROPOSTA_SERVICE] Documento ID={} associado à proposta ID={}", doc.getId(),
                        propostaSalva.getId());
            });
            log.info("[PROPOSTA_SERVICE] {} documentos associados à proposta ID={}", documentos.size(),
                    propostaSalva.getId());
        }
        return propostaSalva;
    }

    /**
     * SRP: Isola a lógica de sincronização entre Proposta e Tabela de Juros.
     */
    private void sincronizarDadosTabela(PropostaModel proposta) {
        log.trace("[PROPOSTA_SERVICE] sincronizarDadosTabela: Proposta ID={}, tabelaId={}", proposta.getId(),
                proposta.getTabelaId());
        if (proposta.getTabelaId() != null) {
            TabelaJurosModel tabela = tabelaJurosRepository.findById(proposta.getTabelaId()).orElse(null);
            if (tabela != null) {
                proposta.setConvenioOrgao(tabela.getTipoConvenio());
                log.trace("[PROPOSTA_SERVICE] Convênio definido como {} a partir da tabela", tabela.getTipoConvenio());

                BigDecimal baseCalculo = proposta.getValorAprovado() != null ? proposta.getValorAprovado()
                        : proposta.getValorSolicitado();
                BigDecimal comissao = calcularComissaoEstimada(baseCalculo, tabela.getId());
                proposta.setComissaoEstimada(comissao);
                log.trace("[PROPOSTA_SERVICE] Comissão estimada definida como {}", comissao);
            } else {
                log.warn("[PROPOSTA_SERVICE] Tabela ID={} não encontrada para sincronização", proposta.getTabelaId());
            }
        }
    }

    /**
     * SRP: Gerencia o Ciclo de Pagamento (Quarta/Quinta/Sexta).
     */
    private void processarCicloPagamento(PropostaModel proposta) {
        log.debug("[PROPOSTA_SERVICE] processarCicloPagamento: Iniciando para proposta ID={}", proposta.getId());
        ComissaoModel comissao = comissaoRepository.findByPropostaId(proposta.getId())
                .stream().findFirst().orElse(new ComissaoModel());

        if (proposta.getComissaoEstimada() != null && proposta.getComissaoEstimada().compareTo(BigDecimal.ZERO) > 0) {
            LocalDateTime agora = LocalDateTime.now();
            log.trace("[PROPOSTA_SERVICE] Referência temporal: {}", agora);

            comissao.setProposta(proposta);
            comissao.setUsuario(proposta.getUsuario());
            comissao.setValorBrutoComissao(proposta.getComissaoEstimada());
            comissao.setValorLiquidoConsultor(proposta.getComissaoEstimada());

            comissao.setDataRecebimentoBanco(CicloFinanceiroUtils.obterQuartaDeFechamento(agora));
            comissao.setPrevisaoPagamento(CicloFinanceiroUtils.calcularSextaDePagamento(agora).toLocalDate());
            comissao.setCicloReferencia(CicloFinanceiroUtils.identificarCiclo(agora));
            comissao.setDataLimiteContestacao(CicloFinanceiroUtils.calcularLimiteContestacao(agora));
            comissao.setStatusPagamento("Pendente");
            proposta.setValorFinalCliente(proposta.getValorAprovado());

            comissaoRepository.save(comissao);
            log.info(
                    "[PROPOSTA_SERVICE] Ciclo de pagamento processado para proposta ID={}, comissão ID={}, cicloReferencia={}",
                    proposta.getId(), comissao.getId(), comissao.getCicloReferencia());
        } else {
            log.warn("[PROPOSTA_SERVICE] Comissão estimada para proposta ID={} é zero ou nula, ciclo não processado",
                    proposta.getId());
        }
    }

    public PropostaModel buscarPorId(Long id) {
        log.debug("[PROPOSTA_SERVICE] buscarPorId: Buscando proposta ID={}", id);
        PropostaModel proposta = propostaRepository.findById(id).orElse(null);
        if (proposta == null) {
            log.warn("[PROPOSTA_SERVICE] Proposta ID={} não encontrada", id);
        } else {
            log.trace("[PROPOSTA_SERVICE] Proposta ID={} encontrada", id);
        }
        return proposta;
    }

    @Transactional
    public void excluirProposta(Long id) {
        log.info("[PROPOSTA_SERVICE] excluirProposta: Excluindo proposta ID={}", id);
        propostaRepository.deleteById(id);
        log.debug("[PROPOSTA_SERVICE] Publicando evento PropostaExcluidaEvent para ID={}", id);
        eventPublisher.publishEvent(new PropostaExcluidaEvent(id));
    }

    @Transactional(readOnly = true)
    public PropostaModel carregarPropostaDetalhada(Long id) {
        log.debug("[PROPOSTA_SERVICE] carregarPropostaDetalhada: Carregando proposta ID={} com detalhes", id);
        PropostaModel proposta = propostaRepository.findByIdWithDetails(id).orElse(null);
        if (proposta == null) {
            log.warn("[PROPOSTA_SERVICE] Proposta detalhada ID={} não encontrada", id);
        } else {
            log.info("[PROPOSTA_SERVICE] Proposta detalhada ID={} carregada com sucesso", id);
        }
        return proposta;
    }

    public PropostaModel converterRascunhoParaProposta(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado,
            ProponenteModel cliente) {
        if (resultado == null || resultado.tabela() == null || resultado.tabela().getBanco() == null) {
            log.error("[PROPOSTA_SERVICE] converterRascunhoParaProposta: Resultado ou tabela de simulação inválido.");
            throw new IllegalArgumentException("Resultado de simulação inválido para conversão de rascunho em proposta.");
        }

        if (cliente == null) {
            log.error("[PROPOSTA_SERVICE] converterRascunhoParaProposta: Cliente nulo.");
            throw new IllegalArgumentException("Cliente inválido para conversão de rascunho em proposta.");
        }

        log.info("[PROPOSTA_SERVICE] Montando rascunho de proposta em memória para o cliente: {}", cliente.getNomeCompleto());

        PropostaModel novaProposta = new PropostaModel();
        novaProposta.setProponente(cliente);
        novaProposta.setBanco(resultado.tabela().getBanco());
        novaProposta.setTabelaId(resultado.tabela().getId());

        novaProposta.setValorSolicitado(rascunho.valorDesejado());
        novaProposta.setPrazoDesejado(rascunho.prazoDesejado());

        try {
            novaProposta.setConvenioOrgao(TipoConvenioModel.valueOf(rascunho.tipoConvenio()));
        } catch (Exception e) {
            novaProposta.setConvenioOrgao(TipoConvenioModel.PADRAO);
        }

        novaProposta.setStatus(StatusPropostaModel.DIGITADA);
        novaProposta.setObservacoes("✨ Proposta originada automaticamente via Copiloto de Vendas.");
        novaProposta.setUsuario(authService.getUsuarioLogado());

        // NÃO SALVAMOS NO BANCO AQUI. Retornamos apenas o objeto em memória.
        log.debug("[PROPOSTA_SERVICE] Rascunho de proposta montado com sucesso.");
        return novaProposta;
    }

}