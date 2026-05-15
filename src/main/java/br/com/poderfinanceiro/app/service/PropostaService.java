package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.ComissaoModel;
import br.com.poderfinanceiro.app.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.model.PropostaModel;
import br.com.poderfinanceiro.app.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.utils.CicloFinanceiroUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final TabelaJurosRepository tabelaJurosRepository;
    private final ComissaoRepository comissaoRepository;
    private final DocumentoProponenteRepository documentoRepository;

    public PropostaService(PropostaRepository propostaRepository,
            TabelaJurosRepository tabelaJurosRepository,
            ComissaoRepository comissaoRepository,
            DocumentoProponenteRepository documentoRepository) {
        this.propostaRepository = propostaRepository;
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.comissaoRepository = comissaoRepository;
        this.documentoRepository = documentoRepository;
    }

    /**
     * Calcula o repasse baseado na tabela de juros selecionada.
     */
    public BigDecimal calcularComissaoEstimada(BigDecimal valorAprovado, Long tabelaId) {
        if (valorAprovado == null || valorAprovado.compareTo(BigDecimal.ZERO) <= 0 || tabelaId == null) {
            return BigDecimal.ZERO;
        }

        return tabelaJurosRepository.findById(tabelaId)
                .map(tabela -> {
                    BigDecimal percentual = tabela.getComissaoPercentual()
                            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    return valorAprovado.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
                })
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public PropostaModel salvarProposta(PropostaModel proposta) {
        // 1. Sincronização de metadados da Tabela de Juros
        sincronizarDadosTabela(proposta);

        // 2. Persistência da Proposta
        PropostaModel propostaSalva = propostaRepository.save(proposta);

        // 3. Gatilho de Ciclo Financeiro (Liquidação)
        if (propostaSalva.getStatus() == StatusPropostaModel.PAGO) {
            processarCicloPagamento(propostaSalva);
        }

        return propostaSalva;
    }

    @Transactional
    public PropostaModel salvarProposta(PropostaModel proposta, List<DocumentoProponenteModel> documentos) {
        PropostaModel propostaSalva = this.salvarProposta(proposta);

        if (documentos != null && !documentos.isEmpty()) {
            documentos.forEach(doc -> {
                doc.setProposta(propostaSalva);
                documentoRepository.save(doc);
            });
        }
        return propostaSalva;
    }

    /**
     * SRP: Isola a lógica de sincronização entre Proposta e Tabela de Juros.
     */
    private void sincronizarDadosTabela(PropostaModel proposta) {
        if (proposta.getTabelaId() != null) {
            TabelaJurosModel tabela = tabelaJurosRepository.findById(proposta.getTabelaId()).orElse(null);
            if (tabela != null) {
                // Herda o convênio da tabela para a proposta
                proposta.setConvenioOrgao(tabela.getTipoConvenio());

                BigDecimal baseCalculo = proposta.getValorAprovado() != null ? proposta.getValorAprovado()
                        : proposta.getValorSolicitado();

                proposta.setComissaoEstimada(calcularComissaoEstimada(baseCalculo, tabela.getId()));
            }
        }
    }

    /**
     * SRP: Gerencia o Ciclo de Pagamento (Quarta/Quinta/Sexta).
     */
    /**
     * SRP: Gerencia o Ciclo de Pagamento (Quarta/Quinta/Sexta).
     */
    private void processarCicloPagamento(PropostaModel proposta) {
        ComissaoModel comissao = comissaoRepository.findByPropostaId(proposta.getId())
                .stream().findFirst().orElse(new ComissaoModel());

        if (proposta.getComissaoEstimada() != null && proposta.getComissaoEstimada().compareTo(BigDecimal.ZERO) > 0) {
            LocalDateTime agora = LocalDateTime.now(); // Ponto de referência único

            comissao.setProposta(proposta);
            comissao.setUsuario(proposta.getUsuario());
            comissao.setValorBrutoComissao(proposta.getComissaoEstimada());
            comissao.setValorLiquidoConsultor(proposta.getComissaoEstimada());

            // --- CALIBRAÇÃO DO CICLO ---

            // ✅ CORREÇÃO: Em vez de .now(), pegamos a Quarta de Fechamento do Ciclo
            comissao.setDataRecebimentoBanco(CicloFinanceiroUtils.obterQuartaDeFechamento(agora));

            // Marco 3: Previsão de Pagamento (Sexta-feira seguinte à quarta de fechamento)
            comissao.setPrevisaoPagamento(
                    CicloFinanceiroUtils.calcularSextaDePagamento(agora).toLocalDate());

            comissao.setCicloReferencia(CicloFinanceiroUtils.identificarCiclo(agora));
            comissao.setDataLimiteContestacao(CicloFinanceiroUtils.calcularLimiteContestacao(agora));

            // ✅ A CORREÇÃO: A comissão nasce "Pendente" aguardando o ciclo correr
            comissao.setStatusPagamento("Pendente");
            proposta.setValorFinalCliente(proposta.getValorAprovado());

            comissaoRepository.save(comissao);
        }
    }
}