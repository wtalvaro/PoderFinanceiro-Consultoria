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

    // NOVO: Adicionado apenas o repositório de documentos
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
     * A "Calculadora de Dosagem": Descobre quanto a Solange vai ganhar.
     */
    public BigDecimal calcularComissaoEstimada(BigDecimal valorAprovado, Long tabelaId) {
        if (valorAprovado == null || valorAprovado.compareTo(BigDecimal.ZERO) <= 0 || tabelaId == null) {
            return BigDecimal.ZERO;
        }

        TabelaJurosModel tabela = tabelaJurosRepository.findById(tabelaId).orElse(null);
        if (tabela == null || tabela.getComissaoPercentual() == null) {
            return BigDecimal.ZERO;
        }

        // Fórmula: Valor Aprovado * (Percentual da Tabela / 100)
        BigDecimal percentual = tabela.getComissaoPercentual().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return valorAprovado.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * O "Centro Cirúrgico" ORIGINAL (Contrato mantido intacto).
     */
    @Transactional
    public PropostaModel salvarProposta(PropostaModel proposta) {

        // 1. Garante que a comissão estimada está atualizada com a tabela escolhida
        if (proposta.getTabelaId() != null) {
            BigDecimal comissaoCalculada = calcularComissaoEstimada(
                    proposta.getValorAprovado() != null ? proposta.getValorAprovado() : proposta.getValorSolicitado(),
                    proposta.getTabelaId().longValue());
            proposta.setComissaoEstimada(comissaoCalculada);
        }

        // 2. Salva o "Prontuário" da Proposta
        PropostaModel propostaSalva = propostaRepository.save(proposta);

        // 3. SE A PROPOSTA TEVE ALTA (PAGO), GERAMOS O REPASSE NA TABELA DE COMISSÕES
        if (propostaSalva.getStatus() == StatusPropostaModel.PAGO) {
            gerarOuAtualizarComissao(propostaSalva);
        }

        return propostaSalva;
    }

    /**
     * NOVO "Centro Cirúrgico" EXPANDIDO (Sobrecarga de Método).
     * Salva a proposta usando a lógica original e, em seguida, vincula os
     * documentos.
     */
    @Transactional
    public PropostaModel salvarProposta(PropostaModel proposta, List<DocumentoProponenteModel> documentosParaVincular) {
        // 1. Reaproveita 100% da lógica original (não quebra regras de comissão)
        PropostaModel propostaSalva = this.salvarProposta(proposta);

        // 2. Faz a sutura dos documentos com esta proposta específica
        if (documentosParaVincular != null && !documentosParaVincular.isEmpty()) {
            for (DocumentoProponenteModel doc : documentosParaVincular) {
                doc.setProposta(propostaSalva);
                documentoRepository.save(doc);
            }
        }

        return propostaSalva;
    }

    /**
     * Auxiliar: Cria a fatura de cobrança para o Banco na tela da Solange.
     */
    private void gerarOuAtualizarComissao(PropostaModel proposta) {
    // 1. Tenta localizar uma comissão já existente para esta proposta
    ComissaoModel comissao = comissaoRepository.findAll().stream()
            .filter(c -> c.getProposta().getId().equals(proposta.getId()))
            .findFirst()
            .orElse(new ComissaoModel()); // Se não existir, prepara uma nova

    if (proposta.getComissaoEstimada() != null && proposta.getComissaoEstimada().compareTo(BigDecimal.ZERO) > 0) {
        comissao.setProposta(proposta);
        comissao.setUsuario(proposta.getUsuario());
        
        // Mantém a regra 1:1 conforme solicitado
        comissao.setValorBrutoComissao(proposta.getComissaoEstimada());
        comissao.setValorLiquidoConsultor(proposta.getComissaoEstimada());

        // 🚀 A CURA: Se a proposta está PAGO, a comissão também recebe o status 'Pago'
        if (proposta.getStatus() == StatusPropostaModel.PAGO) {
            comissao.setStatusPagamento("Pago");
            proposta.setValorFinalCliente(proposta.getValorAprovado()); // Se proposta aceita valor será o mesmo do valor_aprovado
            comissao.setDataRecebimento(LocalDateTime.now().plusDays(1)); // Previsão de 1 dia para recebimento
            comissao.setDataRecebimento(LocalDateTime.now()); // Registra o recebimento hoje
        } else {
            // Caso volte o status por algum motivo
            comissao.setStatusPagamento("Pendente");
        }

        comissaoRepository.save(comissao);
    }
}
}