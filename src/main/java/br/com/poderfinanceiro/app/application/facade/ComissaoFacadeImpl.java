package br.com.poderfinanceiro.app.application.facade;

import br.com.poderfinanceiro.app.common.util.CicloFinanceiroUtils;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ComissaoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementação da Facade de Comissões. Atua como orquestrador entre a
 * interface de usuário e os serviços de domínio financeiro.
 */
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
        log.info("{} [SISTEMA] Facade de Comissões inicializada com sucesso.", LOG_PREFIX);
    }

    @Override public List<ComissaoModel> listarComissoes() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de comissões ao serviço de domínio.", LOG_PREFIX);
        return comissaoService.listarTodasComDetalhes();
    }

    /**
     * Busca uma comissão por ID, tratando o Optional retornado pelo serviço.
     * RESOLUÇÃO DO ERRO: Type mismatch: cannot convert from
     * Optional<ComissaoModel> to ComissaoModel
     */
    @Override public ComissaoModel buscarComissaoPorId(Long id) {
        log.trace("{} [TELEMETRIA] Buscando comissão por ID: {}", LOG_PREFIX, id);
        return comissaoService.buscarPorId(id).orElseGet(() -> {
            log.warn("{} [NEGOCIO] Comissão ID {} não localizada no banco de dados.", LOG_PREFIX, id);
            return null;
        });
    }

    @Override @Transactional public ComissaoModel salvarConciliacao(ComissaoModel comissao) {
        log.info("{} [TELEMETRIA] Iniciando orquestração de conciliação para ID: {}", LOG_PREFIX, comissao.getId());
        try {
            ComissaoModel salva = comissaoService.salvarConciliacao(comissao);
            log.info("{} [AUDITORIA] Conciliação processada com sucesso. ID: {}", LOG_PREFIX, salva.getId());
            return salva;
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao salvar conciliação: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    @Override public void atualizarContextoComissoes(List<ComissaoModel> comissoes) {
        log.trace("{} [SISTEMA] Sincronizando lista de comissões com o contexto global.", LOG_PREFIX);
        if (contextoService != null) {
            contextoService.setComissoesAtivas(comissoes);
        }
    }

    @Override public BigDecimal calcularTotalPendente(List<ComissaoModel> comissoes) {
        log.trace("{} [NEGOCIO] Calculando somatório de comissões pendentes.", LOG_PREFIX);
        if (comissoes == null)
            return BigDecimal.ZERO;

        return comissoes.stream().filter(c -> !STATUS_PAGO.equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorBrutoComissao() != null ? c.getValorBrutoComissao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override public BigDecimal calcularTotalRecebido(List<ComissaoModel> comissoes) {
        log.trace("{} [NEGOCIO] Calculando somatório de comissões recebidas.", LOG_PREFIX);
        if (comissoes == null)
            return BigDecimal.ZERO;

        return comissoes.stream().filter(c -> STATUS_PAGO.equalsIgnoreCase(c.getStatusPagamento()))
                .map(c -> c.getValorPagoPelaPoder() != null ? c.getValorPagoPelaPoder() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override public String resolverNomeCiclo(ComissaoModel comissao) {
        if (comissao == null)
            return "N/A";

        String ciclo = comissao.getCicloReferencia();
        if (ciclo != null && !ciclo.isBlank()) {
            return ciclo;
        }

        if (comissao.getDataRecebimentoBanco() != null) {
            log.debug("{} [NEGOCIO] Ciclo ausente, derivando da data de recebimento para ID: {}", LOG_PREFIX,
                    comissao.getId());
            return CicloFinanceiroUtils.identificarCiclo(comissao.getDataRecebimentoBanco());
        }

        log.warn("{} [NEGOCIO] Comissão ID {} sem metadados de ciclo. Classificada como 'Legado'.", LOG_PREFIX,
                comissao.getId());
        return "Legado";
    }

    @Override public boolean isTravaQuintaFeiraAtiva() {
        LocalDateTime agora = LocalDateTime.now();
        boolean isQuinta = agora.getDayOfWeek() == DayOfWeek.THURSDAY;

        // Regra de Negócio: Após as 15h de quinta-feira, o ciclo financeiro é
        // travado para fechamento
        boolean travaAtiva = isQuinta && agora.getHour() >= 15;

        if (travaAtiva) {
            log.debug("{} [NEGOCIO] Trava de fechamento financeiro ativa (Quinta-feira pós 15h).", LOG_PREFIX);
        }

        return travaAtiva;
    }
}
