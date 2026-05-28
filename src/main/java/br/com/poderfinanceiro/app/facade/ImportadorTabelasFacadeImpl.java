package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import br.com.poderfinanceiro.app.domain.service.GeminiService;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.infrastructure.repository.BancoRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Service
public class ImportadorTabelasFacadeImpl implements IImportadorTabelasFacade {

    private static final Logger log = LoggerFactory.getLogger(ImportadorTabelasFacadeImpl.class);
    private static final String LOG_PREFIX = "[ImportadorTabelasFacade]";

    private final GeminiService geminiService;
    private final AuthService authService;
    private final TabelaJurosService tabelaJurosService;
    private final BancoRepository bancoRepository;

    public ImportadorTabelasFacadeImpl(GeminiService geminiService, AuthService authService, TabelaJurosService tabelaJurosService,
            BancoRepository bancoRepository) {
        this.geminiService = geminiService;
        this.authService = authService;
        this.tabelaJurosService = tabelaJurosService;
        this.bancoRepository = bancoRepository;
        log.debug("{} [SISTEMA] Facade do Importador de Tabelas instanciada.", LOG_PREFIX);
    }

    @Override public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Solicitando modelos de IA disponíveis.", LOG_PREFIX);
        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            log.warn("{} [NEGOCIO] Token ausente. Retornando lista vazia.", LOG_PREFIX);
            return List.of();
        }
        return geminiService.listarModelosMultimodais(token);
    }

    @Override public List<BancoModel> listarBancosAtivos() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de bancos ativos.", LOG_PREFIX);
        return bancoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Override public List<TabelaImportadaDTO> extrairTabelasDeImagem(File arquivo, String modeloEscolhido) throws Exception {
        log.info("{} [TELEMETRIA] Iniciando extração OCR via IA. Arquivo: {}, Modelo: {}", LOG_PREFIX, arquivo.getName(), modeloEscolhido);

        String token = authService.estaLogado() ? authService.getUsuarioLogado().getGeminiApiKey() : null;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("API Key do Gemini não configurada.");
        }

        String jsonResposta = geminiService.extrairTabelasEmLote(arquivo, token, modeloEscolhido);

        log.trace("{} [SISTEMA] Realizando parse do JSON retornado pela IA.", LOG_PREFIX);
        ObjectMapper mapper = new ObjectMapper();
        List<TabelaImportadaDTO> tabelasExtraidas = mapper.readValue(jsonResposta, new TypeReference<>() {
        });

        log.info("{} [AUDITORIA] Extração concluída. {} tabelas identificadas.", LOG_PREFIX, tabelasExtraidas.size());
        return tabelasExtraidas;
    }

    @Override @Transactional public void salvarLoteTabelas(List<TabelaImportadaDTO> lote) {
        log.info("{} [TELEMETRIA] Iniciando salvamento em lote. Quantidade: {}", LOG_PREFIX, lote.size());
        tabelaJurosService.salvarLoteTabelasImportadas(lote);
        log.info("{} [AUDITORIA] Lote de tabelas salvo com sucesso.", LOG_PREFIX);
    }
}
