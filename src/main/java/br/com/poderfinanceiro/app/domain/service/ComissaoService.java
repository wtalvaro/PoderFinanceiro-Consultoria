package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service
public class ComissaoService {

    private static final Logger log = LoggerFactory.getLogger(ComissaoService.class);

    private final ComissaoRepository repository;

    public ComissaoService(ComissaoRepository repository) {
        this.repository = repository;
        log.debug("[COMISSAO_SERVICE] Construtor: Serviço de comissão instanciado");
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
            throw new IllegalStateException("Este ciclo financeiro já foi liquidado. O registro é imutável.");
        }

        if (comissao.getDataLimiteContestacao() != null && agora.isAfter(comissao.getDataLimiteContestacao())) {
            log.info("[COMISSAO_SERVICE] salvarConciliacao: Prazo de contestação expirado em {} para comissão ID={}",
                    comissao.getDataLimiteContestacao(), comissao.getId());

            if (!comissao.isVerificadoConsultor()) {
                comissao.setVerificadoConsultor(true);
                String obsAtual = comissao.getObservacaoAjuste() != null ? comissao.getObservacaoAjuste() + " | " : "";
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
        log.info("[COMISSAO_SERVICE] salvarConciliacao: Comissão ID={} salva com status={}, verificadoConsultor={}",
                salva.getId(), salva.getStatusPagamento(), salva.isVerificadoConsultor());
        return salva;
    }
}