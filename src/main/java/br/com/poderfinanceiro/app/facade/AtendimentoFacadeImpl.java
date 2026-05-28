package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import br.com.poderfinanceiro.app.util.SummaryGeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AtendimentoFacadeImpl implements IAtendimentoFacade {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoFacadeImpl.class);
    private static final String LOG_PREFIX = "[AtendimentoFacade]";
    private static final String URL_WHATSAPP_BASE = "https://wa.me/";
    private static final String PREFIXO_BRASIL_WHATSAPP = "55";

    private final ProponenteService proponenteService;
    private final AtendimentoContextService contextoService;

    public AtendimentoFacadeImpl(ProponenteService proponenteService, AtendimentoContextService contextoService) {
        this.proponenteService = proponenteService;
        this.contextoService = contextoService;
        log.debug("{} [SISTEMA] Facade de Atendimento instanciada.", LOG_PREFIX);
    }

    @Override @Transactional public ProponenteModel salvarAtendimentoCompleto(ProponenteModel lead, EnderecoProponenteModel endereco) {
        log.info("{} [TELEMETRIA] Iniciando orquestração de salvamento do atendimento. Lead ID: {}", LOG_PREFIX,
                lead.getId() != null ? lead.getId() : "NOVO");

        if (endereco != null) {
            endereco.setProponente(lead);
            lead.setEnderecos(new ArrayList<>(List.of(endereco)));
            log.trace("{} [NEGOCIO] Endereço vinculado ao lead antes da persistência.", LOG_PREFIX);
        }

        ProponenteModel salvo = proponenteService.salvarProponente(lead);
        log.info("{} [AUDITORIA] Atendimento salvo com sucesso. ID: {}", LOG_PREFIX, salvo.getId());
        return salvo;
    }

    @Override public String gerarResumoParaCopia(ProponenteModel lead, String rendaFormatada) {
        log.trace("{} [TELEMETRIA] Gerando resumo do lead para área de transferência.", LOG_PREFIX);
        // Como o SummaryGeneratorUtils espera um ViewModel, vamos adaptar para
        // receber o Model. Se o seu SummaryGeneratorUtils só aceita ViewModel,
        // você pode mantera chamada no Controller, mas o ideal é que a Facade faça essa ponte.
        return SummaryGeneratorUtils.gerarJsonContextualParaIA(lead, true); 
    }

    @Override public String formatarLinkWhatsApp(String telefone) {
        if (telefone == null || telefone.trim().isEmpty())
            return null;

        String numeroLimpo = telefone.replaceAll("[^0-9]", "");
        boolean jaTemPrefixo = numeroLimpo.startsWith(PREFIXO_BRASIL_WHATSAPP);
        String linkFinal = jaTemPrefixo ? numeroLimpo : PREFIXO_BRASIL_WHATSAPP + numeroLimpo;

        log.trace("{} [NEGOCIO] Link do WhatsApp formatado com sucesso.", LOG_PREFIX);
        return URL_WHATSAPP_BASE + linkFinal;
    }

    @Override public void limparContextoAtendimento() {
        log.trace("{} [SISTEMA] Limpando contexto global de atendimento.", LOG_PREFIX);
        contextoService.limparContexto();
    }

    @Override public void definirLeadAtivo(ProponenteModel lead) {
        log.trace("{} [SISTEMA] Definindo lead ativo no contexto global. ID: {}", LOG_PREFIX, lead != null ? lead.getId() : "NOVO");
        contextoService.setLeadAtivo(lead);
    }
}
