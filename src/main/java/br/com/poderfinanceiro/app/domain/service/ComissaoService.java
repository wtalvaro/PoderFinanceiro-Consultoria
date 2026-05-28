package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.common.util.CicloFinanceiroUtils;
import br.com.poderfinanceiro.app.domain.event.ComissaoAtualizadaEvent;
import br.com.poderfinanceiro.app.domain.event.ComissaoCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaPagaEvent;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de Domínio para Gestão de Comissões e Conciliação Financeira.
 * Responsável por orquestrar o ciclo de vida dos repasses, garantindo a
 * imutabilidade de registros liquidados e a automação de prazos de contestação.
 */
@Service
@Transactional(readOnly = true)
public class ComissaoService {

        private static final Logger log = LoggerFactory.getLogger(ComissaoService.class);
        private static final String LOG_PREFIX = "[ComissaoService]";

        private final ComissaoRepository repository;
        private final ApplicationEventPublisher eventPublisher;

        public ComissaoService(ComissaoRepository repository, ApplicationEventPublisher eventPublisher) {
                this.repository = repository;
                this.eventPublisher = eventPublisher;
                log.info("{} [SISTEMA] Serviço de comissões inicializado com motor de conciliação.", LOG_PREFIX);
        }

        /**
         * Listener reativo para propostas que atingiram o status PAGO. Garante
         * que a comissão seja gerada ou atualizada automaticamente.
         */
        @EventListener @Transactional public void onPropostaPaga(PropostaPagaEvent event) {
                log.info("{} [TELEMETRIA] Capturado evento de Proposta Paga. ID: {}", LOG_PREFIX,
                                event.proposta().getId());
                gerarOuAtualizarComissaoParaProposta(event.proposta());
        }

        /**
         * Orquestra a criação ou atualização de uma comissão baseada nos dados
         * da proposta.
         */
        @Transactional public ComissaoModel gerarOuAtualizarComissaoParaProposta(PropostaModel proposta) {
                log.debug("{} [NEGOCIO] Sincronizando comissão para proposta ID: {}", LOG_PREFIX, proposta.getId());

                List<ComissaoModel> existentes = repository.findByPropostaId(proposta.getId());

                if (!existentes.isEmpty()) {
                        ComissaoModel comissao = existentes.get(0);
                        log.info("{} [NEGOCIO] Comissão já existente (ID: {}). Atualizando valores financeiros.",
                                        LOG_PREFIX, comissao.getId());

                        comissao.setValorBrutoComissao(
                                        proposta.getComissaoEstimada() != null ? proposta.getComissaoEstimada()
                                                        : BigDecimal.ZERO);
                        comissao.setValorLiquidoConsultor(comissao.getValorBrutoComissao());

                        ComissaoModel salva = repository.save(comissao);
                        eventPublisher.publishEvent(new ComissaoAtualizadaEvent(salva.getId()));
                        return salva;
                }

                // Fluxo de criação de nova comissão
                ComissaoModel novaComissao = new ComissaoModel();
                novaComissao.setProposta(proposta);
                novaComissao.setUsuario(proposta.getUsuario());
                novaComissao.setValorBrutoComissao(
                                proposta.getComissaoEstimada() != null ? proposta.getComissaoEstimada()
                                                : BigDecimal.ZERO);
                novaComissao.setValorLiquidoConsultor(novaComissao.getValorBrutoComissao());
                novaComissao.setStatusPagamento("Pendente");

                return gerarNovaComissao(novaComissao);
        }

        /**
         * Persiste uma nova comissão definindo os metadados de ciclo
         * financeiro.
         */
        @Transactional public ComissaoModel gerarNovaComissao(ComissaoModel novaComissao) {
                log.info("{} [TELEMETRIA] Iniciando criação de novo registro de comissão.", LOG_PREFIX);

                if (novaComissao == null) {
                        log.error("{} [NEGOCIO] Falha: Tentativa de gerar comissão nula.", LOG_PREFIX);
                        throw new IllegalArgumentException("A comissão não pode ser nula.");
                }

                LocalDateTime agora = LocalDateTime.now();
                novaComissao.setCicloReferencia(CicloFinanceiroUtils.identificarCiclo(agora));
                novaComissao.setDataLimiteContestacao(CicloFinanceiroUtils.calcularLimiteContestacao(agora));

                try {
                        ComissaoModel salva = repository.save(novaComissao);
                        log.info("{} [AUDITORIA] Nova comissão gerada com sucesso. ID: {}, Ciclo: {}", LOG_PREFIX,
                                        salva.getId(), salva.getCicloReferencia());

                        eventPublisher.publishEvent(new ComissaoCriadaEvent(salva.getId()));
                        return salva;
                } catch (Exception e) {
                        log.error("{} [SISTEMA] Erro ao persistir nova comissão: {}", LOG_PREFIX, e.getMessage());
                        throw e;
                }
        }

        /**
         * Realiza a conciliação financeira, aplicando regras de imutabilidade e
         * auditoria automática.
         */
        @Transactional public ComissaoModel salvarConciliacao(ComissaoModel comissao) {
                log.info("{} [TELEMETRIA] Iniciando processo de conciliação para comissão ID: {}", LOG_PREFIX,
                                comissao.getId());

                LocalDateTime agora = LocalDateTime.now();

                // 1. Regra de Ouro: Imutabilidade de Ciclos Liquidados
                if ("Pago".equalsIgnoreCase(comissao.getStatusPagamento())
                                || "Liquidado".equalsIgnoreCase(comissao.getStatusPagamento())) {
                        log.warn("{} [NEGOCIO] Bloqueio de alteração: Comissão ID {} já está liquidada.", LOG_PREFIX,
                                        comissao.getId());
                        throw new IllegalStateException(
                                        "Este ciclo financeiro já foi liquidado e não permite alterações.");
                }

                // 2. Automação de Conferência por Decurso de Prazo
                if (comissao.getDataLimiteContestacao() != null && agora.isAfter(comissao.getDataLimiteContestacao())) {
                        if (!comissao.isVerificadoConsultor()) {
                                log.info("{} [AUDITORIA] Aplicando conferência automática por decurso de prazo. ID: {}",
                                                LOG_PREFIX, comissao.getId());
                                comissao.setVerificadoConsultor(true);
                                String rastro = comissao.getObservacaoAjuste() != null
                                                ? comissao.getObservacaoAjuste() + " | "
                                                : "";
                                comissao.setObservacaoAjuste(rastro
                                                + "[Auditoria: Conferência automática aplicada por decurso de prazo.]");
                        }
                }

                try {
                        ComissaoModel salva = repository.save(comissao);
                        log.info("{} [AUDITORIA] Conciliação salva com sucesso. ID: {}, Status: {}", LOG_PREFIX,
                                        salva.getId(), salva.getStatusPagamento());

                        eventPublisher.publishEvent(new ComissaoAtualizadaEvent(salva.getId()));
                        return salva;
                } catch (Exception e) {
                        log.error("{} [SISTEMA] Falha crítica na conciliação da comissão ID {}: {}", LOG_PREFIX,
                                        comissao.getId(), e.getMessage());
                        throw e;
                }
        }

        /**
         * Recupera todas as comissões com carregamento otimizado de
         * relacionamentos.
         */
        public List<ComissaoModel> listarTodasComDetalhes() {
                log.trace("{} [TELEMETRIA] Listagem completa de comissões solicitada.", LOG_PREFIX);
                return repository.findAllComDetalhes();
        }

        /**
         * Busca uma comissão específica pelo identificador.
         */
        public Optional<ComissaoModel> buscarPorId(Long id) {
                log.trace("{} [TELEMETRIA] Buscando comissão por ID: {}", LOG_PREFIX, id);
                return repository.findById(id);
        }
}
