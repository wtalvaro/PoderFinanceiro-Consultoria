package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.util.FinanceiroUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TabelaJurosFacadeImpl implements ITabelaJurosFacade {

    private static final Logger log = LoggerFactory.getLogger(TabelaJurosFacadeImpl.class);
    private static final String LOG_PREFIX = "[TabelaJurosFacade]";

    private final TabelaJurosService tabelaJurosService;
    private final BancoRepository bancoRepository;

    public TabelaJurosFacadeImpl(TabelaJurosService tabelaJurosService, BancoRepository bancoRepository) {
        this.tabelaJurosService = tabelaJurosService;
        this.bancoRepository = bancoRepository;
        log.debug("{} [SISTEMA] Facade de Tabelas de Juros instanciada.", LOG_PREFIX);
    }

    @Override public List<TabelaJurosModel> listarTabelasAtivas() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de tabelas ativas.", LOG_PREFIX);
        return tabelaJurosService.listarAtivas();
    }

    @Override public List<BancoModel> listarBancosAtivos() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de bancos ativos.", LOG_PREFIX);
        return bancoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Override @Transactional public TabelaJurosModel salvarTabela(TabelaJurosModel tabela) {
        log.info("{} [TELEMETRIA] Iniciando salvamento da tabela. ID: {}", LOG_PREFIX, tabela.getId() != null ? tabela.getId() : "NOVA");

        if (tabela.getNomeTabela() == null || tabela.getNomeTabela().trim().isEmpty()) {
            log.warn("{} [NEGOCIO] Tentativa de salvar tabela sem nome bloqueada.", LOG_PREFIX);
            throw new IllegalArgumentException("O Nome da tabela é obrigatório!");
        }

        TabelaJurosModel salva = tabelaJurosService.salvarComRegraDeOuro(tabela);
        log.info("{} [AUDITORIA] Tabela salva com sucesso. ID: {}", LOG_PREFIX, salva.getId());
        return salva;
    }

    @Override @Transactional public void arquivarTabela(TabelaJurosModel tabela) {
        log.warn("{} [AUDITORIA] Solicitando arquivamento da tabela ID: {}", LOG_PREFIX, tabela != null ? tabela.getId() : "null");
        tabelaJurosService.arquivarTabela(tabela);
        log.info("{} [AUDITORIA] Tabela arquivada com sucesso.", LOG_PREFIX);
    }

    @Override public List<TabelaJurosModel> filtrarTabelas(String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca: '{}'", LOG_PREFIX, termoBusca);
        List<TabelaJurosModel> todas = listarTabelasAtivas();

        if (termoBusca == null || termoBusca.isBlank()) {
            return todas;
        }

        String termoLower = termoBusca.toLowerCase().trim();
        return todas.stream().filter(tabela -> gerarStringDeBusca(tabela).contains(termoLower)).toList();
    }

    private String gerarStringDeBusca(TabelaJurosModel t) {
        return ((t.getBanco() != null ? t.getBanco().getNome() : "") + " " + (t.getNomeTabela() != null ? t.getNomeTabela() : "") + " "
                + (t.getTipoConvenio() != null ? t.getTipoConvenio().name() : "") + " "
                + FinanceiroUtils.formatarParaExibicao(t.getTaxaMensal()) + " "
                + FinanceiroUtils.formatarParaExibicao(t.getComissaoPercentual()) + " " + t.getValorMinimoEmprestimo() + " "
                + t.getValorMaximoEmprestimo() + " " + t.getIdadeMinima() + " " + t.getIdadeMaxima() + " " + t.getPrazoMinimo() + " "
                + t.getPrazoMaximo()).toLowerCase();
    }
}
