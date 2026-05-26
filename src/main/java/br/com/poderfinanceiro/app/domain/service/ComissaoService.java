package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import br.com.poderfinanceiro.app.domain.event.ComissaoCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaPagaEvent;
import br.com.poderfinanceiro.app.domain.event.ComissaoAtualizadaEvent;

@Service
public class ComissaoService {

        private static final Logger log = LoggerFactory.getLogger(ComissaoService.class);

        private final ComissaoRepository repository;
        private final ApplicationEventPublisher eventPublisher;

        public ComissaoService(ComissaoRepository repository, ApplicationEventPublisher eventPublisher) {
                this.repository = repository;
                this.eventPublisher = eventPublisher;
                log.debug("[COMISSAO_SERVICE] Construtor: Serviço de comissão instanciado");
        }

        @EventListener
        @Transactional
        public void onPropostaPaga(PropostaPagaEvent event) {
                log.info("[COMISSAO_SERVICE] Capturou PropostaPagaEvent para Proposta ID={}", event.proposta().getId());
                gerarOuAtualizarComissaoParaProposta(event.proposta());
        }

        @Transactional
        public ComissaoModel gerarOuAtualizarComissaoParaProposta(PropostaModel proposta) {
                List<ComissaoModel> existentes = repository.findByPropostaId(proposta.getId());

                // Se já existir, ATUALIZA (Disparando o ComissaoAtualizadaEvent para a UI)
                if (!existentes.isEmpty()) {
                        ComissaoModel comissao = existentes.get(0);
                        log.info("[COMISSAO_SERVICE] Comissão já existe (ID={}). Atualizando valores...",
                                        comissao.getId());

                        comissao.setValorBrutoComissao(
                                        proposta.getComissaoEstimada() != null ? proposta.getComissaoEstimada()
                                                        : BigDecimal.ZERO);
                        // Você pode adicionar outras regras de atualização aqui se necessário

                        ComissaoModel salva = repository.save(comissao);
                        eventPublisher.publishEvent(new ComissaoAtualizadaEvent(salva.getId()));
                        return salva;
                }

                // Se não existir, CRIA NOVA (Disparando o ComissaoCriadaEvent para a UI)
                ComissaoModel comissao = new ComissaoModel();
                comissao.setProposta(proposta);
                comissao.setUsuario(proposta.getUsuario());
                comissao.setValorBrutoComissao(proposta.getComissaoEstimada() != null ? proposta.getComissaoEstimada()
                                : BigDecimal.ZERO);
                comissao.setValorLiquidoConsultor(comissao.getValorBrutoComissao());
                comissao.setStatusPagamento("Pendente");

                return gerarNovaComissao(comissao);
        }

        @Transactional
        public ComissaoModel gerarNovaComissao(ComissaoModel novaComissao) {
                if (novaComissao == null) {
                        log.warn("[COMISSAO_SERVICE] gerarNovaComissao: Tentativa de criar comissão nula");
                        throw new IllegalArgumentException("Comissão não pode ser nula");
                }

                log.debug("[COMISSAO_SERVICE] gerarNovaComissao: Criando nova comissão para proposta ID={}",
                                novaComissao.getProposta() != null ? novaComissao.getProposta().getId() : "null");

                LocalDateTime agora = LocalDateTime.now();

                novaComissao.setCicloReferencia(CicloFinanceiroUtils.identificarCiclo(agora));
                novaComissao.setDataLimiteContestacao(CicloFinanceiroUtils.calcularLimiteContestacao(agora));

                ComissaoModel salva = repository.save(novaComissao);
                eventPublisher.publishEvent(new ComissaoCriadaEvent(salva.getId()));
                log.info(
                                "[COMISSAO_SERVICE] gerarNovaComissao: Nova comissão criada com ID={}, cicloReferencia={}, limiteContestacao={}",
                                salva.getId(), salva.getCicloReferencia(), salva.getDataLimiteContestacao());
                return salva;
        }

        @Transactional
        public ComissaoModel salvarConciliacao(ComissaoModel comissao) {
                log.debug("[COMISSAO_SERVICE] salvarConciliacao: Iniciando conciliação para comissão ID={}, status atual={}",
                                comissao.getId(), comissao.getStatusPagamento());
                LocalDateTime agora = LocalDateTime.now();

                if ("Pago".equalsIgnoreCase(comissao.getStatusPagamento())
                                || "Liquidado".equalsIgnoreCase(comissao.getStatusPagamento())) {
                        log.warn(
                                        "[COMISSAO_SERVICE] salvarConciliacao: Tentativa de modificar comissão liquidada/paga - ID={}, status={}",
                                        comissao.getId(), comissao.getStatusPagamento());
                        throw new IllegalStateException(
                                        "Este ciclo financeiro já foi liquidado. O registro é imutável.");
                }

                if (comissao.getDataLimiteContestacao() != null && agora.isAfter(comissao.getDataLimiteContestacao())) {
                        log.info("[COMISSAO_SERVICE] salvarConciliacao: Prazo de contestação expirado em {} para comissão ID={}",
                                        comissao.getDataLimiteContestacao(), comissao.getId());

                        if (!comissao.isVerificadoConsultor()) {
                                comissao.setVerificadoConsultor(true);
                                String obsAtual = comissao.getObservacaoAjuste() != null
                                                ? comissao.getObservacaoAjuste() + " | "
                                                : "";
                                comissao.setObservacaoAjuste(
                                                obsAtual + "[Auditoria do Sistema: Conferência automática aplicada por decurso de prazo.]");
                                log.debug("[COMISSAO_SERVICE] salvarConciliacao: Conferência automática aplicada para comissão ID={}",
                                                comissao.getId());
                        } else {
                                log.trace("[COMISSAO_SERVICE] salvarConciliacao: Comissão ID={} já verificada, apenas salvando",
                                                comissao.getId());
                        }
                } else {
                        log.trace("[COMISSAO_SERVICE] salvarConciliacao: Prazo de contestação vigente até {}",
                                        comissao.getDataLimiteContestacao());
                }

                ComissaoModel salva = repository.save(comissao);
                eventPublisher.publishEvent(new ComissaoAtualizadaEvent(salva.getId()));
                log.info("[COMISSAO_SERVICE] salvarConciliacao: Comissão ID={} salva com status={}, verificadoConsultor={}",
                                salva.getId(), salva.getStatusPagamento(), salva.isVerificadoConsultor());
                return salva;
        }

        @Transactional(readOnly = true)
        public List<ComissaoModel> listarTodasComDetalhes() {
                return repository.findAllComDetalhes();
        }

        @Transactional(readOnly = true)
        public ComissaoModel buscarPorId(Long id) {
                return repository.findById(id).orElse(null);
        }
}