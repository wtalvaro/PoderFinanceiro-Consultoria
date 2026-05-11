package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.Comissao;
import br.com.poderfinanceiro.app.model.Proposta;
import br.com.poderfinanceiro.app.model.TabelaJuros;
import br.com.poderfinanceiro.app.model.enums.StatusProposta;
import br.com.poderfinanceiro.app.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.repository.PropostaRepository;
import br.com.poderfinanceiro.app.repository.TabelaJurosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final TabelaJurosRepository tabelaJurosRepository;
    private final ComissaoRepository comissaoRepository;

    public PropostaService(PropostaRepository propostaRepository,
            TabelaJurosRepository tabelaJurosRepository,
            ComissaoRepository comissaoRepository) {
        this.propostaRepository = propostaRepository;
        this.tabelaJurosRepository = tabelaJurosRepository;
        this.comissaoRepository = comissaoRepository;
    }

    /**
     * A "Calculadora de Dosagem": Descobre quanto a Solange vai ganhar.
     */
    public BigDecimal calcularComissaoEstimada(BigDecimal valorAprovado, Long tabelaId) {
        if (valorAprovado == null || valorAprovado.compareTo(BigDecimal.ZERO) <= 0 || tabelaId == null) {
            return BigDecimal.ZERO;
        }

        TabelaJuros tabela = tabelaJurosRepository.findById(tabelaId).orElse(null);
        if (tabela == null || tabela.getComissaoPercentual() == null) {
            return BigDecimal.ZERO;
        }

        // Fórmula: Valor Aprovado * (Percentual da Tabela / 100)
        BigDecimal percentual = tabela.getComissaoPercentual().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return valorAprovado.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * O "Centro Cirúrgico": Salva a proposta e automatiza o fluxo de repasses.
     */
    @Transactional
    public Proposta salvarProposta(Proposta proposta) {

        // 1. Garante que a comissão estimada está atualizada com a tabela escolhida
        if (proposta.getTabelaId() != null) {
            BigDecimal comissaoCalculada = calcularComissaoEstimada(
                    proposta.getValorAprovado() != null ? proposta.getValorAprovado() : proposta.getValorSolicitado(),
                    proposta.getTabelaId().longValue());
            proposta.setComissaoEstimada(comissaoCalculada);
        }

        // 2. Salva o "Prontuário" da Proposta
        Proposta propostaSalva = propostaRepository.save(proposta);

        // 3. SE A PROPOSTA TEVE ALTA (PAGO), GERAMOS O REPASSE NA TABELA DE COMISSÕES
        if (propostaSalva.getStatus() == StatusProposta.PAGO) {
            gerarOuAtualizarComissao(propostaSalva);
        }

        return propostaSalva;
    }

    /**
     * Auxiliar: Cria a fatura de cobrança para o Banco na tela da Solange.
     */
    private void gerarOuAtualizarComissao(Proposta proposta) {
        // Verifica se já existe uma comissão para não duplicar (caso a Solange edite
        // uma proposta já paga)
        boolean jaExiste = comissaoRepository.findAll().stream()
                .anyMatch(c -> c.getProposta().getId().equals(proposta.getId()));

        if (!jaExiste && proposta.getComissaoEstimada().compareTo(BigDecimal.ZERO) > 0) {
            Comissao novaComissao = new Comissao();
            novaComissao.setProposta(proposta);
            novaComissao.setUsuario(proposta.getUsuario()); // A comissão vai pro dono da proposta
            novaComissao.setValorBrutoComissao(proposta.getComissaoEstimada());
            novaComissao.setValorLiquidoConsultor(proposta.getComissaoEstimada()); // Inicialmente, bruto = líquido
            novaComissao.setStatusPagamento("Pendente");

            // Estima o pagamento para daqui a 2 dias úteis (Exemplo de regra de negócio)
            novaComissao.setDataPrevisaoPagamento(LocalDate.now().plusDays(2));

            comissaoRepository.save(novaComissao);
        }
    }
}