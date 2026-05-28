package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.PropostaAtualizadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaExcluidaEvent;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.infrastructure.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.infrastructure.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.infrastructure.repository.PropostaRepository;
import br.com.poderfinanceiro.app.infrastructure.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de Domínio para Gestão de Propostas de Crédito. Orquestra o ciclo de
 * vida da esteira, cálculos de comissionamento e integração com o fluxo de
 * caixa do consultor.
 */
@Service
@Transactional(readOnly = true)
public class PropostaService {

    private static final Logger log = LoggerFactory.getLogger(PropostaService.class);
    private static final String LOG_PREFIX = "[PropostaService]";

    private final PropostaRepository propostaRepository;
    private final TabelaJurosRepository tabelaJurosRepository;
    private final ComissaoRepository comissaoRepository;
    private final DocumentoProponenteRepository documentoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthService authService;

    public PropostaService(PropostaRepository propostaRepository, TabelaJurosRepository tabelaJurosRepository,
            ComissaoRepository comissaoRepository, DocumentoProponenteRepository documentoRepository,
            ApplicationEventPublisher eventPublisher, AuthService authService) {
        this.propostaRepository = propostaRepository;
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.comissaoRepository = comissaoRepository;
        this.documentoRepository = documentoRepository;
        this.eventPublisher = eventPublisher;
        this.authService = authService;
        log.info("{} [SISTEMA] Serviço de Propostas inicializado com motor de ciclo financeiro.", LOG_PREFIX);
    }

    /**
     * Calcula a comissão estimada baseada na tabela de juros selecionada.
     * Utiliza precisão de 4 casas decimais para cálculos intermediários.
     */
    public BigDecimal calcularComissaoEstimada(BigDecimal valorAprovado, Long tabelaId) {
        log.trace("{} [TELEMETRIA] Calculando comissão estimada. Valor: {}, Tabela: {}", LOG_PREFIX, valorAprovado,
                tabelaId);

        if (valorAprovado == null || valorAprovado.compareTo(BigDecimal.ZERO) <= 0 || tabelaId == null) {
            log.warn("{} [NEGOCIO] Parâmetros inválidos para cálculo de comissão.", LOG_PREFIX);
            return BigDecimal.ZERO;
        }

        return tabelaJurosRepository.findById(tabelaId).map(tabela -> {
            BigDecimal percentual = tabela.getComissaoPercentual().divide(BigDecimal.valueOf(100), 4,
                    RoundingMode.HALF_UP);
            BigDecimal resultado = valorAprovado.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
            log.debug("{} [NEGOCIO] Comissão calculada: {} para a tabela {}", LOG_PREFIX, resultado,
                    tabela.getNomeTabela());
            return resultado;
        }).orElseGet(() -> {
            log.warn("{} [NEGOCIO] Tabela ID {} não encontrada para cálculo.", LOG_PREFIX, tabelaId);
            return BigDecimal.ZERO;
        });
    }

    /**
     * Salva ou atualiza uma proposta, disparando o ciclo financeiro se o status
     * for PAGO.
     */
    @Transactional public PropostaModel salvarProposta(PropostaModel proposta) {
        log.info("{} [TELEMETRIA] Iniciando persistência de proposta.", LOG_PREFIX);

        if (proposta == null) {
            log.error("{} [NEGOCIO] Tentativa de salvar proposta nula.", LOG_PREFIX);
            throw new IllegalArgumentException("Proposta não pode ser nula.");
        }

        // CAPTURA DE ESTADO PRÉ-PERSISTÊNCIA (Fix para Dead Code Warning)
        // Justificativa: Capturamos se o ID é nulo antes do save(), pois o
        // Hibernate populará o ID
        // automaticamente no objeto 'proposta' após a persistência.
        final boolean isNovoRegistro = (proposta.getId() == null);

        // 1. Garantir Vínculo com Consultor Logado
        if (proposta.getUsuario() == null) {
            proposta.setUsuario(authService.getUsuarioLogado());
            log.debug("{} [SISTEMA] Associando consultor logado à proposta.", LOG_PREFIX);
        }

        // 2. Sincronizar Regras de Negócio da Tabela (Convênio e Comissão)
        sincronizarDadosTabela(proposta);

        // 3. Persistência no Banco de Dados
        PropostaModel salva = propostaRepository.save(proposta);
        log.info("{} [AUDITORIA] Proposta {} com sucesso. ID: {}, Status: {}", LOG_PREFIX,
                isNovoRegistro ? "CRIADA" : "ATUALIZADA", salva.getId(), salva.getStatus());

        // 4. Gatilho de Ciclo Financeiro (Apenas se PAGO)
        if (salva.getStatus() == StatusPropostaModel.PAGO) {
            log.info("{} [NEGOCIO] Status PAGO detectado. Orquestrando ciclo de pagamento.", LOG_PREFIX);
            processarCicloPagamento(salva);
        }

        // 5. Notificação de Eventos para Sincronização de UI
        if (isNovoRegistro) {
            log.trace("{} [SISTEMA] Disparando evento de criação para ID: {}", LOG_PREFIX, salva.getId());
            eventPublisher.publishEvent(new PropostaCriadaEvent(salva.getId()));
        } else {
            log.trace("{} [SISTEMA] Disparando evento de atualização para ID: {}", LOG_PREFIX, salva.getId());
            eventPublisher.publishEvent(new PropostaAtualizadaEvent(salva.getId()));
        }

        return salva;
    }

    /**
     * Sobrecarga para salvar proposta com associação em lote de documentos.
     */
    @Transactional public PropostaModel salvarProposta(PropostaModel proposta,
            List<DocumentoProponenteModel> documentos) {
        log.debug("{} [TELEMETRIA] Salvando proposta com lote de {} documentos.", LOG_PREFIX,
                documentos != null ? documentos.size() : 0);
        PropostaModel salva = this.salvarProposta(proposta);

        if (documentos != null && !documentos.isEmpty()) {
            documentos.forEach(doc -> {
                doc.setProposta(salva);
                documentoRepository.save(doc);
                log.trace("{} [SISTEMA] Documento ID {} vinculado à proposta ID {}", LOG_PREFIX, doc.getId(),
                        salva.getId());
            });
            log.info("{} [AUDITORIA] {} documentos vinculados com sucesso à proposta ID: {}", LOG_PREFIX,
                    documentos.size(), salva.getId());
        }
        return salva;
    }

    /**
     * Exclui uma proposta e notifica os interessados via evento.
     */
    @Transactional public void excluirProposta(Long id) {
        log.info("{} [TELEMETRIA] Solicitada exclusão da proposta ID: {}", LOG_PREFIX, id);
        if (id == null)
            return;

        try {
            propostaRepository.deleteById(id);
            log.info("{} [AUDITORIA] Proposta ID {} removida permanentemente.", LOG_PREFIX, id);
            eventPublisher.publishEvent(new PropostaExcluidaEvent(id));
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha crítica ao excluir proposta ID {}: {}", LOG_PREFIX, id, e.getMessage());
            throw e;
        }
    }

    /**
     * Carrega uma proposta com todos os relacionamentos (Eager Loading via
     * Repository).
     */
    public PropostaModel carregarPropostaDetalhada(Long id) {
        log.trace("{} [TELEMETRIA] Carregando detalhes completos da proposta ID: {}", LOG_PREFIX, id);
        return propostaRepository.findByIdWithDetails(id).orElseGet(() -> {
            log.warn("{} [NEGOCIO] Proposta detalhada ID {} não localizada.", LOG_PREFIX, id);
            return null;
        });
    }

    /**
     * Converte um rascunho de simulação em um objeto Proposta pronto para
     * edição. Operação puramente em memória (Factory Method).
     */
    public PropostaModel converterRascunhoParaProposta(SimulacaoRascunhoDTO rascunho, ResultadoSimulacaoDTO resultado,
            ProponenteModel cliente) {

        log.info("{} [NEGOCIO] Convertendo rascunho de simulação para proposta. Cliente: {}", LOG_PREFIX,
                cliente != null ? cliente.getNomeCompleto() : "NULL");

        if (resultado == null || resultado.tabela() == null || cliente == null) {
            log.error("{} [NEGOCIO] Falha na conversão: Dados de simulação ou cliente ausentes.", LOG_PREFIX);
            throw new IllegalArgumentException("Dados insuficientes para conversão de rascunho.");
        }

        PropostaModel nova = new PropostaModel();
        nova.setProponente(cliente);
        nova.setBanco(resultado.tabela().getBanco());
        nova.setTabelaId(resultado.tabela().getId());
        nova.setValorSolicitado(rascunho.valorDesejado());
        nova.setPrazoDesejado(rascunho.prazoDesejado());
        nova.setStatus(StatusPropostaModel.DIGITADA);
        nova.setObservacoes("✨ Proposta originada automaticamente via Copiloto de Vendas.");
        nova.setUsuario(authService.getUsuarioLogado());

        try {
            nova.setConvenioOrgao(TipoConvenioModel.valueOf(rascunho.tipoConvenio()));
        } catch (Exception e) {
            log.warn("{} [SISTEMA] Convênio inválido no rascunho. Usando PADRAO.", LOG_PREFIX);
            nova.setConvenioOrgao(TipoConvenioModel.PADRAO);
        }

        log.debug("{} [NEGOCIO] Rascunho de proposta montado com sucesso em memória.", LOG_PREFIX);
        return nova;
    }

    /**
     * Sincroniza dados da tabela de juros com a proposta (Comissão e Convênio).
     */
    private void sincronizarDadosTabela(PropostaModel proposta) {
        log.trace("{} [SISTEMA] Sincronizando dados da tabela para proposta ID: {}", LOG_PREFIX, proposta.getId());
        if (proposta.getTabelaId() != null) {
            tabelaJurosRepository.findById(proposta.getTabelaId()).ifPresentOrElse(tabela -> {
                proposta.setConvenioOrgao(tabela.getTipoConvenio());
                BigDecimal base = proposta.getValorAprovado() != null ? proposta.getValorAprovado()
                        : proposta.getValorSolicitado();
                proposta.setComissaoEstimada(calcularComissaoEstimada(base, tabela.getId()));
                log.debug("{} [SISTEMA] Sincronização concluída: Tabela={}, Comissão={}", LOG_PREFIX,
                        tabela.getNomeTabela(), proposta.getComissaoEstimada());
            }, () -> log.warn("{} [SISTEMA] Tabela ID {} não encontrada para sincronização.", LOG_PREFIX,
                    proposta.getTabelaId()));
        }
    }

    /**
     * Processa o ciclo financeiro de pagamento (Quarta/Quinta/Sexta).
     */
    private void processarCicloPagamento(PropostaModel proposta) {
        log.debug("{} [NEGOCIO] Processando ciclo financeiro para proposta ID: {}", LOG_PREFIX, proposta.getId());

        ComissaoModel comissao = comissaoRepository.findByPropostaId(proposta.getId()).stream().findFirst()
                .orElse(new ComissaoModel());

        if (proposta.getComissaoEstimada() != null && proposta.getComissaoEstimada().compareTo(BigDecimal.ZERO) > 0) {
            LocalDateTime agora = LocalDateTime.now();

            comissao.setProposta(proposta);
            comissao.setUsuario(proposta.getUsuario());
            comissao.setValorBrutoComissao(proposta.getComissaoEstimada());
            comissao.setValorLiquidoConsultor(proposta.getComissaoEstimada());

            // Regras de Ciclo via Utils
            comissao.setDataRecebimentoBanco(CicloFinanceiroUtils.obterQuartaDeFechamento(agora));
            comissao.setPrevisaoPagamento(CicloFinanceiroUtils.calcularSextaDePagamento(agora).toLocalDate());
            comissao.setCicloReferencia(CicloFinanceiroUtils.identificarCiclo(agora));
            comissao.setDataLimiteContestacao(CicloFinanceiroUtils.calcularLimiteContestacao(agora));
            comissao.setStatusPagamento("Pendente");

            proposta.setValorFinalCliente(proposta.getValorAprovado());

            comissaoRepository.save(comissao);
            log.info("{} [AUDITORIA] Ciclo financeiro registrado. Previsão de pagamento: {}", LOG_PREFIX,
                    comissao.getPrevisaoPagamento());
        } else {
            log.warn("{} [NEGOCIO] Ciclo financeiro ignorado: Comissão estimada é zero ou nula.", LOG_PREFIX);
        }
    }

    /**
     * Busca uma proposta simples pelo ID.
     */
    public PropostaModel buscarPorId(Long id) {
        log.trace("{} [TELEMETRIA] Buscando proposta ID: {}", LOG_PREFIX, id);
        return propostaRepository.findById(id).orElse(null);
    }
}
