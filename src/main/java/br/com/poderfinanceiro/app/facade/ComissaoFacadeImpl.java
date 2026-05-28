package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ComissaoService;
import br.com.poderfinanceiro.app.util.CicloFinanceiroUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComissaoFacadeImpl implements IComissaoFacade {

    private static final Logger log = LoggerFactory.getLogger(ComissaoFacadeImpl.class);
    private static final String LOG_PREFIX = "[ComissaoFacade]";
    private static final String STATUS_PAGO = "Pago";

    private final ComissaoService comissaoService;
    private final AtendimentoContextService contextoService;

    public ComissaoFacadeImpl(ComissaoService comissaoService, AtendimentoContextService contextoService) {
        this.comissaoService = comissaoService;
        this.contextoService = contextoService;
        log.debug("{} [SISTEMA] Facade de Comissões instanciada.", LOG_PREFIX);
    }

    @Override public List<ComissaoModel> listarComissoes() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de comissões com detalhes.", LOG_PREFIX);
        return comissaoService.listarTodasComDetalhes();
    }

    @Override public ComissaoModel buscarComissaoPorId(Long id) {
        log.trace("{} [TELEMETRIA] Buscando comissão por ID: {}", LOG_PREFIX, id);
        return comissaoService.buscarPorId(id);
    }

    @Override @Transactional public ComissaoModel salvarConciliacao(ComissaoModel comissao) {
        log.info("{} [TELEMETRIA] Iniciando conciliação da comissão ID: {}", LOG_PREFIX, comissao.getId());
        ComissaoModel salva = comissaoService.salvarConciliacao(comissao);
        log.info("{} [AUDITORIA] Conciliação salva com sucesso. ID: {}", LOG_PREFIX, salva.getId());
        return salva;
    }

    @Override public void atualizarContextoComissoes(List<ComissaoModel> comissoes) {
        log.trace("{} [SISTEMA] Atualizando contexto global de comissões.", LOG_PREFIX);
        if (contextoService != null) {
            contextoService.setComissoesAtivas(comissoes);
        }
    }

    @Override public BigDecimal calcularTotalPendente(List<ComissaoModel> comissoes) {
        log.trace("{} [NEGOCIO] Calculando total financeiro pendente.", LOG_PREFIX);
        return comissoes.stream().filter(c -> !STATUS_PAGO.equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorBrutoComissao() != null ? c.getValorBrutoComissao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override public BigDecimal calcularTotalRecebido(List<ComissaoModel> comissoes) {
        log.trace("{} [NEGOCIO] Calculando total financeiro recebido.", LOG_PREFIX);
        return comissoes.stream().filter(c -> STATUS_PAGO.equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorPagoPelaPoder() != null ? c.getValorPagoPelaPoder() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override public String resolverNomeCiclo(ComissaoModel comissao) {
        String ciclo = comissao.getCicloReferencia();
        if (ciclo != null) {
            return ciclo;
        }
        if (comissao.getDataRecebimentoBanco() != null) {
            return CicloFinanceiroUtils.identificarCiclo(comissao.getDataRecebimentoBanco());
        }
        log.warn("{} [NEGOCIO] Comissão ID={} sem ciclo de referência e sem data de recebimento. Classificada como 'Legado'.", LOG_PREFIX,
                comissao.getId());
        return "Legado";
    }

    @Override public boolean isTravaQuintaFeiraAtiva() {
        LocalDateTime agora = LocalDateTime.now();
        boolean isQuinta = agora.getDayOfWeek() == DayOfWeek.THURSDAY;
        boolean prazoUltrapassado = isQuinta && agora.getHour() >= 15;
        log.trace("{} [NEGOCIO] Verificação de trava de quinta-feira: {}", LOG_PREFIX, prazoUltrapassado);
        return prazoUltrapassado;
    }
}
