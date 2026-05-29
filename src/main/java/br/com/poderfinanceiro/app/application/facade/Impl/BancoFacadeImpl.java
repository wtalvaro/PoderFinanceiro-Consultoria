package br.com.poderfinanceiro.app.application.facade.Impl;

import br.com.poderfinanceiro.app.application.facade.IBancoFacade;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.service.BancoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BancoFacadeImpl implements IBancoFacade {

    private static final Logger log = LoggerFactory.getLogger(BancoFacadeImpl.class);
    private static final String LOG_PREFIX = "[BancoFacade]";
    private static final String URL_WHATSAPP_BASE = "https://wa.me/";

    private final BancoService bancoService;

    public BancoFacadeImpl(BancoService bancoService) {
        this.bancoService = bancoService;
        log.debug("{} [SISTEMA] Facade de Bancos instanciada.", LOG_PREFIX);
    }

    @Override public List<BancoModel> listarTodos() {
        log.trace("{} [TELEMETRIA] Solicitando listagem completa de bancos.", LOG_PREFIX);
        return bancoService.listarTodos();
    }

    @Override @Transactional public BancoModel salvarBanco(BancoModel banco) {
        log.info("{} [TELEMETRIA] Iniciando salvamento do banco. ID: {}", LOG_PREFIX, banco.getId() != null ? banco.getId() : "NOVO");

        if (banco.getNome() == null || banco.getNome().trim().isEmpty()) {
            log.warn("{} [NEGOCIO] Tentativa de salvar banco sem nome bloqueada.", LOG_PREFIX);
            throw new IllegalArgumentException("O Nome do banco é obrigatório!");
        }

        BancoModel salvo = bancoService.salvar(banco);
        log.info("{} [AUDITORIA] Banco salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
        return salvo;
    }

    @Override @Transactional public void excluirBanco(Long id) {
        log.warn("{} [AUDITORIA] Solicitando exclusão do banco ID: {}", LOG_PREFIX, id);
        bancoService.excluir(id);
        log.info("{} [AUDITORIA] Banco ID: {} excluído com sucesso.", LOG_PREFIX, id);
    }

    @Override public List<BancoModel> filtrarBancos(String termoBusca) {
        log.trace("{} [NEGOCIO] Aplicando filtro de busca: '{}'", LOG_PREFIX, termoBusca);
        List<BancoModel> todos = listarTodos();

        if (termoBusca == null || termoBusca.isBlank()) {
            return todos;
        }

        String termoLower = termoBusca.toLowerCase();
        return todos.stream().filter(banco -> {
            boolean matchNome = banco.getNome().toLowerCase().contains(termoLower);
            boolean matchCodigo = banco.getCodigo() != null && banco.getCodigo().contains(termoBusca);
            return matchNome || matchCodigo;
        }).toList();
    }

    @Override public boolean isExclusaoBloqueada(BancoModel banco) {
        boolean bloqueada = banco != null && banco.getTabelas() != null && !banco.getTabelas().isEmpty();
        log.trace("{} [NEGOCIO] Verificação de bloqueio de exclusão para banco ID {}: {}", LOG_PREFIX,
                banco != null ? banco.getId() : "null", bloqueada);
        return bloqueada;
    }

    @Override public String formatarUrlPortal(String urlOriginal) {
        if (urlOriginal == null || urlOriginal.trim().isEmpty())
            return null;

        String urlFormatada = urlOriginal;
        if (!urlFormatada.startsWith("http://") && !urlFormatada.startsWith("https://")) {
            urlFormatada = "https://" + urlFormatada;
        }
        log.trace("{} [NEGOCIO] URL do portal formatada.", LOG_PREFIX);
        return urlFormatada;
    }

    @Override public String formatarLinkWhatsApp(String telefone) {
        if (telefone == null || telefone.trim().isEmpty())
            return null;

        String numeroLimpo = telefone.replaceAll("[^0-9]", "");
        if (numeroLimpo.length() <= 11) {
            numeroLimpo = "55" + numeroLimpo;
        }

        log.trace("{} [NEGOCIO] Link do WhatsApp formatado.", LOG_PREFIX);
        return URL_WHATSAPP_BASE + numeroLimpo;
    }
}
