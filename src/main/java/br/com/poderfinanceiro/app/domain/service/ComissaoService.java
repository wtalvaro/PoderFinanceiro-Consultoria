package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ComissaoService {

    private final ComissaoRepository repository;

    // Construtor limpo: Sem necessidade de AuthService pois não há hierarquia de
    // usuários
    public ComissaoService(ComissaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ComissaoModel gerarNovaComissao(ComissaoModel novaComissao) {
        LocalDateTime agora = LocalDateTime.now();

        // Carimba o DNA temporal da comissão no momento da criação
        novaComissao.setCicloReferencia(CicloFinanceiroUtils.identificarCiclo(agora));
        novaComissao.setDataLimiteContestacao(CicloFinanceiroUtils.calcularLimiteContestacao(agora));

        return repository.save(novaComissao);
    }

    @Transactional
    public ComissaoModel salvarConciliacao(ComissaoModel comissao) {
        LocalDateTime agora = LocalDateTime.now();

        // 🛡️ TRAVA 1: Imutabilidade (O fim da linha)
        if ("Pago".equalsIgnoreCase(comissao.getStatusPagamento())
                || "Liquidado".equalsIgnoreCase(comissao.getStatusPagamento())) {
            throw new IllegalStateException("Este ciclo financeiro já foi liquidado. O registro é imutável.");
        }

        // 🛡️ TRAVA 2: Anuência Tácita (Com Proteção Null)
        if (comissao.getDataLimiteContestacao() != null && agora.isAfter(comissao.getDataLimiteContestacao())) {

            if (!comissao.isVerificadoConsultor()) {
                comissao.setVerificadoConsultor(true); // Confirma automaticamente

                String obsAtual = comissao.getObservacaoAjuste() != null ? comissao.getObservacaoAjuste() + " | " : "";
                comissao.setObservacaoAjuste(
                        obsAtual + "[Auditoria do Sistema: Conferência automática aplicada por decurso de prazo.]");
            }
        }

        return repository.save(comissao);
    }
}