package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.PropostaAtualizadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaExcluidaEvent;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.DocumentoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.domain.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

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
        // 🚀 Descobre se é um insert ou update antes de salvar
        boolean isNovo = proposta.getId() == null;
        
        // 🚀 Garantia de integridade: se não tem usuário, define o logado
        if (proposta.getUsuario() == null) {
            proposta.setUsuario(authService.getUsuarioLogado());
        }
        
        // 1. Sincronização de metadados da Tabela de Juros
        sincronizarDadosTabela(proposta);

        // 2. Persistência da Proposta
        PropostaModel propostaSalva = propostaRepository.save(proposta);

        // 3. Gatilho de Ciclo Financeiro (Liquidação)
        if (propostaSalva.getStatus() == StatusPropostaModel.PAGO) {
            processarCicloPagamento(propostaSalva);
        }

        // 🚀 DISPARO DOS EVENTOS
        if (isNovo) {
            eventPublisher.publishEvent(new PropostaCriadaEvent(propostaSalva.getId()));
        } else {
            eventPublisher.publishEvent(new PropostaAtualizadaEvent(propostaSalva.getId()));
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

    // Adicione dentro de PropostaService.java
    public PropostaModel buscarPorId(Long id) {
        return propostaRepository.findById(id).orElse(null);
    }

    @Transactional
    public void excluirProposta(Long id) {
        propostaRepository.deleteById(id);
        
        // 🚀 DISPARO DO EVENTO DE EXCLUSÃO
        eventPublisher.publishEvent(new PropostaExcluidaEvent(id));
    }

    // PropostaService.java
    @Transactional(readOnly = true)
    public PropostaModel carregarPropostaDetalhada(Long id) {
        return propostaRepository.findByIdWithDetails(id).orElse(null);
    }
}