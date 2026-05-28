package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.infrastructure.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.infrastructure.repository.PropostaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardFacadeImpl implements IDashboardFacade {

    private static final Logger log = LoggerFactory.getLogger(DashboardFacadeImpl.class);
    private static final String LOG_PREFIX = "[DashboardFacade]";

    private static final String STATUS_PAGO_COMISSAO = "Pago";
    private static final String STATUS_LIQUIDADO_COMISSAO = "Liquidado";

    private final PropostaRepository propostaRepository;
    private final ComissaoRepository comissaoRepository;
    private final AuthService authService;

    public DashboardFacadeImpl(PropostaRepository propostaRepository, ComissaoRepository comissaoRepository, AuthService authService) {
        this.propostaRepository = propostaRepository;
        this.comissaoRepository = comissaoRepository;
        this.authService = authService;
        log.debug("{} [SISTEMA] Facade do Dashboard instanciada.", LOG_PREFIX);
    }

    @Override public String obterNomeConsultorLogado() {
        return authService.estaLogado() ? authService.getUsuarioLogado().getNome() : "Consultor Offline";
    }

    @Override public MetricasDashboardDTO calcularMetricasGerais() {
        log.trace("{} [TELEMETRIA] Iniciando cálculo de métricas do Dashboard.", LOG_PREFIX);

        List<PropostaModel> propostas = propostaRepository.findAllComDetalhes();
        List<ComissaoModel> comissoes = comissaoRepository.findAll();

        long aguardando = propostas.stream().filter(this::isPropostaAguardando).count();
        BigDecimal volumeAprovado = somarVolumeAprovado(propostas);
        BigDecimal comissaoPendente = somarComissoes(comissoes, false);
        BigDecimal comissaoPaga = somarComissoes(comissoes, true);

        log.info("{} [NEGOCIO] Métricas calculadas: Aguardando={}, Volume={}, Pendente={}, Paga={}", LOG_PREFIX, aguardando, volumeAprovado,
                comissaoPendente, comissaoPaga);

        return new MetricasDashboardDTO(propostas, aguardando, volumeAprovado, comissaoPendente, comissaoPaga);
    }

    @Override public List<PropostaModel> filtrarPropostas(List<PropostaModel> propostas, String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca no Dashboard: '{}'", LOG_PREFIX, termoBusca);
        if (termoBusca == null || termoBusca.isBlank()) {
            return propostas;
        }

        String filtro = termoBusca.toLowerCase().trim();
        return propostas.stream().filter(p -> atendeFiltroDeBusca(p, filtro)).toList();
    }

    // --- MÉTODOS PRIVADOS DE REGRA DE NEGÓCIO ---

    private boolean isPropostaAguardando(PropostaModel p) {
        return p.getStatus() == StatusPropostaModel.DIGITADA || p.getStatus() == StatusPropostaModel.PENDENTE;
    }

    private BigDecimal somarVolumeAprovado(List<PropostaModel> propostas) {
        return propostas.stream().filter(p -> p.getStatus() == StatusPropostaModel.PAGO)
                .map(p -> p.getValorFinalCliente() != null ? p.getValorFinalCliente() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarComissoes(List<ComissaoModel> comissoes, boolean isPaga) {
        return comissoes.stream().filter(c -> isComissaoPaga(c) == isPaga).map(c -> {
            BigDecimal valor = isPaga ? c.getValorPagoPelaPoder() : c.getValorBrutoComissao();
            return valor != null ? valor : BigDecimal.ZERO;
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isComissaoPaga(ComissaoModel c) {
        String status = c.getStatusPagamento();
        return STATUS_PAGO_COMISSAO.equalsIgnoreCase(status) || STATUS_LIQUIDADO_COMISSAO.equalsIgnoreCase(status);
    }

    private boolean atendeFiltroDeBusca(PropostaModel proposta, String filtro) {
        String nome = proposta.getProponente().getNomeCompleto().toLowerCase();
        String cpf = proposta.getProponente().getCpf().replaceAll("[^0-9]", "");
        String banco = proposta.getBanco().getNome().toLowerCase();
        return nome.contains(filtro) || cpf.contains(filtro) || banco.contains(filtro);
    }
}
