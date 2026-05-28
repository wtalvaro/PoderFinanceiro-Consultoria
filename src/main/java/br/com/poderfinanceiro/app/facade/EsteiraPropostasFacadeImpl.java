package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.PropostaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class EsteiraPropostasFacadeImpl implements IEsteiraPropostasFacade {

    private static final Logger log = LoggerFactory.getLogger(EsteiraPropostasFacadeImpl.class);
    private static final String LOG_PREFIX = "[EsteiraPropostasFacade]";

    private final PropostaRepository propostaRepository;
    private final AuthService authService;

    public EsteiraPropostasFacadeImpl(PropostaRepository propostaRepository, PropostaService propostaService, AuthService authService) {
        this.propostaRepository = propostaRepository;
        this.authService = authService;
        log.debug("{} [SISTEMA] Facade da Esteira de Propostas instanciada.", LOG_PREFIX);
    }

    @Override public List<PropostaModel> listarPropostasDoUsuario() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de propostas do usuário logado.", LOG_PREFIX);
        Long usuarioId = authService.getUsuarioLogado().getId();
        return propostaRepository.findByUsuarioId(usuarioId);
    }

    @Override public PropostaModel criarNovaPropostaEmBranco() {
        log.info("{} [TELEMETRIA] Iniciando criação de nova proposta em branco (apenas em memória).", LOG_PREFIX);
        PropostaModel nova = new PropostaModel();
        nova.setStatus(StatusPropostaModel.DIGITADA);
        nova.setUsuario(authService.getUsuarioLogado());

        // NÃO SALVAMOS NO BANCO AQUI. O salvamento ocorrerá no
        // PropostaController.
        log.trace("{} [NEGOCIO] Proposta em branco criada em memória.", LOG_PREFIX);
        return nova;
    }

    @Override public List<PropostaModel> filtrarPropostas(String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca: '{}'", LOG_PREFIX, termoBusca);
        List<PropostaModel> todas = listarPropostasDoUsuario();

        if (termoBusca == null || termoBusca.isBlank()) {
            return todas;
        }

        String termoLower = termoBusca.toLowerCase().trim();
        return todas.stream().filter(proposta -> atendeCriterioDeBusca(proposta, termoLower)).toList();
    }

    private boolean atendeCriterioDeBusca(PropostaModel p, String termoLower) {
        if (p.getProponente() != null && p.getProponente().getNomeCompleto() != null
                && p.getProponente().getNomeCompleto().toLowerCase().contains(termoLower))
            return true;

        if (p.getProponente() != null && p.getProponente().getCpf() != null && p.getProponente().getCpf().contains(termoLower))
            return true;

        if (p.getBanco() != null && p.getBanco().getNome() != null && p.getBanco().getNome().toLowerCase().contains(termoLower))
            return true;

        if (p.getStatus() != null && p.getStatus().getLabel().toLowerCase().contains(termoLower))
            return true;

        if (termoLower.startsWith(">") || termoLower.startsWith("<") || termoLower.contains("-")) {
            return atendeCriterioValor(p, termoLower);
        }

        return false;
    }

    private boolean atendeCriterioValor(PropostaModel p, String termo) {
        BigDecimal valor = p.getValorSolicitado() != null ? p.getValorSolicitado() : BigDecimal.ZERO;
        try {
            if (termo.startsWith(">")) {
                BigDecimal limite = new BigDecimal(termo.substring(1).trim());
                return valor.compareTo(limite) > 0;
            } else if (termo.startsWith("<")) {
                BigDecimal limite = new BigDecimal(termo.substring(1).trim());
                return valor.compareTo(limite) < 0;
            } else if (termo.contains("-")) {
                String[] partes = termo.split("-");
                if (partes.length == 2) {
                    BigDecimal min = new BigDecimal(partes[0].trim());
                    BigDecimal max = new BigDecimal(partes[1].trim());
                    return valor.compareTo(min) >= 0 && valor.compareTo(max) <= 0;
                }
            }
        } catch (Exception e) {
            log.warn("{} [NEGOCIO] Erro ao parsear filtro de valor: '{}'", LOG_PREFIX, termo);
        }
        return false;
    }
}
